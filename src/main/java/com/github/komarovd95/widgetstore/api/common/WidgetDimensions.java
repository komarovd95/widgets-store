package com.github.komarovd95.widgetstore.api.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Objects;

/**
 * A widget's dimensions.
 */
@Schema(description = "A widget's dimensions")
public class WidgetDimensions {

    /**
     * A widget's width.
     */
    @NotNull
    @Positive
    @Schema(description = "A widget's width", required = true, minimum = "1")
    private final Integer width;

    /**
     * A widget's height.
     */
    @NotNull
    @Positive
    @Schema(description = "A widget's height", required = true, minimum = "1")
    private final Integer height;

    @JsonCreator
    private WidgetDimensions(@JsonProperty("width") Integer width, @JsonProperty("height") Integer height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @return the widget's width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * @return the widget's height
     */
    public Integer getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "WidgetDimensions{" +
            "width=" + width +
            ", height=" + height +
            '}';
    }

    /**
     * @return an empty builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link WidgetDimensions}.
     */
    public static class Builder {

        private Integer width;
        private Integer height;

        private Builder() {
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public WidgetDimensions build() {
            return new WidgetDimensions(
                Objects.requireNonNull(width, "width"),
                Objects.requireNonNull(height, "height")
            );
        }
    }
}
