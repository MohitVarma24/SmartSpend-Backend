package com.smartspend;

import com.smartspend.dto.*;
import com.smartspend.entity.Category;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.exception.BadRequestException;
import com.smartspend.exception.ResourceNotFoundException;
import com.smartspend.repository.CategoryRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.UserRepository;
import com.smartspend.security.UserPrincipal;
import com.smartspend.service.BudgetService;
import com.smartspend.service.CategoryService;
import com.smartspend.service.ExpenseService;
import com.smartspend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseAndBudgetServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        categoryService.seedDefaultCategories();

        userA = userRepository.save(new User("User A", "usera@example.com", passwordEncoder.encode("pass123"), "INR"));
        userB = userRepository.save(new User("User B", "userb@example.com", passwordEncoder.encode("pass123"), "INR"));

        foodCategory = categoryRepository.findByNameIgnoreCaseAndUserIsNull("Food")
                .orElseThrow();
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testExpenseCrudAndDataIsolation() {
        // Authenticate as User A
        authenticateAs(userA);

        ExpenseRequest req = new ExpenseRequest();
        req.setAmount(new BigDecimal("350.50"));
        req.setDescription("Lunch with colleagues");
        req.setCategoryId(foodCategory.getId());
        req.setExpenseDate(LocalDate.now());
        req.setPaymentMethod("UPI");
        req.setNotes("Office lunch");

        ExpenseResponse created = expenseService.createExpense(req);
        assertNotNull(created.getId());
        assertEquals(new BigDecimal("350.50"), created.getAmount());
        assertEquals("Lunch with colleagues", created.getDescription());

        // Update expense as User A
        req.setDescription("Buffet Lunch");
        req.setAmount(new BigDecimal("400.00"));
        ExpenseResponse updated = expenseService.updateExpense(created.getId(), req);
        assertEquals("Buffet Lunch", updated.getDescription());
        assertEquals(new BigDecimal("400.00"), updated.getAmount());

        // Switch context to User B and attempt to access or modify User A's expense
        authenticateAs(userB);

        assertThrows(ResourceNotFoundException.class, () -> expenseService.getExpenseById(created.getId()));
        assertThrows(ResourceNotFoundException.class, () -> expenseService.updateExpense(created.getId(), req));
        assertThrows(ResourceNotFoundException.class, () -> expenseService.deleteExpense(created.getId()));

        // Back to User A: deletion succeeds
        authenticateAs(userA);
        assertDoesNotThrow(() -> expenseService.deleteExpense(created.getId()));
    }

    @Test
    void testBudgetCalculationsAndThresholds() {
        authenticateAs(userA);

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // 1. Create Overall Budget: 1000
        BudgetRequest bReq = new BudgetRequest();
        bReq.setAmount(new BigDecimal("1000.00"));
        bReq.setBudgetMonth(month);
        bReq.setBudgetYear(year);
        bReq.setCategoryId(null);

        BudgetResponse budgetRes = budgetService.createBudget(bReq);
        assertEquals(new BigDecimal("1000.00"), budgetRes.getAmount());
        assertEquals(new BigDecimal("1000.00"), budgetRes.getRemainingAmount());
        assertEquals(0.0, budgetRes.getPercentageUsed());
        assertEquals("NORMAL", budgetRes.getStatus());

        // Prevent duplicate overall budget for same month
        assertThrows(BadRequestException.class, () -> budgetService.createBudget(bReq));

        // Add expense: 800 (80% used -> APPROACHING)
        ExpenseRequest eReq = new ExpenseRequest();
        eReq.setAmount(new BigDecimal("800.00"));
        eReq.setDescription("Groceries");
        eReq.setCategoryId(foodCategory.getId());
        eReq.setExpenseDate(LocalDate.now());
        expenseService.createExpense(eReq);

        List<BudgetResponse> budgets = budgetService.getBudgetsForMonth(month, year);
        assertEquals(1, budgets.size());
        assertEquals(new BigDecimal("800.00"), budgets.get(0).getSpentAmount());
        assertEquals(new BigDecimal("200.00"), budgets.get(0).getRemainingAmount());
        assertEquals(80.0, budgets.get(0).getPercentageUsed());
        assertEquals("APPROACHING", budgets.get(0).getStatus());

        // Add expense: 300 (total 1100 -> 110% EXCEEDED, remaining -100)
        eReq.setAmount(new BigDecimal("300.00"));
        eReq.setDescription("Dinner");
        expenseService.createExpense(eReq);

        budgets = budgetService.getBudgetsForMonth(month, year);
        assertEquals(new BigDecimal("1100.00"), budgets.get(0).getSpentAmount());
        assertEquals(new BigDecimal("-100.00"), budgets.get(0).getRemainingAmount());
        assertEquals(110.0, budgets.get(0).getPercentageUsed());
        assertEquals("EXCEEDED", budgets.get(0).getStatus());
    }

    @Test
    void testPdfAndCsvReportGeneration() {
        authenticateAs(userA);

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        ExpenseRequest eReq = new ExpenseRequest();
        eReq.setAmount(new BigDecimal("250.00"));
        eReq.setDescription("Coffee & Snacks");
        eReq.setCategoryId(foodCategory.getId());
        eReq.setExpenseDate(LocalDate.now());
        eReq.setPaymentMethod("Cash");
        expenseService.createExpense(eReq);

        byte[] pdfBytes = reportService.generatePdfReport(month, year);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should contain valid PDF structure");

        byte[] csvBytes = reportService.generateCsvReport(month, year);
        assertNotNull(csvBytes);
        String csvString = new String(csvBytes);
        assertTrue(csvString.contains("Coffee & Snacks"));
        assertTrue(csvString.contains("Food"));
        assertTrue(csvString.contains("250.00"));
    }
}
