package com.mindata.hotel.application.service;

import com.mindata.hotel.application.port.in.CountSearchesUseCase;
import com.mindata.hotel.application.port.out.SearchRepository;
import com.mindata.hotel.domain.exception.SearchNotFoundException;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchCount;
import com.mindata.hotel.domain.model.SearchId;

import java.util.Objects;

public class CountSearchesService implements CountSearchesUseCase {

    private final SearchRepository repository;

    public CountSearchesService(SearchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public SearchCount count(SearchId searchId) {
        RegisteredSearch registered = repository.findById(searchId)
                .orElseThrow(() -> new SearchNotFoundException(searchId));
        long count = repository.countByFingerprint(registered.search().fingerprint());
        return new SearchCount(registered, count);
    }
}
