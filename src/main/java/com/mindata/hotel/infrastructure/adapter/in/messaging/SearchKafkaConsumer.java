package com.mindata.hotel.infrastructure.adapter.in.messaging;

import tools.jackson.databind.ObjectMapper;
import com.mindata.hotel.application.port.in.PersistSearchUseCase;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.infrastructure.config.AppProperties;
import com.mindata.hotel.infrastructure.messaging.SearchMessage;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Consumidor Kafka (adaptador de entrada): consume el topic en lotes y persiste cada mensaje en su propio hilo virtual.
 *
 * <p>El listener no retorna hasta que todo el lote terminó, así el offset solo se confirma cuando
 * los mensajes ya están en la base (entrega "al menos una vez"; la persistencia es idempotente).
 * Si un mensaje falla se lanza {@link BatchListenerFailedException} con su posición: Spring confirma
 * los anteriores, reintenta/deriva al DLT ese mensaje y sigue con el resto.
 */
@Component
public class SearchKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SearchKafkaConsumer.class);

    private final PersistSearchUseCase persistSearch;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore databasePermits;

    public SearchKafkaConsumer(PersistSearchUseCase persistSearch,
                               ObjectMapper objectMapper,
                               AppProperties properties) {
        this.persistSearch = persistSearch;
        this.objectMapper = objectMapper;
        this.databasePermits = new Semaphore(properties.kafka().consumer().maxConcurrency());
    }

    @KafkaListener(topics = "${app.kafka.topic}")
    public void onSearches(List<ConsumerRecord<String, String>> records) {
        List<Future<?>> futures = new ArrayList<>(records.size());
        for (ConsumerRecord<String, String> record : records) {
            futures.add(virtualThreads.submit(() -> process(record)));
        }

        BatchListenerFailedException firstFailure = null;
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get();
            } catch (ExecutionException e) {
                log.warn("No se pudo guardar el registro, key={} partition={} offset={}: {}",
                        records.get(i).key(), records.get(i).partition(), records.get(i).offset(),
                        e.getCause().toString());
                if (firstFailure == null) {
                    firstFailure = new BatchListenerFailedException("No se pudo guardar la búsqueda", e.getCause(), i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupcion mientras ejecutaba las búsquedas", e);
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    private void process(ConsumerRecord<String, String> record) {
        RegisteredSearch search = parse(record);
        databasePermits.acquireUninterruptibly();
        try {
            persistSearch.persist(search);
        } finally {
            databasePermits.release();
        }
    }

    private RegisteredSearch parse(ConsumerRecord<String, String> record) {
        try {
            return objectMapper.readValue(record.value(), SearchMessage.class).toDomain();
        } catch (Exception e) {
            throw new InvalidSearchMessageException(
                    "Mensaje invalido en la particion " + record.partition() + " offset " + record.offset(), e);
        }
    }

    @PreDestroy
    void shutdown() {
        virtualThreads.close();
    }
}
