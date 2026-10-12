package com.smartspend.service;

import com.smartspend.dto.CategoryResponse;
import com.smartspend.dto.ExpensePageResponse;
import com.smartspend.dto.ExpenseRequest;
import com.smartspend.dto.ExpenseResponse;
import com.smartspend.entity.Category;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.exception.BadRequestException;
import com.smartspend.exception.ResourceNotFoundException;
import com.smartspend.repository.ExpenseRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AuthService authService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          AuthService authService,
                          CategoryService categoryService,
                          @Lazy NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.authService = authService;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = authService.getCurrentUserEntity();
        Category category = categoryService.getCategoryEntityForUser(request.getCategoryId());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        Expense expense = new Expense(
                user,
                category,
                request.getAmount(),
                request.getDescription().trim(),
                request.getExpenseDate(),
                request.getPaymentMethod(),
                request.getNotes()
        );

        Expense saved = expenseRepository.save(expense);

        // Check if budget thresholds are exceeded or approaching
        notificationService.checkAndCreateBudgetAlerts(user, request.getExpenseDate().getMonthValue(), request.getExpenseDate().getYear());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        User user = authService.getCurrentUserEntity();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return mapToResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = authService.getCurrentUserEntity();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        Category category = categoryService.getCategoryEntityForUser(request.getCategoryId());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription().trim());
        expense.setCategory(category);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNotes(request.getNotes());

        Expense updated = expenseRepository.save(expense);

        notificationService.checkAndCreateBudgetAlerts(user, request.getExpenseDate().getMonthValue(), request.getExpenseDate().getYear());

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteExpense(Long id) {
        User user = authService.getCurrentUserEntity();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse getExpenses(String search,
                                           Long categoryId,
                                           String paymentMethod,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           int page,
                                           int size,
                                           String sortBy,
                                           String sortDir) {
        User user = authService.getCurrentUserEntity();

        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                StringUtils.hasText(sortBy) ? sortBy : "expenseDate"
        );
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

        Specification<Expense> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly filter by current user
            predicates.add(cb.equal(root.get("user"), user));

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate notesPredicate = cb.like(cb.lower(root.get("notes")), searchPattern);
                predicates.add(cb.or(descPredicate, notesPredicate));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (StringUtils.hasText(paymentMethod)) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod.trim()));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Expense> expensePage = expenseRepository.findAll(spec, pageable);

        List<ExpenseResponse> responseList = expensePage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new ExpensePageResponse(
                responseList,
                expensePage.getNumber(),
                expensePage.getSize(),
                expensePage.getTotalElements(),
                expensePage.getTotalPages(),
                expensePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getRecentExpenses(int limit, Integer month, Integer year) {
        User user = authService.getCurrentUserEntity();
        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            List<Expense> list = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, start, end);
            return list.stream().limit(limit).map(this::mapToResponse).toList();
        } else {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "expenseDate", "createdAt"));
            return expenseRepository.findByUserOrderByExpenseDateDescCreatedAtDesc(user, pageable)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesForPeriod(User user, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user, start, end)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ExpenseResponse mapToResponse(Expense expense) {
        CategoryResponse categoryResponse = new CategoryResponse(
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getIcon(),
                expense.getCategory().getColor(),
                expense.getCategory().getUser() != null
        );

        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                categoryResponse,
                expense.getExpenseDate(),
                expense.getPaymentMethod(),
                expense.getNotes(),
                expense.getCreatedAt()
        );
    }
}
