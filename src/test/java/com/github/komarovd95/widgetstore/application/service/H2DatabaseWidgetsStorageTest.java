package com.github.komarovd95.widgetstore.application.service;

import com.github.komarovd95.widgetstore.application.repository.WidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.generator.UuidWidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

@SpringBootTest
@ActiveProfiles("database")
public class H2DatabaseWidgetsStorageTest extends AbstractWidgetsServiceTest {

    @Autowired
    private TransactionsService transactionsService;

    @Autowired
    private WidgetsRepository widgetsRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("TRUNCATE TABLE widget", Collections.emptyMap());
    }

    @Override
    protected WidgetsService getService() {
        return new WidgetsService(
            transactionsService,
            widgetsRepository,
            new UuidWidgetIdGenerator(),
            new UniqueClock()
        );
    }
}
