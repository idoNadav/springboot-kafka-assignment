package com.assignment.orderservice.services.implementation;

import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.orderservice.errorhandling.exceptions.KafkaPublishException;
import com.assignment.orderservice.services.interfaces.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaPublisherImpl implements KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPublisherImpl.class);
    private final KafkaTemplate<String, Object> orderEventProducer;

    @Value("${app.topics.orders}")
    private String orderTopic;

    @Override
    public void publishOrderCreated(OrderEvent event) {
        String orderId = event.getOrderId();

        try {
            logger.info("Publishing Order Event for orderId={} to topic={}", event.getOrderId(), orderTopic);

            orderEventProducer.send(orderTopic, event.getOrderId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logger.error("Failed to publish Order Event for orderId={}",orderId, exception);
                        } else {
                            logger.debug("Order Event for orderId={} published. Partition={}, Offset={}",
                                    event.getOrderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        }catch (KafkaException e) {
            logger.error("Kafka publish error for orderId={}", orderId, e);
            throw new KafkaPublishException("Kafka publish failed, Error occurred in Kafka process", e);
        }
    }
}
