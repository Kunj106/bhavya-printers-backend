package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductInputDto {
    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private Double price;

    @NotBlank(message = "category is required")
    private String category;

    private String imageUrl;
}
