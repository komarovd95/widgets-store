package com.github.komarovd95.widgetstore.application.storage;

import java.util.Optional;

/**
 * A parameters that used for storing a widget in the storage.
 */
public class StoreWidgetParameters {

    /**
     * An X coordinate.
     */
    private final int x;

    /**
     * A Y coordinate.
     */
    private final int y;

    /**
     * A Z-index. Might be null.
     */
    private final Integer z;

    /**
     * A width.
     */
    private final int width;

    /**
     * A height.
     */
    private final int height;

    private StoreWidgetParameters(int x, int y, Integer z, int width, int height) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
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

    /**
     * @return the optional Z-index, not null
     */
    public Optional<Integer> getZ() {
        return Optional.ofNullable(z);
    }

    /**
     * @return the width, not null
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Returns the height of widget.
     *
     * @return the height, not null
     */
    public Integer getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "StoreWidgetParameters{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", width=" + width +
            ", height=" + height +
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
     * A builder of {@link StoreWidgetParameters}.
     */
    public static class Builder {

        private Integer x;
        private Integer y;
        private Integer z;
        private Integer width;
        private Integer height;

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

        public Builder setZ(Integer z) {
            this.z = z;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public StoreWidgetParameters build() {
            return new StoreWidgetParameters(x, y, z, width, height);
        }
    }
}
