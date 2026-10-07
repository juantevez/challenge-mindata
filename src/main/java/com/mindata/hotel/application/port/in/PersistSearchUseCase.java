package com.mindata.hotel.application.port.in;

import com.mindata.hotel.domain.model.RegisteredSearch;

public interface PersistSearchUseCase {

    /** Persiste una búsqueda. Debe ser idempotente: Kafka garantiza entrega at-least-one. */
    void persist(RegisteredSearch search);
}
