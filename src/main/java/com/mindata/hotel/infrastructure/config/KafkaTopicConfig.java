package com.mindata.hotel.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics compartidos por el productor y el consumidor. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic hotelAvailabilitySearchesTopic(AppProperties properties) {
        AppProperties.Kafka kafka = properties.kafka();
        return TopicBuilder.name(kafka.topic())
                .partitions(kafka.partitions())
                .replicas(kafka.replicationFactor())
                .build();
    }

    @Bean
    NewTopic hotelAvailabilitySearchesDeadLetterTopic(AppProperties properties) {
        AppProperties.Kafka kafka = properties.kafka();
        return TopicBuilder.name(kafka.deadLetterTopic())
                .partitions(kafka.partitions())
                .replicas(kafka.replicationFactor())
                .build();
    }
}
