package com.smartspend.service;

import com.smartspend.dto.NotificationResponse;
import com.smartspend.entity.Budget;
import com.smartspend.entity.Notification;
import com.smartspend.entity.User;
import com.smartspend.exception.ResourceNotFoundException;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.NotificationRepository;
import com.smartspend.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthService authService;

    public NotificationService(NotificationRepository notificationRepository,
                               BudgetRepository budgetRepository,
                               ExpenseRepository expenseRepository,
                               AuthService authService) {
        this.notificationRepository = notificationRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        User user = authService.getCurrentUserEntity();
        return notificationRepository.findTop20ByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = authService.getCurrentUserEntity();
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long id) {
        User user = authService.getCurrentUserEntity();
        Notification notification = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = authService.getCurrentUserEntity();
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }

    @Transactional
    public void checkAndCreateBudgetAlerts(User user, int month, int year) {
        LocalDate startDate = DateUtils.getStartOfMonth(month, year);
        LocalDate endDate = DateUtils.getEndOfMonth(month, year);
        LocalDateTime since = LocalDateTime.now().minusHours(12);

        // 1. Overall Budget Check
        budgetRepository.findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(user, month, year)
                .ifPresent(budget -> {
                    BigDecimal spent = expenseRepository.sumAmountByUserAndDateRange(user, startDate, endDate);
                    if (spent == null) spent = BigDecimal.ZERO;
                    if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        double pct = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).doubleValue();

                        if (pct >= 100.0) {
                            String title = "Budget Exceeded Alert (" + DateUtils.getMonthName(month) + ")";
                            if (!notificationRepository.existsByUserAndTitleAndCreatedAtAfter(user, title, since)) {
                                Notification n = new Notification(
                                        user,
                                        title,
                                        "You have exceeded your monthly budget of ₹" + budget.getAmount() +
                                                ". Total spent: ₹" + spent + " (" + String.format("%.1f", pct) + "%).",
                                        "BUDGET_EXCEEDED"
                                );
                                notificationRepository.save(n);
                            }
                        } else if (pct >= 90.0) {
                            String title = "Critical Budget Alert (" + DateUtils.getMonthName(month) + ")";
                            if (!notificationRepository.existsByUserAndTitleAndCreatedAtAfter(user, title, since)) {
                                Notification n = new Notification(
                                        user,
                                        title,
                                        "Critical warning: " + String.format("%.1f", pct) + "% of your monthly budget used. Only ₹" +
                                                budget.getAmount().subtract(spent) + " remaining.",
                                        "BUDGET_WARNING"
                                );
                                notificationRepository.save(n);
                            }
                        } else if (pct >= 75.0) {
                            String title = "Approaching Budget Limit (" + DateUtils.getMonthName(month) + ")";
                            if (!notificationRepository.existsByUserAndTitleAndCreatedAtAfter(user, title, since)) {
                                Notification n = new Notification(
                                        user,
                                        title,
                                        "You have used " + String.format("%.1f", pct) + "% of your monthly budget. Remaining: ₹" +
                                                budget.getAmount().subtract(spent) + ".",
                                        "BUDGET_WARNING"
                                );
                                notificationRepository.save(n);
                            }
                        }
                    }
                });

        // 2. Category Budgets Check
        List<Budget> categoryBudgets = budgetRepository.findByUserAndBudgetMonthAndBudgetYear(user, month, year);
        for (Budget b : categoryBudgets) {
            if (b.getCategory() != null) {
                BigDecimal spent = expenseRepository.sumAmountByUserAndCategoryAndDateRange(user, b.getCategory(), startDate, endDate);
                if (spent == null) spent = BigDecimal.ZERO;
                if (b.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    double pct = spent.divide(b.getAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    if (pct >= 100.0) {
                        String title = b.getCategory().getName() + " Budget Exceeded";
                        if (!notificationRepository.existsByUserAndTitleAndCreatedAtAfter(user, title, since)) {
                            Notification n = new Notification(
                                    user,
                                    title,
                                    "Your " + b.getCategory().getName() + " budget has been exceeded! Spent: ₹" +
                                            spent + " / ₹" + b.getAmount() + ".",
                                    "BUDGET_EXCEEDED"
                            );
                            notificationRepository.save(n);
                        }
                    }
                }
            }
        }
    }

    public NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getNotificationType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
