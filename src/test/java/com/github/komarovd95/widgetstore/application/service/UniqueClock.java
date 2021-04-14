package com.github.komarovd95.widgetstore.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class UniqueClock extends Clock {

    private final Instant timestamp = Instant.now();
    private int ticks = 0;

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return timestamp.plusMillis(ticks++);
    }
}
