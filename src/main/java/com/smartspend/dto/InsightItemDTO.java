package com.smartspend.dto;

public class InsightItemDTO {
    private String type; // CATEGORY_INCREASE, HIGHEST_CATEGORY, BUDGET_WARNING, REMAINING_BUDGET, HABIT_RECOMMENDATION
    private String title;
    private String message;
    private String details;
    private String severity; // INFO, WARNING, DANGER, SUCCESS
    private String actionableTip;
    private String calculationExplanation;

    public InsightItemDTO() {}

    public InsightItemDTO(String type, String title, String message, String details,
                          String severity, String actionableTip, String calculationExplanation) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.details = details;
        this.severity = severity;
        this.actionableTip = actionableTip;
        this.calculationExplanation = calculationExplanation;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getActionableTip() { return actionableTip; }
    public void setActionableTip(String actionableTip) { this.actionableTip = actionableTip; }

    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
}
