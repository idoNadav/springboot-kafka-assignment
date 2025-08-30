package com.assignment.inventoryservice.model;


import lombok.*;
import jakarta.validation.constraints.NotBlank;
import com.assignment.commonmodel.model.Category;
import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetails {

    @NonNull
    @NotBlank(message = "productId cannot be empty")
    private String productId;

    @NonNull
    private Category category;

    private int availableQuantity;
    private LocalDate expirationDate;
}
