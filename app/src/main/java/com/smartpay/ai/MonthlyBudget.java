
package com.smartpay.ai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "monthly_budgets")
public class MonthlyBudget {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Parent budget ID
    public int budgetId;

    // Budget details
    public String budgetName;
    public String category;

    // Amount stored in paise
    public long amount;

    // Example: 2026-11
    public String budgetMonth;

    // Due date (1 to 31)
    // 0 means no due date
    public int dueDay;

    // Payment status
    @ColumnInfo(defaultValue = "'PENDING'")
    public String paymentStatus;

    // Actual payment date
    public long paidAt;

    // Reminder enabled or disabled
    @ColumnInfo(defaultValue = "0")
    public boolean reminderEnabled;

    // Creation timestamp
    public long createdAt;

    public MonthlyBudget() {
    }
}
