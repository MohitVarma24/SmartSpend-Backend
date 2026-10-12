package com.smartspend.repository;

import com.smartspend.entity.Category;
import com.smartspend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIsNullOrderByNameAsc();

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user = :user ORDER BY c.name ASC")
    List<Category> findAllAvailableForUser(@Param("user") User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findByNameIgnoreCaseAndUserIsNull(String name);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);
}
