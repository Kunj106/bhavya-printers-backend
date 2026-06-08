package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.DashboardStatsDto;
import com.bhavyaprinters.dto.MonthlyGstDto;
import com.bhavyaprinters.dto.MonthlyRevenueDto;
import com.bhavyaprinters.dto.TopBankDto;
import com.bhavyaprinters.repository.BankRepository;
import com.bhavyaprinters.repository.OrderRepository;
import com.bhavyaprinters.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository   orderRepository;
    private final BankRepository    bankRepository;
    private final ProductRepository productRepository;

    public List<MonthlyRevenueDto> getMonthlyRevenue() {
        return orderRepository.findMonthlyRevenue().stream().map(row -> new MonthlyRevenueDto(
                toInt(row[0]),
                toInt(row[1]),
                toDouble(row[2]),
                toInt(row[3]),
                toDouble(row[4])
        )).toList();
    }

    public List<TopBankDto> getTopBanks() {
        return orderRepository.findTopBanks().stream().map(row -> new TopBankDto(
                toLong(row[0]),
                String.valueOf(row[1]),
                String.valueOf(row[2]),
                toInt(row[3]),
                toDouble(row[4])
        )).toList();
    }

    public List<MonthlyGstDto> getMonthlyGst() {
        return orderRepository.findMonthlyGst().stream().map(row -> new MonthlyGstDto(
                toInt(row[0]),
                toInt(row[1]),
                toDouble(row[2]),
                toDouble(row[3]),
                toDouble(row[4]),
                toDouble(row[5]),
                toInt(row[6])
        )).toList();
    }

    public DashboardStatsDto getDashboardStats() {
        List<Object[]> statsList = orderRepository.findDashboardOrderStats();
        Object[] stats = (statsList != null && !statsList.isEmpty())
                ? statsList.get(0)
                : new Object[]{0, 0, 0, 0, 0, 0};

        long totalBanks    = bankRepository.count();
        long totalProducts = productRepository.count();

        return new DashboardStatsDto(
                toInt(stats[0]),            // totalOrders
                toDouble(stats[1]),         // totalRevenue
                (int) totalBanks,           // totalBanks
                (int) totalProducts,        // totalProducts
                toInt(stats[2]),            // deliveredOrders
                toDouble(stats[3]),         // thisMonthRevenue
                toInt(stats[4]),            // pendingOrders
                toInt(stats[5])             // thisMonthOrders
        );
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof BigDecimal bd) return bd.doubleValue();
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }

}
