package com.ritesh.expensetracker.service;

import com.ritesh.expensetracker.model.Expense;
import com.ritesh.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    // Get all expenses
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    // Get expense by ID
    public Expense getExpenseById(Long id) {
        Optional<Expense> expense = expenseRepository.findById(id);
        return expense.orElse(null);
    }

    // Create new expense
    public Expense createExpense(Expense expense) {
        // Development mode - minimal validation
        if (expense.getAmount() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        // Set current date if not provided
        if (expense.getDate() == null || expense.getDate().isEmpty()) {
            expense.setDate(java.time.LocalDate.now().toString());
        }
        
        return expenseRepository.save(expense);
    }

    // Update expense
    public Expense updateExpense(Long id, Expense expense) {
        Optional<Expense> existingExpenseOpt = expenseRepository.findById(id);
        if (existingExpenseOpt.isEmpty()) {
            throw new IllegalArgumentException("Expense not found with id: " + id);
        }
        
        Expense existingExpense = existingExpenseOpt.get();
        
        // Update fields
        existingExpense.setDescription(expense.getDescription());
        existingExpense.setAmount(expense.getAmount());
        existingExpense.setCategory(expense.getCategory());
        existingExpense.setDate(expense.getDate());
        
        return expenseRepository.save(existingExpense);
    }

    // Delete expense
    public boolean deleteExpense(Long id) {
        if (expenseRepository.existsById(id)) {
            expenseRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Get total expenses amount
    public double getTotalExpenses() {
        return expenseRepository.findAll()
                .stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    //Get total expenses amount by category
    public double getTotalExpensesByCategory(String category){
        return expenseRepository.findAll()
                .stream()
                .filter(expense -> expense.getCategory().equals(category))
                .mapToDouble(Expense::getAmount)
                .sum();
    }
} 