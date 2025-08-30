package com.assignment.inventoryservice.services.implementations;

import com.assignment.commonmodel.model.*;
import com.assignment.inventoryservice.model.ProductDetails;
import com.assignment.inventoryservice.services.interfaces.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private final Map<String, ProductDetails> productCatalog;

    private final KafkaEventPublisherImpl KafkaEventPublisher;
    @Override
    public InventoryCheckResultEvent checkOrder(@Payload OrderEvent orderEvent) {

        Assert.notEmpty(orderEvent.getItems(), "Items cannot be empty");

        logger.info("Checking result for orderId={} with {} items", orderEvent.getOrderId(),
                orderEvent.getItems());

        List<InventoryIssue> missingItems = new ArrayList<>();
        String orderId = orderEvent.getOrderId();

        for (OrderItem orderItem : orderEvent.getItems()) {

            String productId = orderItem.getProductId();

            if (!productCatalog.containsKey(productId))
            {
                missingItems.add(buildRejectedItem(productId, InventoryCheckReason.UNKNOWN_PRODUCT));
                continue;
            }

            if (orderItem.getCategory() == Category.UNKNOWN) {
                missingItems.add(buildRejectedItem(productId, InventoryCheckReason.UNKNOWN_CATEGORY));
                continue;
            }

            ProductDetails itemFromCatalog = productCatalog.get(productId);
            if (itemFromCatalog == null)
            {
                logger.error("product Catalog is empty, skipping on items validations");
                return null;
            }
            boolean hasSufficientQuantity = hasSufficientQuantity(itemFromCatalog,orderItem.getQuantity());

            switch (itemFromCatalog.getCategory()) {
                case STANDARD ->
                {
                    if (!hasSufficientQuantity)
                        missingItems.add(buildRejectedItem(productId,InventoryCheckReason.INSUFFICIENT_QUANTITY));
                }
                case PERISHABLE ->
                {
                    if (isExpired(itemFromCatalog,LocalDate.now())) {
                        missingItems.add(buildRejectedItem(productId, InventoryCheckReason.EXPIRED));
                    }
                    else if (!hasSufficientQuantity)
                        missingItems.add(buildRejectedItem(productId,InventoryCheckReason.INSUFFICIENT_QUANTITY));
                }
                case DIGITAL ->
                {
                    logger.debug("Digital OK: orderId={}, productId={}, qty={}",
                            orderId,productId, orderItem.getQuantity());
                }
            }
        }
        InventoryStatus status = missingItems.isEmpty()
                ? InventoryStatus.APPROVED
                : InventoryStatus.REJECTED;

        logger.info("Inventory result: {} (orderId={}, missing={})",
                status, orderId, missingItems);

        InventoryCheckResultEvent inventoryEvent = new InventoryCheckResultEvent(orderId,status,List.copyOf(missingItems));
        KafkaEventPublisher.publishInventoryCheckEvent(inventoryEvent);
        return null;
    }

    private static InventoryIssue buildRejectedItem(String productId, InventoryCheckReason reason) {
        return new InventoryIssue(productId, reason);
    }
    private boolean isExpired(ProductDetails productDetails, LocalDate today) {
        LocalDate expirationDate = productDetails.getExpirationDate();
        return expirationDate != null && !today.isBefore(expirationDate);
    }
    private boolean hasSufficientQuantity(ProductDetails productDetails, int quantityRequested) {
        return productDetails.getAvailableQuantity() >= quantityRequested;
    }

}
