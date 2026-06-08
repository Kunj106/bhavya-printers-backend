package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyRevenueDto {
    private int month;
    private int year;
    private double totalRevenue;
    private int orderCount;
    private double gstCollected;
}
