package com.assignment.orderservice.services.interfaces;

import com.assignment.commonmodel.model.OrderEvent;

public interface KafkaEventPublisher {
    void publishOrderCreated(OrderEvent event);
}
