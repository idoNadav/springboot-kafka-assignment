package com.assignment.orderservice.services.implementation;

import com.assignment.commonmodel.constants.Constants;
import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.orderservice.services.interfaces.OrderCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
public class OrderCacheServiceImpl implements OrderCacheService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCacheServiceImpl.class);
    private final Map<String, Entry> localCache = new ConcurrentHashMap<>();
    private final Map<String, Entry> pendingItemsToStoreInRedis = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redis;
    @Value("${app.cache.ttl}")
    private final Duration ttl = Duration.ofMinutes(30);

    private static class Entry {
        final Object value;
        final Instant expiresAt;
        Entry(Object v, Instant exp) {
            this.value = v; this.expiresAt = exp;
        }
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public String saveOrder(OrderEvent event) {
        String key = buildKey(event.getOrderId());
        boolean isRedisOk = tryWriteRedis(() -> redis.opsForValue().set(key, event));

        if (isRedisOk) {
            pendingItemsToStoreInRedis.remove(key);
            logger.info("Order saved to Redis with key={}, event{}", key,event);
        }else {
            Entry localCacheEntry = new Entry(event, Instant.now().plus(ttl));
            localCache.put(key, localCacheEntry);
            pendingItemsToStoreInRedis.put(key, localCacheEntry);
            logger.error("Redis unavailable. Saved to local fallback and queued for retry. key={}", key);
        }
        return key;
    }
    private String buildKey(String orderId){
        return Constants.ORDER_PREFIX + orderId;
    }
    @Override
    public void setOrderStatus(String orderId, InventoryStatus status) {
        String key = buildKey(orderId);
        OrderEvent updatedOrder= tryReadRedis(() -> (OrderEvent) redis.opsForValue().get(key));

        if (updatedOrder == null) {
            Entry entryLocal = localCache.get(key);
            if (entryLocal != null && !entryLocal.expired() && entryLocal.value instanceof OrderEvent orderEvent) {
                updatedOrder = orderEvent;
            }
        }
        assert updatedOrder != null;
        updatedOrder.setStatus(status);
        saveOrder(updatedOrder);
    }

    @Override
    public String getStatus(String orderId) {
        return null;
    }

    private boolean tryWriteRedis(Runnable operation) {
        try {
            operation.run();
            return true;
        } catch (org.springframework.dao.DataAccessException ex) {
            logger.error("Redis data access error: {}", ex.getClass().getSimpleName());
            return false;
        } catch (RuntimeException ex) {
            logger.error("Redis runtime error: {}", ex.toString());
            return false;
        }
    }

    private OrderEvent tryReadRedis(java.util.function.Supplier<OrderEvent> op) {
        try {
            return op.get();
        } catch (Exception exception) {
            logger.error("Redis read failed: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    @Scheduled(fixedDelayString = "${app.cache.redisRetryInterval:15000}")
    public void flushPendingToRedis() {
        if (pendingItemsToStoreInRedis.isEmpty())
            return;

        pendingItemsToStoreInRedis.forEach((key, entry) -> {
            if (entry.expired()) {
                localCache.remove(key);
                pendingItemsToStoreInRedis.remove(key);
                logger.debug("Flushed pending order key={}", key);
                return;
            }
            if (tryWriteRedis(() -> redis.opsForValue().set(key, entry.value))) {
                pendingItemsToStoreInRedis.remove(key);
                logger.debug("Redis Up ,Flushed pending order from cache, adding to Redis. key={}", key);
            }
        });
    }

    public OrderEvent getOrder(String orderId) {
        String key = buildKey(orderId);

        OrderEvent fromRedis = tryReadRedis(() -> (OrderEvent) redis.opsForValue().get(key));
        if (fromRedis != null)
            return fromRedis;

        Entry entry = localCache.get(key);
        if (entry == null || entry.expired()) {
            localCache.remove(key);
            pendingItemsToStoreInRedis.remove(key);
            return null;
        }

        if (tryWriteRedis(() -> redis.opsForValue().set(key, entry.value))) {
            pendingItemsToStoreInRedis.remove(key);
        }
        return (OrderEvent) entry.value;
    }

}
