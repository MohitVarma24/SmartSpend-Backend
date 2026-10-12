package com.smartspend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyTrendDTO {
    private LocalDate date;
    private int dayOfMonth;
    private BigDecimal amount;
    private long transactionCount;

    public DailyTrendDTO() {}

    public DailyTrendDTO(LocalDate date, int dayOfMonth, BigDecimal amount, long transactionCount) {
        this.date = date;
        this.dayOfMonth = dayOfMonth;
        this.amount = amount;
        this.transactionCount = transactionCount;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
}
