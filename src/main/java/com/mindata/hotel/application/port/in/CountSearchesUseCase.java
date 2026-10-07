package com.mindata.hotel.application.port.in;

import com.mindata.hotel.domain.exception.SearchNotFoundException;
import com.mindata.hotel.domain.model.SearchCount;
import com.mindata.hotel.domain.model.SearchId;

public interface CountSearchesUseCase {

    /**
     * @throws SearchNotFoundException si el identificador no existe
     */
    SearchCount count(SearchId searchId);
}
