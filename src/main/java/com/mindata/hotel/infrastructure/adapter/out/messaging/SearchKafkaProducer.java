package com.mindata.hotel.infrastructure.adapter.out.messaging;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mindata.hotel.application.exception.SearchPublicationException;
import com.mindata.hotel.application.port.out.SearchEventPublisher;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.infrastructure.config.AppProperties;
import com.mindata.hotel.infrastructure.messaging.SearchMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Productor Kafka (adaptador de salida): publica cada búsqueda en Kafka (clave = searchId) y espera el ack del broker. */
@Component
public class SearchKafkaProducer implements SearchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SearchKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties.Kafka kafkaProperties;

    public SearchKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     AppProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaProperties = properties.kafka();
    }

    @Override
    public void publish(RegisteredSearch search) {
        String key = search.id().value();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(SearchMessage.from(search));
        } catch (JacksonException e) {
            throw new SearchPublicationException("Could not serialize search " + key, e);
        }

        try {
            kafkaTemplate.send(kafkaProperties.topic(), key, payload)
                    .get(kafkaProperties.producer().publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Search {} published to {}", key, kafkaProperties.topic());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SearchPublicationException("Interrupted while publishing search " + key, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new SearchPublicationException("Could not publish search " + key + " to Kafka", e);
        }
    }
}
