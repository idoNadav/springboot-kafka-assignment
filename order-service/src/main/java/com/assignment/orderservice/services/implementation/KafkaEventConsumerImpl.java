package com.assignment.orderservice.services.implementation;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;
import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.orderservice.services.interfaces.KafkaEventConsumer;
import com.assignment.orderservice.services.interfaces.OrderCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class KafkaEventConsumerImpl implements KafkaEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumerImpl.class);
    private final OrderCacheService orderCacheService;

    @Override
    @KafkaListener(
            topics = "${app.topics.inventoryResults}",
            groupId = "${spring.kafka.consumer.group-id:order-service-results-consumer}"
    )
    public void consumeInventoryResult(@Payload InventoryCheckResultEvent event) {
        logger.debug("inventory Results event received {}", event);

        String orderId = event.getOrderId();
        InventoryStatus status = event.getStatus();

        orderCacheService.setOrderStatus(orderId,status);
        logger.info("Order {} status updated to {}", orderId, status);
    }
}
