package com.smartspend.repository;

import com.smartspend.entity.Category;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUser(Long id, User user);

    List<Expense> findByUserAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(User user, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserOrderByExpenseDateDescCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.category = :category AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndCategoryAndDateRange(@Param("user") User user, @Param("category") Category category, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT e.category.id as categoryId, e.category.name as categoryName, e.category.icon as icon, e.category.color as color, SUM(e.amount) as totalAmount, COUNT(e) as transactionCount " +
           "FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category.id, e.category.name, e.category.icon, e.category.color " +
           "ORDER BY totalAmount DESC")
    List<CategorySpendingProjection> getCategorySpendingBreakdown(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT e.expenseDate as expenseDate, SUM(e.amount) as totalAmount, COUNT(e) as transactionCount " +
           "FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.expenseDate " +
           "ORDER BY e.expenseDate ASC")
    List<DailySpendingProjection> getDailySpending(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    interface CategorySpendingProjection {
        Long getCategoryId();
        String getCategoryName();
        String getIcon();
        String getColor();
        BigDecimal getTotalAmount();
        Long getTransactionCount();
    }

    interface DailySpendingProjection {
        LocalDate getExpenseDate();
        BigDecimal getTotalAmount();
        Long getTransactionCount();
    }
}
