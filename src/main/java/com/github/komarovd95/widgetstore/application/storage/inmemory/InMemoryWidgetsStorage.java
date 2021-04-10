package com.github.komarovd95.widgetstore.application.storage.inmemory;

import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.storage.StoreWidgetParameters;
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
    private final NavigableMap<WidgetSortingKey, Widget> widgetsByZIndex = new TreeMap<>();

    /**
     * A map for searching widgets by ID.
     * <p>
     * All operations with this map MUST be guarded by {@link #lock}.
     */
    private final Map<String, Widget> widgetsByIds = new HashMap<>();

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
                .setX(parameters.getX())
                .setY(parameters.getY())
                .setZ(key.z)
                .setWidth(parameters.getWidth())
                .setHeight(parameters.getHeight())
                .setModifiedAt(clock.instant())
                .build();
            shiftOverlyingWidgets(key, widget.getModifiedAt());
            widgetsByZIndex.put(key, widget);
            widgetsByIds.put(id, widget);
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
            if (widget.getZ() != widgetToUpdate.getZ()) {
                widgetsByZIndex.remove(new WidgetSortingKey(widget.getZ()));
                shiftOverlyingWidgets(key, widgetToUpdate.getModifiedAt());
            }
            widgetsByZIndex.put(key, widgetToUpdate);
            widgetsByIds.put(id, widgetToUpdate);
            log.info("Widget has been updated successfully: widget={}", widgetToUpdate);
            return Optional.of(widgetToUpdate);
        });
    }

    private void shiftOverlyingWidgets(WidgetSortingKey key, Instant modificationTimestamp) {
        NavigableMap<WidgetSortingKey, Widget> tailMap = widgetsByZIndex.tailMap(key, true);
        int previousZ = key.z;
        for (Map.Entry<WidgetSortingKey, Widget> entry : tailMap.entrySet()) {
            WidgetSortingKey entryKey = entry.getKey();
            if (entryKey.z == previousZ) {
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
            } else {
                break;
            }
        }
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
    public List<Widget> getWidgets() {
        return executeWithLock(lock.readLock(), () -> new ArrayList<>(widgetsByZIndex.values()));
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
}
