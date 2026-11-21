
package com.smartpay.ai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "app_notifications")
public class AppNotification {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;

    public String message;

    public int monthlyBudgetId;

    public String notificationType;

    @ColumnInfo(defaultValue = "0")
    public boolean isRead = false;

    public long createdAt;

    @ColumnInfo(defaultValue = "0")
    public boolean actionEnabled = false;

    public AppNotification() {
    }
}
