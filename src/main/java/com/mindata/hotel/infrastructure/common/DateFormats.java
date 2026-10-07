package com.mindata.hotel.infrastructure.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public final class DateFormats {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private DateFormats() {
    }

    public static LocalDate parse(String text) {
        return LocalDate.parse(text, FORMATTER);
    }

    public static String format(LocalDate date) {
        return FORMATTER.format(date);
    }
}
