package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.*;
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

    private final OrderRepository orderRepository;
    private final BankRepository bankRepository;
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

    /**
     * Order-wise GST Report
     */
    public List<OrderGstDto> getMonthlyGst() {

        return orderRepository.findOrderWiseGstReport()
                .stream()
                .map(row -> new OrderGstDto(
                        toLong(row[0]),

                        row[1] == null
                                ? null
                                : ((java.sql.Timestamp) row[1]).toLocalDateTime(),

                        String.valueOf(row[2]),

                        String.valueOf(row[3]),

                        toDouble(row[4]),

                        toDouble(row[5]),

                        toDouble(row[6]),

                        row[1] == null ? "" : row[1].toString()
                ))
                .toList();
    }

    public DashboardStatsDto getDashboardStats() {

        List<Object[]> statsList = orderRepository.findDashboardOrderStats();

        Object[] stats = (statsList != null && !statsList.isEmpty())
                ? statsList.get(0)
                : new Object[]{0, 0, 0, 0, 0, 0};

        long totalBanks = bankRepository.count();
        long totalProducts = productRepository.count();

        return new DashboardStatsDto(
                toInt(stats[0]),
                toDouble(stats[1]),
                (int) totalBanks,
                (int) totalProducts,
                toInt(stats[2]),
                toDouble(stats[3]),
                toInt(stats[4]),
                toInt(stats[5])
        );
    }

    private int toInt(Object val) {
        if (val == null) return 0;

        if (val instanceof Number n)
            return n.intValue();

        return Integer.parseInt(val.toString());
    }

    private long toLong(Object val) {
        if (val == null) return 0L;

        if (val instanceof Number n)
            return n.longValue();

        return Long.parseLong(val.toString());
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;

        if (val instanceof BigDecimal bd)
            return bd.doubleValue();

        if (val instanceof Number n)
            return n.doubleValue();

        return Double.parseDouble(val.toString());
    }


}
