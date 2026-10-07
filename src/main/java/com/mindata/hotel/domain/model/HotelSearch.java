package com.mindata.hotel.domain.model;

import com.mindata.hotel.domain.exception.InvalidSearchException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Búsqueda de disponibilidad de un hotel. Objeto de valor inmutable: valida sus
 * invariantes al construirse y hace copia defensiva de la lista de edades.
 */
public record HotelSearch(String hotelId, LocalDate checkIn, LocalDate checkOut, List<Integer> ages) {

    public static final int MAX_HOTEL_ID_LENGTH = 64;
    public static final int MAX_AGES = 20;
    public static final int MAX_AGE = 120;

    public HotelSearch {
        if (hotelId == null || hotelId.isBlank()) {
            throw new InvalidSearchException("hotelId no debe estar en blanco");
        }
        if (hotelId.length() > MAX_HOTEL_ID_LENGTH) {
            throw new InvalidSearchException("hotelId debe tener como máximo " + MAX_HOTEL_ID_LENGTH + " caracteres");
        }
        if (checkIn == null || checkOut == null) {
            throw new InvalidSearchException("checkIn y checkOut son obligatorios");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidSearchException("checkOut debe ser posterior a checkIn");
        }
        if (ages == null || ages.isEmpty()) {
            throw new InvalidSearchException("ages debe contener al menos un elemento");
        }
        if (ages.size() > MAX_AGES) {
            throw new InvalidSearchException("ages debe contener como máximo " + MAX_AGES + " elementos");
        }
        for (Integer age : ages) {
            if (age == null || age < 0 || age > MAX_AGE) {
                throw new InvalidSearchException("cada edad de ages debe estar entre 0 y " + MAX_AGE);
            }
        }
        ages = List.copyOf(ages);
    }

    /** Edades ordenadas: [30, 29, 1, 3] y [3, 29, 30, 1] describen a los mismos huéspedes. */
    public List<Integer> sortedAges() {
        return ages.stream().sorted().toList();
    }

    /**
     * Huella estable de la búsqueda (SHA-256 en hexadecimal). Dos búsquedas son "iguales"
     * si tienen el mismo hotel, las mismas fechas y las mismas edades sin importar el orden.
     */
    public String fingerprint() {
        String canonical = hotelId.length() + ":" + hotelId
                + "|" + checkIn
                + "|" + checkOut
                + "|" + sortedAges().stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }
}
