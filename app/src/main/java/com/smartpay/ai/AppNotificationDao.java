
package com.smartpay.ai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppNotificationDao {

    // Save notification
    @Insert
    long insertNotification(AppNotification notification);


    // Get all notification history
    @Query("SELECT * FROM app_notifications ORDER BY createdAt DESC")
    List<AppNotification> getAllNotifications();


    // Get unread notification count
    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    int getUnreadCount();


    // Mark notification as read
    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);


    // Mark all notifications as read
    @Query("UPDATE app_notifications SET isRead = 1")
    void markAllAsRead();


    // Disable old due-date notification buttons
    @Query("UPDATE app_notifications SET actionEnabled = 0 WHERE monthlyBudgetId = :monthlyBudgetId AND notificationType = 'BUDGET_DUE_TODAY'")
    void disableOldDueDateActions(int monthlyBudgetId);


    // Enable action buttons for a particular notification
    @Query("UPDATE app_notifications SET actionEnabled = 1 WHERE id = :notificationId AND notificationType = 'BUDGET_DUE_TODAY'")
    void enableNotificationAction(int notificationId);


    // Disable all action buttons for a particular monthly budget
    @Query("UPDATE app_notifications SET actionEnabled = 0 WHERE monthlyBudgetId = :monthlyBudgetId")
    void disableAllBudgetActions(int monthlyBudgetId);


    // Get latest active due-date notification
    @Query("SELECT * FROM app_notifications WHERE monthlyBudgetId = :monthlyBudgetId AND notificationType = 'BUDGET_DUE_TODAY' AND actionEnabled = 1 ORDER BY createdAt DESC LIMIT 1")
    AppNotification getActiveDueNotification(int monthlyBudgetId);


    // Delete one notification
    @Query("DELETE FROM app_notifications WHERE id = :notificationId")
    void deleteNotification(int notificationId);


    // Delete all notification history
    @Query("DELETE FROM app_notifications")
    void deleteAllNotifications();
}
