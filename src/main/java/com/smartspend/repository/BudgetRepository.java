package com.smartspend.repository;

import com.smartspend.entity.Budget;
import com.smartspend.entity.Category;
import com.smartspend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndBudgetMonthAndBudgetYear(User user, Integer budgetMonth, Integer budgetYear);

    Optional<Budget> findByIdAndUser(Long id, User user);

    Optional<Budget> findByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(User user, Integer budgetMonth, Integer budgetYear);

    Optional<Budget> findByUserAndCategoryAndBudgetMonthAndBudgetYear(User user, Category category, Integer budgetMonth, Integer budgetYear);

    boolean existsByUserAndCategoryIsNullAndBudgetMonthAndBudgetYear(User user, Integer budgetMonth, Integer budgetYear);

    boolean existsByUserAndCategoryAndBudgetMonthAndBudgetYear(User user, Category category, Integer budgetMonth, Integer budgetYear);
}
