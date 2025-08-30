package com.assignment.orderservice.model;

import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String customerName;
    private List<OrderItem> items;
    private InventoryStatus status;
    private String message;
}
