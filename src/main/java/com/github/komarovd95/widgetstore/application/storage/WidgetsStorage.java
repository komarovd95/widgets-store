package com.github.komarovd95.widgetstore.application.storage;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;

import java.util.List;
import java.util.Optional;

/**
 * Widget storage. There are some implementation requirements:
 * <ul>
 *     <li>All operations MUST be thread-safe. Also, write operations MUST be atomic.</li>
 *     <li>While creating a new widget, the implementation MUST generate a unique identifier.</li>
 *     <li>
 *         The implementation MUST maintain the uniqueness of the Z-index. Gaps are allowed.
 *         When creating or modifying a widget with an existing Z-index, it MUST shift the overlying widgets if needed.
 *     </li>
 *     <li>The implementation MAY consider the idempotency of some write operations.</li>
 * </ul>
 */
public interface WidgetsStorage {

    /**
     * Adds a new widget to this storage based on the given parameters.
     * <p>
     * Creation MUST be thread-safe and atomic: any of the other threads cannot observe the stored widgets'
     * intermediate state.
     * <p>
     * If there is a widget with the same Z-index in the storage, then a newly created one MUST shift the overlying
     * widgets upwards.
     *
     * @param parameters the parameters to create a new widget (never null)
     * @return the created widget with a generated identifier. Never returns null.
     */
    Widget createWidget(StoreWidgetParameters parameters);

    /**
     * Updates an existing widget (identified by the given ID) in this storage based on the given parameters.
     * <p>
     * Update MUST be thread-safe and atomic: any of the other threads cannot observe the stored widgets' intermediate
     * state.
     * <p>
     * If there is a widget with the same Z-index in the storage after the update, then it MUST shift the overlying
     * widgets upwards.
     *
     * @param id         the identifier of the updating widget (never null)
     * @param parameters the parameters to update an existing widget (never null)
     * @return the updated widget or {@link Optional#empty()} if there is no existing widget with the given ID.
     * Never returns null.
     */
    Optional<Widget> updateWidget(String id, StoreWidgetParameters parameters);

    /**
     * Deletes an existing widget (identified by the given ID) from this storage.
     * <p>
     * Deletion MUST be thread-safe and atomic: any of the other threads cannot observe the stored widgets'
     * intermediate state.
     * <p>
     * If there is no existing widget with the given ID, then deletion MUST have no side effects.
     *
     * @param id the identifier of the deleting widget (never null)
     */
    void deleteWidget(String id);

    /**
     * Returns an existing widget identified by the given ID.
     * <p>
     * The implementation of this method MUST be thread-safe.
     *
     * @param id an existing widget's identifier
     * @return the existing widget or {@link Optional#empty()} if there is no existing widget with the given ID.
     * Never returns null.
     */
    Optional<Widget> getWidgetById(String id);

    /**
     * Returns a list of all widgets. The widgets in the resulting list MUST be sorted by Z-index in ascending order.
     * <p>
     * The implementation of this method MUST be thread-safe.
     * <p>
     * The implementation supports pagination.
     *
     * @param filter a parameters that used for filtering widgets
     * @return a paged list of all widgets
     */
    PagedList<Widget> getWidgets(WidgetsFilter filter);
}
