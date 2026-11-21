
package com.smartpay.ai;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationHistoryActivity extends AppCompatActivity {

    private LinearLayout llNotifications;
    private TextView tvEmpty;

    private AppNotificationRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notification_history);

        TextView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        llNotifications = findViewById(R.id.llNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        repository = new AppNotificationRepository(
                getApplicationContext()
        );

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repository != null) {
            loadNotifications();
        }
    }

    private void loadNotifications() {

        repository.getAllNotifications(
                new AppNotificationRepository.OnNotificationsLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<AppNotification> notifications) {

                        runOnUiThread(() -> {

                            llNotifications.removeAllViews();

                            if (notifications == null
                                    || notifications.isEmpty()) {

                                tvEmpty.setVisibility(View.VISIBLE);
                                return;
                            }

                            tvEmpty.setVisibility(View.GONE);

                            for (AppNotification notification : notifications) {
                                addNotificationCard(notification);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    NotificationHistoryActivity.this,
                                    "Unable to load notifications: " + error,
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                }
        );
    }

    private void addNotificationCard(AppNotification notification) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 16, 18, 16);

        card.setBackgroundResource(R.drawable.bg_white_card);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 12);

        card.setLayoutParams(cardParams);

        card.setAlpha(notification.isRead ? 0.75f : 1f);

        // TITLE
        TextView title = new TextView(this);

        title.setText(notification.title == null
                ? "SmartPay Notification"
                : notification.title);

        title.setTextSize(16);

        title.setTextColor(getColor(R.color.text_primary));

        title.setTypeface(
                null,
                notification.isRead
                        ? android.graphics.Typeface.NORMAL
                        : android.graphics.Typeface.BOLD
        );

        card.addView(title);

        // MESSAGE
        TextView message = new TextView(this);

        message.setText(notification.message == null
                ? ""
                : notification.message);

        message.setTextSize(14);

        message.setTextColor(getColor(R.color.text_secondary));

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.topMargin = 8;

        message.setLayoutParams(messageParams);

        card.addView(message);

        // DATE
        TextView date = new TextView(this);

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMM yyyy • hh:mm a",
                Locale.getDefault()
        );

        date.setText(dateFormat.format(
                new Date(notification.createdAt)
        ));

        date.setTextSize(12);

        date.setTextColor(getColor(R.color.text_secondary));

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.topMargin = 8;

        date.setLayoutParams(dateParams);

        card.addView(date);

        // ACTION BUTTON CONTAINER
        LinearLayout actions = new LinearLayout(this);

        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        LinearLayout.LayoutParams actionsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        actionsParams.topMargin = 10;

        actions.setLayoutParams(actionsParams);

        // MARK AS READ
        final TextView[] readButton = new TextView[1];

        if (!notification.isRead) {

            readButton[0] = new TextView(this);

            readButton[0].setText("MARK AS READ");
            readButton[0].setTextSize(12);

            readButton[0].setTextColor(
                    getColor(R.color.primary_green)
            );

            readButton[0].setPadding(12, 10, 12, 10);

            actions.addView(readButton[0]);

            readButton[0].setOnClickListener(v -> {

                repository.markAsRead(notification.id);

                notification.isRead = true;

                title.setTypeface(
                        null,
                        android.graphics.Typeface.NORMAL
                );

                card.setAlpha(0.75f);

                actions.removeView(readButton[0]);

                Toast.makeText(
                        this,
                        "Notification marked as read",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        // PAID / NOT PAID - ONLY ACTIVE DUE DATE NOTIFICATION
        boolean showPaymentActions =
                notification.actionEnabled
                        && "BUDGET_DUE_TODAY".equals(
                        notification.notificationType
                );

        if (showPaymentActions) {

            // PAID BUTTON
            TextView paidButton = new TextView(this);

            paidButton.setText("PAID");
            paidButton.setTextSize(12);

            paidButton.setTextColor(
                    android.graphics.Color.WHITE
            );

            paidButton.setBackgroundColor(
                    getColor(R.color.primary_green)
            );

            paidButton.setPadding(18, 10, 18, 10);

            // NOT PAID BUTTON
            TextView notPaidButton = new TextView(this);

            notPaidButton.setText("NOT PAID");
            notPaidButton.setTextSize(12);

            notPaidButton.setTextColor(
                    android.graphics.Color.WHITE
            );

            notPaidButton.setBackgroundColor(
                    android.graphics.Color.RED
            );

            notPaidButton.setPadding(18, 10, 18, 10);

            LinearLayout.LayoutParams paidParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            paidParams.setMargins(8, 0, 8, 0);

            paidButton.setLayoutParams(paidParams);

            LinearLayout.LayoutParams notPaidParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            notPaidParams.setMargins(8, 0, 8, 0);

            notPaidButton.setLayoutParams(notPaidParams);

            actions.addView(paidButton);
            actions.addView(notPaidButton);

            // PAID CLICK
            paidButton.setOnClickListener(v -> {

                paidButton.setEnabled(false);
                notPaidButton.setEnabled(false);

                notification.actionEnabled = false;

                BudgetNotificationReceiver.updatePaymentStatusFromHistory(
                        getApplicationContext(),
                        notification.monthlyBudgetId,
                        true
                );

                actions.removeView(paidButton);
                actions.removeView(notPaidButton);

                Toast.makeText(
                        this,
                        "Budget marked as PAID",
                        Toast.LENGTH_SHORT
                ).show();
            });

            // NOT PAID CLICK
            notPaidButton.setOnClickListener(v -> {

                paidButton.setEnabled(false);
                notPaidButton.setEnabled(false);

                notification.actionEnabled = false;

                BudgetNotificationReceiver.updatePaymentStatusFromHistory(
                        getApplicationContext(),
                        notification.monthlyBudgetId,
                        false
                );

                actions.removeView(paidButton);
                actions.removeView(notPaidButton);

                Toast.makeText(
                        this,
                        "Payment still PENDING. Reminder will continue.",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        // DELETE BUTTON
        TextView deleteButton = new TextView(this);

        deleteButton.setText("DELETE");
        deleteButton.setTextSize(12);

        deleteButton.setTextColor(
                android.graphics.Color.RED
        );

        deleteButton.setPadding(12, 10, 12, 10);

        deleteButton.setOnClickListener(v -> {

            repository.deleteNotification(notification.id);

            llNotifications.removeView(card);

            if (llNotifications.getChildCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
            }

            Toast.makeText(
                    this,
                    "Notification deleted",
                    Toast.LENGTH_SHORT
            ).show();
        });

        actions.addView(deleteButton);

        card.addView(actions);

        llNotifications.addView(card);

        // CARD CLICK - MARK AS READ
        card.setOnClickListener(v -> {

            if (!notification.isRead) {

                repository.markAsRead(notification.id);

                notification.isRead = true;

                title.setTypeface(
                        null,
                        android.graphics.Typeface.NORMAL
                );

                card.setAlpha(0.75f);

                if (readButton[0] != null) {
                    actions.removeView(readButton[0]);
                }
            }
        });
    }
}
