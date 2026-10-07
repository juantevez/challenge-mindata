package com.mindata.hotel.infrastructure.adapter.out.persistence;

import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Usa H2 en modo Oracle con el mismo script Flyway que se aplica en Oracle. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
@Import(JpaSearchRepositoryAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaSearchRepositoryAdapterTest {

    private static final LocalDate IN = LocalDate.of(2023, 12, 29);
    private static final LocalDate OUT = LocalDate.of(2023, 12, 31);

    @Autowired
    JpaSearchRepositoryAdapter repository;

    private static String uniqueHotel() {
        return "hotel-" + UUID.randomUUID();
    }

    @Test
    void savesAndReadsBackTheSearchKeepingTheOriginalAgeOrder() {
        RegisteredSearch saved = new RegisteredSearch(
                SearchId.generate(), new HotelSearch(uniqueHotel(), IN, OUT, List.of(30, 29, 1, 3)));

        repository.save(saved);

        assertThat(repository.findById(saved.id())).contains(saved);
    }

    @Test
    void findByIdIsEmptyForUnknownIds() {
        assertThat(repository.findById(SearchId.generate())).isEmpty();
    }

    @Test
    void countsSearchesWithTheSameHotelDatesAndAgesRegardlessOfAgeOrder() {
        String hotel = uniqueHotel();
        HotelSearch first = new HotelSearch(hotel, IN, OUT, List.of(30, 29, 1, 3));
        HotelSearch reordered = new HotelSearch(hotel, IN, OUT, List.of(3, 29, 30, 1));
        HotelSearch otherDates = new HotelSearch(hotel, IN, OUT.plusDays(1), List.of(30, 29, 1, 3));

        repository.save(new RegisteredSearch(SearchId.generate(), first));
        repository.save(new RegisteredSearch(SearchId.generate(), reordered));
        repository.save(new RegisteredSearch(SearchId.generate(), otherDates));

        assertAll(
                () -> assertThat(repository.countByFingerprint(first.fingerprint())).isEqualTo(2),
                () -> assertThat(repository.countByFingerprint(otherDates.fingerprint())).isEqualTo(1));
    }

    @Test
    void savingTheSameSearchTwiceIsIdempotent() {
        RegisteredSearch search = new RegisteredSearch(
                SearchId.generate(), new HotelSearch(uniqueHotel(), IN, OUT, List.of(30)));

        repository.save(search);
        repository.save(search); // redelivery de Kafka

        assertThat(repository.countByFingerprint(search.search().fingerprint())).isEqualTo(1);
    }

    @Test
    void sqlInjectionAttemptsAreStoredAsPlainData() {
        // Las consultas son derivadas por Spring Data (parámetros enlazados): el texto nunca se interpreta como SQL.
        String hotel = "x' OR '1'='1'; DROP TABLE hotel_availability_search; --";
        RegisteredSearch malicious = new RegisteredSearch(
                SearchId.generate(), new HotelSearch(hotel, IN, OUT, List.of(30)));
        RegisteredSearch innocent = new RegisteredSearch(
                SearchId.generate(), new HotelSearch(uniqueHotel(), IN, OUT, List.of(30)));

        repository.save(malicious);
        repository.save(innocent);

        assertAll(
                () -> assertThat(repository.findById(malicious.id())).contains(malicious),
                () -> assertThat(repository.findById(new SearchId("' OR '1'='1"))).isEmpty(),
                () -> assertThat(repository.countByFingerprint(malicious.search().fingerprint())).isEqualTo(1),
                () -> assertThat(repository.findById(innocent.id())).contains(innocent));
    }
}
