package com.smartspend.dto;

import java.math.BigDecimal;

public class DashboardSummaryResponse {
    private BigDecimal totalExpensesThisMonth;
    private BigDecimal monthlyBudget;
    private BigDecimal remainingBudget;
    private double budgetUsedPercentage;
    private long transactionCount;
    private BigDecimal averageDailySpending;
    private String budgetStatus;
    private String budgetStatusLabel;
    private int month;
    private int year;

    public DashboardSummaryResponse() {}

    public BigDecimal getTotalExpensesThisMonth() { return totalExpensesThisMonth; }
    public void setTotalExpensesThisMonth(BigDecimal totalExpensesThisMonth) { this.totalExpensesThisMonth = totalExpensesThisMonth; }

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

    public String getBudgetStatus() { return budgetStatus; }
    public void setBudgetStatus(String budgetStatus) { this.budgetStatus = budgetStatus; }

    public String getBudgetStatusLabel() { return budgetStatusLabel; }
    public void setBudgetStatusLabel(String budgetStatusLabel) { this.budgetStatusLabel = budgetStatusLabel; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
