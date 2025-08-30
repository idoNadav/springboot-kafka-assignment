package com.assignment.inventoryservice.services.interfaces;

import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.commonmodel.model.InventoryCheckResultEvent;

public interface InventoryService {

    public InventoryCheckResultEvent checkOrder(OrderEvent order);

}
