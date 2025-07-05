package com.ritesh.expensetracker.repository;

import com.ritesh.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // Spring Data JPA provides all basic CRUD operations:
    // - save(Expense entity) - save or update
    // - findById(Long id) - find by ID
    // - findAll() - get all expenses
    // - deleteById(Long id) - delete by ID
    // - delete(Expense entity) - delete entity
    
    // You can add custom query methods here if needed
    // For example:
    // List<Expense> findByCategory(String category);
    // List<Expense> findByAmountGreaterThan(double amount);
} 