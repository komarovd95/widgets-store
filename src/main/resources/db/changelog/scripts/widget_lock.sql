--liquibase formatted sql

--changeset dkomarov:WIDGET-LOCK
CREATE TABLE widget_lock
(
    lock VARCHAR NOT NULL PRIMARY KEY
);