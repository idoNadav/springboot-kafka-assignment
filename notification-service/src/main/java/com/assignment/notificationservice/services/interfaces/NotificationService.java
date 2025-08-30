package com.assignment.notificationservice.services.interfaces;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;

public interface NotificationService {

    void handleInventoryResult(InventoryCheckResultEvent event);
}
