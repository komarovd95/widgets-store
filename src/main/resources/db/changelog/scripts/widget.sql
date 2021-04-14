--liquibase formatted sql

--changeset dkomarov:WIDGET
CREATE TABLE widget
(
    id          IDENTITY  NOT NULL PRIMARY KEY,
    widget_id   VARCHAR   NOT NULL UNIQUE,
    x           INTEGER   NOT NULL,
    y           INTEGER   NOT NULL,
    width       INTEGER   NOT NULL,
    height      INTEGER   NOT NULL,
    z_index     INTEGER   NOT NULL UNIQUE,
    boundaries  GEOMETRY  NOT NULL,
    modified_at TIMESTAMP NOT NULL
);

CREATE SPATIAL INDEX idx_widget_boundaries ON widget(boundaries);