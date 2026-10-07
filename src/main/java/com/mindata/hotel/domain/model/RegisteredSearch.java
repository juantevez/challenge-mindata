package com.mindata.hotel.domain.model;

import java.util.Objects;

/** Una búsqueda ya identificada (lo que viaja por Kafka y se persiste). */
public record RegisteredSearch(SearchId id, HotelSearch search) {

    public RegisteredSearch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(search, "search");
    }
}
