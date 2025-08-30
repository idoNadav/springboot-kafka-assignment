package com.assignment.notificationservice.services.implementations;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;
import com.assignment.notificationservice.services.interfaces.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerImpl implements KafkaEventConsumer {
    private final NotificationServiceImpl notificationService;
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerImpl.class);

    @Override
    @KafkaListener(
            topics = "${app.topics.inventoryResults}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeInventoryResult(InventoryCheckResultEvent inventoryCheckEvent) {
        logger.info("Received OrderEvent orderId={} from Inventory-service", inventoryCheckEvent.getOrderId());
        logger.debug("Event details event{}", inventoryCheckEvent);
        notificationService.handleInventoryResult(inventoryCheckEvent);
    }

}
