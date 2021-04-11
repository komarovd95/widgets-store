package com.github.komarovd95.widgetstore.application.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

/**
 * A properties for in-memory widgets storage.
 */
@ConfigurationProperties(prefix = "widgets-storage.in-memory")
@ConstructorBinding
public class InMemoryWidgetsStorageProperties {

    /**
     * A timeout for the read-write lock acquisition.
     */
    private final Duration lockAcquisitionTimeout;

    /**
     * A maximum number of versions to be stored. This parameter is used to control the size of the versions list.
     */
    private final int maxVersionsToStore;

    public InMemoryWidgetsStorageProperties(Duration lockAcquisitionTimeout, int maxVersionsToStore) {
        this.lockAcquisitionTimeout = lockAcquisitionTimeout;
        this.maxVersionsToStore = maxVersionsToStore;
    }

    public Duration getLockAcquisitionTimeout() {
        return lockAcquisitionTimeout;
    }

    public int getMaxVersionsToStore() {
        return maxVersionsToStore;
    }
}
