package com.assignment.orderservice.services.implementation;

import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.commonmodel.constants.Messages;
import com.assignment.orderservice.errorhandling.exceptions.OrderProcessingException;
import com.assignment.orderservice.mappers.OrderMapper;
import com.assignment.orderservice.model.OrderRequest;
import com.assignment.orderservice.model.OrderResponse;
import com.assignment.orderservice.services.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final KafkaPublisherImpl kafkaPublisherService;
    private final OrderCacheServiceImpl orderCacheService;

    @Override
    public OrderResponse createOrder(OrderRequest request) {

        logger.info("createOrder() - Creating order for customer: {}", request.getCustomerName());
        logger.debug("Order details: {}", request);

        String orderId = UUID.randomUUID().toString();
        final OrderEvent event;


        event = OrderMapper.toEvent(request, orderId);

        try {
            String key = orderCacheService.saveOrder(event);
            logger.debug("Order saved to Redis with key={}", key);

            kafkaPublisherService.publishOrderCreated(event);
            logger.info("Order sent to Kafka topic=orders, orderId={}", orderId);

        }catch (Exception e) {
            logger.error("Unexpected error for orderId={}", orderId, e);
            throw new OrderProcessingException("Error occurred during the process ", e);
        }

        return OrderMapper.toResponse(event, Messages.CREATED_ORDERS);
    }

}
