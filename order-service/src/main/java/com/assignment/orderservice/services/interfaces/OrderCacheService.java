package com.assignment.orderservice.services.interfaces;

import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderEvent;

public interface OrderCacheService {
    String saveOrder(OrderEvent event);
    void setOrderStatus(String orderId, InventoryStatus status);
    String getStatus(String orderId);
}
