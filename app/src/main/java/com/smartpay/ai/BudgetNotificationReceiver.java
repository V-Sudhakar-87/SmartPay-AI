
package com.smartpay.ai;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetNotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_PAID =
            "com.smartpay.ai.BUDGET_PAID";

    public static final String ACTION_NOT_PAID =
            "com.smartpay.ai.BUDGET_NOT_PAID";

    public static final String ACTION_REPEAT =
            "com.smartpay.ai.BUDGET_REPEAT";

    private static final long REPEAT_INTERVAL =
            4 * 60 * 60 * 1000L;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;

        String action = intent.getAction();

        int monthlyBudgetId =
                intent.getIntExtra("monthlyBudgetId", -1);

        if (monthlyBudgetId == -1) return;


        // PAID / NOT PAID button clicked
        if (ACTION_PAID.equals(action)
                || ACTION_NOT_PAID.equals(action)) {

            savePaymentStatus(
                    context,
                    monthlyBudgetId,
                    ACTION_PAID.equals(action)
            );

            return;
        }


        String budgetName =
                intent.getStringExtra("budgetName");

        int daysBefore =
                intent.getIntExtra("daysBefore", 0);


        // Repeat reminder
        if (ACTION_REPEAT.equals(action)) {
            daysBefore = 0;
        }


        showBudgetNotification(
                context,
                monthlyBudgetId,
                budgetName,
                daysBefore
        );
    }


    // Show Android notification after checking payment status
    private void showBudgetNotification(
            Context context,
            int monthlyBudgetId,
            String budgetName,
            int daysBefore) {

        final PendingResult pendingResult = goAsync();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            try {

                SmartPayDatabase database =
                        SmartPayDatabase.getDatabase(context);

                BudgetDao budgetDao = database.budgetDao();

                MonthlyBudget budget =
                        budgetDao.getMonthlyBudgetById(monthlyBudgetId);


                // Do not send reminders if budget is missing or already paid
                if (budget == null
                        || "PAID".equalsIgnoreCase(budget.paymentStatus)) {

                    cancelRepeatReminder(context, monthlyBudgetId);

                    NotificationManager manager =
                            (NotificationManager) context.getSystemService(
                                    Context.NOTIFICATION_SERVICE);

                    if (manager != null) {
                        manager.cancel(monthlyBudgetId);
                    }

                    return;
                }


                NotificationManager manager =
                        (NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);

                if (manager == null) return;


                String title = budgetName == null
                        ? budget.budgetName
                        : budgetName;

                if (title == null) {
                    title = "Budget Reminder";
                }


                String message;

                if (daysBefore == 2) {

                    message = "Payment is due in 2 days.";

                } else if (daysBefore == 1) {

                    message = "Your payment is due tomorrow.";

                } else {

                    message = "Your payment is due today. Have you paid?";
                }


                // Save notification in Room Database
                AppNotification notification =
                        new AppNotification();

                notification.title = title;
                notification.message = message;
                notification.monthlyBudgetId = monthlyBudgetId;

                if (daysBefore == 2) {

                    notification.notificationType =
                            "BUDGET_2_DAYS_BEFORE";

                } else if (daysBefore == 1) {

                    notification.notificationType =
                            "BUDGET_1_DAY_BEFORE";

                } else {

                    notification.notificationType =
                            "BUDGET_DUE_TODAY";
                }

                notification.isRead = false;
                notification.actionEnabled = (daysBefore == 0);
                notification.createdAt = System.currentTimeMillis();


                // Only latest due-date notification has active buttons
                if (daysBefore == 0) {

                    database.appNotificationDao()
                            .disableOldDueDateActions(monthlyBudgetId);
                }

                database.appNotificationDao()
                        .insertNotification(notification);


                // Build Android notification
                Notification.Builder builder =
                        new Notification.Builder(
                                context,
                                BudgetNotificationHelper.CHANNEL_ID
                        );

                builder.setSmallIcon(R.drawable.smartpay_logo);
                builder.setContentTitle(title);
                builder.setContentText(message);

                builder.setStyle(
                        new Notification.BigTextStyle()
                                .bigText(message)
                );

                builder.setAutoCancel(true);
                builder.setPriority(Notification.PRIORITY_HIGH);


                // PAID / NOT PAID buttons only on due date
                if (daysBefore == 0) {

                    Intent paidIntent = new Intent(
                            context,
                            BudgetNotificationReceiver.class
                    );

                    paidIntent.setAction(ACTION_PAID);
                    paidIntent.putExtra(
                            "monthlyBudgetId",
                            monthlyBudgetId
                    );

                    PendingIntent paidPendingIntent =
                            PendingIntent.getBroadcast(
                                    context,
                                    monthlyBudgetId * 10 + 5,
                                    paidIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                            | PendingIntent.FLAG_IMMUTABLE
                            );


                    Intent notPaidIntent = new Intent(
                            context,
                            BudgetNotificationReceiver.class
                    );

                    notPaidIntent.setAction(ACTION_NOT_PAID);
                    notPaidIntent.putExtra(
                            "monthlyBudgetId",
                            monthlyBudgetId
                    );

                    PendingIntent notPaidPendingIntent =
                            PendingIntent.getBroadcast(
                                    context,
                                    monthlyBudgetId * 10 + 6,
                                    notPaidIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                            | PendingIntent.FLAG_IMMUTABLE
                            );


                    builder.addAction(
                            0,
                            "PAID",
                            paidPendingIntent
                    );

                    builder.addAction(
                            0,
                            "NOT PAID",
                            notPaidPendingIntent
                    );


                    // Schedule next reminder after 4 hours
                    scheduleNextRepeatReminder(
                            context,
                            monthlyBudgetId,
                            title
                    );
                }


                manager.notify(
                        monthlyBudgetId,
                        builder.build()
                );


            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                pendingResult.finish();
                executor.shutdown();
            }
        });
    }


    // Save PAID / NOT PAID status
    private void savePaymentStatus(
            Context context,
            int monthlyBudgetId,
            boolean isPaid) {

        final PendingResult pendingResult = goAsync();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            try {

                updatePaymentStatusInDatabase(
                        context,
                        monthlyBudgetId,
                        isPaid
                );

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                pendingResult.finish();
                executor.shutdown();
            }
        });
    }


    // Shared payment update logic
    private static void updatePaymentStatusInDatabase(
            Context context,
            int monthlyBudgetId,
            boolean isPaid) {

        SmartPayDatabase database =
                SmartPayDatabase.getDatabase(context);

        BudgetDao dao = database.budgetDao();

        MonthlyBudget budget =
                dao.getMonthlyBudgetById(monthlyBudgetId);

        if (budget == null) return;


        if (isPaid) {

            budget.paymentStatus = "PAID";
            budget.paidAt = System.currentTimeMillis();

        } else {

            budget.paymentStatus = "PENDING";
            budget.paidAt = 0;
        }


        dao.updateMonthlyBudget(budget);


        // Disable action buttons in notification history
        database.appNotificationDao()
                .disableAllBudgetActions(monthlyBudgetId);


        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.cancel(monthlyBudgetId);
        }


        if (isPaid) {

            // Stop all reminders after payment
            BudgetReminderScheduler.cancelReminders(
                    context,
                    monthlyBudgetId
            );

            cancelRepeatReminder(context, monthlyBudgetId);

        } else {

            // Keep pending and remind again after 4 hours
            scheduleNextRepeatReminder(
                    context,
                    monthlyBudgetId,
                    budget.budgetName
            );
        }
    }


    // Public method for Notification History Activity
    public static void updatePaymentStatusFromHistory(
            Context context,
            int monthlyBudgetId,
            boolean isPaid) {

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            try {

                updatePaymentStatusInDatabase(
                        context.getApplicationContext(),
                        monthlyBudgetId,
                        isPaid
                );

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                executor.shutdown();
            }
        });
    }


    // Schedule next reminder after 4 hours
    public static void scheduleNextRepeatReminder(
            Context context,
            int monthlyBudgetId,
            String budgetName) {

        Intent intent = new Intent(
                context,
                BudgetNotificationReceiver.class
        );

        intent.setAction(ACTION_REPEAT);

        intent.putExtra(
                "monthlyBudgetId",
                monthlyBudgetId
        );

        intent.putExtra(
                "budgetName",
                budgetName
        );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        monthlyBudgetId * 10 + 7,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager != null) {

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REPEAT_INTERVAL,
                    pendingIntent
            );
        }
    }


    // Cancel repeat reminder
    public static void cancelRepeatReminder(
            Context context,
            int monthlyBudgetId) {

        Intent intent = new Intent(
                context,
                BudgetNotificationReceiver.class
        );

        intent.setAction(ACTION_REPEAT);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        monthlyBudgetId * 10 + 7,
                        intent,
                        PendingIntent.FLAG_NO_CREATE
                                | PendingIntent.FLAG_IMMUTABLE
                );

        if (pendingIntent != null) {

            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE
                    );

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }

            pendingIntent.cancel();
        }
    }
}
