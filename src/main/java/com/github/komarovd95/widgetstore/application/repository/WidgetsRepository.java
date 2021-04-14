package com.github.komarovd95.widgetstore.application.repository;

import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.service.WidgetsService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A repository for widgets.
 * <p>
 * The implementations MAY NOT be thread-safe not atomic. So, thread-safety must be implemented on the caller side.
 *
 * @see WidgetsService
 */
public interface WidgetsRepository {

    /**
     * Returns current foreground Z-index (the current maximum Z-index).
     *
     * @return the current maximum Z-index or {@link Optional#empty()} if no widgets are stored in this repository
     */
    Optional<Integer> getCurrentForegroundZIndex();

    /**
     * Inserts a new widget into this repository.
     *
     * @param id the identifier of the widget, not null
     * @param boundaries the boundaries of the widget, not null
     * @param zIndex the Z-index of the widget
     * @param modificationTimestamp the timestamp of the insertion, not null
     */
    void insert(String id, Region boundaries, int zIndex, Instant modificationTimestamp);

    /**
     * Updates an existing widget in this repository.
     *
     * @param widget the existing widget, not null
     * @param newBoundaries the new value of boundaries of the widget, not null
     * @param newZIndex the new value of Z-index of the widget
     * @param modificationTimestamp the timestamp of the update, not null
     */
    void update(Widget widget, Region newBoundaries, int newZIndex, Instant modificationTimestamp);

    /**
     * Deletes an existing widget in this repository.
     *
     * @param id the identifier of the widget, not null
     * @return {@code true} if widget has been deleted or {@code false} if there is no widget with the given ID
     *         in this repository
     */
    boolean deleteById(String id);

    /**
     * Returns an existing widget by given ID.
     *
     * @param id the identifier of the widget, not null
     * @return the found widget or {@link Optional#empty()} if there is no widget with the given ID in this repository
     */
    Optional<Widget> getWidgetById(String id);

    /**
     * Returns a list of widgets by given filter.
     *
     * @param regionToSearch the region to search into. Might be null. If present, then all widgets
     *                       that contained by this region will be returned
     * @param zIndexCursor the Z-index cursor for cursor-based pagination. Might be null. If present, then all widgets
     *                     with Z-index more than given value will be returned
     * @param limit the maximum number of widgets that will be returned in the list
     * @return the list of widgets
     */
    List<Widget> getWidgets(Region regionToSearch, Integer zIndexCursor, int limit);
}
