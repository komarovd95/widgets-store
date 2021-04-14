package com.github.komarovd95.widgetstore.application.service;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.StoreWidgetParameters;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

public abstract class AbstractWidgetsServiceTest {

    private static final Region DEFAULT_BOUNDARIES = Region.builder()
        .setX(0)
        .setY(0)
        .setWidth(100)
        .setHeight(100)
        .builder();

    private static final StoreWidgetParameters DEFAULT_PARAMETERS = StoreWidgetParameters.builder()
        .setBoundaries(DEFAULT_BOUNDARIES)
        .build();

    private static final WidgetsFilter DEFAULT_FILTER = new WidgetsFilter(null, null, 100);

    protected abstract WidgetsService getService();

    @Test
    public void should_create_new_widget_with_default_Z_index_when_no_other_widgets_exist() {
        // given
        WidgetsService service = getService();

        // when
        Widget createdWidget = service.createWidget(DEFAULT_PARAMETERS);

        // then
        assertWidget(createdWidget, DEFAULT_BOUNDARIES, 0);

        // and then
        Widget widgetFoundById = service.getWidgetById(createdWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, createdWidget.getId(), DEFAULT_BOUNDARIES, 0, createdWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), createdWidget.getId(), DEFAULT_BOUNDARIES, 0, createdWidget.getModifiedAt());
    }

    @Test
    public void should_create_new_widget_with_incremented_Z_index_when_some_widgets_already_exist() {
        // given
        WidgetsService service = getService();

        // when
        Widget createdWidget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget createdWidget2 = service.createWidget(DEFAULT_PARAMETERS);

        // then
        assertWidget(createdWidget1, DEFAULT_BOUNDARIES, 0);

        Widget widget1FoundById = service.getWidgetById(createdWidget1.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, createdWidget1.getId(), DEFAULT_BOUNDARIES, 0, createdWidget1.getModifiedAt());

        // and then
        assertWidget(createdWidget2, DEFAULT_BOUNDARIES, 1);

        Widget widget2FoundById = service.getWidgetById(createdWidget2.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, createdWidget2.getId(), DEFAULT_BOUNDARIES, 1, createdWidget2.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        assertWidget(widgets.get(0), createdWidget1.getId(), DEFAULT_BOUNDARIES, 0, createdWidget1.getModifiedAt());
        assertWidget(widgets.get(1), createdWidget2.getId(), DEFAULT_BOUNDARIES, 1, createdWidget2.getModifiedAt());
    }

    @Test
    public void should_create_new_widget_when_Z_index_is_defined_in_the_parameters() {
        // given
        WidgetsService service = getService();

        // when
        Widget createdWidget = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(3)
                .build()
        );

        // then
        assertWidget(createdWidget, DEFAULT_BOUNDARIES, 3);

        Widget widgetFoundById = service.getWidgetById(createdWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, createdWidget.getId(), DEFAULT_BOUNDARIES, 3, createdWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), createdWidget.getId(), DEFAULT_BOUNDARIES, 3, createdWidget.getModifiedAt());
    }

    @Test
    public void should_create_new_widget_and_shift_overlying_when_Z_index_is_defined_in_the_parameters() {
        // given
        WidgetsService service = getService();

        // and given
        Widget createdWidget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget createdWidget2 = service.createWidget(DEFAULT_PARAMETERS);

        // when
        Widget createdWidget3 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(createdWidget1.getZ())
                .build()
        );

        // then
        assertWidget(createdWidget3, DEFAULT_BOUNDARIES, 0);

        // and then
        Widget widget1FoundById = service.getWidgetById(createdWidget1.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, createdWidget1.getId(), DEFAULT_BOUNDARIES, 1, createdWidget3.getModifiedAt());

        Widget widget2FoundById = service.getWidgetById(createdWidget2.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, createdWidget2.getId(), DEFAULT_BOUNDARIES, 2, createdWidget3.getModifiedAt());

        Widget widget3FoundById = service.getWidgetById(createdWidget3.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget3FoundById, createdWidget3.getId(), DEFAULT_BOUNDARIES, 0, createdWidget3.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), createdWidget3.getId(), DEFAULT_BOUNDARIES, 0, createdWidget3.getModifiedAt());
        assertWidget(widgets.get(1), createdWidget1.getId(), DEFAULT_BOUNDARIES, 1, createdWidget3.getModifiedAt());
        assertWidget(widgets.get(2), createdWidget2.getId(), DEFAULT_BOUNDARIES, 2, createdWidget3.getModifiedAt());
    }

    @Test
    public void should_create_new_widget_and_shift_overlying_if_needed_when_Z_index_is_defined_in_the_parameters() {
        // given
        WidgetsService service = getService();

        // and given
        Widget createdWidget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget createdWidget2 = service.createWidget(DEFAULT_PARAMETERS);
        Widget createdWidget3 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(3)
                .build()
        );

        // when
        Widget createdWidget4 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(0)
                .build()
        );

        // then
        Widget widget1FoundById = service.getWidgetById(createdWidget1.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, createdWidget1.getId(), DEFAULT_BOUNDARIES, 1, createdWidget4.getModifiedAt());

        Widget widget2FoundById = service.getWidgetById(createdWidget2.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, createdWidget2.getId(), DEFAULT_BOUNDARIES, 2, createdWidget4.getModifiedAt());

        Widget widget3FoundById = service.getWidgetById(createdWidget3.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget3FoundById, createdWidget3.getId(), DEFAULT_BOUNDARIES, 3, createdWidget3.getModifiedAt());

        Widget widget4FoundById = service.getWidgetById(createdWidget4.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget4FoundById, createdWidget4.getId(), DEFAULT_BOUNDARIES, 0, createdWidget4.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(4, widgets.size());
        assertWidget(widgets.get(0), createdWidget4.getId(), DEFAULT_BOUNDARIES, 0, createdWidget4.getModifiedAt());
        assertWidget(widgets.get(1), createdWidget1.getId(), DEFAULT_BOUNDARIES, 1, createdWidget4.getModifiedAt());
        assertWidget(widgets.get(2), createdWidget2.getId(), DEFAULT_BOUNDARIES, 2, createdWidget4.getModifiedAt());
        assertWidget(widgets.get(3), createdWidget3.getId(), DEFAULT_BOUNDARIES, 3, createdWidget3.getModifiedAt());
    }

    @Test
    public void should_update_widget_when_Z_index_was_not_changed() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget = service.createWidget(DEFAULT_PARAMETERS);

        // and given
        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();

        // when
        Widget updatedWidget = service
            .updateWidget(
                widget.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(
                        boundaries
                    )
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));

        // then
        assertWidget(updatedWidget, boundaries, 0);
        Assertions.assertEquals(widget.getId(), updatedWidget.getId());
        Assertions.assertNotEquals(widget.getModifiedAt(), updatedWidget.getModifiedAt());

        // and then
        Widget widgetFoundById = service.getWidgetById(updatedWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, updatedWidget.getId(), boundaries, 0, updatedWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), updatedWidget.getId(), boundaries, 0, updatedWidget.getModifiedAt());
    }

    @Test
    public void should_move_widget_to_the_foreground_when_Z_index_was_not_changed() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = service.createWidget(DEFAULT_PARAMETERS);

        // and given
        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();

        // when
        Widget updatedWidget = service
            .updateWidget(
                widget1.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));

        // then
        assertWidget(updatedWidget, boundaries, 2);
        Assertions.assertEquals(widget1.getId(), updatedWidget.getId());
        Assertions.assertNotEquals(widget1.getModifiedAt(), updatedWidget.getModifiedAt());

        // and then
        Widget widgetFoundById = service.getWidgetById(updatedWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, updatedWidget.getId(), boundaries, 2, updatedWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), DEFAULT_BOUNDARIES, 1, widget2.getModifiedAt());
        assertWidget(widgets.get(1), widget1.getId(), boundaries, 2, updatedWidget.getModifiedAt());
    }

    @Test
    public void should_update_widget_and_shift_overlying_when_Z_index_is_defined_in_the_parameters() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = service.createWidget(DEFAULT_PARAMETERS);

        // and given
        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();

        // when
        Widget updatedWidget = service
            .updateWidget(
                widget3.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .setZ(widget1.getZ())
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));

        // then
        assertWidget(updatedWidget, boundaries, 0);
        Assertions.assertEquals(widget3.getId(), updatedWidget.getId());
        Assertions.assertNotEquals(widget3.getModifiedAt(), updatedWidget.getModifiedAt());

        // and then
        Widget widgetFoundById = service.getWidgetById(updatedWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, updatedWidget.getId(), boundaries, 0, updatedWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), widget3.getId(), boundaries, 0, updatedWidget.getModifiedAt());
        assertWidget(widgets.get(1), widget1.getId(), DEFAULT_BOUNDARIES, 1, updatedWidget.getModifiedAt());
        assertWidget(widgets.get(2), widget2.getId(), DEFAULT_BOUNDARIES, 2, updatedWidget.getModifiedAt());
    }

    @Test
    public void should_update_widget_and_shift_overlying_if_needed_when_Z_index_is_defined_in_the_parameters() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = service.createWidget(DEFAULT_PARAMETERS);

        // and given
        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();

        // when
        Widget updatedWidget = service
            .updateWidget(
                widget2.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .setZ(widget1.getZ())
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));

        // then
        assertWidget(updatedWidget, boundaries, 0);
        Assertions.assertEquals(widget2.getId(), updatedWidget.getId());
        Assertions.assertNotEquals(widget2.getModifiedAt(), updatedWidget.getModifiedAt());

        // and then
        Widget widgetFoundById = service.getWidgetById(updatedWidget.getId())
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, updatedWidget.getId(), boundaries, 0, updatedWidget.getModifiedAt());

        // and then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), boundaries, 0, updatedWidget.getModifiedAt());
        assertWidget(widgets.get(1), widget1.getId(), DEFAULT_BOUNDARIES, 1, updatedWidget.getModifiedAt());
        assertWidget(widgets.get(2), widget3.getId(), DEFAULT_BOUNDARIES, 2, widget3.getModifiedAt());
    }

    @Test
    public void should_not_update_widget_when_update_is_not_needed() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget = service.createWidget(DEFAULT_PARAMETERS);

        // when
        Widget updatedWidget = service
            .updateWidget(widget.getId(), DEFAULT_PARAMETERS)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));

        // then
        assertWidget(updatedWidget, widget.getId(), widget.getBoundaries(), widget.getZ(), widget.getModifiedAt());
    }

    @Test
    public void should_delete_widget_when_widget_already_exist() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = service.createWidget(DEFAULT_PARAMETERS);

        // when
        service.deleteWidget(widget1.getId());

        // then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), widget2.getBoundaries(), widget2.getZ(), widget2.getModifiedAt());
    }

    @Test
    public void should_do_nothing_while_deletion_when_there_is_no_widget_with_the_given_id() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget = service.createWidget(DEFAULT_PARAMETERS);

        // when
        service.deleteWidget("other_test");

        // then
        List<Widget> widgets = service.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), widget.getId(), widget.getBoundaries(), widget.getZ(), widget.getModifiedAt());
    }

    @Test
    public void should_return_all_widgets_when_paging_is_used() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = service.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = service.createWidget(DEFAULT_PARAMETERS);

        // when
        PagedList<Widget> page1 = service.getWidgets(new WidgetsFilter(null, null, 1));
        Assertions.assertEquals(1, page1.getItems().size());
        assertWidget(
            page1.getItems().get(0),
            widget1.getId(),
            widget1.getBoundaries(),
            widget1.getZ(),
            widget1.getModifiedAt()
        );
        Integer page1Cursor = page1.getCursor()
            .orElseGet(() -> Assertions.fail("Cursor must be presented for page 1"));
        Assertions.assertEquals(widget1.getZ(), page1Cursor);

        // and when
        PagedList<Widget> page2 = service.getWidgets(new WidgetsFilter(null, page1Cursor, 1));
        Assertions.assertEquals(1, page2.getItems().size());
        assertWidget(
            page2.getItems().get(0),
            widget2.getId(),
            widget2.getBoundaries(),
            widget2.getZ(),
            widget2.getModifiedAt()
        );
        Integer page2Cursor = page2.getCursor()
            .orElseGet(() -> Assertions.fail("Cursor must be presented for page 2"));
        Assertions.assertEquals(widget2.getZ(), page2Cursor);

        // and when
        PagedList<Widget> page3 = service.getWidgets(new WidgetsFilter(null, page2Cursor, 1));
        Assertions.assertEquals(1, page3.getItems().size());
        assertWidget(
            page3.getItems().get(0),
            widget3.getId(),
            widget3.getBoundaries(),
            widget3.getZ(),
            widget3.getModifiedAt()
        );
        Assertions.assertFalse(page3.getCursor().isPresent());
    }

    @Test
    public void should_return_all_widgets_when_spatial_search_is_used() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(0)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        Widget widget2 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(50)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );

        // when
        PagedList<Widget> pagedWidgets = service.getWidgets(
            new WidgetsFilter(
                Region.builder()
                    .setX(0)
                    .setY(0)
                    .setWidth(100)
                    .setHeight(150)
                    .builder(),
                null,
                100
            )
        );

        // then
        Assertions.assertEquals(2, pagedWidgets.getItems().size());
        assertWidget(
            pagedWidgets.getItems().get(0),
            widget1.getId(),
            widget1.getBoundaries(),
            widget1.getZ(),
            widget1.getModifiedAt()
        );
        assertWidget(
            pagedWidgets.getItems().get(1),
            widget2.getId(),
            widget2.getBoundaries(),
            widget2.getZ(),
            widget2.getModifiedAt()
        );
        Assertions.assertFalse(pagedWidgets.getCursor().isPresent());
    }

    @Test
    public void should_return_all_widgets_when_spatial_search_and_paging_are_used() {
        // given
        WidgetsService service = getService();

        // and given
        Widget widget1 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(0)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        Widget widget2 = service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(50)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );

        // and given
        Region searchBoundaries = Region.builder()
            .setX(0)
            .setY(0)
            .setWidth(100)
            .setHeight(150)
            .builder();

        // when
        PagedList<Widget> page1 = service.getWidgets(
            new WidgetsFilter(
                searchBoundaries,
                null,
                1
            )
        );
        Assertions.assertEquals(1, page1.getItems().size());
        assertWidget(
            page1.getItems().get(0),
            widget1.getId(),
            widget1.getBoundaries(),
            widget1.getZ(),
            widget1.getModifiedAt()
        );
        Integer page1Cursor = page1.getCursor()
            .orElseGet(() -> Assertions.fail("Cursor must be presented for page 1"));
        Assertions.assertEquals(widget1.getZ(), page1Cursor);

        // and when
        PagedList<Widget> page2 = service.getWidgets(
            new WidgetsFilter(
                searchBoundaries,
                page1Cursor,
                1
            )
        );
        Assertions.assertEquals(1, page2.getItems().size());
        assertWidget(
            page2.getItems().get(0),
            widget2.getId(),
            widget2.getBoundaries(),
            widget2.getZ(),
            widget2.getModifiedAt()
        );
        Assertions.assertFalse(page2.getCursor().isPresent());
    }

    @Test
    public void should_return_no_widgets_when_spatial_search_returned_0_entries() {
        // given
        WidgetsService service = getService();

        // and given
        service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(0)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(0)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );
        service.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(
                    Region.builder()
                        .setX(50)
                        .setY(50)
                        .setWidth(100)
                        .setHeight(100)
                        .builder()
                )
                .build()
        );

        // and given
        Region searchBoundaries = Region.builder()
            .setX(0)
            .setY(0)
            .setWidth(50)
            .setHeight(50)
            .builder();

        // when
        PagedList<Widget> page = service.getWidgets(
            new WidgetsFilter(
                searchBoundaries,
                null,
                1
            )
        );

        // then
        Assertions.assertEquals(0, page.getItems().size());
        Assertions.assertFalse(page.getCursor().isPresent());
    }

    private static void assertWidget(Widget widget, Region expectedBoundaries, int expectedZIndex) {
        assertWidget(widget, widget.getId(), expectedBoundaries, expectedZIndex, widget.getModifiedAt());
    }

    private static void assertWidget(
        Widget widget,
        String expectedId,
        Region expectedBoundaries,
        int expectedZIndex,
        Instant expectedTimestamp
    ) {
        Assertions.assertAll(
            () -> Assertions.assertEquals(expectedId, widget.getId()),
            () -> Assertions.assertEquals(expectedBoundaries, widget.getBoundaries()),
            () -> Assertions.assertEquals(expectedZIndex, widget.getZ()),
            () -> Assertions.assertEquals(expectedTimestamp.toEpochMilli(), widget.getModifiedAt().toEpochMilli())
        );
    }
}
