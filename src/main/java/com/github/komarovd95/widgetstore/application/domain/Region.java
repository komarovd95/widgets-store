package com.github.komarovd95.widgetstore.application.domain;

import java.util.Objects;

/**
 * A rectangular region on the 2D plane.
 */
public class Region {

    /**
     * An X coordinate of the lower left corner.
     */
    private final int x;

    /**
     * An Y coordinate of the lower left corner.
     */
    private final int y;

    /**
     * A width of the rectangle.
     */
    private final int width;

    /**
     * A height of the rectangle.
     */
    private final int height;

    private Region(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * @return the X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * @return the Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Region{" +
            "x=" + x +
            ", y=" + y +
            ", width=" + width +
            ", height=" + height +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Region region = (Region) obj;
        return x == region.x && y == region.y && width == region.width && height == region.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
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
     * A builder of {@link Region}.
     */
    public static class Builder {

        private Integer x;
        private Integer y;
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

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Region builder() {
            return new Region(x, y, width, height);
        }
    }
}
