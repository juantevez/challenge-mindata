package com.mindata.hotel.domain.model;

import java.util.UUID;

/** Identificador atribuido a cada invocación de /search. */
public record SearchId(String value) {

    public SearchId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("searchId no debe estar en blanco");
        }
    }

    public static SearchId generate() {
        return new SearchId(UUID.randomUUID().toString());
    }
}
