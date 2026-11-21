
package com.smartpay.ai;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.content.res.ColorStateList;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SMARTPAY_DEBUG";

    private LinearLayout llRecentTransactions;
    private TextView tvRecentEmpty;

    private TextView tvMonthlyIncome;
    private TextView tvMonthlyExpense;

    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;

    private TextView tvBudgetSpent;
    private TextView tvBudgetLimit;
    private TextView tvBudgetRemaining;

    private ProgressBar progressMonthlyBudget;

    // Notification
    private TextView tvNotificationBadge;
    private View btnNotifications;
    private AppNotificationRepository appNotificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create notification channel
        BudgetNotificationHelper.createNotificationChannel(this);

        // Request notification permission
        checkNotificationPermission();

        setupRecentTransactions();
        setupMenu();
        setupNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (transactionRepository != null) {
            loadRecentTransactions();
            loadHomeBudget();
        }

        // Refresh unread notification count
        if (appNotificationRepository != null) {
            loadUnreadNotificationCount();
        }
    }

    private void setupRecentTransactions() {

        llRecentTransactions =
                findViewById(R.id.llRecentTransactions);

        tvRecentEmpty =
                findViewById(R.id.tvRecentEmpty);

        tvMonthlyIncome =
                findViewById(R.id.tvMonthlyIncome);

        tvMonthlyExpense =
                findViewById(R.id.tvMonthlyExpense);

        transactionRepository =
                new TransactionRepository(getApplicationContext());

        budgetRepository =
                new BudgetRepository(getApplicationContext());

        tvBudgetSpent = findViewById(R.id.tvBudgetSpent);
        tvBudgetLimit = findViewById(R.id.tvBudgetLimit);
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining);

        progressMonthlyBudget =
                findViewById(R.id.progressMonthlyBudget);

        loadRecentTransactions();
    }

    // Notification Setup
    private void setupNotifications() {

        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        appNotificationRepository =
                new AppNotificationRepository(getApplicationContext());

        if (btnNotifications != null) {

            btnNotifications.setOnClickListener(v -> {

                Intent intent = new Intent(
                        MainActivity.this,
                        NotificationHistoryActivity.class
                );

                startActivity(intent);
            });
        }

        loadUnreadNotificationCount();
    }

    // Load Unread Notification Count

    private void loadUnreadNotificationCount() {

        appNotificationRepository.getUnreadCount(
                new AppNotificationRepository.OnUnreadCountListener() {

                    @Override
                    public void onSuccess(int count) {

                        runOnUiThread(() -> {

                            if (tvNotificationBadge == null) return;

                            if (count > 0) {

                                tvNotificationBadge.setText(
                                        count > 99
                                                ? "99+"
                                                : String.valueOf(count)
                                );

                                tvNotificationBadge.setVisibility(
                                        View.VISIBLE
                                );

                            } else {

                                tvNotificationBadge.setVisibility(
                                        View.GONE
                                );
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {

                        Log.e(
                                "SMARTPAY_NOTIFICATION",
                                "Unread count error: " + error
                        );
                    }
                }
        );
    }


    private void loadRecentTransactions() {

        transactionRepository.getAllTransactions(transactions -> {

            runOnUiThread(() -> {

                loadMonthlyIncomeExpense(transactions);

                List<Transaction> recentTransactions =
                        new ArrayList<>();

                if (transactions != null) {

                    Log.d(TAG, "DATABASE TRANSACTION COUNT: "
                            + transactions.size());

                    for (int i = 0;
                         i < transactions.size() && i < 3;
                         i++) {

                        recentTransactions.add(transactions.get(i));
                    }
                }

                Log.d(TAG, "HOME RECENT COUNT: "
                        + recentTransactions.size());

                llRecentTransactions.removeAllViews();

                if (recentTransactions.isEmpty()) {

                    tvRecentEmpty.setVisibility(View.VISIBLE);

                } else {

                    tvRecentEmpty.setVisibility(View.GONE);

                    for (Transaction transaction : recentTransactions) {
                        addRecentTransactionRow(transaction);
                    }
                }
            });
        });
    }

    // Monthly Income and Expense Calculation
    private void loadMonthlyIncomeExpense(List<Transaction> transactions) {

        double monthlyIncome = 0;
        double monthlyExpense = 0;

        Calendar calendar = Calendar.getInstance();

        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        if (transactions != null) {

            for (Transaction transaction : transactions) {

                if (transaction.transactionDate <= 0) {
                    continue;
                }

                calendar.setTimeInMillis(
                        transaction.transactionDate
                );

                int transactionMonth =
                        calendar.get(Calendar.MONTH);

                int transactionYear =
                        calendar.get(Calendar.YEAR);

                if (transactionMonth == currentMonth &&
                        transactionYear == currentYear) {

                    double amount =
                            transaction.amount / 100.0;

                    if ("INCOME".equalsIgnoreCase(
                            transaction.transactionType)) {

                        monthlyIncome += amount;

                    } else if ("EXPENSE".equalsIgnoreCase(
                            transaction.transactionType)) {

                        monthlyExpense += amount;
                    }
                }
            }
        }

        tvMonthlyIncome.setText(String.format(
                Locale.getDefault(),
                "₹ %.2f",
                monthlyIncome
        ));

        tvMonthlyExpense.setText(String.format(
                Locale.getDefault(),
                "₹ %.2f",
                monthlyExpense
        ));

        Log.d(TAG, "MONTHLY INCOME: " + monthlyIncome);
        Log.d(TAG, "MONTHLY EXPENSE: " + monthlyExpense);
    }

    // Home Monthly Budget
    private void loadHomeBudget() {

        String currentMonth = new SimpleDateFormat(
                "yyyy-MM",
                Locale.US
        ).format(new Date());

        budgetRepository.getMonthlyBudgets(
                currentMonth,
                new BudgetRepository.OnMonthlyBudgetsLoadedListener() {

                    @Override
                    public void onSuccess(List<MonthlyBudget> monthlyBudgets) {

                        long totalBudgetPaise = 0;

                        if (monthlyBudgets != null) {
                            for (MonthlyBudget budget : monthlyBudgets) {
                                totalBudgetPaise += budget.amount;
                            }
                        }

                        long finalTotalBudgetPaise = totalBudgetPaise;

                        transactionRepository.getExpenseTransactions(
                                transactions -> {

                                    long spentPaise = 0;

                                    Calendar calendar = Calendar.getInstance();

                                    int currentMonthValue =
                                            calendar.get(Calendar.MONTH);

                                    int currentYear =
                                            calendar.get(Calendar.YEAR);

                                    if (transactions != null) {
                                        for (Transaction transaction : transactions) {

                                            if (transaction.transactionDate <= 0) {
                                                continue;
                                            }

                                            calendar.setTimeInMillis(
                                                    transaction.transactionDate
                                            );

                                            if (calendar.get(Calendar.MONTH) == currentMonthValue &&
                                                    calendar.get(Calendar.YEAR) == currentYear) {

                                                spentPaise += transaction.amount;
                                            }
                                        }
                                    }

                                    long finalSpentPaise = spentPaise;

                                    runOnUiThread(() ->
                                            updateHomeBudgetSummary(
                                                    finalTotalBudgetPaise,
                                                    finalSpentPaise
                                            )
                                    );
                                }
                        );
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Budget loading error: " + error);
                    }
                }
        );
    }

    private void updateHomeBudgetSummary(
            long totalBudgetPaise,
            long spentPaise) {

        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("en", "IN")
                );

        // Total Budget
        if (totalBudgetPaise <= 0) {

            tvBudgetSpent.setText(
                    formatter.format(0) + " spent"
            );

            tvBudgetLimit.setText("Budget not set");

            tvBudgetRemaining.setText(
                    "Set a monthly budget to get started"
            );

            progressMonthlyBudget.setProgress(0);

            progressMonthlyBudget.setProgressTintList(
                    ColorStateList.valueOf(Color.GREEN)
            );

            return;
        }

        // Spent Amount
        tvBudgetSpent.setText(
                formatter.format(spentPaise / 100.0) + " spent"
        );

        // Budget Limit
        tvBudgetLimit.setText(
                "of " + formatter.format(totalBudgetPaise / 100.0)
        );

        // Remaining Amount
        long remainingPaise =
                totalBudgetPaise - spentPaise;

        tvBudgetRemaining.setText(
                formatter.format(remainingPaise / 100.0) + " remaining"
        );

        // Usage Percentage
        int percentage = (int) Math.round(
                (spentPaise * 100.0) / totalBudgetPaise
        );

        // Progress Bar
        progressMonthlyBudget.setMax(100);

        progressMonthlyBudget.setProgress(
                Math.min(percentage, 100)
        );

        // Progress Bar Color
        if (percentage > 90) {

            progressMonthlyBudget.setProgressTintList(
                    ColorStateList.valueOf(Color.RED)
            );

        } else if (percentage > 50) {

            progressMonthlyBudget.setProgressTintList(
                    ColorStateList.valueOf(Color.YELLOW)
            );

        } else {

            progressMonthlyBudget.setProgressTintList(
                    ColorStateList.valueOf(Color.GREEN)
            );
        }
    }

    private void addRecentTransactionRow(Transaction transaction) {

        View row = LayoutInflater.from(this).inflate(
                R.layout.item_recent_transaction,
                llRecentTransactions,
                false
        );

        TextView tvTitle = row.findViewById(R.id.tvRecentTitle);
        TextView tvDetails = row.findViewById(R.id.tvRecentDetails);
        TextView tvDate = row.findViewById(R.id.tvRecentDate);
        TextView tvAmount = row.findViewById(R.id.tvRecentAmount);

        tvTitle.setText(getTitle(transaction));
        tvDetails.setText(getDetails(transaction));
        tvDate.setText(getDate(transaction));

        String amount = String.format(
                Locale.getDefault(),
                "₹ %.2f",
                transaction.amount / 100.0
        );

        if ("INCOME".equalsIgnoreCase(transaction.transactionType)) {

            tvAmount.setText("+ " + amount);
            tvAmount.setTextColor(Color.parseColor("#168653"));

        } else if ("EXPENSE".equalsIgnoreCase(transaction.transactionType)) {

            tvAmount.setText("- " + amount);
            tvAmount.setTextColor(Color.parseColor("#E05252"));

        } else {

            tvAmount.setText(amount);
            tvAmount.setTextColor(Color.DKGRAY);
        }

        llRecentTransactions.addView(row);
    }

    private String getTitle(Transaction transaction) {

        if (transaction.merchantName != null &&
                !transaction.merchantName.trim().isEmpty()) {

            return transaction.merchantName;
        }

        if (transaction.senderName != null &&
                !transaction.senderName.trim().isEmpty()) {

            return transaction.senderName;
        }

        if (transaction.receiverName != null &&
                !transaction.receiverName.trim().isEmpty()) {

            return transaction.receiverName;
        }

        return "Transaction";
    }

    private String getDetails(Transaction transaction) {

        if (transaction.upiId != null &&
                !transaction.upiId.trim().isEmpty()) {

            return transaction.upiId;
        }

        if (transaction.bankName != null &&
                !transaction.bankName.trim().isEmpty()) {

            return transaction.bankName;
        }

        return "Bank details unavailable";
    }

    private String getDate(Transaction transaction) {

        if (transaction.transactionDate > 0) {

            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "dd MMM yyyy • hh:mm a",
                    Locale.getDefault()
            );

            return dateFormat.format(
                    new Date(transaction.transactionDate)
            );
        }

        if (transaction.transactionDateText != null &&
                !transaction.transactionDateText.trim().isEmpty()) {

            return transaction.transactionDateText;
        }

        return "";
    }

    // Notification Permission
    private void checkNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        1001
                );
            }
        }
    }

    private void setupMenu() {

        View menuButton = findViewById(R.id.btnMenu);

        if (menuButton == null) {
            Log.e(TAG, "Menu button not found");
            return;
        }

        menuButton.setOnClickListener(v -> {

            ContextThemeWrapper wrapper =
                    new ContextThemeWrapper(
                            MainActivity.this,
                            R.style.SmartPayPopupTheme
                    );

            PopupMenu popupMenu =
                    new PopupMenu(wrapper, menuButton);

            popupMenu.getMenuInflater().inflate(
                    R.menu.main_menu,
                    popupMenu.getMenu()
            );

            popupMenu.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();

                if (id == R.id.nav_home) {

                    Toast.makeText(
                            this,
                            "Home",
                            Toast.LENGTH_SHORT
                    ).show();

                } else if (id == R.id.nav_transactions) {

                    Intent intent = new Intent(
                            MainActivity.this,
                            TransactionsActivity.class
                    );

                    startActivity(intent);

                } else if (id == R.id.nav_budget) {

                    Intent intent = new Intent(
                            MainActivity.this,
                            BudgetActivity.class
                    );

                    startActivity(intent);

                } else {

                    Toast.makeText(
                            this,
                            "Coming Soon!",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return true;
            });

            popupMenu.show();
        });
    }
}
