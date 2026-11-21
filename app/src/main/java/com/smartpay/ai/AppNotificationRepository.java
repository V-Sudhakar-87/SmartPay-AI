
package com.smartpay.ai;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppNotificationRepository {

    private final AppNotificationDao notificationDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    public AppNotificationRepository(Context context) {

        SmartPayDatabase database =
                SmartPayDatabase.getDatabase(context);

        notificationDao = database.appNotificationDao();
    }


    // Save notification
    public void insertNotification(
            AppNotification notification,
            OnNotificationSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                long id = notificationDao.insertNotification(notification);

                if (listener != null) {
                    listener.onSuccess((int) id);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }


    // Load notification history
    public void getAllNotifications(
            OnNotificationsLoadedListener listener
    ) {

        executorService.execute(() -> {

            try {

                List<AppNotification> notifications =
                        notificationDao.getAllNotifications();

                if (listener != null) {
                    listener.onSuccess(notifications);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }


    // Get unread count
    public void getUnreadCount(
            OnUnreadCountListener listener
    ) {

        executorService.execute(() -> {

            try {

                int count = notificationDao.getUnreadCount();

                if (listener != null) {
                    listener.onSuccess(count);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }


    // Mark notification as read
    public void markAsRead(int notificationId) {

        executorService.execute(() ->
                notificationDao.markAsRead(notificationId)
        );
    }


    // Mark all as read
    public void markAllAsRead() {

        executorService.execute(() ->
                notificationDao.markAllAsRead()
        );
    }


    // Disable previous due-date notification buttons
    public void disableOldDueDateActions(int monthlyBudgetId) {

        executorService.execute(() ->
                notificationDao.disableOldDueDateActions(monthlyBudgetId)
        );
    }


    // Enable action buttons for a notification
    public void enableNotificationAction(int notificationId) {

        executorService.execute(() ->
                notificationDao.enableNotificationAction(notificationId)
        );
    }


    // Disable all action buttons for a budget
    public void disableAllBudgetActions(int monthlyBudgetId) {

        executorService.execute(() ->
                notificationDao.disableAllBudgetActions(monthlyBudgetId)
        );
    }


    // Get latest active due-date notification
    public void getActiveDueNotification(
            int monthlyBudgetId,
            OnActiveDueNotificationListener listener
    ) {

        executorService.execute(() -> {

            try {

                AppNotification notification =
                        notificationDao.getActiveDueNotification(
                                monthlyBudgetId
                        );

                if (listener != null) {
                    listener.onSuccess(notification);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }


    // Delete one notification
    public void deleteNotification(int notificationId) {

        executorService.execute(() ->
                notificationDao.deleteNotification(notificationId)
        );
    }


    // Delete all history
    public void deleteAllNotifications() {

        executorService.execute(() ->
                notificationDao.deleteAllNotifications()
        );
    }


    // CALLBACKS

    public interface OnNotificationSavedListener {

        void onSuccess(int notificationId);

        void onError(String error);
    }


    public interface OnNotificationsLoadedListener {

        void onSuccess(List<AppNotification> notifications);

        void onError(String error);
    }


    public interface OnUnreadCountListener {

        void onSuccess(int count);

        void onError(String error);
    }


    public interface OnActiveDueNotificationListener {

        void onSuccess(AppNotification notification);

        void onError(String error);
    }
}
