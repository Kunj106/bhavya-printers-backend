package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private Long bankId;
    private String bankName;
    private String branchName;
    private String gstNo;
    private String panNo;
    private String address;
    private String mobile;
    private String email;
    private List<OrderItemDto> items;
    private double subtotal;
    private double gstRate;
    private double gstAmount;
    private double total;
    private String paymentMethod;
    private String upiId;
    private String status;
    private String paymentStatus;
    private String bankCode;
    private String createdAt;
}
