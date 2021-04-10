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

    public InMemoryWidgetsStorageProperties(Duration lockAcquisitionTimeout) {
        this.lockAcquisitionTimeout = lockAcquisitionTimeout;
    }

    public Duration getLockAcquisitionTimeout() {
        return lockAcquisitionTimeout;
    }
}
