package com.github.komarovd95.widgetstore.application.service.transaction.locks;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * An exclusive lock that uses WIDGET_LOCK table to achieve explicit access to the guarded resources..
 */
public class H2DatabaseWidgetExclusiveLock implements ExclusiveLock {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final String name;

    public H2DatabaseWidgetExclusiveLock(NamedParameterJdbcTemplate jdbcTemplate, String name) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Inits this lock.
     */
    @PostConstruct
    public void init() {
        jdbcTemplate.update(
            "MERGE INTO widget_lock USING DUAL ON lock = :lock_name " +
                "WHEN NOT MATCHED THEN INSERT (lock) VALUES (:lock_name)",
            new MapSqlParameterSource()
                .addValue("lock_name", name)
        );
    }

    /**
     * Acquires this lock.
     *
     * @return an AutoCloseable for usage in the try-with-resources
     */
    @Override
    public AutoCloseable acquire() {
        jdbcTemplate.queryForObject(
            "SELECT 1 FROM widget_lock WHERE lock = :lock_name FOR UPDATE",
            new MapSqlParameterSource()
                .addValue("lock_name", name),
            Integer.class
        );
        return () -> {};
    }
}
