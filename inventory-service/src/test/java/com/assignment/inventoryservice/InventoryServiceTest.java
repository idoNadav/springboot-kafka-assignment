package com.assignment.inventoryservice;

import com.assignment.commonmodel.model.*;
import com.assignment.inventoryservice.config.InventoryCatalogConfig;
import com.assignment.inventoryservice.model.ProductDetails;
import com.assignment.inventoryservice.services.implementations.InventoryServiceImpl;
import com.assignment.inventoryservice.services.implementations.KafkaEventPublisherImpl;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private KafkaEventPublisherImpl publisher;
    private Map<String, ProductDetails> catalog;
    private InventoryServiceImpl inventoryService;

    private String orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        catalog = new InventoryCatalogConfig().productCatalog();
        inventoryService  = new InventoryServiceImpl(catalog, publisher);

        doNothing().when(publisher)
                .publishInventoryCheckEvent(any(InventoryCheckResultEvent.class));
    }

    private static OrderEvent orderEventBuilder(String id, OrderItem... items) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(id);
        event.setItems(List.of(items));
        return event;
    }

    private static OrderItem itemBuilder(Category cat, String pid, int quantity) {
        OrderItem item = new OrderItem();
        item.setCategory(cat);
        item.setProductId(pid);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void perishable_expired_REJECTED_with_issue() {
        inventoryService = new InventoryServiceImpl(catalog, publisher);

        OrderEvent orderEvent = orderEventBuilder(orderId, itemBuilder(Category.PERISHABLE, "P1006", 1));

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher).publishInventoryCheckEvent(eventCaptor.capture());

        InventoryCheckResultEvent actual = eventCaptor.getValue();

        assertEquals(InventoryStatus.REJECTED, actual.getStatus());
        assertEquals(orderId, actual.getOrderId());

        assertEquals(1, actual.getIssues().size());
        InventoryIssue issue = actual.getIssues().get(0);
        assertEquals("P1006", issue.productId());
        assertEquals(InventoryCheckReason.EXPIRED, issue.reason());
    }

    @Test
    void standard_and_digital_APPROVED_with_empty_issues() {

        var orderEvent = orderEventBuilder(orderId,
                itemBuilder(Category.STANDARD, "P1001", 2),
                itemBuilder(Category.DIGITAL,  "P1003", 1)
        );

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher, times(1)).publishInventoryCheckEvent(eventCaptor.capture());

        var inventoryCheckResult = eventCaptor.getValue();

        assertThat(inventoryCheckResult.getOrderId()).isEqualTo(orderId);
        assertThat(inventoryCheckResult.getStatus()).isEqualTo(InventoryStatus.APPROVED);
        assertThat(inventoryCheckResult.getIssues()).isEmpty();
    }

    @Test
    void standard_insufficient_quantity_REJECTED_with_missing_issue() {

        OrderEvent orderEvent = orderEventBuilder(orderId,
                itemBuilder(Category.STANDARD, "P1001", 999));

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher).publishInventoryCheckEvent(eventCaptor.capture());

        InventoryCheckResultEvent capturedEvent = eventCaptor.getValue();

        assertEquals(orderId, capturedEvent.getOrderId());
        assertEquals(InventoryStatus.REJECTED, capturedEvent.getStatus());
        assertEquals(1, capturedEvent.getIssues().size());

        InventoryIssue issue = capturedEvent.getIssues().get(0);
        assertEquals("P1001", issue.productId());
        assertEquals(InventoryCheckReason.INSUFFICIENT_QUANTITY, issue.reason());
    }

    @Test
    void request_with_UNKNOWN_category__publishes_REJECTED_with_UNKNOWN_CATEGORY() {
        OrderEvent orderEvent = orderEventBuilder(orderId,
                itemBuilder(Category.UNKNOWN, "P1001", 1));

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher).publishInventoryCheckEvent(eventCaptor.capture());

        InventoryCheckResultEvent capturedEvent = eventCaptor.getValue();

        assertEquals(orderId, capturedEvent.getOrderId());
        assertEquals(InventoryStatus.REJECTED, capturedEvent.getStatus());
        assertEquals(1, capturedEvent.getIssues().size());

        InventoryIssue issue = capturedEvent.getIssues().get(0);

        assertEquals("P1001", issue.productId());
        assertEquals(InventoryCheckReason.UNKNOWN_CATEGORY, issue.reason());
    }

    @Test
    void unknown_product__publishes_REJECTED_with_UNKNOWN_PRODUCT() {

        OrderEvent orderEvent = orderEventBuilder(orderId,
                itemBuilder(Category.STANDARD, "PX404", 1));

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher).publishInventoryCheckEvent(eventCaptor.capture());

        InventoryCheckResultEvent capturedEvent = eventCaptor.getValue();

        assertEquals(orderId, capturedEvent.getOrderId());
        assertEquals(InventoryStatus.REJECTED, capturedEvent.getStatus());
        assertEquals(1, capturedEvent.getIssues().size());

        InventoryIssue issue = capturedEvent.getIssues().get(0);
        assertEquals("PX404", issue.productId());
        assertEquals(InventoryCheckReason.UNKNOWN_PRODUCT, issue.reason());
    }

    @Test
    void perishable_null_expiration_and_insufficient_quantity__publishes_REJECTED_with_INSUFFICIENT_QUANTITY() {

        catalog.put("P2001", new ProductDetails("P2001", Category.PERISHABLE, 2, null));
        inventoryService = new InventoryServiceImpl(catalog, publisher);

        OrderEvent orderEvent = orderEventBuilder(orderId,
                itemBuilder(Category.PERISHABLE, "P2001", 5));

        inventoryService.checkOrder(orderEvent);

        ArgumentCaptor<InventoryCheckResultEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCheckResultEvent.class);
        verify(publisher).publishInventoryCheckEvent(eventCaptor.capture());

        InventoryCheckResultEvent capturedEvent = eventCaptor.getValue();
        assertEquals(orderId, capturedEvent.getOrderId());
        assertEquals(InventoryStatus.REJECTED, capturedEvent.getStatus());
        assertEquals(1, capturedEvent.getIssues().size());

        InventoryIssue issue = capturedEvent.getIssues().get(0);

        assertEquals("P2001", issue.productId());
        assertEquals(InventoryCheckReason.INSUFFICIENT_QUANTITY, issue.reason());
    }


}
