package com.github.komarovd95.widgetstore.application.storage.inmemory;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.storage.StoreWidgetParameters;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import com.github.komarovd95.widgetstore.application.storage.WidgetsStorage;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.storage.inmemory.rtree.WidgetRTree;
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
     * A map for searching widgets by Z-index. Widgets are stored in this map in the sorted order (ascending).
     * <p>
     * All operations with this map MUST be guarded by {@link #lock}.
     */
    private final NavigableMap<WidgetSortingKey, WidgetWrapper> widgetsByZIndex = new TreeMap<>();

    /**
     * A map for searching widgets by ID.
     * <p>
     * All operations with this map MUST be guarded by {@link #lock}.
     */
    private final Map<String, WidgetWrapper> widgetsByIds = new HashMap<>();

    /**
     * An R-tree for spatial search.
     */
    private final WidgetRTree spatialIndex = new WidgetRTree();

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

    public InMemoryWidgetsStorage(
        WidgetIdGenerator idGenerator,
        Clock clock,
        Duration lockAcquisitionTimeout
    ) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lockAcquisitionTimeout = Objects.requireNonNull(lockAcquisitionTimeout, "lockAcquisitionTimeout");
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
                .setBoundaries(parameters.getBoundaries())
                .setZ(key.z)
                .setModifiedAt(clock.instant())
                .build();
            WidgetWrapper wrapper = new WidgetWrapper(widget);
            shiftOverlyingWidgets(key, widget.getModifiedAt());
            widgetsByZIndex.put(key, wrapper);
            widgetsByIds.put(id, wrapper);
            spatialIndex.add(id, wrapper.boundaries);
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
            WidgetWrapper widget = widgetsByIds.get(id);
            if (widget == null) {
                log.warn("Widget was not found by given ID: id={}", id);
                return Optional.empty();
            }
            WidgetSortingKey key = parameters.getZ()
                .map(WidgetSortingKey::new)
                .orElseGet(() -> {
                    WidgetSortingKey lastKey = widgetsByZIndex.lastKey();
                    if (lastKey.z == widget.z) {
                        return lastKey; // it's already on the foreground
                    } else {
                        return new WidgetSortingKey(lastKey.z + 1);
                    }
                });
            if (isUpdateNotNeeded(widget, parameters.getBoundaries(), key.z)) {
                log.info("Update is not needed: widget={}", widget);
                return Optional.of(toWidget(widget));
            }
            widget.modifiedAt = clock.instant();
            if (widget.z != key.z) {
                widgetsByZIndex.remove(new WidgetSortingKey(widget.z));
                shiftOverlyingWidgets(key, widget.modifiedAt);
                widget.z = key.z;
                widgetsByZIndex.put(key, widget);
            }
            if (!Objects.equals(widget.boundaries, parameters.getBoundaries())) {
                spatialIndex.remove(id, widget.boundaries);
                widget.boundaries = parameters.getBoundaries();
                spatialIndex.add(id, widget.boundaries);
            }
            log.info("Widget has been updated successfully: widget={}", widget);
            return Optional.of(toWidget(widget));
        });
    }

    private void shiftOverlyingWidgets(WidgetSortingKey key, Instant modificationTimestamp) {
        NavigableMap<WidgetSortingKey, WidgetWrapper> tailMap = widgetsByZIndex.tailMap(key, true);
        int previousZ = key.z;
        for (Map.Entry<WidgetSortingKey, WidgetWrapper> entry : tailMap.entrySet()) {
            WidgetSortingKey entryKey = entry.getKey();
            WidgetWrapper widget = entry.getValue();
            if (entryKey.z != previousZ) {
                break;
            }
            // it's a hacky solution to mutate keys of the map.
            // But we can reduce complexity of shifting the overlying widgets from O(n * log n) to O(n)
            // in the worst case
            int shiftedZIndex = ++entryKey.z;
            widget.z = shiftedZIndex;
            widget.modifiedAt = modificationTimestamp;
            previousZ = shiftedZIndex;
            log.debug("Widget's Z-index has been shifted: id={}", widget.id);
        }
    }

    private boolean isUpdateNotNeeded(WidgetWrapper existingWidget, Region newBoundaries, int newZIndex) {
        return Objects.equals(existingWidget.boundaries, newBoundaries)
            && existingWidget.z == newZIndex;
    }

    /**
     * @inheritDocs
     */
    @Override
    public void deleteWidget(String id) {
        Objects.requireNonNull(id, "id");
        executeWithLock(lock.writeLock(), () -> {
            WidgetWrapper removedWidget = widgetsByIds.remove(id);
            if (removedWidget != null) {
                widgetsByZIndex.remove(new WidgetSortingKey(removedWidget.z));
                spatialIndex.remove(id, removedWidget.boundaries);
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
        return executeWithLock(lock.readLock(), () ->
            Optional.ofNullable(widgetsByIds.get(id))
                .map(InMemoryWidgetsStorage::toWidget)
        );
    }

    /**
     * @inheritDocs
     */
    @Override
    public PagedList<Widget> getWidgets(WidgetsFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return executeWithLock(lock.readLock(), () -> {
            Integer cursor = filter.getCursor().orElse(null);
            int limit = filter.getLimit() + 1;
            List<Widget> items = filter.getRegion()
                .map(region -> getWidgetsBySpatialIndex(region, cursor, limit))
                .orElseGet(() -> getWidgetsByZIndex(cursor, limit));
            if (items.size() > filter.getLimit()) {
                List<Widget> pageItems = items.subList(0, filter.getLimit());
                return PagedList.nonLastPage(pageItems, pageItems.get(pageItems.size() - 1).getZ());
            } else {
                return PagedList.lastPage(items);
            }
        });
    }

    private List<Widget> getWidgetsBySpatialIndex(Region region, Integer cursor, int limit) {
        // top-N sort
        List<WidgetWrapper> widgets = new ArrayList<>();
        spatialIndex.contains(region, widgetId -> {
            WidgetWrapper widget = widgetsByIds.get(widgetId);
            if (cursor == null || widget.z > cursor) {
                int index = -Collections.binarySearch(widgets, widget, Comparator.comparingInt(w -> w.z)) - 1;
                if (index < limit) {
                    widgets.add(index, widget);
                    if (widgets.size() > limit) {
                        widgets.subList(limit, widgets.size()).clear();
                    }
                }
            }
        });
        return widgets
            .stream()
            .map(InMemoryWidgetsStorage::toWidget)
            .collect(Collectors.toList());
    }

    private List<Widget> getWidgetsByZIndex(Integer cursor, int limit) {
        Map<WidgetSortingKey, WidgetWrapper> widgets = cursor != null
            ? widgetsByZIndex.tailMap(new WidgetSortingKey(cursor), false)
            : widgetsByZIndex;
        return widgets
            .values()
            .stream()
            .limit(limit)
            .map(InMemoryWidgetsStorage::toWidget)
            .collect(Collectors.toList());
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

    private static class WidgetWrapper {

        private final String id;
        private Region boundaries;
        private int z;
        private Instant modifiedAt;

        private WidgetWrapper(Widget widget) {
            Objects.requireNonNull(widget, "widget");
            this.id = widget.getId();
            this.boundaries = widget.getBoundaries();
            this.z = widget.getZ();
            this.modifiedAt = widget.getModifiedAt();
        }

        @Override
        public String toString() {
            return "WidgetWrapper{" +
                "id='" + id + '\'' +
                ", boundaries=" + boundaries +
                ", z=" + z +
                ", modifiedAt=" + modifiedAt +
                '}';
        }
    }

    private static Widget toWidget(WidgetWrapper wrapper) {
        return Widget.builder()
            .setId(wrapper.id)
            .setBoundaries(wrapper.boundaries)
            .setZ(wrapper.z)
            .setModifiedAt(wrapper.modifiedAt)
            .build();
    }
}
