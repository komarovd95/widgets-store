package com.github.komarovd95.widgetstore.application.controller;

import com.github.komarovd95.widgetstore.api.*;
import com.github.komarovd95.widgetstore.api.common.Point2D;
import com.github.komarovd95.widgetstore.api.common.WidgetDimensions;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.storage.StoreWidgetParameters;

/**
 * The class with helper methods for the DTOs conversion.
 */
public final class WidgetsApiConverters {

    private WidgetsApiConverters() {
    }

    /**
     * Converts a widget creation request to the internal creation parameters' representation.
     *
     * @param request the request
     * @return the creation parameters. Never returns null
     */
    public static StoreWidgetParameters toParameters(CreateWidgetRequest request) {
        return StoreWidgetParameters.builder()
            .setX(request.getCoordinates().getX())
            .setY(request.getCoordinates().getY())
            .setZ(request.getzIndex().orElse(null))
            .setWidth(request.getDimensions().getWidth())
            .setHeight(request.getDimensions().getHeight())
            .build();
    }

    /**
     * Converts a widget update request to the internal update parameters' representation.
     *
     * @param request the request
     * @return the update parameters. Never returns null
     */
    public static StoreWidgetParameters toParameters(UpdateWidgetRequest request) {
        return StoreWidgetParameters.builder()
            .setX(request.getCoordinates().getX())
            .setY(request.getCoordinates().getY())
            .setZ(request.getzIndex().orElse(null))
            .setWidth(request.getDimensions().getWidth())
            .setHeight(request.getDimensions().getHeight())
            .build();
    }

    /**
     * Converts a widget from the internal representation to the API view.
     *
     * @param widget the internal representation of the widget
     * @return widget's API view. Never returns null
     */
    public static WidgetView toApiView(Widget widget) {
        return WidgetView.builder()
            .setId(widget.getId())
            .setCoordinates(
                Point2D.builder()
                    .setX(widget.getX())
                    .setY(widget.getY())
                    .build()
            )
            .setZIndex(widget.getZ())
            .setDimensions(
                WidgetDimensions.builder()
                    .setWidth(widget.getWidth())
                    .setHeight(widget.getHeight())
                    .build()
            )
            .setModifiedAt(widget.getModifiedAt())
            .build();
    }
}
