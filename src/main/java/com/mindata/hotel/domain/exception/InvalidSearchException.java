package com.mindata.hotel.domain.exception;

/** Se lanza cuando una búsqueda viola alguna invariante del dominio. */
public class InvalidSearchException extends RuntimeException {

    public InvalidSearchException(String message) {
        super(message);
    }
}
