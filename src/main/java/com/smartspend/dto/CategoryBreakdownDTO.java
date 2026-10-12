package com.smartspend.dto;

import java.math.BigDecimal;

public class CategoryBreakdownDTO {
    private Long categoryId;
    private String categoryName;
    private String icon;
    private String color;
    private BigDecimal amount;
    private double percentage;
    private long count;

    public CategoryBreakdownDTO() {}

    public CategoryBreakdownDTO(Long categoryId, String categoryName, String icon, String color,
                                BigDecimal amount, double percentage, long count) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.icon = icon;
        this.color = color;
        this.amount = amount;
        this.percentage = percentage;
        this.count = count;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
