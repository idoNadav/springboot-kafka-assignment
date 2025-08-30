package com.assignment.notificationservice.services.interfaces;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;

public interface KafkaEventConsumer {

    void consumeInventoryResult(InventoryCheckResultEvent inventoryCheckEvent);
}
