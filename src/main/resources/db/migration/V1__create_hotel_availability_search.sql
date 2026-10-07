-- Compatible con Oracle 19c/23ai y con H2 en modo Oracle.
CREATE TABLE hotel_availability_search (
    search_id   VARCHAR2(36)   NOT NULL,
    hotel_id    VARCHAR2(256)  NOT NULL,
    check_in    DATE           NOT NULL,
    check_out   DATE           NOT NULL,
    ages        VARCHAR2(512)  NOT NULL,
    search_hash VARCHAR2(64)   NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT pk_hotel_availability_search PRIMARY KEY (search_id)
);

-- /count busca por hash (hotel + fechas + edades normalizadas).
CREATE INDEX ix_has_search_hash ON hotel_availability_search (search_hash);
