package com.assignment.orderservice.services.interfaces;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;

public interface KafkaEventConsumer {
    void consumeInventoryResult(InventoryCheckResultEvent event);
}
