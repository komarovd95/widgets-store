package com.github.komarovd95.widgetstore.application.storage.inmemory;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.storage.StoreWidgetParameters;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import com.github.komarovd95.widgetstore.application.domain.WidgetsPagingCursor;
import com.github.komarovd95.widgetstore.application.storage.WidgetsStorage;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A widget storage that stores all data in the memory.
 * <p>
 * The implementation uses a read-write pessimistic locking approach: every operation (even read operations) supports
 * lock acquisition for the thread-safety and atomicity.
 * <p>
 * The maximum time for the lock acquisition is configurable.
 * <p>
 * This implementation assumes that there are many more reads than writes.
 */
public class InMemoryWidgetsStorage implements WidgetsStorage {

    private static final Logger log = LoggerFactory.getLogger(InMemoryWidgetsStorage.class);

    /**
     * The Z-index that used when there is no stored widgets.
     */
    private static final int INITIAL_Z_INDEX = 0;

    /**
     * The initial version.
     */
    private static final long INITIAL_VERSION = 1L;

    /**
     * A map for searching widgets by Z-index. Widgets are stored in this map in the sorted order (ascending).
     * <p>
     * All operations with this map MUST be guarded by {@link #lock}.
     */
    private final NavigableMap<WidgetSortingKey, Widget> widgetsByZIndex = new TreeMap<>();

    /**
     * A map for searching widgets by ID.
     * <p>
     * All operations with this map MUST be guarded by {@link #lock}.
     */
    private final Map<String, Widget> widgetsByIds = new HashMap<>();

    /**
     * A list of versions. Each version represents a modification of the particular Z-index range.
     * <p>
     * Versions are used to avoid misses while paging requests.
     * <p>
     * The maximum size of this list is controlled by {@link #maxVersionsToStore}.
     */
    private final List<Version> versions = new ArrayList<>();

    /**
     * A lock that guards all operations with the stored widgets.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * An ID generator.
     */
    private final WidgetIdGenerator idGenerator;

    /**
     * A clock that provides timestamps while creations or updates.
     */
    private final Clock clock;

    /**
     * A timeout for lock acquisition.
     */
    private final Duration lockAcquisitionTimeout;

    /**
     * The maximum number of elements stored in the versions list.
     */
    private final int maxVersionsToStore;

