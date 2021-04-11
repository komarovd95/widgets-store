package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.komarovd95.widgetstore.api.common.Point2D;
import com.github.komarovd95.widgetstore.api.common.WidgetDimensions;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

/**
 * A widget.
 */
@Schema(description = "A widget")
public class WidgetView {

    @Schema(description = "An unique identifier of the widget", required = true)
    private final String id;

    @Schema(description = "Coordinates", required = true)
    private final Point2D coordinates;

    @Schema(description = "A Z-index", required = true)
    private final Integer zIndex;

    @Schema(description = "Dimensions", required = true)
    private final WidgetDimensions dimensions;

    @Schema(description = "A timestamp of the last modification", required = true)
    private final Instant modifiedAt;

    @JsonCreator
    private WidgetView(
        @JsonProperty("id") String id,
        @JsonProperty("coordinates") Point2D coordinates,
        @JsonProperty("zIndex") Integer zIndex,
        @JsonProperty("dimensions") WidgetDimensions dimensions,
        @JsonProperty("modifiedAt") Instant modifiedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.coordinates = Objects.requireNonNull(coordinates, "coordinates");
        this.zIndex = Objects.requireNonNull(zIndex, "zIndex");
        this.dimensions = Objects.requireNonNull(dimensions, "dimensions");
        this.modifiedAt = Objects.requireNonNull(modifiedAt, "modifiedAt");
    }

    /**
     * @return the widget's identifier, not null
     */
    public String getId() {
        return id;
    }

    /**
     * @return the coordinates, not null
     */
    public Point2D getCoordinates() {
        return coordinates;
    }

    /**
     * @return the Z-index, not null
     */
    public Integer getzIndex() {
        return zIndex;
    }

    /**
     * @return the dimensions, not null
     */
    public WidgetDimensions getDimensions() {
        return dimensions;
    }

    /**
     * @return the timestamp of the last modification, not  null
     */
    public Instant getModifiedAt() {
        return modifiedAt;
    }

    @Override
    public String toString() {
        return "WidgetView{" +
            "id='" + id + '\'' +
            ", coordinates=" + coordinates +
            ", zIndex=" + zIndex +
            ", dimensions=" + dimensions +
            ", modifiedAt=" + modifiedAt +
            '}';
    }

    /**
     * Returns an empty builder instance.
     *
     * @return the empty builder instance, not null
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder of {@link WidgetView}.
     */
    public static class Builder {

        private String id;
        private Point2D coordinates;
        private Integer zIndex;
        private WidgetDimensions dimensions;
        private Instant modifiedAt;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setCoordinates(Point2D coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        public Builder setZIndex(Integer zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public Builder setDimensions(WidgetDimensions dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder setModifiedAt(Instant modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        public WidgetView build() {
            return new WidgetView(
                id,
                coordinates,
                zIndex,
                dimensions,
                modifiedAt
            );
        }
    }
}
