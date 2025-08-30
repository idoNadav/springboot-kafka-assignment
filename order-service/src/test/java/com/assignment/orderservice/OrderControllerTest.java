package com.assignment.orderservice;

import com.assignment.commonmodel.constants.Constants;
import com.assignment.commonmodel.constants.Messages;
import com.assignment.commonmodel.model.Category;
import com.assignment.orderservice.controller.OrderController;
import com.assignment.orderservice.errorhandling.exceptions.GlobalExceptionHandler;
import com.assignment.orderservice.model.OrderRequest;
import com.assignment.orderservice.model.OrderResponse;
import com.assignment.orderservice.services.implementation.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderServiceImpl orderServiceImpl;

    @Test
    void createOrder_validRequest_returns200() throws Exception {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId("cb1667d2-b7fd-4316-a153-7c88b3ef1131");
        orderResponse.setMessage("CREATED_ORDER");
        when(orderServiceImpl.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        String json = """
                  {
                    "customerName": "Daniel",
                    "items": [
                      { "productId": "P1001", "quantity": 2, "category": "standard" },
                      { "productId": "P1002", "quantity": 1, "category": "perishable" }
                    ],
                    "requestedAt": "%s"
                  }
                """.formatted(Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderResponse.getOrderId()))
                .andExpect(jsonPath("$.message").value("CREATED_ORDER"));
    }

    @Test
    void createOrder_missingCustomerName_returns400() throws Exception {
        String json = """
                  {
                    "customerName": "",
                    "items": [
                      { "productId": "P1001", "quantity": 1, "category": "standard" }
                    ],
                    "requestedAt": "%s"
                  }
                """.formatted(Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_quantityZero_returns400() throws Exception {
        String json = """
                  {
                    "customerName": "Alice",
                    "items": [
                      { "productId": "P1001", "quantity": 0, "category": "standard" }
                    ],
                    "requestedAt": "%s"
                  }
                """.formatted(Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_requestedAtInPast_returns400() throws Exception {
        String json = """
                  {
                    "customerName": "Alice",
                    "items": [
                      { "productId": "P1001", "quantity": 1, "category": "standard" }
                    ],
                    "requestedAt": "%s"
                  }
                """.formatted(Instant.now().minusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_invalidCategory_returns200_with_UNKNOWN_Category() throws Exception {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setOrderId("cb1667d2-b7fd-4316-a153-7c88b3ef1131");
        orderResponse.setMessage(Messages.CREATED_ORDERS);
        when(orderServiceImpl.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        String json = """
      {
        "customerName": "Alice",
        "items": [
          { "productId": "P1001", "quantity": 1, "category": "other"}
        ],
        "requestedAt": "%s"
      }
      """.formatted(Instant.now().plusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderResponse.getOrderId()))
                .andExpect(jsonPath("$.message").value(Messages.CREATED_ORDERS));

        var captor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderServiceImpl, times(1)).createOrder(captor.capture());

        OrderRequest sent = captor.getValue();
        assertThat(sent.getItems()).hasSize(1);
        assertThat(sent.getItems().get(0).getCategory()).isEqualTo(Category.UNKNOWN);
    }

}
