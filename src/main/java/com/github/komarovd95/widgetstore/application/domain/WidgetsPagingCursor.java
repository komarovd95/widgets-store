package com.github.komarovd95.widgetstore.application.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A cursor that references to the particular page.
 */
public class WidgetsPagingCursor {

    /**
     * A version of this page.
     */
    private final Long version;

    /**
     * A Z-index of the last returned item.
     */
    private final Integer zIndex;

    @JsonCreator
    public WidgetsPagingCursor(@JsonProperty("v") Long version, @JsonProperty("z") Integer zIndex) {
        this.version = Objects.requireNonNull(version, "version");
        this.zIndex = Objects.requireNonNull(zIndex, "zIndex");
    }

    /**
     * @return the version of this page, not null
     */
    @JsonProperty("v")
    public Long getVersion() {
        return version;
    }

    /**
     * @return the Z-index of the last returned item, not null
     */
    @JsonProperty("z")
    public Integer getZIndex() {
        return zIndex;
    }

    @Override
    public String toString() {
        return "WidgetsPagingCursor{" +
            "version=" + version +
            ", zIndex=" + zIndex +
            '}';
    }
}
