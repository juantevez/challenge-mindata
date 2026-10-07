package com.mindata.hotel.infrastructure.adapter.in.messaging;

/** El mensaje recibido no respeta el contrato; reintentar no lo arregla, por eso va directo al DLT. */
public class InvalidSearchMessageException extends RuntimeException {

    public InvalidSearchMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
