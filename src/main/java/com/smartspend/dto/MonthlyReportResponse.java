package com.smartspend.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlyReportResponse {
    private int month;
    private int year;
    private String monthName;
    private UserResponse user;

    private BigDecimal totalSpending;
    private BigDecimal monthlyBudget;
    private BigDecimal remainingBudget;
    private double budgetUsedPercentage;
    private long transactionCount;
    private BigDecimal averageDailySpending;

    private String highestCategoryName;
    private BigDecimal highestCategoryAmount;

    private MonthComparisonDTO comparison;
    private List<CategoryBreakdownDTO> categoryBreakdown;
    private List<InsightItemDTO> insights;
    private List<ExpenseResponse> transactions;

    public MonthlyReportResponse() {}

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    public BigDecimal getTotalSpending() { return totalSpending; }
    public void setTotalSpending(BigDecimal totalSpending) { this.totalSpending = totalSpending; }

    public BigDecimal getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(BigDecimal monthlyBudget) { this.monthlyBudget = monthlyBudget; }

    public BigDecimal getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(BigDecimal remainingBudget) { this.remainingBudget = remainingBudget; }

    public double getBudgetUsedPercentage() { return budgetUsedPercentage; }
    public void setBudgetUsedPercentage(double budgetUsedPercentage) { this.budgetUsedPercentage = budgetUsedPercentage; }

    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }

    public BigDecimal getAverageDailySpending() { return averageDailySpending; }
    public void setAverageDailySpending(BigDecimal averageDailySpending) { this.averageDailySpending = averageDailySpending; }

    public String getHighestCategoryName() { return highestCategoryName; }
    public void setHighestCategoryName(String highestCategoryName) { this.highestCategoryName = highestCategoryName; }

    public BigDecimal getHighestCategoryAmount() { return highestCategoryAmount; }
    public void setHighestCategoryAmount(BigDecimal highestCategoryAmount) { this.highestCategoryAmount = highestCategoryAmount; }

    public MonthComparisonDTO getComparison() { return comparison; }
    public void setComparison(MonthComparisonDTO comparison) { this.comparison = comparison; }

    public List<CategoryBreakdownDTO> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryBreakdownDTO> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public List<InsightItemDTO> getInsights() { return insights; }
    public void setInsights(List<InsightItemDTO> insights) { this.insights = insights; }

    public List<ExpenseResponse> getTransactions() { return transactions; }
    public void setTransactions(List<ExpenseResponse> transactions) { this.transactions = transactions; }
}
