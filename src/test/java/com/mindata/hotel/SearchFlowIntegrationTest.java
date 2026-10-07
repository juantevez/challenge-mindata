package com.mindata.hotel;

import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo: POST /search -> Kafka (embebido) -> consumidor con hilos virtuales -> base (H2 modo Oracle)
 * -> GET /count.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@EmbeddedKafka(
        partitions = 3,
        topics = {"hotel_availability_searches", "hotel_availability_searches.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        // Sin los 3 s de espera por defecto antes del primer rebalance: los consumidores arrancan enseguida.
        brokerProperties = "group.initial.rebalance.delay.ms=0")
class SearchFlowIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Test
    void equalSearchesAreCountedTogetherRegardlessOfAgeOrder() throws Exception {
        String hotel = "hotel-" + UUID.randomUUID();

        String first = postSearch(hotel, "[30, 29, 1, 3]");
        String reordered = postSearch(hotel, "[3, 29, 30, 1]");
        String otherAges = postSearch(hotel, "[30, 29]");
        String otherHotel = postSearch("other-" + hotel, "[30, 29, 1, 3]");

        awaitCount(first, 2);
        awaitCount(reordered, 2);
        awaitCount(otherAges, 1);
        awaitCount(otherHotel, 1);

        // /count devuelve la búsqueda tal como se envió.
        mockMvc.perform(get("/count").param("searchId", reordered)).andExpectAll(
                status().isOk(),
                jsonPath("$.searchId").value(reordered),
                jsonPath("$.search.hotelId").value(hotel),
                jsonPath("$.search.checkIn").value("29/12/2023"),
                jsonPath("$.search.checkOut").value("31/12/2023"),
                jsonPath("$.search.ages[0]").value(3),
                jsonPath("$.search.ages[3]").value(1));
    }

    @Test
    void identicalSearchesGetDifferentIds() throws Exception {
        String hotel = "hotel-" + UUID.randomUUID();

        String first = postSearch(hotel, "[30, 29, 1, 3]");
        String second = postSearch(hotel, "[30, 29, 1, 3]");

        assertThat(first).isNotEqualTo(second);
        awaitCount(first, 2);
        awaitCount(second, 2);
    }

    /** Muchas peticiones simultáneas (cada una en su hilo virtual): ningún ID se repite y no se pierde ninguna. */
    @Test
    void concurrentRequestsAreThreadSafe() throws Exception {
        int requests = 200;
        String hotel = "hotel-" + UUID.randomUUID();

        List<String> ids = new ArrayList<>(requests);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>(requests);
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> postSearch(hotel, "[30, 29, 1, 3]")));
            }
            for (Future<String> future : futures) {
                ids.add(future.get());
            }
        }

        assertThat(new HashSet<>(ids)).hasSize(requests);
        awaitCount(ids.getFirst(), requests);
    }

    @Test
    void unknownSearchIdReturns404() throws Exception {
        mockMvc.perform(get("/count").param("searchId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidMessageGoesToTheDeadLetterTopicAndDoesNotBlockTheConsumer() throws Exception {
        String poisonKey = "poison-" + UUID.randomUUID();
        kafkaTemplate.send("hotel_availability_searches", poisonKey, "{this is not json").get();

        // Una búsqueda válida enviada después se sigue procesando.
        String hotel = "hotel-" + UUID.randomUUID();
        String valid = postSearch(hotel, "[40]");
        awaitCount(valid, 1);

        Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "dlt-test-" + UUID.randomUUID(), true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of("hotel_availability_searches.DLT"));
            List<ConsumerRecord<String, String>> received = new ArrayList<>();

            await().atMost(TIMEOUT).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(300)).forEach(received::add);
                assertThat(received).anySatisfy(record -> {
                    assertThat(record.key()).isEqualTo(poisonKey);
                    assertThat(record.value()).isEqualTo("{this is not json");
                });
            });
        }
    }

    // ---------------------------------------------------------------- helpers

    private String postSearch(String hotelId, String ages) throws Exception {
        String body = """
                {"hotelId": "%s", "checkIn": "29/12/2023", "checkOut": "31/12/2023", "ages": %s}
                """.formatted(hotelId, ages);
        MvcResult result = mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.searchId").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("searchId").asString();
    }

    /** La persistencia es asíncrona: se reintenta hasta que /count responda con el valor esperado. */
    private void awaitCount(String searchId, long expected) {
        await().atMost(TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                mockMvc.perform(get("/count").param("searchId", searchId)).andExpectAll(
                        status().isOk(),
                        jsonPath("$.count").value(expected)));
    }
}
