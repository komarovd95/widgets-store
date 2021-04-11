package com.github.komarovd95.widgetstore.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;
import java.util.Optional;

/**
 * A metadata for paged response.
 */
@Schema(description = "A metadata for paged response")
public class Paging {

    @Schema(description = "A flag indicates that the next page can be requested", required = true)
    private final Boolean hasMore;

    @Schema(
        description = "A cursor that can be used to request the next page. Cursor is present only if hasMore == true"
    )
    private final String cursor;

    private Paging(Boolean hasMore, String cursor) {
        this.hasMore = Objects.requireNonNull(hasMore, "hasMore");
        this.cursor = cursor;
    }

    /**
     * @return the flag, not null
     */
    public Boolean getHasMore() {
        return hasMore;
    }

    /**
     * @return the optional cursor, not null
     */
    public Optional<String> getCursor() {
        return Optional.ofNullable(cursor);
    }

    @Override
    public String toString() {
        return "Paging{" +
            "hasMore=" + hasMore +
            ", cursor='" + cursor + '\'' +
            '}';
    }

    /**
     * Returns a paging instance for the last page (hasMore == false, cursor == null).
     *
     * @return the paging instance
     */
    public static Paging forLastPage() {
        return new Paging(false, null);
    }

    /**
     * Returns a paging instance for the non-last page (hasMore == true, cursor != null).
     *
     * @param cursor the cursor for the next page, not null
     * @return the paging instance
     */
    public static Paging forNonLastPage(String cursor) {
        Objects.requireNonNull(cursor, "cursor");
        return new Paging(true, cursor);
    }
}
