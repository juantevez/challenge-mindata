package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.infrastructure.common.DateFormats;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Lee fechas dd/MM/yyyy con {@link DateFormats} (calendario estricto). Solo acepta texto: un número o un
 * objeto se rechaza. El texto vacío se lee como null para que @NotNull lo informe como campo faltante.
 * No tiene estado, por eso Jackson comparte una única instancia entre hilos sin riesgo.
 */
class StrictDateDeserializer extends StdScalarDeserializer<LocalDate> {

    StrictDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (LocalDate) context.handleUnexpectedToken(LocalDate.class, parser);
        }
        String text = parser.getString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return DateFormats.parse(text);
        } catch (DateTimeParseException e) {
            throw context.weirdStringException(text, LocalDate.class, "expected a valid date with format dd/MM/yyyy");
        }
    }
}
