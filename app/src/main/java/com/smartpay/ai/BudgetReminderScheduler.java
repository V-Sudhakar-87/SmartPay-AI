
package com.smartpay.ai;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class BudgetReminderScheduler {

    public static void scheduleReminders(
            Context context,
            int monthlyBudgetId,
            String budgetName,
            String budgetMonth,
            int dueDay,
            boolean reminderEnabled
    ) {

        // First cancel old reminders including repeat reminder
        cancelReminders(context, monthlyBudgetId);

        if (!reminderEnabled || dueDay <= 0) {
            return;
        }

        try {

            String[] monthParts = budgetMonth.split("-");

            int year = Integer.parseInt(monthParts[0]);
            int month = Integer.parseInt(monthParts[1]) - 1;

            Calendar dueCalendar = Calendar.getInstance();

            dueCalendar.clear();
            dueCalendar.set(Calendar.YEAR, year);
            dueCalendar.set(Calendar.MONTH, month);

            int maximumDay = dueCalendar.getActualMaximum(
                    Calendar.DAY_OF_MONTH
            );

            dueCalendar.set(
                    Calendar.DAY_OF_MONTH,
                    Math.min(dueDay, maximumDay)
            );

            dueCalendar.set(Calendar.HOUR_OF_DAY, 9);
            dueCalendar.set(Calendar.MINUTE, 0);
            dueCalendar.set(Calendar.SECOND, 0);
            dueCalendar.set(Calendar.MILLISECOND, 0);


            // Two days before
            scheduleOneReminder(
                    context,
                    monthlyBudgetId,
                    budgetName,
                    dueCalendar,
                    2
            );


            // One day before
            scheduleOneReminder(
                    context,
                    monthlyBudgetId,
                    budgetName,
                    dueCalendar,
                    1
            );


            // Due date
            scheduleOneReminder(
                    context,
                    monthlyBudgetId,
                    budgetName,
                    dueCalendar,
                    0
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    private static void scheduleOneReminder(
            Context context,
            int monthlyBudgetId,
            String budgetName,
            Calendar dueCalendar,
            int daysBefore
    ) {

        Calendar reminderCalendar =
                (Calendar) dueCalendar.clone();

        reminderCalendar.add(
                Calendar.DAY_OF_MONTH,
                -daysBefore
        );


        long reminderTime = reminderCalendar.getTimeInMillis();
        long currentTime = System.currentTimeMillis();


        if (reminderTime <= currentTime) {

            // If due date is today and 9 AM has passed,
            // trigger the due reminder immediately.
            if (daysBefore == 0) {

                Calendar today = Calendar.getInstance();

                boolean sameDay =
                        today.get(Calendar.YEAR)
                                == reminderCalendar.get(Calendar.YEAR)
                                &&
                                today.get(Calendar.DAY_OF_YEAR)
                                        == reminderCalendar.get(Calendar.DAY_OF_YEAR);

                if (!sameDay) {
                    return;
                }

                reminderTime = currentTime + 1000;

            } else {

                return;
            }
        }


        Intent intent = new Intent(
                context,
                BudgetNotificationReceiver.class
        );

        intent.setAction(
                "BUDGET_REMINDER_" + monthlyBudgetId + "_" + daysBefore
        );

        intent.putExtra("monthlyBudgetId", monthlyBudgetId);
        intent.putExtra("budgetName", budgetName);
        intent.putExtra("daysBefore", daysBefore);


        int requestCode = monthlyBudgetId * 10 + daysBefore;

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
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
                    reminderTime,
                    pendingIntent
            );
        }
    }


    // Cancel all reminders including 4-hour repeat reminder
    public static void cancelReminders(
            Context context,
            int monthlyBudgetId
    ) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );


        // Cancel 2 days before, 1 day before and due date alarms
        for (int daysBefore = 0; daysBefore <= 2; daysBefore++) {

            Intent intent = new Intent(
                    context,
                    BudgetNotificationReceiver.class
            );

            intent.setAction(
                    "BUDGET_REMINDER_" + monthlyBudgetId + "_" + daysBefore
            );

            int requestCode = monthlyBudgetId * 10 + daysBefore;

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_NO_CREATE
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            if (pendingIntent != null) {

                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }

                pendingIntent.cancel();
            }
        }


        // Also cancel 4-hour repeat reminder
        BudgetNotificationReceiver.cancelRepeatReminder(
                context,
                monthlyBudgetId
        );
    }


    // Test due-date notification
    public static void testNotification(
            Context context,
            int monthlyBudgetId,
            String budgetName
    ) {

        Intent intent = new Intent(
                context,
                BudgetNotificationReceiver.class
        );

        intent.putExtra("monthlyBudgetId", monthlyBudgetId);
        intent.putExtra("budgetName", budgetName);
        intent.putExtra("daysBefore", 0);

        context.sendBroadcast(intent);
    }
}
