package com.example.sampleapi.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must not be negative")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 8 integer and 2 decimal digits")
        BigDecimal price,

        @NotNull(message = "stock is required")
        @Min(value = 0, message = "stock must not be negative")
        Integer stock
) {
}
