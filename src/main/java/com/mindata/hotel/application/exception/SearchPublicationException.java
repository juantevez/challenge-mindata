package com.mindata.hotel.application.exception;

/** No se pudo publicar la búsqueda en el broker de mensajería. */
public class SearchPublicationException extends RuntimeException {

    public SearchPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
