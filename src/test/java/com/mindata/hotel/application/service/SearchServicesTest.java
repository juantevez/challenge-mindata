package com.mindata.hotel.application.service;

import com.mindata.hotel.application.exception.SearchPublicationException;
import com.mindata.hotel.application.port.out.SearchEventPublisher;
import com.mindata.hotel.application.port.out.SearchRepository;
import com.mindata.hotel.domain.exception.SearchNotFoundException;
import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchCount;
import com.mindata.hotel.domain.model.SearchId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServicesTest {

    private static final HotelSearch SEARCH =
            new HotelSearch("1234aBc", LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18), List.of(30, 29, 1, 3));

    @Mock
    SearchEventPublisher publisher;

    @Mock
    SearchRepository repository;

    @Test
    void registerPublishesTheSearchWithAFreshIdAndReturnsIt() {
        SearchId id = new RegisterSearchService(publisher).register(SEARCH);

        ArgumentCaptor<RegisteredSearch> published = ArgumentCaptor.forClass(RegisteredSearch.class);
        verify(publisher).publish(published.capture());
        assertAll(
                () -> assertThat(published.getValue().id()).isEqualTo(id),
                () -> assertThat(published.getValue().search()).isEqualTo(SEARCH));
    }

    @Test
    void registerGeneratesADifferentIdForEveryInvocation() {
        RegisterSearchService service = new RegisterSearchService(publisher);

        assertThat(service.register(SEARCH)).isNotEqualTo(service.register(SEARCH));
    }

    @Test
    void registerPropagatesPublicationFailures() {
        doThrow(new SearchPublicationException("down", new RuntimeException())).when(publisher).publish(any());

        assertThatThrownBy(() -> new RegisterSearchService(publisher).register(SEARCH))
                .isInstanceOf(SearchPublicationException.class);
    }

    @Test
    void persistSavesTheSearch() {
        RegisteredSearch registered = new RegisteredSearch(SearchId.generate(), SEARCH);

        new PersistSearchService(repository).persist(registered);

        verify(repository).save(registered);
    }

    @Test
    void countUsesTheFingerprintOfTheStoredSearch() {
        SearchId id = SearchId.generate();
        when(repository.findById(id)).thenReturn(Optional.of(new RegisteredSearch(id, SEARCH)));
        when(repository.countByFingerprint(SEARCH.fingerprint())).thenReturn(100L);

        SearchCount result = new CountSearchesService(repository).count(id);

        assertAll(
                () -> assertThat(result.count()).isEqualTo(100),
                () -> assertThat(result.registered().search()).isEqualTo(SEARCH));
    }

    @Test
    void countFailsWhenTheSearchDoesNotExist() {
        SearchId id = SearchId.generate();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CountSearchesService(repository).count(id))
                .isInstanceOf(SearchNotFoundException.class);
    }
}
