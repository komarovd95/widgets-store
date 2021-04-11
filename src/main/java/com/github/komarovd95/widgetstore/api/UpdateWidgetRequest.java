package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.komarovd95.widgetstore.api.common.Point2D;
import com.github.komarovd95.widgetstore.api.common.WidgetDimensions;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * A body of the widget update request.
 */
@Schema(description = "A body of the widget update request")
public class UpdateWidgetRequest {

    @NotNull
    @Valid
    @Schema(description = "New coordinates of the updating widget", required = true)
    private final Point2D coordinates;

    @Schema(
        description = "New Z-index of the updating widget. If it isn't present, then the new widget will be placed " +
            "in the foreground"
    )
    private final Integer zIndex;

    @NotNull
    @Valid
    @Schema(description = "New dimensions of the creating widget", required = true)
    private final WidgetDimensions dimensions;

    @JsonCreator
    public UpdateWidgetRequest(
        @JsonProperty("coordinates") Point2D coordinates,
        @JsonProperty("zIndex") Integer zIndex,
        @JsonProperty("dimensions") WidgetDimensions dimensions
    ) {
        this.coordinates = coordinates;
        this.zIndex = zIndex;
        this.dimensions = dimensions;
    }

    /**
     * @return the coordinates, not null
     */
    public Point2D getCoordinates() {
        return coordinates;
    }

    /**
     * @return the optional Z-index, not null
     */
    public Optional<Integer> getzIndex() {
        return Optional.ofNullable(zIndex);
    }

    /**
     * @return the dimensions, not null
     */
    public WidgetDimensions getDimensions() {
        return dimensions;
    }

    @Override
    public String toString() {
        return "UpdateWidgetRequest{" +
            "coordinates=" + coordinates +
            ", zIndex=" + zIndex +
            ", dimensions=" + dimensions +
            '}';
    }
}
