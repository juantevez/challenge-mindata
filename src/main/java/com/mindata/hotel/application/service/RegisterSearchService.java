package com.mindata.hotel.application.service;

import com.mindata.hotel.application.port.in.RegisterSearchUseCase;
import com.mindata.hotel.application.port.out.SearchEventPublisher;
import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;

import java.util.Objects;

public class RegisterSearchService implements RegisterSearchUseCase {

    private final SearchEventPublisher publisher;

    public RegisterSearchService(SearchEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public SearchId register(HotelSearch search) {
        SearchId id = SearchId.generate();
        publisher.publish(new RegisteredSearch(id, search));
        return id;
    }
}
