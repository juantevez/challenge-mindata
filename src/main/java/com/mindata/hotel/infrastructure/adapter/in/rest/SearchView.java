package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.infrastructure.common.DateFormats;

import java.util.List;

/** La búsqueda tal como se devuelve en GET /count. */
public record SearchView(String hotelId, String checkIn, String checkOut, List<Integer> ages) {

    public SearchView {
        ages = List.copyOf(ages);
    }

    static SearchView from(HotelSearch search) {
        return new SearchView(
                search.hotelId(),
                DateFormats.format(search.checkIn()),
                DateFormats.format(search.checkOut()),
                search.ages());
    }
}
