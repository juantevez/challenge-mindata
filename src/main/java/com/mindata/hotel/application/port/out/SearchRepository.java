package com.mindata.hotel.application.port.out;

import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;

import java.util.Optional;

public interface SearchRepository {

    /** Guarda la búsqueda; guardar dos veces el mismo identificador no es un error. */
    void save(RegisteredSearch search);

    Optional<RegisteredSearch> findById(SearchId id);

    /** Cantidad de búsquedas almacenadas con esa huella (ver {@code HotelSearch#fingerprint()}). */
    long countByFingerprint(String fingerprint);
}
