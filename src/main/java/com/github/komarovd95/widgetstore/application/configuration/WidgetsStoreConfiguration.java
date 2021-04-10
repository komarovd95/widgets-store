package com.github.komarovd95.widgetstore.application.configuration;

import com.github.komarovd95.widgetstore.application.service.generator.UuidWidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The configuration for the application.
 */
@Configuration
public class WidgetsStoreConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public WidgetIdGenerator widgetIdGenerator() {
        return new UuidWidgetIdGenerator();
    }
}
