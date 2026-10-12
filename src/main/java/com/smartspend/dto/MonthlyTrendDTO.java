package com.smartspend.dto;

import java.math.BigDecimal;

public class MonthlyTrendDTO {
    private int month;
    private int year;
    private String monthLabel;
    private BigDecimal totalSpent;
    private BigDecimal budgetAmount;

    public MonthlyTrendDTO() {}

    public MonthlyTrendDTO(int month, int year, String monthLabel, BigDecimal totalSpent, BigDecimal budgetAmount) {
        this.month = month;
        this.year = year;
        this.monthLabel = monthLabel;
        this.totalSpent = totalSpent;
        this.budgetAmount = budgetAmount;
    }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getMonthLabel() { return monthLabel; }
    public void setMonthLabel(String monthLabel) { this.monthLabel = monthLabel; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
}
