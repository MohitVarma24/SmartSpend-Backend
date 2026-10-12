package com.smartspend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartspend.dto.*;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.util.DateUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final AuthService authService;
    private final AnalyticsService analyticsService;
    private final InsightService insightService;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;

    public ReportService(AuthService authService,
                         AnalyticsService analyticsService,
                         InsightService insightService,
                         ExpenseService expenseService,
                         ExpenseRepository expenseRepository) {
        this.authService = authService;
        this.analyticsService = analyticsService;
        this.insightService = insightService;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int month, int year) {
        User user = authService.getCurrentUserEntity();
        UserResponse userResponse = authService.mapToUserResponse(user);

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(month, year);
        List<CategoryBreakdownDTO> breakdown = analyticsService.getCategoryBreakdown(month, year);
        MonthComparisonDTO comparison = analyticsService.getMonthComparison(month, year);
        InsightsResponse insights = insightService.generateInsights(month, year);

        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);
        List<ExpenseResponse> transactions = expenseService.getExpensesForPeriod(user, startDate, endDate);

        String topCatName = "None";
        BigDecimal topCatAmount = BigDecimal.ZERO;
        if (!breakdown.isEmpty()) {
            topCatName = breakdown.get(0).getCategoryName();
            topCatAmount = breakdown.get(0).getAmount();
        }

        MonthlyReportResponse report = new MonthlyReportResponse();
        report.setMonth(month);
        report.setYear(year);
        report.setMonthName(DateUtils.getMonthName(month));
        report.setUser(userResponse);
        report.setTotalSpending(summary.getTotalExpensesThisMonth());
        report.setMonthlyBudget(summary.getMonthlyBudget());
        report.setRemainingBudget(summary.getRemainingBudget());
        report.setBudgetUsedPercentage(summary.getBudgetUsedPercentage());
        report.setTransactionCount(summary.getTransactionCount());
        report.setAverageDailySpending(summary.getAverageDailySpending());
        report.setHighestCategoryName(topCatName);
        report.setHighestCategoryAmount(topCatAmount);
        report.setComparison(comparison);
        report.setCategoryBreakdown(breakdown);
        report.setInsights(insights.getInsights());
        report.setTransactions(transactions);

        return report;
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfReport(int month, int year) {
        MonthlyReportResponse report = getMonthlyReport(month, year);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            Color primaryDark = new Color(15, 23, 42); // Navy
            Color accentGreen = new Color(16, 185, 129); // Emerald
            Color lightGray = new Color(241, 245, 249);
            Color textDark = new Color(51, 65, 85);

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryDark);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 116, 139));
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryDark);
            Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryDark);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textDark);

            // Header Section
            Paragraph title = new Paragraph("SmartSpend — Financial Statement", titleFont);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph period = new Paragraph("Monthly Expense & Budget Report for " +
                    report.getMonthName() + " " + report.getYear() +
                    "  |  Account: " + report.getUser().getFullName() + " (" + report.getUser().getEmail() + ")", subTitleFont);
            period.setSpacingAfter(18);
            document.add(period);

            // KPI Summary Table
            Paragraph summaryTitle = new Paragraph("1. Executive Summary", sectionHeadingFont);
            summaryTitle.setSpacingAfter(8);
            document.add(summaryTitle);

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(16);

            addKpiCell(summaryTable, "Total Spending", "INR " + report.getTotalSpending(), accentGreen, boldCellFont, bodyFont);
            addKpiCell(summaryTable, "Monthly Budget", "INR " + report.getMonthlyBudget(), primaryDark, boldCellFont, bodyFont);
            addKpiCell(summaryTable, "Remaining Budget", "INR " + report.getRemainingBudget(),
                    report.getRemainingBudget().compareTo(BigDecimal.ZERO) >= 0 ? accentGreen : new Color(220, 38, 38), boldCellFont, bodyFont);

            addKpiCell(summaryTable, "Budget Utilization", String.format("%.1f", report.getBudgetUsedPercentage()) + "%", primaryDark, boldCellFont, bodyFont);
            addKpiCell(summaryTable, "Transactions Logged", String.valueOf(report.getTransactionCount()), primaryDark, boldCellFont, bodyFont);
            addKpiCell(summaryTable, "Average Daily Spend", "INR " + report.getAverageDailySpending(), primaryDark, boldCellFont, bodyFont);

            document.add(summaryTable);

            // Category Breakdown Table
            Paragraph catTitle = new Paragraph("2. Category Spending Breakdown", sectionHeadingFont);
            catTitle.setSpacingAfter(8);
            document.add(catTitle);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setWidths(new float[]{40, 20, 20, 20});
            catTable.setSpacingAfter(16);

            addTableHeader(catTable, new String[]{"Category", "Amount (INR)", "Share (%)", "Transactions"}, primaryDark);

            if (report.getCategoryBreakdown().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No expenses recorded for this month.", bodyFont));
                emptyCell.setColspan(4);
                emptyCell.setPadding(8);
                catTable.addCell(emptyCell);
            } else {
                for (CategoryBreakdownDTO c : report.getCategoryBreakdown()) {
                    catTable.addCell(createCell(c.getCategoryName(), bodyFont, Element.ALIGN_LEFT));
                    catTable.addCell(createCell(c.getAmount().toString(), bodyFont, Element.ALIGN_RIGHT));
                    catTable.addCell(createCell(String.format("%.1f%%", c.getPercentage()), bodyFont, Element.ALIGN_RIGHT));
                    catTable.addCell(createCell(String.valueOf(c.getCount()), bodyFont, Element.ALIGN_CENTER));
                }
            }
            document.add(catTable);

            // Transactions Table
            Paragraph txTitle = new Paragraph("3. Detailed Transaction Log", sectionHeadingFont);
            txTitle.setSpacingAfter(8);
            document.add(txTitle);

            PdfPTable txTable = new PdfPTable(5);
            txTable.setWidthPercentage(100);
            txTable.setWidths(new float[]{18, 32, 20, 15, 15});
            txTable.setSpacingAfter(16);

            addTableHeader(txTable, new String[]{"Date", "Description", "Category", "Method", "Amount (INR)"}, primaryDark);

            if (report.getTransactions().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No transactions logged in this period.", bodyFont));
                emptyCell.setColspan(5);
                emptyCell.setPadding(8);
                txTable.addCell(emptyCell);
            } else {
                for (ExpenseResponse tx : report.getTransactions()) {
                    txTable.addCell(createCell(tx.getExpenseDate().toString(), bodyFont, Element.ALIGN_LEFT));
                    txTable.addCell(createCell(tx.getDescription(), bodyFont, Element.ALIGN_LEFT));
                    txTable.addCell(createCell(tx.getCategory() != null ? tx.getCategory().getName() : "-", bodyFont, Element.ALIGN_LEFT));
                    txTable.addCell(createCell(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "Other", bodyFont, Element.ALIGN_CENTER));
                    txTable.addCell(createCell(tx.getAmount().toString(), bodyFont, Element.ALIGN_RIGHT));
                }
            }
            document.add(txTable);

            // Footer note
            Paragraph footer = new Paragraph("Report generated securely by SmartSpend System on " +
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ". All values in Indian Rupees (INR).", subTitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new RuntimeException("Error occurred while generating PDF report: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateCsvReport(int month, int year) {
        User user = authService.getCurrentUserEntity();
        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);

        List<Expense> expenses = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, startDate, endDate);

        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                .setHeader("Date", "Description", "Category", "Payment Method", "Amount", "Currency", "Notes")
                .build())) {

            for (Expense e : expenses) {
                printer.printRecord(
                        e.getExpenseDate(),
                        e.getDescription(),
                        e.getCategory() != null ? e.getCategory().getName() : "Uncategorized",
                        e.getPaymentMethod() != null ? e.getPaymentMethod() : "Other",
                        e.getAmount(),
                        user.getCurrency(),
                        e.getNotes() != null ? e.getNotes() : ""
                );
            }
            printer.flush();
            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Error generating CSV report: " + ex.getMessage(), ex);
        }
    }

    private void addKpiCell(PdfPTable table, String label, String value, Color valueColor, Font boldFont, Font bodyFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));

        Paragraph pLabel = new Paragraph(label, bodyFont);
        pLabel.setSpacingAfter(3);
        cell.addElement(pLabel);

        Font valFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, valueColor);
        Paragraph pVal = new Paragraph(value, valFont);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String[] headers, Color bg) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(bg);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }
}
