package com.assignment.notificationservice.services.implementations;

import com.assignment.commonmodel.constants.Constants;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.notificationservice.services.interfaces.NotificationCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationCacheServiceImpl implements NotificationCacheService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationCacheServiceImpl.class);
    private final RedisTemplate<String, Object> redis;

    @Override
    public OrderEvent getOrder(String orderId) {
        String key = buildKey(orderId);
        return tryReadRedis(() -> (OrderEvent) redis.opsForValue().get(key));
    }

    private OrderEvent tryReadRedis(java.util.function.Supplier<OrderEvent> op) {
        try {
            return op.get();
        } catch (Exception exception) {
            logger.error("Redis read failed: {}", exception.getClass().getSimpleName());
            return null;
        }
    }
    private String buildKey(String orderId){
        return Constants.ORDER_PREFIX + orderId;
    }
}
