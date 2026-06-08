package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderInputDto {
    private Long bankId;

    @NotBlank private String bankName;
    @NotBlank private String branchName;
    @NotBlank private String gstNo;
    @NotBlank private String panNo;
    @NotBlank private String address;
    @NotBlank private String mobile;
    @NotBlank private String email;

    @NotEmpty private List<OrderItemInputDto> items;

    @NotNull private Double gstRate;
    @NotBlank private String paymentMethod;
    private String upiId;
}
