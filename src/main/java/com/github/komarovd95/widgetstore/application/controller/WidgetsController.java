package com.github.komarovd95.widgetstore.application.controller;

import com.github.komarovd95.widgetstore.api.CreateWidgetRequest;
import com.github.komarovd95.widgetstore.api.UpdateWidgetRequest;
import com.github.komarovd95.widgetstore.api.WidgetView;
import com.github.komarovd95.widgetstore.api.WidgetsListView;
import com.github.komarovd95.widgetstore.api.common.Paging;
import com.github.komarovd95.widgetstore.application.domain.PagedList;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.domain.WidgetsFilter;
import com.github.komarovd95.widgetstore.application.storage.WidgetsStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/widgets")
@Tag(name = "Widgets API", description = "REST API for interaction with widgets")
public class WidgetsController {

    private static final String DEFAULT_LIMIT = "10";
    private static final int MAX_LIMIT = 500;

    private final WidgetsStorage widgetsStorage;

    @Autowired
    public WidgetsController(WidgetsStorage widgetsStorage) {
        this.widgetsStorage = widgetsStorage;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        operationId = "CreateWidget",
        summary = "Creates a new widget",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Widget has been created successfully",
                content = @Content(
                    schema = @Schema(implementation = WidgetView.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Request is malformed"
            )
        }
    )
    public ResponseEntity<WidgetView> createWidget(@Valid @RequestBody CreateWidgetRequest request) {
        Widget widget = widgetsStorage.createWidget(WidgetsApiConverters.toParameters(request));
        return ResponseEntity.ok(WidgetsApiConverters.toApiView(widget));
    }

    @PutMapping(
        value = "/{widgetId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        operationId = "UpdateWidget",
        summary = "Updates an existing widget",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Widget has been updated successfully",
                content = @Content(
                    schema = @Schema(implementation = WidgetView.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Request is malformed"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Widget was not found by given ID",
                content = @Content()
            )
        }
    )
    public ResponseEntity<WidgetView> updateWidget(
        @PathVariable("widgetId") String widgetId,
        @Valid @RequestBody UpdateWidgetRequest request
    ) {
        Optional<Widget> optionalWidget = widgetsStorage.updateWidget(
            widgetId,
            WidgetsApiConverters.toParameters(request)
        );
        return optionalWidget
            .map(widget -> ResponseEntity.ok(WidgetsApiConverters.toApiView(widget)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{widgetId}")
    @Operation(
        operationId = "DeleteWidget",
        summary = "Deletes an existing widget",
        responses = @ApiResponse(
            responseCode = "204",
            description = "Widget has been deleted successfully",
            content = @Content()
        )
    )
    public ResponseEntity<Void> deleteWidget(@PathVariable("widgetId") String widgetId) {
        widgetsStorage.deleteWidget(widgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{widgetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        operationId = "GetWidgetById",
        summary = "Returns an existing widget by given ID",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Widget was found",
                content = @Content(
                    schema = @Schema(implementation = WidgetView.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Widget was not found by given ID",
                content = @Content()
            )
        }
    )
    public ResponseEntity<WidgetView> getWidgetById(@PathVariable("widgetId") String widgetId) {
        Optional<Widget> optionalWidget = widgetsStorage.getWidgetById(widgetId);
        return optionalWidget
            .map(widget -> ResponseEntity.ok(WidgetsApiConverters.toApiView(widget)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        operationId = "GetAllWidgets",
        summary = "Returns all existing widgets",
        responses = @ApiResponse(
            responseCode = "200",
            description = "Widgets were found",
            content = @Content(
                schema = @Schema(implementation = WidgetsListView.class)
            )
        )
    )
    public ResponseEntity<WidgetsListView> getWidgets(
        @RequestParam(name = "limit", defaultValue = DEFAULT_LIMIT, required = false)
        @Parameter(
            description = "A maximum number of widgets that will be returned on the page",
            schema = @Schema(defaultValue = DEFAULT_LIMIT)
        )
        @Positive
        @Max(MAX_LIMIT)
            Integer limit,
        @RequestParam(name = "cursor", required = false)
        @Parameter(description = "A cursor of the page. If not presented, then the first page will be returned")
            String cursor
    ) {
        PagedList<Widget> pageWidgets = widgetsStorage.getWidgets(
            new WidgetsFilter(
                cursor != null ? Integer.parseInt(cursor) : null,
                limit
            )
        );
        return ResponseEntity.ok(
            new WidgetsListView(
                pageWidgets.getItems()
                    .stream()
                    .map(WidgetsApiConverters::toApiView)
                    .collect(Collectors.toList()),
                pageWidgets.getCursor()
                    .map(nextPageCursor -> Paging.forNonLastPage(nextPageCursor.toString()))
                    .orElseGet(Paging::forLastPage)
            )
        );
    }
}
