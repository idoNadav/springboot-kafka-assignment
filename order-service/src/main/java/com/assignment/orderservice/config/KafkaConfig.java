package com.assignment.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class KafkaConfig {
    @Value("${app.topics.orders}")
    private String orderTopic;

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(orderTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

}
