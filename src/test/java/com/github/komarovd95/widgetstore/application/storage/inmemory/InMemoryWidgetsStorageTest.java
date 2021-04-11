package com.github.komarovd95.widgetstore.application.storage.inmemory;

import com.github.komarovd95.widgetstore.application.domain.PagedList;
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

    private static final StoreWidgetParameters DEFAULT_PARAMETERS = StoreWidgetParameters.builder()
        .setX(0)
        .setY(0)
        .setWidth(100)
        .setHeight(100)
        .build();

    private static final WidgetsFilter DEFAULT_FILTER = new WidgetsFilter(null, 100);

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
        assertWidget(actualWidget, id, 0, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertSame(actualWidget, widgets.get(0));
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
        assertWidget(actualWidget1, id1, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget1, widget1FoundById);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget2, widget2FoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        Assertions.assertSame(actualWidget1, widgets.get(0));
        Assertions.assertSame(actualWidget2, widgets.get(1));
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
                .setX(0)
                .setY(0)
                .setZ(3)
                .setWidth(100)
                .setHeight(100)
                .build()
        );
        assertWidget(actualWidget, id, 3, timestamp);

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertSame(actualWidget, widgets.get(0));
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
        assertWidget(actualWidget1, id1, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget1, widget1FoundById);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget2, widget2FoundById);

        Widget actualWidget3 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setX(0)
                .setY(0)
                .setZ(0)
                .setWidth(100)
                .setHeight(100)
                .build()
        );
        assertWidget(actualWidget3, id3, 0, timestamp);

        Widget widget3FoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget3, widget3FoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        Assertions.assertSame(actualWidget3, widgets.get(0));
        assertWidget(widgets.get(1), id1, 1, timestamp);
        assertWidget(widgets.get(2), id2, 2, timestamp);
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
        assertWidget(actualWidget1, id1, 0, timestamp);

        Widget widget1FoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget1, widget1FoundById);

        Widget actualWidget2 = storage.createWidget(DEFAULT_PARAMETERS);
        assertWidget(actualWidget2, id2, 1, timestamp);

        Widget widget2FoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget2, widget2FoundById);

        Widget actualWidget3 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setX(0)
                .setY(0)
                .setZ(3)
                .setWidth(100)
                .setHeight(100)
                .build()
        );
        assertWidget(actualWidget3, id3, 3, timestamp);

        Widget widget3FoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget3, widget3FoundById);

        Widget actualWidget4 = storage.createWidget(
            StoreWidgetParameters.builder()
                .setX(0)
                .setY(0)
                .setZ(0)
                .setWidth(100)
                .setHeight(100)
                .build()
        );
        assertWidget(actualWidget4, id4, 0, timestamp);

        Widget widget4FoundById = storage.getWidgetById(id4)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget4, widget4FoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(4, widgets.size());
        Assertions.assertSame(actualWidget4, widgets.get(0));
        assertWidget(widgets.get(1), id1, 1, timestamp);
        assertWidget(widgets.get(2), id2, 2, timestamp);
        Assertions.assertSame(actualWidget3, widgets.get(3));
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

        Widget actualWidget = storage
            .updateWidget(
                widget.getId(),
                StoreWidgetParameters.builder()
                    .setX(100)
                    .setY(100)
                    .setWidth(150)
                    .setHeight(150)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertAll(
            () -> Assertions.assertEquals(id, actualWidget.getId()),
            () -> Assertions.assertEquals(100, actualWidget.getX()),
            () -> Assertions.assertEquals(100, actualWidget.getY()),
            () -> Assertions.assertEquals(0, actualWidget.getZ()),
            () -> Assertions.assertEquals(150, actualWidget.getWidth()),
            () -> Assertions.assertEquals(150, actualWidget.getHeight()),
            () -> Assertions.assertEquals(timestamp, actualWidget.getModifiedAt())
        );

        Widget widgetFoundById = storage.getWidgetById(id)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertSame(actualWidget, widgets.get(0));
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

        Widget actualWidget = storage
            .updateWidget(
                widget1.getId(),
                StoreWidgetParameters.builder()
                    .setX(100)
                    .setY(100)
                    .setWidth(150)
                    .setHeight(150)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertAll(
            () -> Assertions.assertEquals(id1, actualWidget.getId()),
            () -> Assertions.assertEquals(100, actualWidget.getX()),
            () -> Assertions.assertEquals(100, actualWidget.getY()),
            () -> Assertions.assertEquals(2, actualWidget.getZ()),
            () -> Assertions.assertEquals(150, actualWidget.getWidth()),
            () -> Assertions.assertEquals(150, actualWidget.getHeight()),
            () -> Assertions.assertEquals(timestamp, actualWidget.getModifiedAt())
        );

        Widget widgetFoundById = storage.getWidgetById(id1)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(2, widgets.size());
        Assertions.assertSame(widget2, widgets.get(0));
        Assertions.assertSame(actualWidget, widgets.get(1));
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

        Widget actualWidget = storage
            .updateWidget(
                widget3.getId(),
                StoreWidgetParameters.builder()
                    .setX(100)
                    .setY(100)
                    .setZ(0)
                    .setWidth(150)
                    .setHeight(150)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertAll(
            () -> Assertions.assertEquals(id3, actualWidget.getId()),
            () -> Assertions.assertEquals(100, actualWidget.getX()),
            () -> Assertions.assertEquals(100, actualWidget.getY()),
            () -> Assertions.assertEquals(0, actualWidget.getZ()),
            () -> Assertions.assertEquals(150, actualWidget.getWidth()),
            () -> Assertions.assertEquals(150, actualWidget.getHeight()),
            () -> Assertions.assertEquals(timestamp, actualWidget.getModifiedAt())
        );

        Widget widgetFoundById = storage.getWidgetById(id3)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        Assertions.assertSame(actualWidget, widgets.get(0));
        assertWidget(widgets.get(1), widget1.getId(), 1, timestamp);
        assertWidget(widgets.get(2), widget2.getId(), 2, timestamp);
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

        Widget actualWidget = storage
            .updateWidget(
                widget2.getId(),
                StoreWidgetParameters.builder()
                    .setX(100)
                    .setY(100)
                    .setZ(0)
                    .setWidth(150)
                    .setHeight(150)
                    .build()
            )
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertAll(
            () -> Assertions.assertEquals(id2, actualWidget.getId()),
            () -> Assertions.assertEquals(100, actualWidget.getX()),
            () -> Assertions.assertEquals(100, actualWidget.getY()),
            () -> Assertions.assertEquals(0, actualWidget.getZ()),
            () -> Assertions.assertEquals(150, actualWidget.getWidth()),
            () -> Assertions.assertEquals(150, actualWidget.getHeight()),
            () -> Assertions.assertEquals(timestamp, actualWidget.getModifiedAt())
        );

        Widget widgetFoundById = storage.getWidgetById(id2)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(actualWidget, widgetFoundById);

        List<Widget> widgets = storage.getWidgets(DEFAULT_FILTER).getItems();
        Assertions.assertEquals(3, widgets.size());
        Assertions.assertSame(actualWidget, widgets.get(0));
        assertWidget(widgets.get(1), widget1.getId(), 1, timestamp);
        Assertions.assertSame(widget3, widgets.get(2));
    }

    @Test
    public void should_not_update_widget_when_update_is_not_needed() {
        String id = "test";
        Instant timestamp = Instant.now();
        WidgetsStorage storage = new InMemoryWidgetsStorage(
            new FixedWidgetIdGenerator(id),
            Clock.fixed(timestamp, ZoneId.systemDefault()),
            Duration.ZERO
        );

        Widget widget = storage.createWidget(DEFAULT_PARAMETERS);

        Widget actualWidget = storage
            .updateWidget(widget.getId(), DEFAULT_PARAMETERS)
            .orElseGet(() -> Assertions.fail("Widget was not found by ID"));
        Assertions.assertSame(widget, actualWidget);
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
        Assertions.assertSame(widget2, widgets.get(0));
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
        Assertions.assertSame(widget, widgets.get(0));
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

        PagedList<Widget> page1 = storage.getWidgets(new WidgetsFilter(null, 1));
        Assertions.assertEquals(1, page1.getItems().size());
        Assertions.assertSame(widget1, page1.getItems().get(0));
        Integer page1Cursor = page1.getCursor()
            .orElseGet(() -> Assertions.fail("Cursor must be presented for page 1"));
        Assertions.assertEquals(widget1.getZ(), page1Cursor);

        PagedList<Widget> page2 = storage.getWidgets(new WidgetsFilter(page1Cursor, 1));
        Assertions.assertEquals(1, page2.getItems().size());
        Assertions.assertSame(widget2, page2.getItems().get(0));
        Integer page2Cursor = page2.getCursor()
            .orElseGet(() -> Assertions.fail("Cursor must be presented for page 2"));
        Assertions.assertEquals(widget2.getZ(), page2Cursor);

        PagedList<Widget> page3 = storage.getWidgets(new WidgetsFilter(page2Cursor, 1));
        Assertions.assertEquals(1, page3.getItems().size());
        Assertions.assertSame(widget3, page3.getItems().get(0));
        Assertions.assertFalse(page3.getCursor().isPresent());
    }

    private static void assertWidget(Widget widget, String expectedId, int expectedZIndex, Instant expectedTimestamp) {
        Assertions.assertAll(
            () -> Assertions.assertEquals(expectedId, widget.getId()),
            () -> Assertions.assertEquals(0, widget.getX()),
            () -> Assertions.assertEquals(0, widget.getY()),
            () -> Assertions.assertEquals(expectedZIndex, widget.getZ()),
            () -> Assertions.assertEquals(100, widget.getWidth()),
            () -> Assertions.assertEquals(100, widget.getHeight()),
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
