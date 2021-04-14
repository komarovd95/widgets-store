package com.github.komarovd95.widgetstore.application.service;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import com.github.komarovd95.widgetstore.application.repository.WidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
import com.github.komarovd95.widgetstore.application.domain.StoreWidgetParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Widgets service. There are some implementation requirements:
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
public class WidgetsService {

    private static final Logger log = LoggerFactory.getLogger(WidgetsService.class);

    /**
     * The Z-index that used when there is no stored widgets.
     */
    private static final int INITIAL_Z_INDEX = 0;

    private final TransactionsService transactionsService;

    private final WidgetsRepository widgetsRepository;

    private final WidgetIdGenerator idGenerator;

    private final Clock clock;

    public WidgetsService(
        TransactionsService transactionsService,
        WidgetsRepository widgetsRepository,
        WidgetIdGenerator idGenerator,
        Clock clock
    ) {
        this.transactionsService = Objects.requireNonNull(transactionsService, "transactionsService");
        this.widgetsRepository = Objects.requireNonNull(widgetsRepository, "widgetsRepository");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = clock;
    }

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
    public Widget createWidget(StoreWidgetParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        String id = idGenerator.generate();
        Instant modificationTimestamp = clock.instant();
        Widget widget = transactionsService.writeTransaction(() -> {
            int zIndex = parameters.getZ()
                .orElseGet(() ->
                    widgetsRepository.getCurrentForegroundZIndex()
                        .map(z -> z + 1)
                        .orElse(INITIAL_Z_INDEX)
                );
            log.info("Creating a new widget: id={}, zIndex={}, parameters={}", id, zIndex, parameters);
            widgetsRepository.insert(
                id,
                parameters.getBoundaries(),
                zIndex,
                modificationTimestamp
            );
            return Widget.builder()
                .setId(id)
                .setBoundaries(parameters.getBoundaries())
                .setZ(zIndex)
                .setModifiedAt(modificationTimestamp)
                .build();
        });
        log.info("Widget has been created successfully: widget={}", widget);
        return widget;
    }

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
    public Optional<Widget> updateWidget(String id, StoreWidgetParameters parameters) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parameters, "parameters");
        Instant modificationTimestamp = clock.instant();
        return transactionsService.writeTransaction(() -> {
            Optional<Widget> optionalWidget = widgetsRepository.getWidgetById(id);
            if (optionalWidget.isEmpty()) {
                log.warn("Widget was not found by given ID: id={}", id);
                return Optional.empty();
            }
            Widget widget = optionalWidget.get();

            int zIndex = parameters.getZ()
                .orElseGet(() ->
                    widgetsRepository.getCurrentForegroundZIndex()
                        .map(z -> z == widget.getZ() ? z : z + 1)
                        .orElse(INITIAL_Z_INDEX)
                );

            if (updateIsNotNeeded(widget, parameters.getBoundaries(), zIndex)) {
                log.info("Widget doesn't need update: widget={}, parameters={}, zIndex={}", widget, parameters, zIndex);
                return Optional.of(widget);
            }
            log.info("Updating the existing widget: widget={}, parameters={}, zIndex={}", widget, parameters, zIndex);
            widgetsRepository.update(
                widget,
                parameters.getBoundaries(),
                zIndex,
                modificationTimestamp
            );
            return Optional.of(
                Widget.builder(widget)
                    .setBoundaries(parameters.getBoundaries())
                    .setZ(zIndex)
                    .setModifiedAt(modificationTimestamp)
                    .build()
            );
        });
    }

    private boolean updateIsNotNeeded(Widget widget, Region boundaries, int zIndex) {
        return Objects.equals(widget.getBoundaries(), boundaries) && widget.getZ() == zIndex;
    }

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
    public void deleteWidget(String id) {
        Objects.requireNonNull(id, "id");
        log.info("Deleting an existing widget: id={}", id);
        transactionsService.writeTransaction(() -> {
            if (widgetsRepository.deleteById(id)) {
                log.info("Widget has been deleted successfully: id={}", id);
            } else {
                log.info("Widget was not found by ID: id={}", id);
            }
            return null;
        });
    }

    /**
     * Returns an existing widget identified by the given ID.
     * <p>
     * The implementation of this method MUST be thread-safe.
     *
     * @param id an existing widget's identifier
     * @return the existing widget or {@link Optional#empty()} if there is no existing widget with the given ID.
     * Never returns null.
     */
    public Optional<Widget> getWidgetById(String id) {
        Objects.requireNonNull(id, "id");
        return transactionsService.readTransaction(() -> widgetsRepository.getWidgetById(id));
    }

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
    public PagedList<Widget> getWidgets(WidgetsFilter filter) {
        Objects.requireNonNull(filter, "filter");
        List<Widget> widgets = transactionsService.readTransaction(() ->
            widgetsRepository.getWidgets(
                filter.getRegion().orElse(null),
                filter.getCursor().orElse(null),
                filter.getLimit() + 1
            )
        );
        if (widgets.size() > filter.getLimit()) {
            List<Widget> pageItems = widgets.subList(0, filter.getLimit());
            return PagedList.nonLastPage(pageItems, pageItems.get(pageItems.size() - 1).getZ());
        } else {
            return PagedList.lastPage(widgets);
        }
    }
}
