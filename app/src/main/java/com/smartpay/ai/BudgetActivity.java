
package com.smartpay.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class BudgetActivity extends AppCompatActivity {

    private TextView tvBudgetMonth;
    private TextView tvMonthlyBudget;
    private TextView tvBudgetSpent;
    private TextView tvBudgetRemaining;
    private TextView tvBudgetUsage;
    private TextView tvEmptyCategoryBudget;

    private ProgressBar progressBudget;
    private RecyclerView rvMonthlyBudgets;

    private BudgetAdapter budgetAdapter;
    private BudgetRepository budgetRepository;

    private TransactionRepository transactionRepository;

    private String currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        initializeViews();

        budgetRepository = new BudgetRepository(this);
        transactionRepository = new TransactionRepository(this);

        Calendar calendar = Calendar.getInstance();

        currentMonth = String.format(
                Locale.US,
                "%04d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
        );

        SimpleDateFormat monthFormat =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        tvBudgetMonth.setText(monthFormat.format(calendar.getTime()));

        // RecyclerView Adapter
        budgetAdapter = new BudgetAdapter();

        rvMonthlyBudgets.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvMonthlyBudgets.setAdapter(budgetAdapter);


        // EDIT AND DELETE ACTIONS
        budgetAdapter.setOnBudgetActionListener(
                new BudgetAdapter.OnBudgetActionListener() {

                    @Override
                    public void onEdit(MonthlyBudget budget) {

                        Intent intent = new Intent(
                                BudgetActivity.this,
                                CreateBudgetActivity.class
                        );

                        intent.putExtra(
                                "monthlyBudgetId",
                                budget.id
                        );

                        startActivity(intent);
                    }

                    @Override
                    public void onDelete(MonthlyBudget budget) {

                        showDeleteConfirmation(budget);
                    }
                }
        );


        // Back Button
        findViewById(R.id.btnBudgetBack).setOnClickListener(v -> finish());


        // Create Budget Button
        findViewById(R.id.btnCreateBudget).setOnClickListener(v -> {

            Intent intent = new Intent(
                    BudgetActivity.this,
                    CreateBudgetActivity.class
            );

            startActivity(intent);
        });
    }


    private void initializeViews() {

        tvBudgetMonth = findViewById(R.id.tvBudgetMonth);
        tvMonthlyBudget = findViewById(R.id.tvMonthlyBudget);
        tvBudgetSpent = findViewById(R.id.tvBudgetSpent);
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining);
        tvBudgetUsage = findViewById(R.id.tvBudgetUsage);

        tvEmptyCategoryBudget =
                findViewById(R.id.tvEmptyCategoryBudget);

        progressBudget = findViewById(R.id.progressBudget);

        rvMonthlyBudgets =
                findViewById(R.id.rvMonthlyBudgets);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (budgetRepository != null) {
            loadMonthlyBudgets();
        }
    }


    // LOAD CURRENT MONTH BUDGETS
    private void loadMonthlyBudgets() {

        budgetRepository.getAllMonthlyBudgets(

                new BudgetRepository.OnMonthlyBudgetsLoadedListener() {

                    @Override
                    public void onSuccess(List<MonthlyBudget> budgets) {

                        runOnUiThread(() -> {

                            List<MonthlyBudget> currentMonthBudgets =
                                    new ArrayList<>();

                            long total = 0;

                            if (budgets != null) {

                                for (MonthlyBudget budget : budgets) {

                                    if (currentMonth.equals(budget.budgetMonth)) {

                                        currentMonthBudgets.add(budget);

                                        total += budget.amount;
                                    }
                                }
                            }

                            // Display current month budget items
                            budgetAdapter.setBudgets(currentMonthBudgets);

                            boolean isEmpty = currentMonthBudgets.isEmpty();

                            tvEmptyCategoryBudget.setVisibility(
                                    isEmpty
                                            ? View.VISIBLE
                                            : View.GONE
                            );

                            rvMonthlyBudgets.setVisibility(
                                    isEmpty
                                            ? View.GONE
                                            : View.VISIBLE
                            );

                            // Update monthly total
                            updateBudgetSummary(total);

                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> Toast.makeText(
                                BudgetActivity.this,
                                "Unable to load budgets: " + error,
                                Toast.LENGTH_LONG
                        ).show());
                    }
                }
        );
    }


    // DELETE CONFIRMATION
    private void showDeleteConfirmation(MonthlyBudget budget) {

        new AlertDialog.Builder(this)

                .setTitle("Delete Budget")

                .setMessage(
                        "Are you sure you want to delete \""
                                + budget.budgetName
                                + "\"?"
                )

                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })

                .setPositiveButton("Delete", (dialog, which) -> {

                    deleteBudget(budget);

                })

                .show();
    }


    // DELETE SELECTED MONTHLY BUDGET
    private void deleteBudget(MonthlyBudget budget) {

        budgetRepository.deleteMonthlyBudget(
                budget,
                new BudgetRepository.OnBudgetSavedListener() {

                    @Override
                    public void onSuccess() {

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    BudgetActivity.this,
                                    "Budget deleted successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // Refresh list and monthly total
                            loadMonthlyBudgets();

                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> Toast.makeText(
                                BudgetActivity.this,
                                "Delete failed: " + error,
                                Toast.LENGTH_LONG
                        ).show());
                    }
                }
        );
    }


    // UPDATE MONTHLY SUMMARY
    private void updateBudgetSummary(long totalPaise) {

        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("en", "IN")
                );

        // Total Budget
        tvMonthlyBudget.setText(
                formatter.format(totalPaise / 100.0)
        );

        // Get existing expense transactions
        transactionRepository.getExpenseTransactions(
                transactions -> {

                    long spentPaise = 0;

                    Calendar calendar = Calendar.getInstance();

                    int currentYear = calendar.get(Calendar.YEAR);
                    int currentMonthValue = calendar.get(Calendar.MONTH);

                    for (Transaction transaction : transactions) {

                        if (transaction.transactionDate <= 0) {
                            continue;
                        }

                        Calendar transactionCalendar =
                                Calendar.getInstance();

                        transactionCalendar.setTimeInMillis(
                                transaction.transactionDate
                        );

                        if (transactionCalendar.get(Calendar.YEAR) == currentYear &&
                                transactionCalendar.get(Calendar.MONTH) == currentMonthValue) {

                            spentPaise += transaction.amount;
                        }
                    }

                    long finalSpentPaise = spentPaise;

                    runOnUiThread(() -> {

                        // Amount Spent
                        tvBudgetSpent.setText(
                                formatter.format(finalSpentPaise / 100.0)
                        );

                        // Remaining Amount
                        long remainingPaise =
                                totalPaise - finalSpentPaise;

                        tvBudgetRemaining.setText(
                                formatter.format(remainingPaise / 100.0)
                        );

                        // Usage Percentage
                        int percentage = 0;

                        if (totalPaise > 0) {

                            percentage = (int) Math.round(
                                    (finalSpentPaise * 100.0) / totalPaise
                            );
                        }

                        tvBudgetUsage.setText(
                                percentage + "% used"
                        );

                        // Progress Bar
                        progressBudget.setMax(100);

                        progressBudget.setProgress(
                                Math.min(percentage, 100)
                        );
                        // Progress Bar Color
                        if (percentage > 90) {
                            progressBudget.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.RED
                                    )
                            );
                        } else if (percentage > 50) {
                            progressBudget.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.YELLOW
                                    )
                            );
                        } else {
                            progressBudget.setProgressTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.GREEN
                                    )
                            );
                        }
                    });
                }
        );
    }
}
