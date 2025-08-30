# Spring Boot Kafka Assignment

A microservices-based system for handling orders, inventory checks, and notifications.  
The project demonstrates event communication using Kafka and caching with Redis.


------------------------------Services:-------------------------------------------

- **Order Service** (port 8080)  
  - REST API `/orders` for creating orders.  
  - Stores orders in Redis.  
  - Publishes an `OrderEvent` to Kafka (`orders` topic).

- **Inventory Service** (port 8081)  
  - Listen to the `orders` Kafka topic.  
  - Checks product availability against a catalog.  
  - Publishes `InventoryCheckResultEvent` to Kafka (`inventory-results` topic).

- **Notification Service** (port 8082)  
  - Listen to the `inventory-results` Kafka topic.  
  - Retrieves order details from Redis.  
  - Send log notifications about inventory approval/rejection.


Kafka- localhost:9092

Redis- localhost:6379 with key Format: order:<orderId>


--------------------API Calls:------------------------------------------

curl --location 'http://localhost:8080/orders' \
--header 'Content-Type: application/json' \
--data '{
    "customerName": "Daniel",
    "items": [
      { "productId": "P1001", "quantity": 2, "category": "standard"},
      { "productId": "P1002", "quantity": 1, "category": "perishable" }
    ],
    "requestedAt": "2025-09-27T14:00:00Z"
  }'
    
-----------------------------Response sample:---------------------------

{
    "orderId": "20b4bd37-450b-4b5f-8892-5737ca841473",
    "customerName": "Daniel",
    "items": [
        {
            "category": "standard",
            "productId": "P1001",
            "quantity": 2
        },
        {
            "category": "perishable",
            "productId": "P1002",
            "quantity": 1
        }
    ],
    "status": "PENDING",
    "message": "Order created"
}

  
------------------Build & Run:-------------------------------------------

## Option 1 – Run with Docker Compose:

1. From the project root (where `docker-compose.yml` is located):
   - mvn -DskipTests clean package
   - docker compose up --build
   
   
##Option 2 – Run locally with Maven:
 - verify Kafka server and Redis Server Up
 - mvn clean install
 - mvn -pl order-service spring-boot:run
 - mvn -pl inventory-service spring-boot:run
 - mvn -pl notification-service spring-boot:run
 
 
------------------------Event Flow-----------------------------------

***Order Service

Saves the order in Redis under key order:<orderId>.

Publishes OrderEvent → Kafka orders.

Consumes from inventory-results For upating last status of the order.

***Inventory Service

Consumes from orders.

Validates stock and category.

Publishes InventoryCheckResultEvent → Kafka inventory-results.

***Notification Service

Consumes from inventory-results.

Fetches original order from Redis.

Logs/sends notification Order "Appproved" or "Rejected"

--------------------------------------------------------------------------

-------------------Redis fallback Flow------------------------------------
1.Run the create order request while Redis down

The request will waiting untill we get QueryTimeoutException and should to get orderId
and then you should to see the logs:

 Redis data access error: QueryTimeoutException
