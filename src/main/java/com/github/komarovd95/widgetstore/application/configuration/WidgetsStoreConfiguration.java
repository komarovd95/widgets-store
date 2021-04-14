package com.github.komarovd95.widgetstore.application.configuration;

import com.github.komarovd95.widgetstore.application.repository.WidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.WidgetsService;
import com.github.komarovd95.widgetstore.application.service.generator.UuidWidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.generator.WidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
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

    @Bean
    public WidgetsService widgetsService(
        TransactionsService transactionsService,
        WidgetsRepository widgetsRepository,
        WidgetIdGenerator widgetIdGenerator,
        Clock clock
    ) {
        return new WidgetsService(transactionsService, widgetsRepository, widgetIdGenerator, clock);
    }
}
