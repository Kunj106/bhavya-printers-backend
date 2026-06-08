package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {
    private int    totalOrders;
    private double totalRevenue;
    private int    totalBanks;
    private int    totalProducts;
    private int    deliveredOrders;
    private double thisMonthRevenue;
    private int    pendingOrders;
    private int    thisMonthOrders;
}
