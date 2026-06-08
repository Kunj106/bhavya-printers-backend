package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
}
