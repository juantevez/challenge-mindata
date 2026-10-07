package com.mindata.hotel.domain.exception;

import com.mindata.hotel.domain.model.SearchId;

/** No existe (o todavía no fue persistida) una búsqueda con ese identificador. */
public class SearchNotFoundException extends RuntimeException {

    public SearchNotFoundException(SearchId searchId) {
        super("La búsqueda '" + searchId.value() + "' no fue encontrada (puede que todavía se esté procesando)");
    }
}
