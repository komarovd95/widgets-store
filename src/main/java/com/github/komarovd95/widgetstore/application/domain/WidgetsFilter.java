package com.github.komarovd95.widgetstore.application.domain;

import java.util.Optional;

/**
 * A filter for the widgets list.
 */
public class WidgetsFilter {

    /**
     * A cursor that references on the particular page. If null, then the first page will be returned.
     */
    private final WidgetsPagingCursor cursor;

    /**
     * A maximum number of items on the returned paged.
     */
    private final int limit;

    public WidgetsFilter(WidgetsPagingCursor cursor, int limit) {
        this.cursor = cursor;
        this.limit = limit;
    }

    /**
     * @return the optional cursor, not null
     */
    public Optional<WidgetsPagingCursor> getCursor() {
        return Optional.ofNullable(cursor);
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        return "WidgetsFilter{" +
            "cursor=" + cursor +
            ", limit=" + limit +
            '}';
    }
}
