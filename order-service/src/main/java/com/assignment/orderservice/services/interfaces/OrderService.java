package com.assignment.orderservice.services.interfaces;

import com.assignment.orderservice.model.OrderRequest;
import com.assignment.orderservice.model.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);

}
