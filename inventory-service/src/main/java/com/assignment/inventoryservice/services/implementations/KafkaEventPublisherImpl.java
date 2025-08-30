package com.assignment.inventoryservice.services.implementations;

import com.assignment.inventoryservice.errorhandling.excepions.KafkaPublishException;
import com.assignment.commonmodel.model.InventoryCheckResultEvent;
import com.assignment.inventoryservice.services.interfaces.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class KafkaEventPublisherImpl implements KafkaEventPublisher {

    @Value("${app.topics.inventoryResults:inventory-results}")
    private String inventoryResultsTopic;
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherImpl.class);
    private final KafkaTemplate<String, Object> orderEventProducer;

    @Override
    public void publishInventoryCheckEvent(InventoryCheckResultEvent event) {

        String orderId = event.getOrderId();
        try {
            logger.info("Publishing InventoryCheck Event for orderId={} to topic={}", event.getOrderId(), inventoryResultsTopic);

            orderEventProducer.send(inventoryResultsTopic, event.getOrderId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logger.error("Failed to publish InventoryCheck Event for orderId={}",orderId, exception);
                        } else {
                            logger.debug("InventoryCheck Event for orderId={} published. Partition={}, Offset={}",
                                    event.getOrderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        }catch (KafkaException e) {
            logger.error("Error occurred during publish event for InventoryCheck, orderId={}", orderId, e);
            throw new KafkaPublishException("Kafka publish failed, Error occurred in Kafka process", e);
        }
    }

}
