package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderGstDto
{
    private Long orderId;

    private LocalDateTime orderDate;

    private String bankName;

    private String branchName;

    private double taxableAmount;

    private double gstAmount;

    private double totalAmount;

}
