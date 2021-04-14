package com.github.komarovd95.widgetstore.application.service;

import com.github.komarovd95.widgetstore.application.repository.InMemoryWidgetsRepository;
import com.github.komarovd95.widgetstore.application.service.generator.UuidWidgetIdGenerator;
import com.github.komarovd95.widgetstore.application.service.transaction.InMemoryTransactionsService;

public class InMemoryWidgetsStorageTest extends AbstractWidgetsServiceTest {
    @Override
    protected WidgetsService getService() {
        return new WidgetsService(
            new InMemoryTransactionsService(),
            new InMemoryWidgetsRepository(),
            new UuidWidgetIdGenerator(),
            new UniqueClock()
        );
    }
}
