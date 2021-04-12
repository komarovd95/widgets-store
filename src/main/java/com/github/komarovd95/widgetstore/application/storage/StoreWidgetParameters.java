package com.github.komarovd95.widgetstore.application.storage;

import com.github.komarovd95.widgetstore.application.domain.Region;

import java.util.Objects;
import java.util.Optional;

/**
 * A parameters that used for storing a widget in the storage.
 */
public class StoreWidgetParameters {

    /**
     * A boundaries.
     */
    private final Region boundaries;

    /**
     * A Z-index. Might be null.
     */
    private final Integer z;

    private StoreWidgetParameters(Region boundaries, Integer z) {
        this.boundaries = Objects.requireNonNull(boundaries, "boundaries");
        this.z = z;
    }

    /**
     * @return the boundaries, not null
     */
    public Region getBoundaries() {
        return boundaries;
    }

    /**
     * @return the optional Z-index, not null
     */
    public Optional<Integer> getZ() {
        return Optional.ofNullable(z);
    }

    @Override
    public String toString() {
        return "StoreWidgetParameters{" +
            "boundaries=" + boundaries +
            ", z=" + z +
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

        private Region boundaries;
        private Integer z;

        private Builder() {
        }

        public Builder setBoundaries(Region boundaries) {
            this.boundaries = boundaries;
            return this;
        }

        public Builder setZ(Integer z) {
            this.z = z;
            return this;
        }

        public StoreWidgetParameters build() {
            return new StoreWidgetParameters(boundaries, z);
        }
    }
}
