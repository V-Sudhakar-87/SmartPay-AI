
package com.smartpay.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class BudgetNotificationHelper {

    public static final String CHANNEL_ID = "budget_reminders";

    public static void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE
                    );

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Budget Payment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "SmartPay AI budget due date reminders"
            );

            notificationManager.createNotificationChannel(channel);
        }
    }
}
