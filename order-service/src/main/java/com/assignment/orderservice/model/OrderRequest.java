package com.assignment.orderservice.model;
import com.assignment.commonmodel.model.OrderItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest{

    @NotEmpty
    @Valid
    private List<OrderItem> items;

    @NotBlank
    private String customerName;

    @NonNull
    @FutureOrPresent(message = "requested date must be now or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    private Instant requestedAt;

}


