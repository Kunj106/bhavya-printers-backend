package com.bhavyaprinters.dto;

import lombok.Data;

@Data
public class ProductUpdateDto {
    private String name;
    private String description;
    private Double price;
    private String category;
    private String imageUrl;
}