2025-08-30T22:41:23.738+03:00 ERROR 21084 --- [assignment] [nio-8080-exec-5] c.a.o.s.i.OrderCacheServiceImpl          : Redis unavailable. Saved to local fallback and queued for retry. key=order:17696385-332c-4b5e-a233-72e70b79c7c4
2025-08-30T22:41:23.738+03:00  INFO 21084 --- [assignment] [nio-8080-exec-5] c.a.o.s.i.KafkaPublisherImpl             : Publishing Order Event for orderId=17696385-332c-4b5e-a233-72e70b79c7c4 to topic=orders
2025-08-30T22:41:23.739+03:00  INFO 21084 --- [assignment] [nio-8080-exec-5] c.a.o.s.implementation.OrderServiceImpl  : Order sent to Kafka topic=orders, orderId=17696385-332c-4b5e-a233-72e70b79c7c4
2025-08-30T22:41:23.739+03:00  INFO 21084 --- [assignment] [nio-8080-exec-5] c.a.o.controller.OrderController         : Order created successfully. orderId=17696385-332c-4b5e-a233-72e70b79c7c4
2025-08-30T22:41:23.746+03:00 DEBUG 21084 --- [assignment] [ntainer#0-0-C-1] c.a.o.s.i.KafkaEventConsumerImpl         : inventory Results event received InventoryCheckResultEvent(orderId=17696385-332c-4b5e-a233-72e70b79c7c4, status=APPROVED, issues=[])
2025-08-30T22:41:46.001+03:00  INFO 21084 --- [assignment] [ecutorLoop-1-16] i.l.core.protocol.ConnectionWatchdog     : Reconnecting, last destination was localhost/<unresolved>:6379
2025-08-30T22:41:46.003+03:00  INFO 21084 --- [assignment] [ioEventLoop-4-2] i.l.core.protocol.ReconnectionHandler    : Reconnected to localhost/<unresolved>:6379
2025-08-30T22:41:46.004+03:00 DEBUG 21084 --- [assignment] [   scheduling-1] c.a.o.s.i.OrderCacheServiceImpl          : Redis Up ,Flushed pending order from cache, adding to Redis. key=order:17696385-332c-4b5e-a233-72e70b79c7c4
2025-08-30T22:41:46.004+03:00  INFO 21084 --- [assignment] [ntainer#0-0-C-1] c.a.o.s.i.OrderCacheServiceImpl          : Order saved to Redis with key=order:17696385-332c-4b5e-a233-72e70b79c7c4, eventOrderEvent(orderId=17696385-332c-4b5e-a233-72e70b79c7c4, customerName=Daniel, items=[OrderItem(category=STANDARD, productId=P1001, quantity=2), OrderItem(category=PERISHABLE, productId=P1002, quantity=1)], status=APPROVED)
2025-08-30T22:41:46.004+03:00  INFO 21084 --- [assignment] [ntainer#0-0-C-1] c.a.o.s.i.KafkaEventConsumerImpl         : Order 17696385-332c-4b5e-a233-72e70b79c7c4 status updated to APPROVED

As you can see while Redis down the service saved it in local cach and once Redis Back it will add to Redis and remove from pending order in local cache

And then notification-service will log the status of order ex:

2025-08-30T23:49:44.960+03:00  INFO 24700 --- [notification-service] [oEventLoop-4-16] i.l.core.protocol.ReconnectionHandler    : Reconnected to localhost/<unresolved>:6379
2025-08-30T23:49:44.961+03:00  WARN 24700 --- [notification-service] [ntainer#0-0-C-1] c.a.n.s.i.NotificationServiceImpl        : Queued notification until Redis will back. orderId=f7dae328-8102-493f-b4e6-70bec2f9a582, status=APPROVED
Order f7dae328-8102-493f-b4e6-70bec2f9a582 Approved! for customer=Daniel, items=[OrderItem(category=STANDARD, productId=P1001, quantity=2), OrderItem(category=PERISHABLE, productId=P1002, quantity=1)]

--------------------------------------------Catalog in inventory-service for testing---------------------------------------------------------------------------
        
		("P1001", new ProductDetails("P1001", Category.STANDARD, 10, null));
        ("P1002", new ProductDetails("P1002", Category.PERISHABLE, 3, LocalDate.of(2025, 9, 1)));
        ("P1003", new ProductDetails("P1003", Category.DIGITAL, 0, null));
        ("P1005", new ProductDetails("P1005", Category.PERISHABLE, 5, LocalDate.now().plusDays(10)));
        ("P1006", new ProductDetails("P1006", Category.PERISHABLE, 2, LocalDate.now().minusDays(2)));
        ("P1007", new ProductDetails("P1007", Category.DIGITAL, 9999, null));
        ("P1008", new ProductDetails("P1008", Category.STANDARD, 0, null));
		
--------------------------------------------------Using Ai(chatGpt)---------------------------------------------------------------------------		

- Unit Tests to generete few cases
- Some configuration for redis anf kafka

-----------------------------------------------------------------------------------------------------------------------------------------------