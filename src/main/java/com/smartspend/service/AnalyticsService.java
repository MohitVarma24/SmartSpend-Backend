package com.smartspend.service;

import com.smartspend.dto.*;
import com.smartspend.entity.Budget;
import com.smartspend.entity.User;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.ExpenseRepository.CategorySpendingProjection;
import com.smartspend.repository.ExpenseRepository.DailySpendingProjection;
import com.smartspend.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final AuthService authService;

    public AnalyticsService(ExpenseRepository expenseRepository,
                            BudgetRepository budgetRepository,
                            AuthService authService) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(int month, int year) {
        User user = authService.getCurrentUserEntity();
        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);

        BigDecimal totalExpenses = expenseRepository.sumAmountByUserAndDateRange(user, startDate, endDate);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        Optional<Budget> overallBudgetOpt = budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(user, month, year);
        BigDecimal budgetAmount = overallBudgetOpt.map(Budget::getAmount).orElse(BigDecimal.ZERO);

        BigDecimal remainingBudget = budgetAmount.subtract(totalExpenses);

        double percentageUsed = 0.0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = totalExpenses.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        List<ExpenseRepository.DailySpendingProjection> dailyList = expenseRepository.getDailySpending(user, startDate, endDate);
        long txCount = dailyList.stream().mapToLong(DailySpendingProjection::getTransactionCount).sum();

        // Calculate average daily spending based on elapsed days or total days
        LocalDate today = LocalDate.now();
        int daysDivisor;
        if (today.getYear() == year && today.getMonthValue() == month) {
            daysDivisor = Math.max(today.getDayOfMonth(), 1);
        } else {
            daysDivisor = DateUtils.getDaysInMonth(month, year);
        }
        BigDecimal avgDaily = totalExpenses.divide(BigDecimal.valueOf(daysDivisor), 2, RoundingMode.HALF_UP);

        String budgetStatus;
        String statusLabel;
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            budgetStatus = "NO_BUDGET";
            statusLabel = "No Budget Configured";
        } else if (percentageUsed >= 100.0) {
            budgetStatus = "EXCEEDED";
            statusLabel = "Budget Exceeded";
        } else if (percentageUsed >= 90.0) {
            budgetStatus = "CRITICAL";
            statusLabel = "Critical Warning";
        } else if (percentageUsed >= 75.0) {
            budgetStatus = "APPROACHING";
            statusLabel = "Approaching Limit";
        } else {
            budgetStatus = "NORMAL";
            statusLabel = "On Track";
        }

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setTotalExpensesThisMonth(totalExpenses);
        response.setMonthlyBudget(budgetAmount);
        response.setRemainingBudget(remainingBudget);
        response.setBudgetUsedPercentage(percentageUsed);
        response.setTransactionCount(txCount);
        response.setAverageDailySpending(avgDaily);
        response.setBudgetStatus(budgetStatus);
        response.setBudgetStatusLabel(statusLabel);
        response.setMonth(month);
        response.setYear(year);

        return response;
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDTO> getCategoryBreakdown(int month, int year) {
        User user = authService.getCurrentUserEntity();
        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);

        List<CategorySpendingProjection> projections = expenseRepository.getCategorySpendingBreakdown(user, startDate, endDate);
        BigDecimal totalExpenses = expenseRepository.sumAmountByUserAndDateRange(user, startDate, endDate);
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            totalExpenses = BigDecimal.ZERO;
        }

        List<CategoryBreakdownDTO> result = new ArrayList<>();
        for (CategorySpendingProjection p : projections) {
            double pct = 0.0;
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                pct = p.getTotalAmount().divide(totalExpenses, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
            result.add(new CategoryBreakdownDTO(
                    p.getCategoryId(),
                    p.getCategoryName(),
                    p.getIcon(),
                    p.getColor(),
                    p.getTotalAmount(),
                    pct,
                    p.getTransactionCount()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendDTO> getMonthlyTrend() {
        User user = authService.getCurrentUserEntity();
        LocalDate now = LocalDate.now();

        List<MonthlyTrendDTO> trend = new ArrayList<>();
        // Last 6 months in chronological order
        for (int i = 5; i >= 0; i--) {
            LocalDate targetDate = now.minusMonths(i);
            int m = targetDate.getMonthValue();
            int y = targetDate.getYear();

            LocalDate start = DateUtils.getStartOfMonth(m, y);
            LocalDate end = DateUtils.getEndOfMonth(m, y);

            BigDecimal spent = expenseRepository.sumAmountByUserAndDateRange(user, start, end);
            if (spent == null) spent = BigDecimal.ZERO;

            Optional<Budget> budgetOpt = budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(user, m, y);
            BigDecimal budget = budgetOpt.map(Budget::getAmount).orElse(BigDecimal.ZERO);

            String label = DateUtils.getShortMonthName(m) + " " + String.valueOf(y).substring(2);
            trend.add(new MonthlyTrendDTO(m, y, label, spent, budget));
        }

        return trend;
    }

    @Transactional(readOnly = true)
    public List<DailyTrendDTO> getDailyTrend(int month, int year) {
        User user = authService.getCurrentUserEntity();
        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);

        List<DailySpendingProjection> projections = expenseRepository.getDailySpending(user, startDate, endDate);
        Map<LocalDate, DailySpendingProjection> map = new HashMap<>();
        for (DailySpendingProjection p : projections) {
            map.put(p.getExpenseDate(), p);
        }

        List<DailyTrendDTO> result = new ArrayList<>();
        int days = DateUtils.getDaysInMonth(month, year);
        for (int day = 1; day <= days; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DailySpendingProjection p = map.get(date);
            BigDecimal amount = p != null ? p.getTotalAmount() : BigDecimal.ZERO;
            long count = p != null ? p.getTransactionCount() : 0L;
            result.add(new DailyTrendDTO(date, day, amount, count));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MonthComparisonDTO getMonthComparison(int month, int year) {
        User user = authService.getCurrentUserEntity();

        LocalDate currentStart = DateUtils.getStartOfMonth(month, year);
        LocalDate currentEnd = DateUtils.getEndOfMonth(month, year);
        BigDecimal currentSpent = expenseRepository.sumAmountByUserAndDateRange(user, currentStart, currentEnd);
        if (currentSpent == null) currentSpent = BigDecimal.ZERO;

        YearMonth prevYm = YearMonth.of(year, month).minusMonths(1);
        int prevMonth = prevYm.getMonthValue();
        int prevYear = prevYm.getYear();

        LocalDate prevStart = DateUtils.getStartOfMonth(prevMonth, prevYear);
        LocalDate prevEnd = DateUtils.getEndOfMonth(prevMonth, prevYear);
        BigDecimal prevSpent = expenseRepository.sumAmountByUserAndDateRange(user, prevStart, prevEnd);
        if (prevSpent == null) prevSpent = BigDecimal.ZERO;

        BigDecimal diff = currentSpent.subtract(prevSpent);
        boolean hasPreviousData = prevSpent.compareTo(BigDecimal.ZERO) > 0;

        double percentageChange = 0.0;
        if (hasPreviousData) {
            percentageChange = diff.divide(prevSpent, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        MonthComparisonDTO comparison = new MonthComparisonDTO();
        comparison.setCurrentMonth(month);
        comparison.setCurrentYear(year);
        comparison.setCurrentMonthSpending(currentSpent);
        comparison.setPreviousMonth(prevMonth);
        comparison.setPreviousYear(prevYear);
        comparison.setPreviousMonthSpending(prevSpent);
        comparison.setDifference(diff);
        comparison.setPercentageChange(Math.abs(percentageChange));
        comparison.setIncreased(diff.compareTo(BigDecimal.ZERO) >= 0);
        comparison.setHasPreviousData(hasPreviousData);

        return comparison;
    }
}
