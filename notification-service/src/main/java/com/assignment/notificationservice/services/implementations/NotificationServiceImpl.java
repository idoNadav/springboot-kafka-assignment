package com.assignment.notificationservice.services.implementations;

import com.assignment.commonmodel.model.InventoryCheckResultEvent;
import com.assignment.commonmodel.model.InventoryStatus;
import com.assignment.commonmodel.model.OrderEvent;
import com.assignment.notificationservice.services.interfaces.NotificationCacheService;
import com.assignment.notificationservice.services.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationCacheService notificationCacheService;
    private final Map<String, Pending> pendingNotifications = new ConcurrentHashMap<>();
    private final java.time.Duration ttl = java.time.Duration.ofMinutes(5);

    private static final class Pending {
        final InventoryCheckResultEvent event;
        final Instant expiresAt;
        Pending(InventoryCheckResultEvent event, Instant expiresAt) {
            this.event = event;
            this.expiresAt = expiresAt;
        }
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public void handleInventoryResult(InventoryCheckResultEvent checkResultEvent) {
        String orderId = checkResultEvent.getOrderId();

        OrderEvent orderFromCache = getOrder(orderId);
        if (orderFromCache != null) {
            sendNotification(orderFromCache,checkResultEvent);
            return;
        }

        String key = buildKey(orderId, checkResultEvent.getStatus());
        pendingNotifications.put(key, new Pending(checkResultEvent, Instant.now().plus(ttl)));
        logger.warn("Queued notification until Redis will back. orderId={}, status={}", orderId,checkResultEvent.getStatus());
    }

    private static String buildKey(String orderId, InventoryStatus status) {
        return orderId + ":" + status;
    }

    @Scheduled(fixedDelayString = "${app.notification.retryInterval:15000}")
    void processPending() {
        if (pendingNotifications.isEmpty())
            return;

        pendingNotifications.forEach((key, pending) -> {
            String orderId = pending.event.getOrderId();

            if (pending.expired()) {
                logger.info("Pending notification expired , Order {} Rejected! ", orderId);
                pendingNotifications.remove(key);
                return;
            }

            OrderEvent order = getOrder(orderId);
            if (order != null) {
                sendNotification(order, pending.event);
                logger.info("Redis Up ,Flushed pending notification from cache, orderId{} from queued pending notification",orderId);
                pendingNotifications.remove(key);
            }
        });
    }

    private void sendNotification(OrderEvent order, InventoryCheckResultEvent event) {
        if (event.getStatus() == InventoryStatus.APPROVED) {
            logger.info("Order {} Approved! for customer={}, items={}", order.getOrderId(), order.getCustomerName(), order.getItems());
        } else {
            logger.info("Order {} Rejected! for customer={}, with issues={}", order.getOrderId(), order.getCustomerName(), event.getIssues());
        }
    }
    private OrderEvent getOrder(String orderId) {
        try {
            return notificationCacheService.getOrder(orderId);
        } catch (Exception exception) {
            logger.error("Redis read failed for orderId{} , exception{}",
                    orderId, exception);
            return null;
        }
    }
}

