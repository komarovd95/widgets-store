package com.github.komarovd95.widgetstore.application.storage.inmemory;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.storage.StoreWidgetParameters;
import com.github.komarovd95.widgetstore.application.storage.WidgetsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InMemoryWidgetsStorageTest {

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

    @Test
    public void should_create_new_widget_with_default_Z_index_when_no_other_widgets_exist() {
        String id = "test";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget actualWidget = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget, id, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id, DEFAULT_BOUNDARIES, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), id, DEFAULT_BOUNDARIES, 0, timestamp);
    }

    @Test
    public void should_create_new_widget_with_incremented_Z_index_when_some_widgets_already_exist() {
        String id1 = "test1";
        String id2 = "test2";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget actualWidget1 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget1, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        assertWidget(widgets.get(0), id1, DEFAULT_BOUNDARIES, 0, timestamp);
        assertWidget(widgets.get(1), id2, DEFAULT_BOUNDARIES, 1, timestamp);
    }

    @Test
    public void should_create_new_widget_when_Z_index_is_defined_in_the_parameters() {
        String id = "test";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget actualWidget = storage.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(3)
                .build()
        );
        assertWidget(actualWidget, id, DEFAULT_BOUNDARIES, 3, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id, DEFAULT_BOUNDARIES, 3, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), id, DEFAULT_BOUNDARIES, 3, timestamp);
    }

    @Test
    public void should_create_new_widget_and_shift_overlying_when_Z_index_is_defined_in_the_parameters() {
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget actualWidget1 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget1, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        Widget actualWidget3 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(0)
                .build()
        );
        assertWidget(actualWidget3, id3, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widget3FoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget3FoundById, id3, DEFAULT_BOUNDARIES, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), id3, DEFAULT_BOUNDARIES, 0, timestamp);
        assertWidget(widgets.get(1), id1, DEFAULT_BOUNDARIES, 1, timestamp);
        assertWidget(widgets.get(2), id2, DEFAULT_BOUNDARIES, 2, timestamp);
    }

    @Test
    public void should_create_new_widget_and_shift_overlying_if_needed_when_Z_index_is_defined_in_the_parameters() {
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        String id4 = "test4";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3, id4),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget actualWidget1 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget1, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget1FoundById, id1, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget2FoundById, id2, DEFAULT_BOUNDARIES, 1, timestamp);

        Widget actualWidget3 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(3)
                .build()
        );
        assertWidget(actualWidget3, id3, DEFAULT_BOUNDARIES, 3, timestamp);

        Widget widget3FoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget3FoundById, id3, DEFAULT_BOUNDARIES, 3, timestamp);

        Widget actualWidget4 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setBoundaries(DEFAULT_PARAMETERS.getBoundaries())
                .setZ(0)
                .build()
        );
        assertWidget(actualWidget4, id4, DEFAULT_BOUNDARIES, 0, timestamp);

        Widget widget4FoundById = storage.getWidgetById(id4)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widget4FoundById, id4, DEFAULT_BOUNDARIES, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(4, widgets.size());
        assertWidget(widgets.get(0), id4, DEFAULT_BOUNDARIES, 0, timestamp);
        assertWidget(widgets.get(1), id1, DEFAULT_BOUNDARIES, 1, timestamp);
        assertWidget(widgets.get(2), id2, DEFAULT_BOUNDARIES, 2, timestamp);
        assertWidget(widgets.get(3), id3, DEFAULT_BOUNDARIES, 3, timestamp);
    }

    @Test
    public void should_update_widget_when_Z_index_was_not_changed() {
        String id = "test";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget = storage.createWidget(DEFAULT_PARAMETERS);

        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();
        Widget actualWidget = storage
            .updateWidget(
                widget.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(
                        boundaries
                    )
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(actualWidget, id, boundaries, 0, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id, boundaries, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), id, boundaries, 0, timestamp);
    }

    @Test
    public void should_move_widget_to_the_foreground_when_Z_index_was_not_changed() {
        String id1 = "test1";
        String id2 = "test2";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = storage.createWidget(DEFAULT_PARAMETERS);

        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();
        Widget actualWidget = storage
            .updateWidget(
                widget1.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(actualWidget, id1, boundaries, 2, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id1, boundaries, 2, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), DEFAULT_BOUNDARIES, 1, timestamp);
        assertWidget(widgets.get(1), widget1.getId(), boundaries, 2, timestamp);
    }

    @Test
    public void should_update_widget_and_shift_overlying_when_Z_index_is_defined_in_the_parameters() {
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = storage.createWidget(DEFAULT_PARAMETERS);

        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();
        Widget actualWidget = storage
            .updateWidget(
                widget3.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .setZ(0)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(actualWidget, id3, boundaries, 0, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id3, boundaries, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), widget3.getId(), boundaries, 0, timestamp);
        assertWidget(widgets.get(1), widget1.getId(), DEFAULT_BOUNDARIES, 1, timestamp);
        assertWidget(widgets.get(2), widget2.getId(), DEFAULT_BOUNDARIES, 2, timestamp);
    }

    @Test
    public void should_update_widget_and_shift_overlying_if_needed_when_Z_index_is_defined_in_the_parameters() {
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = storage.createWidget(DEFAULT_PARAMETERS);

        Region boundaries = Region.builder()
            .setX(100)
            .setY(100)
            .setWidth(150)
            .setHeight(150)
            .builder();
        Widget actualWidget = storage
            .updateWidget(
                widget2.getId(),
                StoreWidgetParameters.builder()
                    .setBoundaries(boundaries)
                    .setZ(0)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(actualWidget, id2, boundaries, 0, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(widgetFoundById, id2, boundaries, 0, timestamp);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), boundaries, 0, timestamp);
        assertWidget(widgets.get(1), widget1.getId(), DEFAULT_BOUNDARIES, 1, timestamp);
        assertWidget(widgets.get(2), widget3.getId(), DEFAULT_BOUNDARIES, 2, timestamp);
    }

    @Test
    public void should_not_update_widget_when_update_is_not_needed() {
        String id = "test";
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.systemUTC(),
            Duration.ZERO
        );

        Widget widget = storage.createWidget(DEFAULT_PARAMETERS);

        Widget actualWidget = storage
            .updateWidget(widget.getId(), DEFAULT_PARAMETERS)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        assertWidget(actualWidget, widget.getId(), widget.getBoundaries(), widget.getZ(), widget.getModifiedAt());
    }

    @Test
    public void should_delete_widget_when_widget_already_exist() {
        String id1 = "test1";
        String id2 = "test2";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = storage.createWidget(DEFAULT_PARAMETERS);

        storage.deleteWidget(widget1.getId());

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), widget2.getId(), widget2.getBoundaries(), widget2.getZ(), widget2.getModifiedAt());
    }

    @Test
    public void should_do_nothing_while_deletion_when_there_is_no_widget_with_the_given_id() {
        String id = "test";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget = storage.createWidget(DEFAULT_PARAMETERS);

        storage.deleteWidget("other_test");

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        assertWidget(widgets.get(0), widget.getId(), widget.getBoundaries(), widget.getZ(), widget.getModifiedAt());
    }

    @Test
    public void should_return_all_widgets_when_paging_is_used() {
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget2 = storage.createWidget(DEFAULT_PARAMETERS);
        Widget widget3 = storage.createWidget(DEFAULT_PARAMETERS);

        PagedList<Widget> page1 = storage.getWidgets(new WidgetsFilter(null, null, 1));
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

        PagedList<Widget> page2 = storage.getWidgets(new WidgetsFilter(null, page1Cursor, 1));
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

        PagedList<Widget> page3 = storage.getWidgets(new WidgetsFilter(null, page2Cursor, 1));
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
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(
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
        Widget widget2 = storage.createWidget(
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
        Widget widget3 = storage.createWidget(
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

        PagedList<Widget> pagedWidgets = storage.getWidgets(
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
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget1 = storage.createWidget(
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
        Widget widget2 = storage.createWidget(
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
        Widget widget3 = storage.createWidget(
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

        Region searchBoundaries = Region.builder()
            .setX(0)
            .setY(0)
            .setWidth(100)
            .setHeight(150)
            .builder();

        PagedList<Widget> page1 = storage.getWidgets(
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

        PagedList<Widget> page2 = storage.getWidgets(
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
        String id1 = "test1";
        String id2 = "test2";
        String id3 = "test3";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id1, id2, id3),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        storage.createWidget(
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
        storage.createWidget(
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
        storage.createWidget(
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

        Region searchBoundaries = Region.builder()
            .setX(0)
            .setY(0)
            .setWidth(50)
            .setHeight(50)
            .builder();

        PagedList<Widget> page = storage.getWidgets(
            new WidgetsFilter(
                searchBoundaries,
                null,
                1
            )
        );
        Assertions.assertEquals(0, page.getItems().size());
        Assertions.assertFalse(page.getCursor().isPresent());
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
            () -> Assertions.assertEquals(expectedTimestamp, widget.getModifiedAt())
        );
    }

    private static class FixedWidgetIdGenerator implements WidgetIdGenerator {

        private final LinkedList<String> ids;

        private FixedWidgetIdGenerator(String... ids) {
            this.ids = new LinkedList<>(Arrays.asList(ids));
        }

        @Override
        public String generate() {
            return ids.poll();
        }
    }
}
