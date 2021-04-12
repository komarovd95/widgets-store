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
     * A widget's boundaries.
     */
    private final Region boundaries;

    /**
     * A Z-index.
     */
    private final int z;

    /**
     * A timestamp of the last modification.
     */
    private final Instant modifiedAt;

    private Widget(String id, Region boundaries, int z, Instant modifiedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.boundaries = Objects.requireNonNull(boundaries, "boundaries");
        this.z = z;
        this.modifiedAt = Objects.requireNonNull(modifiedAt, "modifiedAt");
    }

    /**
     * @return the widget's identifier, not null
     */
    public String getId() {
        return id;
    }

    /**
     * @return the widget's boundaries, not null
     */
    public Region getBoundaries() {
        return boundaries;
    }

    /**
     * @return the Z-index, not null
     */
    public int getZ() {
        return z;
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
            ", boundaries=" + boundaries +
            ", z=" + z +
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
            .setBoundaries(copy.boundaries)
            .setZ(copy.z)
            .setModifiedAt(copy.modifiedAt);
    }

    /**
     * The builder of {@link Widget}.
     */
    public static class Builder {

        private String id;
        private Region boundaries;
        private Integer z;
        private Instant modifiedAt;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setBoundaries(Region boundaries) {
            this.boundaries = boundaries;
            return this;
        }

        public Builder setZ(int z) {
            this.z = z;
            return this;
        }

        public Builder setModifiedAt(Instant modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        public Widget build() {
            return new Widget(
                id,
                boundaries,
                z,
                modifiedAt
            );
        }
    }
}
