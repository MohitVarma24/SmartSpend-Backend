package com.smartspend.service;

import com.smartspend.dto.CategoryRequest;
import com.smartspend.dto.CategoryResponse;
import com.smartspend.entity.Category;
import com.smartspend.entity.User;
import com.smartspend.exception.BadRequestException;
import com.smartspend.exception.ResourceNotFoundException;
import com.smartspend.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public CategoryService(CategoryRepository categoryRepository, AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultCategories() {
        if (categoryRepository.findByUserIsNullOrderByNameAsc().isEmpty()) {
            List<Category> defaultCategories = Arrays.asList(
                    new Category("Food", "Utensils", "#EF4444", null),
                    new Category("Groceries", "ShoppingCart", "#10B981", null),
                    new Category("Transportation", "Car", "#3B82F6", null),
                    new Category("Shopping", "ShoppingBag", "#8B5CF6", null),
                    new Category("Education", "GraduationCap", "#F59E0B", null),
                    new Category("Entertainment", "Film", "#EC4899", null),
                    new Category("Health", "HeartPulse", "#14B8A6", null),
                    new Category("Bills and Utilities", "Receipt", "#6366F1", null),
                    new Category("Rent", "Home", "#F97316", null),
                    new Category("Travel", "Plane", "#06B6D4", null),
                    new Category("Personal Care", "Sparkles", "#D946EF", null),
                    new Category("Other", "MoreHorizontal", "#64748B", null)
            );
            categoryRepository.saveAll(defaultCategories);
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForCurrentUser() {
        User user = authService.getCurrentUserEntity();
        return categoryRepository.findAllAvailableForUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntityForUser(Long categoryId) {
        User user = authService.getCurrentUserEntity();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Category must either be system default (user is null) or owned by this user
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return category;
    }

    @Transactional
    public CategoryResponse createCustomCategory(CategoryRequest request) {
        User user = authService.getCurrentUserEntity();
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name) ||
                categoryRepository.existsByNameIgnoreCaseAndUser(name, user)) {
            throw new BadRequestException("Category with name '" + name + "' already exists");
        }

        String icon = (request.getIcon() != null && !request.getIcon().isBlank()) ? request.getIcon().trim() : "Tag";
        String color = (request.getColor() != null && !request.getColor().isBlank()) ? request.getColor().trim() : "#64748B";

        Category category = new Category(name, icon, color, user);
        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCustomCategory(Long id, CategoryRequest request) {
        User user = authService.getCurrentUserEntity();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Custom category not found with id: " + id));

        String name = request.getName().trim();
        category.setName(name);
        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            category.setIcon(request.getIcon().trim());
        }
        if (request.getColor() != null && !request.getColor().isBlank()) {
            category.setColor(request.getColor().trim());
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCustomCategory(Long id) {
        User user = authService.getCurrentUserEntity();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Custom category not found with id: " + id));
        categoryRepository.delete(category);
    }

    public CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getUser() != null
        );
    }
}
