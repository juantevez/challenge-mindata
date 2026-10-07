package com.mindata.hotel.infrastructure.adapter.in.messaging;

import com.mindata.hotel.domain.exception.InvalidSearchException;
import com.mindata.hotel.infrastructure.config.AppProperties;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Manejo de errores del consumidor (reintentos y dead letter topic). */
@Configuration
public class SearchKafkaConsumerConfig {

    /**
     * Errores transitorios (p. ej. base caída): 3 reintentos cada 500 ms y luego al DLT.
     * Mensajes inválidos (JSON roto, datos que violan el dominio): directo al DLT, sin reintentos.
     * Spring Boot registra este bean en el contenedor de listeners automáticamente.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate, AppProperties properties) {
        // Destino explícito: el sufijo por defecto de Spring Kafka ("-dlt") no coincide con el topic creado.
        String deadLetterTopic = properties.kafka().deadLetterTopic();
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(500L, 3L));
        handler.addNotRetryableExceptions(InvalidSearchMessageException.class, InvalidSearchException.class);
        return handler;
    }
}
