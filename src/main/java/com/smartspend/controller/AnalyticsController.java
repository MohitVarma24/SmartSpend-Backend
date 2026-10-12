package com.smartspend.controller;

import com.smartspend.dto.CategoryBreakdownDTO;
import com.smartspend.dto.DailyTrendDTO;
import com.smartspend.dto.MonthComparisonDTO;
import com.smartspend.dto.MonthlyTrendDTO;
import com.smartspend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownDTO>> getCategoryBreakdown(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(m, y));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrend() {
        return ResponseEntity.ok(analyticsService.getMonthlyTrend());
    }

    @GetMapping("/daily-trend")
    public ResponseEntity<List<DailyTrendDTO>> getDailyTrend(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getDailyTrend(m, y));
    }

    @GetMapping("/month-comparison")
    public ResponseEntity<MonthComparisonDTO> getMonthComparison(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getMonthComparison(m, y));
    }
}