    public InMemoryWidgetsStorage(
        WidgetIdGenerator idGenerator,
        Clock clock,
        Duration lockAcquisitionTimeout,
        int maxVersionsToStore
    ) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lockAcquisitionTimeout = Objects.requireNonNull(lockAcquisitionTimeout, "lockAcquisitionTimeout");
        this.maxVersionsToStore = maxVersionsToStore;
    }

    /**
     * @inheritDocs
     */
    @Override
    public Widget createWidget(StoreWidgetParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        return executeWithLock(lock.writeLock(), () -> {
            String id = idGenerator.generate();
            WidgetSortingKey key = parameters.getZ()
                .map(WidgetSortingKey::new)
                .orElseGet(this::getNextSortingKeyForCreation);
            Widget widget = Widget.builder()
                .setId(id)
                .setX(parameters.getX())
                .setY(parameters.getY())
                .setZ(key.z)
                .setWidth(parameters.getWidth())
                .setHeight(parameters.getHeight())
                .setModifiedAt(clock.instant())
                .build();
            int toZIndex = shiftOverlyingWidgets(key, widget.getModifiedAt());
            widgetsByZIndex.put(key, widget);
            widgetsByIds.put(id, widget);
            insertNewVersion(key.z, toZIndex);
            log.info("Widget has been created successfully: widget={}", widget);
            return widget;
        });
    }

    private WidgetSortingKey getNextSortingKeyForCreation() {
        if (widgetsByZIndex.isEmpty()) {
            return new WidgetSortingKey(INITIAL_Z_INDEX);
        } else {
            WidgetSortingKey lastKey = widgetsByZIndex.lastKey();
            return new WidgetSortingKey(lastKey.z + 1);
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Widget> updateWidget(String id, StoreWidgetParameters parameters) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parameters, "parameters");
        return executeWithLock(lock.writeLock(), () -> {
            Widget widget = widgetsByIds.get(id);
            if (widget == null) {
                log.warn("Widget was not found by given ID: id={}", id);
                return Optional.empty();
            }
            WidgetSortingKey key = parameters.getZ()
                .map(WidgetSortingKey::new)
                .orElseGet(() -> {
                    WidgetSortingKey lastKey = widgetsByZIndex.lastKey();
                    if (lastKey.z == widget.getZ()) {
                        return lastKey; // it's already on the foreground
                    } else {
                        return new WidgetSortingKey(lastKey.z + 1);
                    }
                });
            Widget widgetToUpdate = Widget.builder(widget)
                .setX(parameters.getX())
                .setY(parameters.getY())
                .setZ(key.z)
                .setWidth(parameters.getWidth())
                .setHeight(parameters.getHeight())
                .setModifiedAt(clock.instant())
                .build();
            if (isUpdateNotNeeded(widget, widgetToUpdate)) {
                log.info("Update is not needed: widget={}", widget);
                return Optional.of(widget);
            }
            int toZIndex;
            if (widget.getZ() != widgetToUpdate.getZ()) {
                widgetsByZIndex.remove(new WidgetSortingKey(widget.getZ()));
                toZIndex = shiftOverlyingWidgets(key, widgetToUpdate.getModifiedAt());
            } else {
                toZIndex = widgetToUpdate.getZ();
            }
            widgetsByZIndex.put(key, widgetToUpdate);
            widgetsByIds.put(id, widgetToUpdate);
            insertNewVersion(key.z, toZIndex);
            log.info("Widget has been updated successfully: widget={}", widgetToUpdate);
            return Optional.of(widgetToUpdate);
        });
    }

    private int shiftOverlyingWidgets(WidgetSortingKey key, Instant modificationTimestamp) {
        NavigableMap<WidgetSortingKey, Widget> tailMap = widgetsByZIndex.tailMap(key, true);
        int previousZ = key.z;
        for (Map.Entry<WidgetSortingKey, Widget> entry : tailMap.entrySet()) {
            WidgetSortingKey entryKey = entry.getKey();
            if (entryKey.z != previousZ) {
                break;
            }
            // it's a hacky solution to mutate keys of the map.
            // But we can reduce complexity of shifting the overlying widgets from O(n * log n) to O(n)
            // in the worst case
            int shiftedZIndex = ++entryKey.z;
            Widget shiftedWidget = Widget.builder(entry.getValue())
                .setZ(shiftedZIndex)
                .setModifiedAt(modificationTimestamp)
                .build();
            entry.setValue(shiftedWidget);
            widgetsByIds.replace(shiftedWidget.getId(), shiftedWidget);
            previousZ = shiftedZIndex;
            log.debug("Widget's Z-index has been shifted: widget={}", shiftedWidget);
        }
        return previousZ == key.z ? previousZ : previousZ - 1;
    }

    private void insertNewVersion(int fromZIndex, int toZIndex) {
        if (versions.isEmpty()) {
            versions.add(new Version(INITIAL_VERSION, fromZIndex, toZIndex));
        } else {
            Version currentVersion = getCurrentVersion();
            versions.add(new Version(currentVersion.version + 1L, fromZIndex, toZIndex));
            if (versions.size() > maxVersionsToStore) {
                versions.subList(0, versions.size() - maxVersionsToStore).clear();
            }
        }
    }

    private Version getCurrentVersion() {
        return versions.get(versions.size() - 1);
    }

    private boolean isUpdateNotNeeded(Widget existingWidget, Widget widgetToUpdate) {
        return existingWidget.getX() == widgetToUpdate.getX()
            && existingWidget.getY() == widgetToUpdate.getY()
            && existingWidget.getZ() == widgetToUpdate.getZ()
            && existingWidget.getWidth() == widgetToUpdate.getWidth()
            && existingWidget.getHeight() == widgetToUpdate.getHeight();
    }

    /**
     * @inheritDocs
     */
    @Override
    public void deleteWidget(String id) {
        Objects.requireNonNull(id, "id");
        executeWithLock(lock.writeLock(), () -> {
            Widget removedWidget = widgetsByIds.remove(id);
            if (removedWidget != null) {
                widgetsByZIndex.remove(new WidgetSortingKey(removedWidget.getZ()));
                log.info("Widget has been removed successfully: widget={}", removedWidget);
            } else {
                log.info("Widget was not found by given ID for removal: id={}", id);
            }
            return null;
        });
    }

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Widget> getWidgetById(String id) {
        Objects.requireNonNull(id, "id");
        return executeWithLock(lock.readLock(), () -> Optional.ofNullable(widgetsByIds.get(id)));
    }

    /**
     * @inheritDocs
     */
    @Override
    public PagedList<Widget> getWidgets(WidgetsFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return executeWithLock(lock.readLock(), () -> {
            List<Widget> items = filter.getCursor()
                    .map(this::getWidgetsFromCursor)
                    .orElseGet(() -> widgetsByZIndex.values().stream())
                    .limit(filter.getLimit() + 1)
                    .collect(Collectors.toList());
            if (items.size() > filter.getLimit()) {
                List<Widget> pageItems = items.subList(0, filter.getLimit());
                return PagedList.nonLastPage(
                    pageItems,
                    new WidgetsPagingCursor(
                        getCurrentVersion().version,
                        items.get(filter.getLimit()).getZ()
                    )
                );
            } else {
                return PagedList.lastPage(items);
            }
        });
    }

    private Stream<Widget> getWidgetsFromCursor(WidgetsPagingCursor cursor) {
        Version cursorVersion = new Version(cursor.getVersion(), cursor.getZIndex(), cursor.getZIndex());
        int index = Collections.binarySearch(versions, cursorVersion);
        List<Version> affectingVersions = versions.subList(index, versions.size())
            .stream()
            .filter(version -> version.fromZIndex < cursor.getZIndex())
            .sorted(Comparator.comparingInt(version -> version.fromZIndex))
            .collect(Collectors.toList());
        List<Version> versionRanges = affectingVersions.isEmpty()
            ? Collections.emptyList()
            : reduceZIndexRanges(affectingVersions);
        Stream<Widget> widgetStream = versionRanges
            .stream()
            .flatMap(version -> widgetsByZIndex
                .subMap(
                    new WidgetSortingKey(version.fromZIndex),
                    version.version != cursor.getVersion(),
                    new WidgetSortingKey(Integer.min(version.toZIndex, cursor.getZIndex())),
                    true
                )
                .values()
                .stream()
            );
        return Stream.concat(
            widgetStream,
            widgetsByZIndex.tailMap(new WidgetSortingKey(cursor.getZIndex()))
                .values()
                .stream()
        );
    }

    private List<Version> reduceZIndexRanges(List<Version> versions) {
        List<Version> result = new ArrayList<>();
        Version previous = versions.get(0);
        for (int i = 1; i < versions.size(); i++) {
            Version version = versions.get(i);
            if (previous.fromZIndex <= version.toZIndex && previous.toZIndex >= version.fromZIndex) {
                previous = new Version(
                    Long.max(previous.version, version.version),
                    Integer.min(previous.fromZIndex, version.fromZIndex),
                    Integer.max(previous.toZIndex, version.toZIndex)
                );
            } else {
                result.add(previous);
                previous = version;
            }
        }
        result.add(previous);
        return result;
    }

    private <T> T executeWithLock(Lock lock, Supplier<T> action) {
        try {
            if (!lock.tryLock(lockAcquisitionTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Failed to acquire lock");
            }
            try {
                return action.get();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class WidgetSortingKey implements Comparable<WidgetSortingKey> {

        private int z;

        private WidgetSortingKey(int zIndex) {
            this.z = zIndex;
        }

        @Override
        public int compareTo(WidgetSortingKey other) {
            return Integer.compare(z, other.z);
        }
    }

    private static class Version implements Comparable<Version> {

        private final long version;
        private final int fromZIndex;
        private final int toZIndex;

        private Version(long version, int fromZIndex, int toZIndex) {
            this.version = version;
            this.fromZIndex = fromZIndex;
            this.toZIndex = toZIndex;
        }

        @Override
        public int compareTo(Version other) {
            return Long.compare(version, other.version);
        }
    }
}
