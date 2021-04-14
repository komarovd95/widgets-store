package com.github.komarovd95.widgetstore.application.domain.rtree;

import com.github.komarovd95.widgetstore.application.domain.Region;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * An R-tree wrapper for widgets.
 */
public class WidgetRTree {

    private final RTree<String> tree = new RTree<>();

    /**
     * Adds a widget's boundaries to the R-tree.
     *
     * @param id an ID of the widget
     * @param boundaries boundaries of the widget
     */
    public void add(String id, Region boundaries) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(boundaries, "boundaries");
        tree.insert(
            new float[] { boundaries.getX(), boundaries.getY() },
            new float[] { boundaries.getWidth(), boundaries.getHeight() },
            id
        );
    }

    /**
     * Removes a widget from the R-tree.
     *
     * @param id an ID of the widget
     * @param boundaries boundaries of the widget
     */
    public void remove(String id, Region boundaries) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(boundaries, "boundaries");
        tree.delete(
            new float[] { boundaries.getX(), boundaries.getY() },
            new float[] { boundaries.getWidth(), boundaries.getHeight() },
            id
        );
    }

    /**
     * Traverses the R-tree and finds all widgets that contained by the given region. Every found widget's ID will be
     * provided to the consumer
     *
     * @param region the region for the spatial search
     * @param widgetConsumer the consumer of found widget IDs
     */
    public void contains(Region region, Consumer<String> widgetConsumer) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(widgetConsumer, "widgetConsumer");
        search(region, tree.root, widgetConsumer);
    }

    private void search(Region region, RTree<String>.Node node, Consumer<String> widgetConsumer) {
        if (node.leaf) {
            for (RTree<String>.Node e : node.children) {
                if (isContained(region, e.coords, e.dimensions)) {
                    String widgetId = ((RTree<String>.Entry) e).entry;
                    widgetConsumer.accept(widgetId);
                }
            }
        } else {
            float[] coords = new float[] { region.getX(), region.getY() };
            float[] dimensions = new float[] { region.getWidth(), region.getHeight() };
            for (RTree<String>.Node c : node.children) {
                if (tree.isOverlap(coords, dimensions, c.coords, c.dimensions)) {
                    search(region, c, widgetConsumer);
                }
            }
        }
    }

    private boolean isContained(Region region, float[] coordinates, float[] dimensions) {
        return Float.compare(region.getX(), coordinates[0]) <= 0
            && Float.compare(region.getY(), coordinates[1]) <= 0
            && Float.compare(region.getX() + region.getWidth(), coordinates[0] + dimensions[0]) >= 0
            && Float.compare(region.getY() + region.getHeight(), coordinates[1] + dimensions[1]) >= 0;
    }
}
