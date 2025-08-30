package com.assignment.commonmodel.model;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem{

    @NotNull
    private Category category;

    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;

}

