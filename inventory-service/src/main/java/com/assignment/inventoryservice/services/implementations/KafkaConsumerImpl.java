package com.assignment.inventoryservice.services.implementations;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.inventoryservice.services.interfaces.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class KafkaConsumerImpl implements KafkaEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerImpl.class);
    private final InventoryServiceImpl inventoryService;

    @KafkaListener(
            topics = "${app.topics.orders:orders}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}"
    )
    public void ListenOrderEvent(OrderEvent event) {
        logger.info("Received OrderEvent orderId={} from order-service", event.getOrderId());
        logger.debug("Order Event details event{}", event);
        InventoryCheckResultEvent result = inventoryService.checkOrder(event);
        logger.debug("After checking result{}", result);
    }


}
