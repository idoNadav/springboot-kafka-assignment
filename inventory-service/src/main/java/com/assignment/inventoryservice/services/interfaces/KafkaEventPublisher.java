package com.assignment.inventoryservice.services.interfaces;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;

public interface KafkaEventPublisher {
    void publishInventoryCheckEvent(InventoryCheckResultEvent inventoryCheckResultEvent);
}
