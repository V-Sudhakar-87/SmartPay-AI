
package com.smartpay.ai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "budgets")
public class Budget {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Budget details
    public String budgetName;
    public String category;

    // Amount stored in paise
    public long amount;

    // Monthly due date (1 to 31)
    // 0 means no due date
    public int dueDay;

    // Recurring budget or one-time budget
    @ColumnInfo(defaultValue = "1")
    public boolean isRecurring;

    // Reminder enabled or disabled
    @ColumnInfo(defaultValue = "0")
    public boolean reminderEnabled;

    // Budget starting month (example: 2026-11)
    public String startMonth;

    // Active or inactive budget
    @ColumnInfo(defaultValue = "1")
    public boolean isActive;

    // Created timestamp
    public long createdAt;

    public Budget() {
    }
}
