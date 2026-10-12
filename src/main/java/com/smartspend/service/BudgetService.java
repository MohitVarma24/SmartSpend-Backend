package com.smartspend.service;

import com.smartspend.dto.BudgetRequest;
import com.smartspend.dto.BudgetResponse;
import com.smartspend.dto.CategoryResponse;
import com.smartspend.entity.Budget;
import com.smartspend.entity.Category;
import com.smartspend.entity.User;
import com.smartspend.exception.BadRequestException;
import com.smartspend.exception.ResourceNotFoundException;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.util.DateUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         AuthService authService,
                         CategoryService categoryService,
                         @Lazy NotificationService notificationService) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.authService = authService;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = authService.getCurrentUserEntity();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than zero");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.getCategoryEntityForUser(request.getCategoryId());
            if (budgetRepository.existsByUserAndCategoryAndBudgetMonthAndBudgetYear(
                    user, category, request.getBudgetMonth(), request.getBudgetYear())) {
                throw new BadRequestException("A budget for category '" + category.getName() + "' already exists for " +
                        DateUtils.getMonthName(request.getBudgetMonth()) + " " + request.getBudgetYear());
            }
        } else {
            if (budgetRepository.existsByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(
                    user, request.getBudgetMonth(), request.getBudgetYear())) {
                throw new BadRequestException("An overall monthly budget already exists for " +
                        DateUtils.getMonthName(request.getBudgetMonth()) + " " + request.getBudgetYear());
            }
        }

        Budget budget = new Budget(
                user,
                category,
                request.getAmount(),
                request.getBudgetMonth(),
                request.getBudgetYear()
        );

        Budget saved = budgetRepository.save(budget);

        // Check alerts
        notificationService.checkAndCreateBudgetAlerts(user, request.getBudgetMonth(), request.getBudgetYear());

        return mapToResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForMonth(int month, int year) {
        User user = authService.getCurrentUserEntity();
        List<Budget> budgets = budgetRepository.findByUserAndBudgetMonthAndBudgetYear(user, month, year);
        return budgets.stream()
                .map(b -> mapToResponse(b, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<BudgetResponse> getOverallBudget(int month, int year) {
        User user = authService.getCurrentUserEntity();
        return budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(user, month, year)
                .map(b -> mapToResponse(b, user));
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        User user = authService.getCurrentUserEntity();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Budget amount must be greater than zero");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.getCategoryEntityForUser(request.getCategoryId());

            // Check if changing category conflicts with an existing one
            Optional<Budget> existing = budgetRepository.findByUserAndCategoryAndBudgetMonthAndBudgetYear(
                    user, category, request.getBudgetMonth(), request.getBudgetYear());
            if (existing.isPresent() && !existing.get().getId().equals(budget.getId())) {
                throw new BadRequestException("A budget for category '" + category.getName() + "' already exists for this period");
            }
        } else {
            Optional<Budget> existing = budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(
                    user, request.getBudgetMonth(), request.getBudgetYear());
            if (existing.isPresent() && !existing.get().getId().equals(budget.getId())) {
                throw new BadRequestException("An overall monthly budget already exists for this period");
            }
        }

        budget.setAmount(request.getAmount());
        budget.setCategory(category);
        budget.setBudgetMonth(request.getBudgetMonth());
        budget.setBudgetYear(request.getBudgetYear());

        Budget updated = budgetRepository.save(budget);

        notificationService.checkAndCreateBudgetAlerts(user, request.getBudgetMonth(), request.getBudgetYear());

        return mapToResponse(updated, user);
    }

    @Transactional
    public void deleteBudget(Long id) {
        User user = authService.getCurrentUserEntity();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        budgetRepository.delete(budget);
    }

    public BudgetResponse mapToResponse(Budget budget, User user) {
        LocalDate startDate = DateUtils.getStartOfMonth(budget.getBudgetMonth(), budget.getBudgetYear());
        LocalDate endDate = DateUtils.getEndOfMonth(budget.getBudgetMonth(), budget.getBudgetYear());

        BigDecimal spent;
        if (budget.getCategory() == null) {
            // overall budget
            spent = expenseRepository.sumAmountByUserAndDateRange(user, startDate, endDate);
        } else {
            spent = expenseRepository.sumAmountByUserAndCategoryAndDateRange(user, budget.getCategory(), startDate, endDate);
        }

        if (spent == null) spent = BigDecimal.ZERO;

        BigDecimal remaining = budget.getAmount().subtract(spent);

        double percentage = 0.0;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        String status;
        String statusLabel;
        if (percentage >= 100.0) {
            status = "EXCEEDED";
            statusLabel = "Budget Exceeded";
        } else if (percentage >= 90.0) {
            status = "CRITICAL";
            statusLabel = "Critical Warning (90%+ Used)";
        } else if (percentage >= 75.0) {
            status = "APPROACHING";
            statusLabel = "Approaching Limit (75%+ Used)";
        } else {
            status = "NORMAL";
            statusLabel = "Normal Spending";
        }

        CategoryResponse categoryResponse = null;
        if (budget.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                    budget.getCategory().getId(),
                    budget.getCategory().getName(),
                    budget.getCategory().getIcon(),
                    budget.getCategory().getColor(),
                    budget.getCategory().getUser() != null
            );
        }

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setAmount(budget.getAmount());
        response.setBudgetMonth(budget.getBudgetMonth());
        response.setBudgetYear(budget.getBudgetYear());
        response.setCategory(categoryResponse);
        response.setSpentAmount(spent);
        response.setRemainingAmount(remaining);
        response.setPercentageUsed(percentage);
        response.setStatus(status);
        response.setStatusLabel(statusLabel);

        return response;
    }
}
