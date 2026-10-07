package com.mindata.hotel.infrastructure.messaging;

import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;
import com.mindata.hotel.infrastructure.common.DateFormats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public record SearchMessage(String searchId, String hotelId, String checkIn, String checkOut, List<Integer> ages) {

    public SearchMessage {
        // Copia defensiva
        ages = ages == null ? null : Collections.unmodifiableList(new ArrayList<>(ages));
    }

    public static SearchMessage from(RegisteredSearch registered) {
        HotelSearch search = registered.search();
        return new SearchMessage(
                registered.id().value(),
                search.hotelId(),
                DateFormats.format(search.checkIn()),
                DateFormats.format(search.checkOut()),
                search.ages());
    }


    public RegisteredSearch toDomain() {
        return new RegisteredSearch(
                new SearchId(searchId),
                new HotelSearch(hotelId, DateFormats.parse(checkIn), DateFormats.parse(checkOut), ages));
    }
}
