package com.github.komarovd95.widgetstore.application.configuration;

import com.github.komarovd95.widgetstore.application.repository.InMemoryWidgetsRepository;
import com.github.komarovd95.widgetstore.application.repository.WidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.transaction.InMemoryTransactionsService;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The configuration for in-memory widgets storage.
 * <p>
 * It activates only with 'in-memory' Spring's profile.
 */
@Configuration
@Profile("in-memory")
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class InMemoryWidgetsStorageConfiguration {

    @Bean
    public TransactionsService inMemoryTransactionsService() {
        return new InMemoryTransactionsService();
    }

    @Bean
    public WidgetsRepository inMemoryWidgetsRepository() {
        return new InMemoryWidgetsRepository();
    }
}
