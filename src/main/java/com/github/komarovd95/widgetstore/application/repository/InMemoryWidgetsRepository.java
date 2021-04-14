package com.github.komarovd95.widgetstore.application.repository;

import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.rtree.WidgetRTree;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The in-memory implementation of the {@link WidgetsRepository}.
 * <p>
 * It uses a {@link TreeMap} (widgets sorted by Z-index) for efficient widgets list queries and overlying widgets
 * shifting while inserts and updates. Also, this map is used for maintaining of Z-indices uniqueness.
 * <p>
 * Additionally, it uses a {@link HashMap} (keys are widgets' IDs) for efficient searched by ID.
 * <p>
 * For spatial search, the implementation uses an R-tree.
 * <p>
 * To achieve atomicity and thread-safety it's required to use in-memory "transactions" mechanism.
 *
 * @see TransactionsService
 */
public class InMemoryWidgetsRepository implements WidgetsRepository {

    /**
     * A map for searching widgets by Z-index. Widgets are stored in this map in the sorted order (ascending).
     */
    private final NavigableMap<WidgetSortingKey, MutableWidget> widgetsByZIndex = new TreeMap<>();

    /**
     * A map for searching widgets by ID.
     */
    private final Map<String, MutableWidget> widgetsByIds = new HashMap<>();

    /**
     * An R-tree for spatial search.
     */
    private final WidgetRTree spatialIndex = new WidgetRTree();

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Integer> getCurrentForegroundZIndex() {
        return widgetsByZIndex.isEmpty() ? Optional.empty() : Optional.of(widgetsByZIndex.lastKey().z);
    }

    /**
     * @inheritDocs
     */
    @Override
    public void insert(String id, Region boundaries, int zIndex, Instant modificationTimestamp) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(boundaries, "boundaries");
        Objects.requireNonNull(modificationTimestamp, "modificationTimestamp");

        MutableWidget widget = new MutableWidget(id, boundaries, zIndex, modificationTimestamp);
        WidgetSortingKey key = new WidgetSortingKey(zIndex);
        shiftOverlyingWidgets(key, modificationTimestamp);
        widgetsByZIndex.put(key, widget);
        widgetsByIds.put(id, widget);
        spatialIndex.add(id, widget.boundaries);
    }

    /**
     * @inheritDocs
     */
    @Override
    public void update(Widget widget, Region newBoundaries, int newZIndex, Instant modificationTimestamp) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(newBoundaries, "newBoundaries");
        Objects.requireNonNull(modificationTimestamp, "modificationTimestamp");

        MutableWidget mutableWidget = null;
        WidgetSortingKey key = new WidgetSortingKey(newZIndex);
        if (widget.getZ() != key.z) {
            mutableWidget = widgetsByZIndex.remove(new WidgetSortingKey(widget.getZ()));
            shiftOverlyingWidgets(key, modificationTimestamp);
            mutableWidget.z = key.z;
            mutableWidget.modifiedAt = modificationTimestamp;
            widgetsByZIndex.put(key, mutableWidget);
        }
        if (!Objects.equals(widget.getBoundaries(), newBoundaries)) {
            spatialIndex.remove(widget.getId(), widget.getBoundaries());
            mutableWidget = mutableWidget != null ? mutableWidget : widgetsByIds.get(widget.getId());
            mutableWidget.boundaries = newBoundaries;
            mutableWidget.modifiedAt = modificationTimestamp;
            spatialIndex.add(widget.getId(), newBoundaries);
        }
    }

    private void shiftOverlyingWidgets(WidgetSortingKey key, Instant modificationTimestamp) {
        NavigableMap<WidgetSortingKey, MutableWidget> tailMap = widgetsByZIndex.tailMap(key, true);
        int previousZ = key.z;
        for (Map.Entry<WidgetSortingKey, MutableWidget> entry : tailMap.entrySet()) {
            WidgetSortingKey entryKey = entry.getKey();
            MutableWidget widget = entry.getValue();
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
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "id");
        MutableWidget removedWidget = widgetsByIds.remove(id);
        if (removedWidget != null) {
            widgetsByZIndex.remove(new WidgetSortingKey(removedWidget.z));
            spatialIndex.remove(id, removedWidget.boundaries);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Widget> getWidgetById(String id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(widgetsByIds.get(id))
            .map(InMemoryWidgetsRepository::toImmutable);
    }

    /**
     * @inheritDocs
     */
    @Override
    public List<Widget> getWidgets(Region regionToSearch, Integer zIndexCursor, int limit) {
        return regionToSearch != null
            ? getWidgetsBySpatialIndex(regionToSearch, zIndexCursor, limit)
            : getWidgetsByZIndex(zIndexCursor, limit);
    }

    private List<Widget> getWidgetsBySpatialIndex(Region region, Integer cursor, int limit) {
        // top-N sort
        List<MutableWidget> widgets = new ArrayList<>();
        spatialIndex.contains(region, widgetId -> {
            MutableWidget widget = widgetsByIds.get(widgetId);
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
            .map(InMemoryWidgetsRepository::toImmutable)
            .collect(Collectors.toList());
    }

    private List<Widget> getWidgetsByZIndex(Integer cursor, int limit) {
        Map<WidgetSortingKey, MutableWidget> widgets = cursor != null
            ? widgetsByZIndex.tailMap(new WidgetSortingKey(cursor), false)
            : widgetsByZIndex;
        return widgets
            .values()
            .stream()
            .limit(limit)
            .map(InMemoryWidgetsRepository::toImmutable)
            .collect(Collectors.toList());
    }

    /**
     * A key for a widgets' {@link TreeMap}.
     */
    private static class WidgetSortingKey implements Comparable<WidgetSortingKey> {

        /**
         * This field is mutable by design. Mutability allows to reduce time complexity of overlying widgets shifting.
         */
        private int z;

        private WidgetSortingKey(int zIndex) {
            this.z = zIndex;
        }

        @Override
        public int compareTo(WidgetSortingKey other) {
            return Integer.compare(z, other.z);
        }
    }

    /**
     * A mutable widget representation.
     * <p>
     * This class is used internally only.
     * <p>
     * Mutability helps us to reduce the number of memory allocations. Also, with mutable class we don't need to call
     * methods like {@link Map#replace(Object, Object)} while updating widgets.
     */
    private static class MutableWidget {

        private final String id;
        private Region boundaries;
        private int z;
        private Instant modifiedAt;

        private MutableWidget(String id, Region boundaries, int z, Instant modifiedAt) {
            this.id = Objects.requireNonNull(id, "id");
            this.boundaries = Objects.requireNonNull(boundaries, "boundaries");
            this.z = z;
            this.modifiedAt = Objects.requireNonNull(modifiedAt, "modifiedAt");
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

    private static Widget toImmutable(MutableWidget wrapper) {
        return Widget.builder()
            .setId(wrapper.id)
            .setBoundaries(wrapper.boundaries)
            .setZ(wrapper.z)
            .setModifiedAt(wrapper.modifiedAt)
            .build();
    }
}
