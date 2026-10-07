package com.mindata.hotel.domain.model;

import com.mindata.hotel.domain.exception.InvalidSearchException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class HotelSearchTest {

    private static final LocalDate IN = LocalDate.of(2023, 12, 29);
    private static final LocalDate OUT = LocalDate.of(2023, 12, 31);

    @Test
    void sameGuestsInDifferentOrderHaveTheSameFingerprint() {
        HotelSearch a = new HotelSearch("1234aBc", IN, OUT, List.of(30, 29, 1, 3));
        HotelSearch b = new HotelSearch("1234aBc", IN, OUT, List.of(3, 29, 30, 1));

        assertThat(a.fingerprint()).isEqualTo(b.fingerprint());
    }

    @Test
    void differentHotelDatesOrGuestsHaveDifferentFingerprints() {
        String base = new HotelSearch("1234aBc", IN, OUT, List.of(30, 29, 1, 3)).fingerprint();

        assertAll(
                () -> assertThat(new HotelSearch("1234abc", IN, OUT, List.of(30, 29, 1, 3)).fingerprint())
                        .as("hotelId is case sensitive").isNotEqualTo(base),
                () -> assertThat(new HotelSearch("1234aBc", IN.plusDays(1), OUT, List.of(30, 29, 1, 3)).fingerprint())
                        .as("checkIn").isNotEqualTo(base),
                () -> assertThat(new HotelSearch("1234aBc", IN, OUT.plusDays(1), List.of(30, 29, 1, 3)).fingerprint())
                        .as("checkOut").isNotEqualTo(base),
                () -> assertThat(new HotelSearch("1234aBc", IN, OUT, List.of(30, 29, 1)).fingerprint())
                        .as("fewer guests").isNotEqualTo(base));
    }

    @Test
    void repeatedAgesAreCountedAsAMultiset() {
        HotelSearch a = new HotelSearch("h", IN, OUT, List.of(1, 1, 2));
        HotelSearch b = new HotelSearch("h", IN, OUT, List.of(1, 2, 2));

        assertThat(a.fingerprint()).isNotEqualTo(b.fingerprint());
    }

    @Test
    void agesAreDefensivelyCopiedAndImmutable() {
        List<Integer> source = new ArrayList<>(List.of(30, 29));
        HotelSearch search = new HotelSearch("h", IN, OUT, source);
        source.add(99);

        assertAll(
                () -> assertThat(search.ages()).containsExactly(30, 29),
                () -> assertThatThrownBy(() -> search.ages().add(1)).isInstanceOf(UnsupportedOperationException.class),
                () -> assertThatThrownBy(() -> search.sortedAges().add(1))
                        .isInstanceOf(UnsupportedOperationException.class));
    }

    @Test
    void keepsTheOriginalAgeOrderButExposesSortedAges() {
        HotelSearch search = new HotelSearch("h", IN, OUT, List.of(30, 29, 1, 3));

        assertAll(
                () -> assertThat(search.ages()).containsExactly(30, 29, 1, 3),
                () -> assertThat(search.sortedAges()).containsExactly(1, 3, 29, 30));
    }

    @Test
    void acceptsDatesInThePast() {
        // El ejemplo del enunciado usa 2023: no se exige que la estadía sea futura.
        assertThat(new HotelSearch("h", IN, OUT, List.of(30))).isNotNull();
    }

    @Test
    void rejectsInvalidSearches() {
        assertAll(
                () -> assertInvalid(() -> new HotelSearch(" ", IN, OUT, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch(null, IN, OUT, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h".repeat(65), IN, OUT, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h", null, OUT, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, null, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, IN, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h", OUT, IN, List.of(30))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, null)),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, List.of())),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, List.of(-1))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, List.of(121))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, Arrays.asList(30, null))),
                () -> assertInvalid(() -> new HotelSearch("h", IN, OUT, Collections.nCopies(21, 30))));
    }

    private static void assertInvalid(ThrowingCallable construction) {
        assertThatThrownBy(construction).isInstanceOf(InvalidSearchException.class);
    }
}
