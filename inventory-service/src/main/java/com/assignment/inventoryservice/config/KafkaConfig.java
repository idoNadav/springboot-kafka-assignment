package com.assignment.inventoryservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic inventoryResultsTopic(
            @Value("${app.topics.inventoryResults:inventory-results}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(2)
                .replicas(1)
                .build();
    }

}
