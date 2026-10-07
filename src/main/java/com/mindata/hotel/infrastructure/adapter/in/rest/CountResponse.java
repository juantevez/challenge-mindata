package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.domain.model.SearchCount;

/** Respuesta de GET /count. */
public record CountResponse(String searchId, SearchView search, long count) {

    static CountResponse from(SearchCount result) {
        return new CountResponse(
                result.registered().id().value(),
                SearchView.from(result.registered().search()),
                result.count());
    }
}
