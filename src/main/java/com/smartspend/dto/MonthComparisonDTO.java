package com.smartspend.dto;

import java.math.BigDecimal;

public class MonthComparisonDTO {
    private int currentMonth;
    private int currentYear;
    private BigDecimal currentMonthSpending;

    private int previousMonth;
    private int previousYear;
    private BigDecimal previousMonthSpending;

    private BigDecimal difference;
    private double percentageChange;
    private boolean increased;
    private boolean hasPreviousData;

    public MonthComparisonDTO() {}

    public int getCurrentMonth() { return currentMonth; }
    public void setCurrentMonth(int currentMonth) { this.currentMonth = currentMonth; }

    public int getCurrentYear() { return currentYear; }
    public void setCurrentYear(int currentYear) { this.currentYear = currentYear; }

    public BigDecimal getCurrentMonthSpending() { return currentMonthSpending; }
    public void setCurrentMonthSpending(BigDecimal currentMonthSpending) { this.currentMonthSpending = currentMonthSpending; }

    public int getPreviousMonth() { return previousMonth; }
    public void setPreviousMonth(int previousMonth) { this.previousMonth = previousMonth; }

    public int getPreviousYear() { return previousYear; }
    public void setPreviousYear(int previousYear) { this.previousYear = previousYear; }

    public BigDecimal getPreviousMonthSpending() { return previousMonthSpending; }
    public void setPreviousMonthSpending(BigDecimal previousMonthSpending) { this.previousMonthSpending = previousMonthSpending; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }

    public double getPercentageChange() { return percentageChange; }
    public void setPercentageChange(double percentageChange) { this.percentageChange = percentageChange; }

    public boolean isIncreased() { return increased; }
    public void setIncreased(boolean increased) { this.increased = increased; }

    public boolean isHasPreviousData() { return hasPreviousData; }
    public void setHasPreviousData(boolean hasPreviousData) { this.hasPreviousData = hasPreviousData; }
}
