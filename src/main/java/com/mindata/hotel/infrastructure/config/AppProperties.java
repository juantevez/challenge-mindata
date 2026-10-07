package com.mindata.hotel.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Kafka kafka, Cors cors) {

    /** Topic compartido más la configuración propia del productor y del consumidor. */
    public record Kafka(String topic, int partitions, int replicationFactor, Producer producer, Consumer consumer) {

        public String deadLetterTopic() {
            return topic + ".DLT";
        }
    }

    /** @param publishTimeout cuánto espera POST /search la confirmación del broker */
    public record Producer(Duration publishTimeout) {
    }

    /** @param maxConcurrency máximo de inserts simultáneos contra la base al procesar un lote */
    public record Consumer(int maxConcurrency) {
    }

    /** @param allowedOrigins orígenes de browser que pueden llamar a la API (p. ej. Swagger UI) */
    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }
}
