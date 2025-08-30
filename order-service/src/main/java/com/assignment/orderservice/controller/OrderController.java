package com.assignment.orderservice.controller;


import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.orderservice.model.OrderRequest;
import com.assignment.orderservice.model.OrderResponse;
import com.assignment.orderservice.services.implementation.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderServiceImpl orderServiceImpl;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request){

        logger.info("Received new order request for customer: {}", request.getCustomerName());
        logger.debug("Order details: {} items, requestedAt={}", request.getItems().size(), request.getRequestedAt());

        OrderResponse orderResponse;
        try {

            orderResponse = orderServiceImpl.createOrder(request);
            logger.info("Order created successfully. orderId={}", orderResponse.getOrderId());
            return ResponseEntity
                    .created(URI.create("/orders/" + orderResponse.getOrderId()))
                    .body(orderResponse);

        } catch (Exception e){
            logger.error("Failed to process order for customer={}", request.getCustomerName(), e);

            OrderResponse error = new OrderResponse(
                    null,
                    request.getCustomerName(),
                    request.getItems(),
                    InventoryStatus.REJECTED,
                    "Order failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }


    }

}
