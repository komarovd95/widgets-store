package com.github.komarovd95.widgetstore.application.service.generator;

import java.util.UUID;

/**
 * A simple ID generator that based on {@link UUID}.
 */
public class UuidWidgetIdGenerator implements WidgetIdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
