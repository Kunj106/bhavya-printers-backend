package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue")
    public ResponseEntity<List<MonthlyRevenueDto>> getRevenueSummary() {
        return ResponseEntity.ok(analyticsService.getMonthlyRevenue());
    }

    @GetMapping("/top-banks")
    public ResponseEntity<List<TopBankDto>> getTopBanks() {
        return ResponseEntity.ok(analyticsService.getTopBanks());
    }

    @GetMapping("/monthly-gst")
    public ResponseEntity<List<MonthlyGstDto>> getMonthlyGst() {
        return ResponseEntity.ok(analyticsService.getMonthlyGst());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }
}
