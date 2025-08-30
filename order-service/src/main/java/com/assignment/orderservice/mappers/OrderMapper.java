package com.assignment.orderservice.mappers;

import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.commonmodel.constants.Constants;

import com.assignment.orderservice.model.OrderRequest;
import com.assignment.orderservice.model.OrderResponse;

public class OrderMapper {

    public static OrderResponse toResponse(OrderEvent event, String message) {
        return new OrderResponse(
                event.getOrderId(),
                event.getCustomerName(),
                event.getItems(),
                event.getStatus(),
                message
        );
    }

    public static OrderEvent toEvent(OrderRequest request, String orderId) {
        return new OrderEvent(
                orderId,
                request.getCustomerName(),
                request.getItems(),
                InventoryStatus.PENDING
        );
    }

}
