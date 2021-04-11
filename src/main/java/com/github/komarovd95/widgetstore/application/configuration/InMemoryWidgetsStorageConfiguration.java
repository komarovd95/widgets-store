package com.github.komarovd95.widgetstore.application.configuration;

import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.storage.WidgetsStorage;
import com.github.komarovd95.widgetstore.application.storage.inmemory.InMemoryWidgetsStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * The configuration for in-memory widgets storage.
 * <p>
 * It activates only with 'in-memory' Spring's profile.
 */
@Configuration
@Profile("in-memory")
@EnableConfigurationProperties(InMemoryWidgetsStorageProperties.class)
public class InMemoryWidgetsStorageConfiguration {

    @Bean
    public WidgetsStorage inMemoryWidgetsStorage(
        WidgetIdGenerator widgetIdGenerator,
        Clock clock,
        InMemoryWidgetsStorageProperties inMemoryWidgetsStorageProperties
    ) {
        return new InMemoryWidgetsStorage(
            widgetIdGenerator,
            clock,
            inMemoryWidgetsStorageProperties.getLockAcquisitionTimeout(),
            inMemoryWidgetsStorageProperties.getMaxVersionsToStore()
        );
    }
}
