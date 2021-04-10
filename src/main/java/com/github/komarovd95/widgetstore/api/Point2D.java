package com.github.komarovd95.widgetstore.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * A representation of the point on the 2D plane.
 */
@Schema(description = "A representation of the point on the 2D plane")
public class Point2D {

    /**
     * An X coordinate.
     */
    @NotNull
    @Schema(description = "An X coordinate", required = true)
    private final Integer x;

    /**
     * A Y coordinate.
     */
    @NotNull
    @Schema(description = "A Y coordinate", required = true)
    private final Integer y;

    @JsonCreator
    private Point2D(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return the X coordinate, not null
     */
    public Integer getX() {
        return x;
    }

    /**
     * @return the Y coordinate, not null
     */
    public Integer getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Point2DView{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }

    /**
     * @return an empty builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder of {@link Point2D}.
     */
    public static class Builder {

        private Integer x;
        private Integer y;

        private Builder() {
        }

        public Builder setX(int x) {
            this.x = x;
            return this;
        }

        public Builder setY(int y) {
            this.y = y;
            return this;
        }

        public Point2D build() {
            return new Point2D(
                Objects.requireNonNull(x, "x"),
                Objects.requireNonNull(y, "y")
            );
        }
    }
}
