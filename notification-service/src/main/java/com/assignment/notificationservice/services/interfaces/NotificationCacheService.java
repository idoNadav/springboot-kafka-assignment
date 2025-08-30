package com.assignment.notificationservice.services.interfaces;

import com.assignment.commonmodel.model.OrderEvent;

public interface NotificationCacheService {
    OrderEvent getOrder(String orderId);
}
