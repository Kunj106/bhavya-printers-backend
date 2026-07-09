package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderGstDto
{
    private Long orderId;

    private String bankName;

    private double taxableAmount;

    private double gstAmount;

    private double totalAmount;

    private String createdAt;
}
