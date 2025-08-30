package com.assignment.notificationservice;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.assignment.commonmodel.constants.Constants;
import com.assignment.commonmodel.model.*;
import com.assignment.notificationservice.services.implementations.NotificationServiceImpl;
import com.assignment.notificationservice.services.interfaces.NotificationCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Logger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {
    @Mock
    NotificationCacheService cache;

    @InjectMocks
    NotificationServiceImpl service;

    private Logger log;
    private ListAppender<ILoggingEvent> appender;

    private OrderEvent orderEvent;

    @BeforeEach
    void setupLogger() {
        String orderId = UUID.randomUUID().toString();
        orderEvent = orderEventBuilder(orderId);
        when(cache.getOrder(orderId)).thenReturn(orderEvent);

        log = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(NotificationServiceImpl.class);
        log.setLevel(ch.qos.logback.classic.Level.DEBUG);
        appender = new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        log.addAppender(appender);
    }

    @AfterEach
    void tearDownLogger() {
        log.detachAppender(appender);
    }

    private static OrderEvent orderEventBuilder(String id) {
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrderId(id);
        orderEvent.setCustomerName("Daniel");
        orderEvent.setItems(List.of());
        orderEvent.setStatus(InventoryStatus.PENDING);
        return orderEvent;
    }

    private String buildKey(String orderId){
       return Constants.ORDER_PREFIX + orderId;
    }
    private static InventoryCheckResultEvent inventoryCheckResult(String id, InventoryStatus s, List<InventoryIssue> issues) {
        return new InventoryCheckResultEvent(id, s, issues);
    }

    @Test
    void when_order_found_and_status_APPROVED_logs_approved() {
        String orderId = orderEvent.getOrderId();
        String key = buildKey(orderId);

        var event = inventoryCheckResult(orderId, InventoryStatus.APPROVED, List.of());

        service.handleInventoryResult(event);

        verify(cache, times(1)).getOrder(orderId);
        verifyNoMoreInteractions(cache);

        boolean hasApprovedLog = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO &&
                        e.getFormattedMessage().contains("Approved") &&
                        e.getFormattedMessage().contains(orderId));
        assertTrue(hasApprovedLog, "Expected INFO log with 'Approved' and orderId");
    }

    @Test
    void when_order_found_and_status_REJECTED__logs_rejected_with_issues() {
        String orderId = orderEvent.getOrderId();
        when(cache.getOrder(orderId)).thenReturn(orderEvent);
        var issue = new InventoryIssue("P1006", InventoryCheckReason.EXPIRED);
        var event = inventoryCheckResult(orderId, InventoryStatus.REJECTED, List.of(issue));

        service.handleInventoryResult(event);
        verify(cache, times(1)).getOrder(orderId);
        verifyNoMoreInteractions(cache);

        boolean hasRejectedLog = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO &&
                        e.getFormattedMessage().toLowerCase().contains("rejected") &&
                        e.getFormattedMessage().contains(orderId) &&
                        e.getFormattedMessage().contains("P1006"));
        assertTrue(hasRejectedLog, "Expected INFO log with 'rejected', orderId and issues");
    }

    @Test
    void when_order_not_found_logs_warning_and_return() {
        String orderId = orderEvent.getOrderId();
        when(cache.getOrder(orderId)).thenReturn(null);

        var event = inventoryCheckResult(orderId, InventoryStatus.APPROVED, List.of());

        service.handleInventoryResult(event);

        verify(cache, times(1)).getOrder(orderId);
        verifyNoMoreInteractions(cache);

        boolean hasWarn = appender.list.stream().anyMatch(e ->
                e.getLevel() == ch.qos.logback.classic.Level.WARN &&
                        e.getFormattedMessage().contains("Queued notification until Redis will back") &&
                        e.getFormattedMessage().contains(orderId) &&
                        e.getFormattedMessage().contains(InventoryStatus.APPROVED.name())
        );
        assertTrue(hasWarn, "Expected WARN log with 'Queued notification until Redis will back', orderId and status");
    }

}
