package com.mindata.hotel.application.port.out;

import com.mindata.hotel.application.exception.SearchPublicationException;
import com.mindata.hotel.domain.model.RegisteredSearch;

public interface SearchEventPublisher {

    /**
     * Publica la búsqueda y espera la confirmación del broker.
     *
     * @throws SearchPublicationException si no se pudo publicar
     */
    void publish(RegisteredSearch search);
}
