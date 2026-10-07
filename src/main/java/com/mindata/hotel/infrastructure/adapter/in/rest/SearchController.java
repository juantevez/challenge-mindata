package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.application.port.in.CountSearchesUseCase;
import com.mindata.hotel.application.port.in.RegisterSearchUseCase;
import com.mindata.hotel.domain.model.SearchId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada REST. */
@RestController
public class SearchController {

    private final RegisterSearchUseCase registerSearch;
    private final CountSearchesUseCase countSearches;

    public SearchController(RegisterSearchUseCase registerSearch, CountSearchesUseCase countSearches) {
        this.registerSearch = registerSearch;
        this.countSearches = countSearches;
    }

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        SearchId id = registerSearch.register(request.toDomain());
        return new SearchResponse(id.value());
    }

    @GetMapping("/count")
    public CountResponse count(@RequestParam("searchId") @NotBlank @Size(max = 64) String searchId) {
        return CountResponse.from(countSearches.count(new SearchId(searchId)));
    }
}
