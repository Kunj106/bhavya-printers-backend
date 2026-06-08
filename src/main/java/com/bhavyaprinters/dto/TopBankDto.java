package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopBankDto {
    private Long bankId;
    private String bankName;
    private String branchName;
    private int orderCount;
    private double totalSpend;
}
