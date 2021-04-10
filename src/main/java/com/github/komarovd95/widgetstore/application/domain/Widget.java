package com.github.komarovd95.widgetstore.application.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A widget.
 */
public class Widget {

    /**
     * A widget's identifier.
     */
    private final String id;

    /**
     * An X coordinate.
     */
    private final int x;

    /**
     * An Y coordinate.
     */
    private final int y;

    /**
     * A Z-index.
     */
    private final int z;

    /**
     * A widget's width.
     */
    private final int width;

    /**
     * A widget's height.
     */
    private final int height;

    /**
     * A timestamp of the last modification.
     */
    private final Instant modifiedAt;

    private Widget(String id, int x, int y, int z, int width, int height, Instant modifiedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.modifiedAt = Objects.requireNonNull(modifiedAt, "modifiedAt");
    }

    /**
     * @return the widget's identifier, not null
     */
    public String getId() {
        return id;
    }

    /**
     * @return the X coordinate, not null
     */
    public int getX() {
        return x;
    }

    /**
     * @return the Y coordinate, not null
     */
    public int getY() {
        return y;
    }

    /**
     * @return the Z-index, not null
     */
    public int getZ() {
        return z;
    }

    /**
     * @return the width, not null
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height, not null
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the timestamp, not null
     */
    public Instant getModifiedAt() {
        return modifiedAt;
    }

    @Override
    public String toString() {
        return "Widget{" +
            "id='" + id + '\'' +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", width=" + width +
            ", height=" + height +
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
     * Returns a builder instance based on the given widget instance.
     *
     * @param copy the widget instance to copy from, not null
     * @return the builder instance, not null
     */
    public static Builder builder(Widget copy) {
        Objects.requireNonNull(copy, "copy");
        return new Builder()
            .setId(copy.id)
            .setX(copy.x)
            .setY(copy.y)
            .setZ(copy.z)
            .setWidth(copy.width)
            .setHeight(copy.height)
            .setModifiedAt(copy.modifiedAt);
    }

    /**
     * The builder of {@link Widget}.
     */
    public static class Builder {

        private String id;
        private Integer x;
        private Integer y;
        private Integer z;
        private Integer width;
        private Integer height;
        private Instant modifiedAt;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setX(int x) {
            this.x = x;
            return this;
        }

        public Builder setY(int y) {
            this.y = y;
            return this;
        }

        public Builder setZ(int z) {
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

        public Builder setModifiedAt(Instant modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        public Widget build() {
            return new Widget(
                id,
                x,
                y,
                z,
                width,
                height,
                modifiedAt
            );
        }
    }
}
