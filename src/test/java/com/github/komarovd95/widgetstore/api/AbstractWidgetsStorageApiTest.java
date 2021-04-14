package com.github.komarovd95.widgetstore.api;

import com.github.komarovd95.widgetstore.api.common.Point2D;
import com.github.komarovd95.widgetstore.api.common.WidgetDimensions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.util.stream.Stream;

public abstract class AbstractWidgetsStorageApiTest {

    private final TestRestTemplate testRestTemplate;

    protected AbstractWidgetsStorageApiTest(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    @Test
    public void should_return_200_OK_and_create_a_new_widget_when_request_is_valid() {
        CreateWidgetRequest request = new CreateWidgetRequest(
            Point2D.builder()
                .setX(0)
                .setY(0)
                .build(),
            0,
            WidgetDimensions.builder()
                .setWidth(100)
                .setHeight(100)
                .build()
        );

        ResponseEntity<WidgetView> response = testRestTemplate.postForEntity(
            "/api/widgets",
            request,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        WidgetView actualWidget = response.getBody();
        Assertions.assertNotNull(actualWidget);
        WidgetView expectedWidget = WidgetView.builder()
            .setId(actualWidget.getId())
            .setCoordinates(request.getCoordinates())
            .setZIndex(0)
            .setDimensions(request.getDimensions())
            .setModifiedAt(actualWidget.getModifiedAt())
            .build();
        assertWidget(expectedWidget, actualWidget);
    }

    @ParameterizedTest
    @MethodSource("invalidCreationRequests")
    public void should_return_400_Bad_Request_when_creation_request_is_valid(CreateWidgetRequest request) {
        ResponseEntity<Void> response = testRestTemplate.postForEntity(
            "/api/widgets",
            request,
            Void.class
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_return_200_OK_and_update_widget_when_widget_exists() {
        CreateWidgetRequest creationRequest = new CreateWidgetRequest(
            Point2D.builder()
                .setX(0)
                .setY(0)
                .build(),
            0,
            WidgetDimensions.builder()
                .setWidth(100)
                .setHeight(100)
                .build()
        );

        ResponseEntity<WidgetView> creationResponse = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse.getStatusCode());
        WidgetView widget = creationResponse.getBody();
        Assertions.assertNotNull(widget);

        UpdateWidgetRequest request = new UpdateWidgetRequest(
            Point2D.builder()
                .setX(100)
                .setY(100)
                .build(),
            6,
            WidgetDimensions.builder()
                .setWidth(150)
                .setHeight(150)
                .build()
        );

        ResponseEntity<WidgetView> response = testRestTemplate.exchange(
            RequestEntity.put("/api/widgets/{widgetId}", widget.getId())
                .body(request),
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        WidgetView updatedWidget = response.getBody();
        Assertions.assertNotNull(updatedWidget);
        Assertions.assertEquals(widget.getId(), updatedWidget.getId());

        WidgetView expectedWidget = WidgetView.builder()
            .setId(updatedWidget.getId())
            .setCoordinates(request.getCoordinates())
            .setZIndex(6)
            .setDimensions(request.getDimensions())
            .setModifiedAt(updatedWidget.getModifiedAt())
            .build();
        assertWidget(expectedWidget, updatedWidget);
    }

    @Test
    public void should_return_404_Not_Found_when_widget_was_not_found_by_id_for_update() {
        UpdateWidgetRequest request = new UpdateWidgetRequest(
            Point2D.builder()
                .setX(100)
                .setY(100)
                .build(),
            6,
            WidgetDimensions.builder()
                .setWidth(150)
                .setHeight(150)
                .build()
        );

        ResponseEntity<WidgetView> response = testRestTemplate.exchange(
            RequestEntity.put("/api/widgets/{widgetId}", "unknown")
                .body(request),
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateRequests")
    public void should_return_400_Bad_Request_when_update_request_is_invalid(UpdateWidgetRequest request) {
        ResponseEntity<Void> response = testRestTemplate.exchange(
            RequestEntity.put("/api/widgets/{widgetId}", "random")
                .body(request),
            Void.class
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void should_return_204_No_Content_and_delete_widget_when_widget_was_found_by_id() {
        CreateWidgetRequest creationRequest = new CreateWidgetRequest(
            Point2D.builder()
                .setX(0)
                .setY(0)
                .build(),
            0,
            WidgetDimensions.builder()
                .setWidth(100)
                .setHeight(100)
                .build()
        );

        ResponseEntity<WidgetView> creationResponse = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse.getStatusCode());
        WidgetView widget = creationResponse.getBody();
        Assertions.assertNotNull(widget);

        ResponseEntity<Void> response = testRestTemplate.exchange(
            RequestEntity.delete("/api/widgets/{widgetId}", widget.getId())
                .build(),
            Void.class
        );
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_return_204_No_Content_and_do_nothing_when_widget_was_not_found_by_id() {
        ResponseEntity<Void> response = testRestTemplate.exchange(
            RequestEntity.delete("/api/widgets/{widgetId}", "random")
                .build(),
            Void.class
        );
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void should_return_widget_data_when_widget_was_found_by_id() {
        CreateWidgetRequest creationRequest = new CreateWidgetRequest(
            Point2D.builder()
                .setX(0)
                .setY(0)
                .build(),
            0,
            WidgetDimensions.builder()
                .setWidth(100)
                .setHeight(100)
                .build()
        );
        ResponseEntity<WidgetView> creationResponse = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse.getStatusCode());
        WidgetView widget = creationResponse.getBody();
        Assertions.assertNotNull(widget);

        ResponseEntity<WidgetView> response = testRestTemplate.getForEntity(
            "/api/widgets/{widgetId}",
            WidgetView.class,
            widget.getId()
        );
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        WidgetView actualWidget = response.getBody();
        Assertions.assertNotNull(actualWidget);
        assertWidget(widget, actualWidget);
    }

    @Test
    public void should_return_404_Not_Found_when_widget_was_not_found_by_id() {
        ResponseEntity<WidgetView> response = testRestTemplate.getForEntity(
            "/api/widgets/{widgetId}",
            WidgetView.class,
            "random"
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_return_all_widgets_when_request_is_valid() {
        CreateWidgetRequest creationRequest = new CreateWidgetRequest(
            Point2D.builder()
                .setX(0)
                .setY(0)
                .build(),
            null,
            WidgetDimensions.builder()
                .setWidth(100)
                .setHeight(100)
                .build()
        );

        ResponseEntity<WidgetView> creationResponse1 = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse1.getStatusCode());
        WidgetView widget1 = creationResponse1.getBody();
        Assertions.assertNotNull(widget1);

        ResponseEntity<WidgetView> creationResponse2 = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse2.getStatusCode());
        WidgetView widget2 = creationResponse2.getBody();
        Assertions.assertNotNull(widget2);

        ResponseEntity<WidgetView> creationResponse3 = testRestTemplate.postForEntity(
            "/api/widgets",
            creationRequest,
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse3.getStatusCode());
        WidgetView widget3 = creationResponse3.getBody();
        Assertions.assertNotNull(widget3);

        ResponseEntity<WidgetsListView> firstPageResponse = testRestTemplate.getForEntity(
            "/api/widgets?limit={limit}&cursor={cursor}",
            WidgetsListView.class,
            2,
            widget1.getZIndex() - 1
        );
        Assertions.assertEquals(HttpStatus.OK, firstPageResponse.getStatusCode());
        WidgetsListView firstPageWidgets = firstPageResponse.getBody();
        Assertions.assertNotNull(firstPageWidgets);
        Assertions.assertEquals(2, firstPageWidgets.getWidgets().size());
        assertWidget(widget1, firstPageWidgets.getWidgets().get(0));
        assertWidget(widget2, firstPageWidgets.getWidgets().get(1));
        Assertions.assertTrue(firstPageWidgets.getPaging().getHasMore());

        ResponseEntity<WidgetsListView> secondPageResponse = testRestTemplate.getForEntity(
            "/api/widgets?limit={limit}&cursor={cursor}",
            WidgetsListView.class,
            2,
            firstPageWidgets.getPaging().getCursor()
                .orElseGet(() -> Assertions.fail("Cursor expected from the first page"))
        );
        Assertions.assertEquals(HttpStatus.OK, secondPageResponse.getStatusCode());
        WidgetsListView secondPageWidgets = secondPageResponse.getBody();
        Assertions.assertNotNull(secondPageWidgets);
        Assertions.assertEquals(1, secondPageWidgets.getWidgets().size());
        assertWidget(widget3, secondPageWidgets.getWidgets().get(0));
        Assertions.assertFalse(secondPageWidgets.getPaging().getHasMore());
    }

    @Test
    public void should_return_all_widgets_when_request_with_spatial_search_is_valid() {
        ResponseEntity<WidgetView> creationResponse1 = testRestTemplate.postForEntity(
            "/api/widgets",
            new CreateWidgetRequest(
                Point2D.builder()
                    .setX(0)
                    .setY(0)
                    .build(),
                null,
                WidgetDimensions.builder()
                    .setWidth(100)
                    .setHeight(100)
                    .build()
            ),
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse1.getStatusCode());
        WidgetView widget1 = creationResponse1.getBody();
        Assertions.assertNotNull(widget1);

        ResponseEntity<WidgetView> creationResponse2 = testRestTemplate.postForEntity(
            "/api/widgets",
            new CreateWidgetRequest(
                Point2D.builder()
                    .setX(0)
                    .setY(50)
                    .build(),
                null,
                WidgetDimensions.builder()
                    .setWidth(100)
                    .setHeight(100)
                    .build()
            ),
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse2.getStatusCode());
        WidgetView widget2 = creationResponse2.getBody();
        Assertions.assertNotNull(widget2);

        ResponseEntity<WidgetView> creationResponse3 = testRestTemplate.postForEntity(
            "/api/widgets",
            new CreateWidgetRequest(
                Point2D.builder()
                    .setX(50)
                    .setY(50)
                    .build(),
                null,
                WidgetDimensions.builder()
                    .setWidth(100)
                    .setHeight(100)
                    .build()
            ),
            WidgetView.class
        );
        Assertions.assertEquals(HttpStatus.OK, creationResponse3.getStatusCode());
        WidgetView widget3 = creationResponse3.getBody();
        Assertions.assertNotNull(widget3);

        ResponseEntity<WidgetsListView> firstPageResponse = testRestTemplate.getForEntity(
            "/api/widgets?limit={limit}&cursor={cursor}&x={x}&y={y}&width={width}&height={height}",
            WidgetsListView.class,
            1,
            widget1.getZIndex() - 1,
            0,
            0,
            100,
            150
        );
        Assertions.assertEquals(HttpStatus.OK, firstPageResponse.getStatusCode());
        WidgetsListView firstPageWidgets = firstPageResponse.getBody();
        Assertions.assertNotNull(firstPageWidgets);
        Assertions.assertEquals(1, firstPageWidgets.getWidgets().size());
        assertWidget(widget1, firstPageWidgets.getWidgets().get(0));
        Assertions.assertTrue(firstPageWidgets.getPaging().getHasMore());

        ResponseEntity<WidgetsListView> secondPageResponse = testRestTemplate.getForEntity(
            "/api/widgets?limit={limit}&cursor={cursor}&x={x}&y={y}&width={width}&height={height}",
            WidgetsListView.class,
            1,
            firstPageWidgets.getPaging().getCursor()
                .orElseGet(() -> Assertions.fail("Cursor expected from the first page")),
            0,
            0,
            100,
            150
        );
        Assertions.assertEquals(HttpStatus.OK, secondPageResponse.getStatusCode());
        WidgetsListView secondPageWidgets = secondPageResponse.getBody();
        Assertions.assertNotNull(secondPageWidgets);
        Assertions.assertEquals(1, secondPageWidgets.getWidgets().size());
        assertWidget(widget2, secondPageWidgets.getWidgets().get(0));
        Assertions.assertFalse(secondPageWidgets.getPaging().getHasMore());
    }

    private static void assertWidget(WidgetView expected, WidgetView actual) {
        Assertions.assertAll(
            () -> Assertions.assertEquals(expected.getId(), actual.getId()),
            () -> Assertions.assertEquals(expected.getCoordinates().getX(), actual.getCoordinates().getX()),
            () -> Assertions.assertEquals(expected.getCoordinates().getY(), actual.getCoordinates().getY()),
            () -> Assertions.assertEquals(expected.getZIndex(), actual.getZIndex()),
            () -> Assertions.assertEquals(expected.getDimensions().getWidth(), actual.getDimensions().getWidth()),
            () -> Assertions.assertEquals(expected.getDimensions().getHeight(), actual.getDimensions().getHeight()),
            () -> Assertions.assertEquals(
                expected.getModifiedAt().toEpochMilli(),
                actual.getModifiedAt().toEpochMilli()
            )
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> invalidCreationRequests() {
        return Stream.of(
            Arguments.of(
                new CreateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(0)
                        .setHeight(100)
                        .build()
                )
            ),
            Arguments.of(
                new CreateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(-1)
                        .setHeight(100)
                        .build()
                )
            ),
            Arguments.of(
                new CreateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(100)
                        .setHeight(0)
                        .build()
                )
            ),
            Arguments.of(
                new CreateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(100)
                        .setHeight(-1)
                        .build()
                )
            )
        );
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> invalidUpdateRequests() {
        return Stream.of(
            Arguments.of(
                new UpdateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(0)
                        .setHeight(100)
                        .build()
                )
            ),
            Arguments.of(
                new UpdateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(-1)
                        .setHeight(100)
                        .build()
                )
            ),
            Arguments.of(
                new UpdateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(100)
                        .setHeight(0)
                        .build()
                )
            ),
            Arguments.of(
                new UpdateWidgetRequest(
                    Point2D.builder()
                        .setX(0)
                        .setY(0)
                        .build(),
                    0,
                    WidgetDimensions.builder()
                        .setWidth(100)
                        .setHeight(-1)
                        .build()
                )
            )
        );
    }
}
