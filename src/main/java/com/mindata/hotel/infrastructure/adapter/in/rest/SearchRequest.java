package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mindata.hotel.domain.model.HotelSearch;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Payload de POST /search. Es un record (inmutable) y hace copia defensiva de las edades.
 * Las fechas se convierten una sola vez, al deserializar: llegan como texto dd/MM/yyyy y se parsean
 * de forma estricta (calendario real); un valor ilegible se rechaza con 400 indicando el campo.
 */
public record SearchRequest(
        @NotBlank @Size(max = HotelSearch.MAX_HOTEL_ID_LENGTH)
        String hotelId,

        @NotNull @JsonDeserialize(using = StrictDateDeserializer.class)
        LocalDate checkIn,

        @NotNull @JsonDeserialize(using = StrictDateDeserializer.class)
        LocalDate checkOut,

        @NotEmpty @Size(max = HotelSearch.MAX_AGES)
        List<@NotNull @Min(0) @Max(HotelSearch.MAX_AGE) Integer> ages) {

    public SearchRequest {
        // Se permiten nulos en la copia para que Bean Validation los reporte como 400 (List.copyOf lanzaría NPE).
        ages = ages == null ? null : Collections.unmodifiableList(new ArrayList<>(ages));
    }

    @JsonIgnore
    @AssertTrue(message = "checkOut debe ser posterior a checkIn")
    public boolean isStayOrderValid() {
        return checkIn == null || checkOut == null || checkOut.isAfter(checkIn); // los nulos ya los informa @NotNull
    }

    public HotelSearch toDomain() {
        return new HotelSearch(hotelId, checkIn, checkOut, ages);
    }
}
