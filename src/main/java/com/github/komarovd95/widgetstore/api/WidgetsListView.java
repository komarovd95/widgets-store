package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A list of widgets.
 */
@Schema(description = "A list of widgets")
public class WidgetsListView {

    /**
     * A list of widgets.
     */
    @ArraySchema(
        arraySchema = @Schema(description = "A list of widgets", required = true)
    )
    private final List<WidgetView> widgets;

    @JsonCreator
    public WidgetsListView(List<WidgetView> widgets) {
        this.widgets = Collections.unmodifiableList(Objects.requireNonNull(widgets, "widgets"));
    }

    /**
     * @return the list of widgets, not null
     */
    public List<WidgetView> getWidgets() {
        return widgets;
    }

    @Override
    public String toString() {
        return "WidgetsListView{" +
            "widgets.size=" + widgets.size() +
            '}';
    }
}
