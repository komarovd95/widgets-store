package com.github.komarovd95.widgetstore.application.service.generator;

/**
 * A generator of unique widgets' identifiers.
 */
public interface WidgetIdGenerator {

    /**
     * Generates a unique identifier.
     *
     * The implementation of this method MUST be thread-safe.
     *
     * @return the unique identifier
     */
    String generate();
}
