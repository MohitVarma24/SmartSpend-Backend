package com.smartspend.controller;

import com.smartspend.dto.DashboardSummaryResponse;
import com.smartspend.dto.ExpenseResponse;
import com.smartspend.service.AnalyticsService;
import com.smartspend.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final ExpenseService expenseService;

    public DashboardController(AnalyticsService analyticsService, ExpenseService expenseService) {
        this.analyticsService = analyticsService;
        this.expenseService = expenseService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getDashboardSummary(m, y));
    }

    @GetMapping("/recent-expenses")
    public ResponseEntity<List<ExpenseResponse>> getRecentExpenses(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(expenseService.getRecentExpenses(limit, month, year));
    }
}
