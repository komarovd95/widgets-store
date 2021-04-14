package com.github.komarovd95.widgetstore.application.configuration;

import com.github.komarovd95.widgetstore.application.repository.H2DatabaseWidgetsRepository;
import com.github.komarovd95.widgetstore.application.repository.WidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.transaction.DatabaseTransactionsService;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
import com.github.komarovd95.widgetstore.application.service.transaction.locks.ExclusiveLock;
import com.github.komarovd95.widgetstore.application.service.transaction.locks.H2DatabaseWidgetExclusiveLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The configuration for in-memory widgets storage.
 * <p>
 * It activates only with 'database' Spring's profile.
 */
@Configuration
@Profile("database")
public class DatabaseWidgetsStorageConfiguration {

    /**
     * A widget table lock name.
     */
    private static final String LOCK_NAME = "widget";

    @Bean
    public ExclusiveLock databaseWidgetLock(NamedParameterJdbcTemplate jdbcTemplate) {
        return new H2DatabaseWidgetExclusiveLock(jdbcTemplate, LOCK_NAME);
    }

    @Bean
    public TransactionsService databaseTransactionsService(
        TransactionTemplate transactionTemplate,
        ExclusiveLock databaseWidgetLock
    ) {
        return new DatabaseTransactionsService(transactionTemplate, databaseWidgetLock);
    }

    @Bean
    public WidgetsRepository h2DatabaseWidgetsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new H2DatabaseWidgetsRepository(jdbcTemplate);
    }
}
