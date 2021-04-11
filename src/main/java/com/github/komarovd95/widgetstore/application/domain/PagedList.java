package com.github.komarovd95.widgetstore.application.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A paged list of items.
 *
 * @param <T> a type of items
 */
public class PagedList<T> {

    /**
     * A list of items on this page.
     */
    private final List<T> items;

    /**
     * A cursor for the next page.
     */
    private final Integer cursor;

    private PagedList(List<T> items, Integer cursor) {
        this.items = Collections.unmodifiableList(items);
        this.cursor = cursor;
    }

    /**
     * @return the list of items, not null
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * @return the optional cursor, not null. Presented only if the next page exists
     */
    public Optional<Integer> getCursor() {
        return Optional.ofNullable(cursor);
    }

    @Override
    public String toString() {
        return "PagedList{" +
            "items.size=" + items.size() +
            ", cursor=" + cursor +
            '}';
    }

    /**
     * Returns a paged list for the last page.
     *
     * @param items the list of items on the last page
     * @param <T> the type of the items
     * @return the paged list
     */
    public static <T> PagedList<T> lastPage(List<T> items) {
        Objects.requireNonNull(items, "items");
        return new PagedList<>(items, null);
    }

    /**
     * Returns a paged list for the non-last page.
     *
     * @param items the list of items on this page
     * @param cursor the cursor for the next page
     * @param <T> the type of items
     * @return the paged list
     */
    public static <T> PagedList<T> nonLastPage(List<T> items, Integer cursor) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(cursor, "cursor");
        return new PagedList<>(items, cursor);
    }
}
