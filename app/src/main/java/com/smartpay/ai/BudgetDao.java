
package com.smartpay.ai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BudgetDao {

    // Insert a new budget
    @Insert
    long insertBudget(Budget budget);

    // Insert a monthly budget record
    @Insert
    long insertMonthlyBudget(MonthlyBudget monthlyBudget);


    // Update an existing parent budget
    @Update
    void updateBudget(Budget budget);

    // Update an existing monthly budget record
    @Update
    void updateMonthlyBudget(MonthlyBudget monthlyBudget);


    // Delete a parent budget
    @Delete
    void deleteBudget(Budget budget);

    // Delete only a selected monthly budget record
    @Delete
    void deleteMonthlyBudget(MonthlyBudget monthlyBudget);


    // Get all active budgets
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY id DESC")
    List<Budget> getAllActiveBudgets();

    // Get all budgets
    @Query("SELECT * FROM budgets ORDER BY id DESC")
    List<Budget> getAllBudgets();


    // Get monthly budgets
    @Query("SELECT * FROM monthly_budgets WHERE budgetMonth = :month ORDER BY id DESC")
    List<MonthlyBudget> getMonthlyBudgets(String month);

    // Get all monthly budget records
    @Query("SELECT * FROM monthly_budgets ORDER BY id DESC")
    List<MonthlyBudget> getAllMonthlyBudgets();


    // Get monthly budget by ID
    @Query("SELECT * FROM monthly_budgets WHERE id = :id LIMIT 1")
    MonthlyBudget getMonthlyBudgetById(int id);

    // Get parent budget by ID
    @Query("SELECT * FROM budgets WHERE id = :budgetId LIMIT 1")
    Budget getBudgetById(int budgetId);


    // Get monthly budget history for one budget
    @Query("SELECT * FROM monthly_budgets WHERE budgetId = :budgetId ORDER BY budgetMonth DESC")
    List<MonthlyBudget> getBudgetHistory(int budgetId);


    // Get monthly total budget amount
    @Query("SELECT COALESCE(SUM(amount), 0) FROM monthly_budgets WHERE budgetMonth = :month")
    long getMonthlyBudgetTotal(String month);


    // Get pending payments
    @Query("SELECT * FROM monthly_budgets WHERE budgetMonth = :month AND paymentStatus = 'PENDING' AND reminderEnabled = 1")
    List<MonthlyBudget> getPendingReminderBudgets(String month);


    // Check whether a monthly record already exists
    @Query("SELECT COUNT(*) FROM monthly_budgets WHERE budgetId = :budgetId AND budgetMonth = :month")
    int getMonthlyBudgetCount(int budgetId, String month);

}
