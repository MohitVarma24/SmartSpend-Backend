package com.smartspend.dto;

import java.util.List;

public class InsightsResponse {
    private int month;
    private int year;
    private List<InsightItemDTO> insights;

    public InsightsResponse() {}

    public InsightsResponse(int month, int year, List<InsightItemDTO> insights) {
        this.month = month;
        this.year = year;
        this.insights = insights;
    }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public List<InsightItemDTO> getInsights() { return insights; }
    public void setInsights(List<InsightItemDTO> insights) { this.insights = insights; }
}
