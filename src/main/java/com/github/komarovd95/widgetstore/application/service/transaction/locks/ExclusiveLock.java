package com.github.komarovd95.widgetstore.application.service.transaction.locks;

/**
 * An exclusive lock.
 */
public interface ExclusiveLock {

    /**
     * Acquires this lock.
     *
     * @return an AutoCloseable for usage in the try-with-resources
     */
    AutoCloseable acquire();
}
