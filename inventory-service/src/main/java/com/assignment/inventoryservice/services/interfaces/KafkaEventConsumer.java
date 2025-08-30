package com.assignment.inventoryservice.services.interfaces;

import com.assignment.commonmodel.model.OrderEvent;

public interface KafkaEventConsumer{
    void ListenOrderEvent(OrderEvent event);
}
