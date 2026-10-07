package com.mindata.hotel.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class SearchViewTest {

    @Test
    void agesAreDefensivelyCopiedAndImmutable() {
        List<Integer> source = new ArrayList<>(List.of(30, 29));
        SearchView view = new SearchView("h", "29/12/2023", "31/12/2023", source);
        source.add(99);

        assertAll(
                () -> assertThat(view.ages()).containsExactly(30, 29),
                () -> assertThatThrownBy(() -> view.ages().add(1)).isInstanceOf(UnsupportedOperationException.class));
    }
}
