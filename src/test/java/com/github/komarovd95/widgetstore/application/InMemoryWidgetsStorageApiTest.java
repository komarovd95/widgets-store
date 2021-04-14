package com.github.komarovd95.widgetstore.application;

import com.github.komarovd95.widgetstore.api.AbstractWidgetsStorageApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("in-memory")
public class InMemoryWidgetsStorageApiTest extends AbstractWidgetsStorageApiTest {

    @Autowired
    public InMemoryWidgetsStorageApiTest(TestRestTemplate testRestTemplate) {
        super(testRestTemplate);
    }
}
