package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.komarovd95.widgetstore.api.common.Paging;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import liquibase.pro.packaged.J;

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

    @Schema(description = "A metadata for paging", required = true)
    private final Paging paging;

    @JsonCreator
    public WidgetsListView(
        @JsonProperty("widgets") List<WidgetView> widgets,
        @JsonProperty("paging") Paging paging
    ) {
        this.widgets = Collections.unmodifiableList(Objects.requireNonNull(widgets, "widgets"));
        this.paging = Objects.requireNonNull(paging, "paging");
    }

    /**
     * @return the list of widgets, not null
     */
    public List<WidgetView> getWidgets() {
        return widgets;
    }

    /**
     * @return the metadata for paging
     */
    public Paging getPaging() {
        return paging;
    }

    @Override
    public String toString() {
        return "WidgetsListView{" +
            "widgets.size=" + widgets.size() +
            ", paging=" + paging +
            '}';
    }
}
