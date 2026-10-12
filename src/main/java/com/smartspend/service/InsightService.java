package com.smartspend.service;

import com.smartspend.dto.CategoryBreakdownDTO;
import com.smartspend.dto.InsightItemDTO;
import com.smartspend.dto.InsightsResponse;
import com.smartspend.entity.Budget;
import com.smartspend.entity.User;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class InsightService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final AuthService authService;
    private final AnalyticsService analyticsService;

    public InsightService(ExpenseRepository expenseRepository,
                          BudgetRepository budgetRepository,
                          AuthService authService,
                          AnalyticsService analyticsService) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.authService = authService;
        this.analyticsService = analyticsService;
    }

    @Transactional(readOnly = true)
    public InsightsResponse generateInsights(int month, int year) {
        User user = authService.getCurrentUserEntity();
        List<InsightItemDTO> insights = new ArrayList<>();

        LocalDate currentStart = DateUtils.getStartOfMonth(month, year);
        LocalDate currentEnd = DateUtils.getEndOfMonth(month, year);

        YearMonth prevYm = YearMonth.of(year, month).minusMonths(1);
        int prevMonth = prevYm.getMonthValue();
        int prevYear = prevYm.getYear();
        LocalDate prevStart = DateUtils.getStartOfMonth(prevMonth, prevYear);
        LocalDate prevEnd = DateUtils.getEndOfMonth(prevMonth, prevYear);

        BigDecimal currentTotal = expenseRepository.sumAmountByUserAndDateRange(user, currentStart, currentEnd);
        if (currentTotal == null) currentTotal = BigDecimal.ZERO;

        BigDecimal prevTotal = expenseRepository.sumAmountByUserAndDateRange(user, prevStart, prevEnd);
        if (prevTotal == null) prevTotal = BigDecimal.ZERO;

        // Current Month Categories
        List<CategoryBreakdownDTO> currentCats = analyticsService.getCategoryBreakdown(month, year);
        // Previous Month Categories
        List<CategoryBreakdownDTO> prevCats = analyticsService.getCategoryBreakdown(prevMonth, prevYear);
        Map<Long, CategoryBreakdownDTO> prevCatMap = new HashMap<>();
        for (CategoryBreakdownDTO c : prevCats) {
            prevCatMap.put(c.getCategoryId(), c);
        }

        // ==========================================
        // Insight 0: Empty Month Check
        // ==========================================
        if (currentTotal.compareTo(BigDecimal.ZERO) == 0) {
            insights.add(new InsightItemDTO(
                    "HABIT_RECOMMENDATION",
                    "No Expenses Recorded This Month",
                    "You have not recorded any expenses for " + DateUtils.getMonthName(month) + " " + year + " yet.",
                    "Starting your tracking early helps build consistent financial discipline and prevents surprise shortfalls at month end.",
                    "INFO",
                    "Click 'Add Expense' to log your first transaction for this month.",
                    "Evaluated based on zero recorded transactions between " + currentStart + " and " + currentEnd + "."
            ));
            return new InsightsResponse(month, year, insights);
        }

        // ==========================================
        // Insight 1: Category Spending Increase
        // ==========================================
        CategoryBreakdownDTO highestIncreaseCat = null;
        double maxPctIncrease = 0.0;
        BigDecimal maxAbsIncrease = BigDecimal.ZERO;

        for (CategoryBreakdownDTO cur : currentCats) {
            CategoryBreakdownDTO prv = prevCatMap.get(cur.getCategoryId());
            if (prv != null && prv.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = cur.getAmount().subtract(prv.getAmount());
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    double pct = diff.divide(prv.getAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    if (pct > maxPctIncrease && diff.compareTo(BigDecimal.valueOf(100)) >= 0) {
                        maxPctIncrease = pct;
                        maxAbsIncrease = diff;
                        highestIncreaseCat = cur;
                    }
                }
            }
        }

        if (highestIncreaseCat != null && maxPctIncrease >= 10.0) {
            insights.add(new InsightItemDTO(
                    "CATEGORY_INCREASE",
                    highestIncreaseCat.getCategoryName() + " Spending Increased by " + String.format("%.1f", maxPctIncrease) + "%",
                    "Your spending on " + highestIncreaseCat.getCategoryName() + " increased by " +
                            String.format("%.1f", maxPctIncrease) + "% (+₹" + maxAbsIncrease + ") compared with " +
                            DateUtils.getMonthName(prevMonth) + ".",
                    "This represents the sharpest percentage surge among your recurring categories.",
                    maxPctIncrease >= 40.0 ? "WARNING" : "INFO",
                    "Review recent transactions under " + highestIncreaseCat.getCategoryName() + " to determine if this increase was planned or avoidable.",
                    "Formula: ((Current Month " + highestIncreaseCat.getCategoryName() + " - Previous Month) / Previous Month) * 100."
            ));
        }

        // ==========================================
        // Insight 2: Highest Spending Category
        // ==========================================
        if (!currentCats.isEmpty()) {
            CategoryBreakdownDTO topCat = currentCats.get(0);
            insights.add(new InsightItemDTO(
                    "HIGHEST_CATEGORY",
                    topCat.getCategoryName() + " is Your Top Expense",
                    topCat.getCategoryName() + " accounted for ₹" + topCat.getAmount() +
                            " (" + String.format("%.1f", topCat.getPercentage()) + "% of total spending) this month.",
                    "You logged " + topCat.getCount() + " transaction(s) in this category.",
                    topCat.getPercentage() > 40.0 ? "WARNING" : "INFO",
                    topCat.getPercentage() > 40.0 ?
                            "Consider establishing a dedicated category budget to avoid over-concentration in " + topCat.getCategoryName() + "." :
                            "Keep an eye on this category to ensure it aligns with your monthly priorities.",
                    "Identified as the category with the highest aggregate sum (₹" + topCat.getAmount() + ") out of ₹" + currentTotal + " total spending."
            ));
        }

        // ==========================================
        // Insight 3 & 4: Budget Warning & Remaining Budget
        // ==========================================
        Optional<Budget> overallBudgetOpt = budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(user, month, year);
        if (overallBudgetOpt.isPresent()) {
            BigDecimal budgetAmt = overallBudgetOpt.get().getAmount();
            BigDecimal remaining = budgetAmt.subtract(currentTotal);
            double pctUsed = budgetAmt.compareTo(BigDecimal.ZERO) > 0 ?
                    currentTotal.divide(budgetAmt, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

            if (pctUsed >= 100.0) {
                BigDecimal overspent = currentTotal.subtract(budgetAmt);
                insights.add(new InsightItemDTO(
                        "BUDGET_WARNING",
                        "Monthly Budget Exceeded by ₹" + overspent,
                        "You have utilized " + String.format("%.1f", pctUsed) + "% of your overall budget (₹" + currentTotal + " spent of ₹" + budgetAmt + ").",
                        "Spending has surpassed your allocated monthly limit by ₹" + overspent + ".",
                        "DANGER",
                        "Immediately pause discretionary spending (shopping, entertainment, dining out) for the remainder of this month.",
                        "Formula: (Total Spent: ₹" + currentTotal + " / Budget: ₹" + budgetAmt + ") * 100 = " + String.format("%.1f", pctUsed) + "%."
                ));
            } else if (pctUsed >= 90.0) {
                insights.add(new InsightItemDTO(
                        "BUDGET_WARNING",
                        "Critical: " + String.format("%.1f", pctUsed) + "% of Monthly Budget Used",
                        "You only have ₹" + remaining + " remaining from your ₹" + budgetAmt + " monthly budget.",
                        "You are in the critical threshold zone (>90% utilization).",
                        "WARNING",
                        "Limit further expenses strictly to essential groceries and necessary bills.",
                        "Calculated by comparing ₹" + currentTotal + " spending against your ₹" + budgetAmt + " overall budget limit."
                ));
            } else if (pctUsed >= 75.0) {
                insights.add(new InsightItemDTO(
                        "BUDGET_WARNING",
                        "Approaching Budget Limit: " + String.format("%.1f", pctUsed) + "% Used",
                        "You have ₹" + remaining + " remaining out of your ₹" + budgetAmt + " monthly budget.",
                        "Your spending rate is approaching the caution threshold (75%+).",
                        "WARNING",
                        "Monitor your upcoming daily expenses carefully to ensure you stay within your limit.",
                        "Remaining = Budget (₹" + budgetAmt + ") - Spending (₹" + currentTotal + ")."
                ));
            } else {
                insights.add(new InsightItemDTO(
                        "REMAINING_BUDGET",
                        "Healthy Budget Utilization (" + String.format("%.1f", pctUsed) + "% Used)",
                        "You have ₹" + remaining + " remaining in your monthly budget with healthy headroom.",
                        "Your current spending velocity is within healthy financial parameters.",
                        "SUCCESS",
                        "Great job staying under budget! Consider allocating surplus funds to savings or investments.",
                        "Remaining budget: ₹" + remaining + " available out of ₹" + budgetAmt + "."
                ));
            }
        } else {
            insights.add(new InsightItemDTO(
                    "BUDGET_WARNING",
                    "No Overall Budget Set",
                    "You have not configured an overall monthly budget for " + DateUtils.getMonthName(month) + " " + year + ".",
                    "Without a budget ceiling, it is difficult to measure overspending or track financial discipline.",
                    "INFO",
                    "Head to the 'Budgets' page and set a target monthly budget to enable automated threshold alerts.",
                    "Triggered because no record exists in the budgets table with category_id = NULL for this month."
            ));
        }

        // ==========================================
        // Insight 5: Spending Habit Recommendations
        // ==========================================
        // Check Food Category specifically
        Optional<CategoryBreakdownDTO> foodCat = currentCats.stream()
                .filter(c -> "Food".equalsIgnoreCase(c.getCategoryName()) || "Groceries".equalsIgnoreCase(c.getCategoryName()))
                .findFirst();

        if (foodCat.isPresent()) {
            CategoryBreakdownDTO prvFood = prevCatMap.get(foodCat.get().getCategoryId());
            if (prvFood != null && foodCat.get().getAmount().compareTo(prvFood.getAmount()) > 0) {
                insights.add(new InsightItemDTO(
                        "HABIT_RECOMMENDATION",
                        "Food & Dining Spending is Higher Than Last Month",
                        "You spent ₹" + foodCat.get().getAmount() + " on " + foodCat.get().getCategoryName() +
                                " vs ₹" + prvFood.getAmount() + " last month.",
                        "Dining out and takeout are common areas where small daily expenses accumulate quickly.",
                        "INFO",
                        "Review recent food transactions and consider cooking at home to lower daily food expenditure.",
                        "Direct comparison of " + foodCat.get().getCategoryName() + " category totals between current and previous month."
                ));
            }
        }

        // Check if any category spending decreased
        for (CategoryBreakdownDTO cur : currentCats) {
            CategoryBreakdownDTO prv = prevCatMap.get(cur.getCategoryId());
            if (prv != null && prv.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal savings = prv.getAmount().subtract(cur.getAmount());
                if (savings.compareTo(BigDecimal.valueOf(200)) >= 0) {
                    double savedPct = savings.divide(prv.getAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    insights.add(new InsightItemDTO(
                            "HABIT_RECOMMENDATION",
                            cur.getCategoryName() + " Spending Decreased by " + String.format("%.1f", savedPct) + "%",
                            "You spent ₹" + savings + " less on " + cur.getCategoryName() +
                                    " compared with " + DateUtils.getMonthName(prevMonth) + ".",
                            "Recognizing cost reductions helps reinforce positive financial habits.",
                            "SUCCESS",
                            "Keep up the disciplined spending in this area!",
                            "Comparison showing a measurable reduction in " + cur.getCategoryName() + " outlays."
                    ));
                    break; // Just one positive reinforcement recommendation
                }
            }
        }

        return new InsightsResponse(month, year, insights);
    }
}
