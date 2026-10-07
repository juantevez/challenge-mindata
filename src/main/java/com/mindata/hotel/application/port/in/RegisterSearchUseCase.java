package com.mindata.hotel.application.port.in;

import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.SearchId;

public interface RegisterSearchUseCase {

    /** Asigna un identificador a la búsqueda y la publica para su persistencia asíncrona. */
    SearchId register(HotelSearch search);
}
