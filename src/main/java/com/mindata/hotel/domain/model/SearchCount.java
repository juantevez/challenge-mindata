package com.mindata.hotel.domain.model;

import java.util.Objects;

/** Resultado de /count: la búsqueda consultada y cuántas búsquedas iguales existen (incluida ella). */
public record SearchCount(RegisteredSearch registered, long count) {

    public SearchCount {
        Objects.requireNonNull(registered, "registrado");
        if (count < 0) {
            throw new IllegalArgumentException("count no debe ser negativo");
        }
    }
}
