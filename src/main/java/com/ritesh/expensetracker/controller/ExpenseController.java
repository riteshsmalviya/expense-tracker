package com.ritesh.expensetracker.controller;

import com.ritesh.expensetracker.model.Expense;
import com.ritesh.expensetracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*") // Allow requests from any origin
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // GET /api/expenses - Get all expenses
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        return ResponseEntity.ok(expenses);
    }

    // GET /api/expenses/{id} - Get expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
        Expense expense = expenseService.getExpenseById(id);
        if (expense == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(expense);
    }

    // POST /api/expenses - Create new expense
    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        try {
            Expense createdExpense = expenseService.createExpense(expense);
            return ResponseEntity.ok(createdExpense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // POST /api/expenses/bulk - Create multiple expenses
    @PostMapping("/bulk")
    public ResponseEntity<List<Expense>> createExpenses(@RequestBody List<Expense> expenses) {
        try {
            List<Expense> createdExpenses = new ArrayList<>();
            for (Expense expense : expenses) {
                createdExpenses.add(expenseService.createExpense(expense));
            }
            return ResponseEntity.ok(createdExpenses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/expenses/{id} - Update expense
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        try {
            Expense updatedExpense = expenseService.updateExpense(id, expense);
            return ResponseEntity.ok(updatedExpense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/expenses/{id} - Delete expense
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        boolean deleted = expenseService.deleteExpense(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/expenses/total - Get total expenses amount
    @GetMapping("/total")
    public ResponseEntity<Double> getTotalExpenses() {
        double total = expenseService.getTotalExpenses();
        return ResponseEntity.ok(total);
    }

    //GET /api/expenses/total/{category} - Get total expenses amount by category
    @GetMapping("total/{category}")
    public ResponseEntity<Double> getTotalExpensesByCategory(@PathVariable String category){
        double total = expenseService.getTotalExpensesByCategory(category);
        return ResponseEntity.ok(total);
    }

    //This is just to show this message on frontend
    @GetMapping("/")
    public ResponseEntity<String> randomMessage() {
        return ResponseEntity.ok("Welcome to the Expense Tracker API");
    }
} 