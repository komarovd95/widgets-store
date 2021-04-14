package com.github.komarovd95.widgetstore.application;

import com.github.komarovd95.widgetstore.api.AbstractWidgetsStorageApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("database")
public class DatabaseWidgetsStorageApiTest extends AbstractWidgetsStorageApiTest {

    @BeforeAll
    public static void beforeAll(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("TRUNCATE TABLE widget", Collections.emptyMap());
    }

    @Autowired
    public DatabaseWidgetsStorageApiTest(TestRestTemplate testRestTemplate) {
        super(testRestTemplate);
    }
}
