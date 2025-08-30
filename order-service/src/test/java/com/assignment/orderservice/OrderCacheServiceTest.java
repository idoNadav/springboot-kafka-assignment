package com.assignment.orderservice;

import com.assignment.commonmodel.constants.Constants;
import com.assignment.commonmodel.model.Category;
import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.commonmodel.model.OrderItem;
import com.assignment.orderservice.services.implementation.OrderCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderCacheServiceTest {

    private RedisTemplate<String, Object> redis;
    private ValueOperations<String, Object> valueOps;
    private OrderCacheServiceImpl service;
    private OrderEvent orderEvent;

    @BeforeEach
    void setUp() {
        redis = Mockito.mock(RedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        orderEvent = orderEventBuilder();
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new OrderCacheServiceImpl(redis);
    }

    private String buildKey(String orderId){
        return Constants.ORDER_PREFIX+ orderId;
    }

    private static OrderItem itemBuilder(Category cat, String pid, int quantity) {
        OrderItem item = new OrderItem();
        item.setCategory(cat);
        item.setProductId(pid);
        item.setQuantity(quantity);
        return item;
    }
    private static OrderEvent orderEventBuilder() {
        OrderItem orderItem = itemBuilder(Category.PERISHABLE,"P1001",2);
        String orderId = UUID.randomUUID().toString();
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setCustomerName("Alice");
        event.setItems(List.of(orderItem));
        event.setStatus(InventoryStatus.PENDING);
        return event;
    }

    @Test
    void saveOrder_RedisUp_writesToRedis_noPendingFlush() {
        String key = buildKey(orderEvent.getOrderId());
        doNothing().when(valueOps).set(key, orderEvent);

        String savedKey = service.saveOrder(orderEvent);
        assertEquals(key, savedKey);

        verify(valueOps, times(1)).set(key, orderEvent);

        service.flushPendingToRedis();
        verify(valueOps, times(1)).set(key, orderEvent);
    }

    @Test
    void saveOrder__redisDown__fallback_thenFlush_writesToRedis() {
        String key = buildKey(orderEvent.getOrderId());

        doThrow(new DataAccessResourceFailureException("down"))
                .when(valueOps).set(key, orderEvent);

        String savedKey = service.saveOrder(orderEvent);
        assertEquals(key, savedKey);
        verify(valueOps, times(1)).set(key, orderEvent);

        doNothing().when(valueOps).set(key, orderEvent);
        service.flushPendingToRedis();

        verify(valueOps, times(2)).set(key, orderEvent);
    }

    @Test
    void getOrder_redisDown_returnsFromFallback_andBack_Fills_WhenUp() {
        String orderId = orderEvent.getOrderId();
        String key = buildKey(orderEvent.getOrderId());

        doThrow(new org.springframework.dao.DataAccessResourceFailureException("down"))
                .when(valueOps).set(key, orderEvent);

        service.saveOrder(orderEvent);

        verify(valueOps, times(1)).set(key, orderEvent);

        when(valueOps.get(key))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"));

        doNothing().when(valueOps).set(key, orderEvent);

        OrderEvent fromCache = service.getOrder(orderId);
        assertNotNull(fromCache);
        assertEquals(orderId, fromCache.getOrderId());

        verify(valueOps, times(1)).get(key);
        verify(valueOps, times(2)).set(key, orderEvent);
        verifyNoMoreInteractions(valueOps);
    }

}
