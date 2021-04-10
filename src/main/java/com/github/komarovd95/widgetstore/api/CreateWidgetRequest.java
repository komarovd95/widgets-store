package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * A body of the widget creation request.
 */
@Schema(description = "A body of the widget creation request")
public class CreateWidgetRequest {

    @NotNull
    @Valid
    @Schema(description = "Coordinates of the creating widget", required = true)
    private final Point2D coordinates;

    @Schema(
        description = "Z-index of the creating widget. If it isn't present, then the new widget will be placed " +
            "in the foreground"
    )
    private final Integer zIndex;

    @NotNull
    @Valid
    @Schema(description = "Dimensions of the creating widget", required = true)
    private final WidgetDimensions dimensions;

    @JsonCreator
    public CreateWidgetRequest(Point2D coordinates, Integer zIndex, WidgetDimensions dimensions) {
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
        return "CreateWidgetRequest{" +
            "coordinates=" + coordinates +
            ", zIndex=" + zIndex +
            ", dimensions=" + dimensions +
            '}';
    }
}
