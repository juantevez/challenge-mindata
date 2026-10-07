package com.mindata.hotel.application.service;

import com.mindata.hotel.application.port.in.PersistSearchUseCase;
import com.mindata.hotel.application.port.out.SearchRepository;
import com.mindata.hotel.domain.model.RegisteredSearch;

import java.util.Objects;

public class PersistSearchService implements PersistSearchUseCase {

    private final SearchRepository repository;

    public PersistSearchService(SearchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void persist(RegisteredSearch search) {
        repository.save(search);
    }
}
