
package com.smartpay.ai;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateBudgetActivity extends AppCompatActivity {

    private EditText etBudgetName;
    private EditText etBudgetAmount;

    private TextView tvBudgetMonth;
    private TextView tvBudgetDueDate;
    private TextView btnSaveBudget;
    private TextView btnBack;

    private CheckBox cbRecurring;
    private CheckBox cbReminder;

    private BudgetRepository budgetRepository;

    private int selectedDueDay = -1;

    private Calendar selectedDate;
    private Calendar selectedMonthDate;

    private String selectedBudgetMonth;

    // Edit mode variables
    private int monthlyBudgetId = -1;
    private boolean isEditMode = false;

    private MonthlyBudget editingMonthlyBudget;
    private Budget editingParentBudget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_budget);

        initializeViews();

        budgetRepository = new BudgetRepository(this);

        selectedDate = Calendar.getInstance();
        selectedMonthDate = Calendar.getInstance();

        // Default budget month = Current month
        updateSelectedBudgetMonth();

        // Check edit mode
        monthlyBudgetId = getIntent().getIntExtra(
                "monthlyBudgetId",
                -1
        );

        if (monthlyBudgetId != -1) {

            isEditMode = true;

            btnSaveBudget.setText("Update Budget");

            loadBudgetForEditing();
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Select budget month
        tvBudgetMonth.setOnClickListener(v -> {

            if (!isEditMode) {
                showBudgetMonthPicker();
            }

        });

        // Select due date
        tvBudgetDueDate.setOnClickListener(v -> showDatePicker());

        // Save budget
        btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    private void initializeViews() {

        etBudgetName = findViewById(R.id.etBudgetName);
        etBudgetAmount = findViewById(R.id.etBudgetAmount);

        tvBudgetMonth = findViewById(R.id.tvBudgetMonth);
        tvBudgetDueDate = findViewById(R.id.tvBudgetDueDate);

        cbRecurring = findViewById(R.id.cbRecurring);
        cbReminder = findViewById(R.id.cbReminder);

        btnSaveBudget = findViewById(R.id.btnSaveBudget);
        btnBack = findViewById(R.id.btnCreateBudgetBack);
    }

    // Update selected month value and display
    private void updateSelectedBudgetMonth() {

        SimpleDateFormat monthFormat =
                new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        selectedBudgetMonth = monthFormat.format(
                selectedMonthDate.getTime()
        );

        SimpleDateFormat displayFormat =
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        tvBudgetMonth.setText(
                displayFormat.format(selectedMonthDate.getTime())
        );
    }

    // Select budget month
    private void showBudgetMonthPicker() {

        DatePickerDialog dialog = new DatePickerDialog(
                CreateBudgetActivity.this,

                (view, year, month, dayOfMonth) -> {

                    Calendar newMonth = Calendar.getInstance();

                    newMonth.clear();
                    newMonth.set(Calendar.YEAR, year);
                    newMonth.set(Calendar.MONTH, month);
                    newMonth.set(Calendar.DAY_OF_MONTH, 1);

                    selectedMonthDate = newMonth;

                    updateSelectedBudgetMonth();

                },

                selectedMonthDate.get(Calendar.YEAR),
                selectedMonthDate.get(Calendar.MONTH),
                1
        );

        dialog.setTitle("Select Budget Month");
        dialog.show();
    }

    // Load existing budget details
    private void loadBudgetForEditing() {

        btnSaveBudget.setEnabled(false);

        budgetRepository.getMonthlyBudgetById(
                monthlyBudgetId,
                new BudgetRepository.OnMonthlyBudgetLoadedListener() {

                    @Override
                    public void onSuccess(MonthlyBudget monthlyBudget) {

                        runOnUiThread(() -> {

                            if (monthlyBudget == null) {

                                Toast.makeText(
                                        CreateBudgetActivity.this,
                                        "Budget record not found",
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                                return;
                            }

                            editingMonthlyBudget = monthlyBudget;

                            budgetRepository.getBudgetById(
                                    monthlyBudget.budgetId,
                                    new BudgetRepository.OnBudgetLoadedListener() {

                                        @Override
                                        public void onSuccess(Budget parentBudget) {

                                            runOnUiThread(() -> {

                                                if (parentBudget == null) {

                                                    Toast.makeText(
                                                            CreateBudgetActivity.this,
                                                            "Parent budget not found",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    finish();
                                                    return;
                                                }

                                                editingParentBudget = parentBudget;

                                                populateBudgetDetails();

                                                btnSaveBudget.setEnabled(true);
                                            });
                                        }

                                        @Override
                                        public void onError(String error) {

                                            runOnUiThread(() -> {

                                                Toast.makeText(
                                                        CreateBudgetActivity.this,
                                                        "Loading failed: " + error,
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                btnSaveBudget.setEnabled(true);
                                            });
                                        }
                                    }
                            );
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    CreateBudgetActivity.this,
                                    "Loading failed: " + error,
                                    Toast.LENGTH_LONG
                            ).show();

                            btnSaveBudget.setEnabled(true);
                        });
                    }
                }
        );
    }

    // Display existing details in fields
    private void populateBudgetDetails() {

        etBudgetName.setText(editingMonthlyBudget.budgetName);

        etBudgetAmount.setText(
                String.format(
                        Locale.US,
                        "%.2f",
                        editingMonthlyBudget.amount / 100.0
                )
        );

        // Preserve existing budget month
        if (editingMonthlyBudget.budgetMonth != null) {

            try {

                SimpleDateFormat databaseFormat =
                        new SimpleDateFormat(
                                "yyyy-MM",
                                Locale.US
                        );

                selectedMonthDate.setTime(
                        databaseFormat.parse(
                                editingMonthlyBudget.budgetMonth
                        )
                );

                updateSelectedBudgetMonth();

            } catch (Exception e) {

                updateSelectedBudgetMonth();
            }
        }

        // Do not change month while editing
        tvBudgetMonth.setEnabled(false);

        selectedDueDay = editingMonthlyBudget.dueDay;

        if (selectedDueDay > 0) {

            Calendar today = Calendar.getInstance();

            int maxDay = today.getActualMaximum(
                    Calendar.DAY_OF_MONTH
            );

            selectedDate.set(
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    Math.min(selectedDueDay, maxDay)
            );

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd MMMM yyyy",
                            Locale.getDefault()
                    );

            tvBudgetDueDate.setText(
                    dateFormat.format(selectedDate.getTime())
            );

        } else {

            tvBudgetDueDate.setText("Select due date");
        }

        cbRecurring.setChecked(editingParentBudget.isRecurring);

        cbReminder.setChecked(editingMonthlyBudget.reminderEnabled);
    }

    // Due date picker
    private void showDatePicker() {

        Calendar today = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    selectedDueDay = dayOfMonth;

                    selectedDate.set(
                            year,
                            month,
                            dayOfMonth
                    );

                    SimpleDateFormat dateFormat =
                            new SimpleDateFormat(
                                    "dd MMMM yyyy",
                                    Locale.getDefault()
                            );

                    tvBudgetDueDate.setText(
                            dateFormat.format(selectedDate.getTime())
                    );
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    // Convert rupees to paise
    private long convertToPaise(String amountText) {

        try {

            double amount = Double.parseDouble(amountText);

            if (amount <= 0 || amount > 999999999999.0) {
                throw new NumberFormatException();
            }

            return Math.round(amount * 100);

        } catch (NumberFormatException e) {

            etBudgetAmount.setError("Enter a valid amount");
            etBudgetAmount.requestFocus();

            return -1;
        }
    }

    // Save or update budget
    private void saveBudget() {

        String budgetName =
                etBudgetName.getText().toString().trim();

        String amountText =
                etBudgetAmount.getText().toString().trim();

        // Validate name
        if (budgetName.isEmpty()) {

            etBudgetName.setError("Enter budget name");
            etBudgetName.requestFocus();

            return;
        }

        // Validate amount
        if (amountText.isEmpty()) {

            etBudgetAmount.setError("Enter amount");
            etBudgetAmount.requestFocus();

            return;
        }

        long amountPaise = convertToPaise(amountText);

        if (amountPaise <= 0) {
            return;
        }

        boolean isRecurring = cbRecurring.isChecked();

        boolean reminderEnabled = cbReminder.isChecked();

        // Due date optional
        int dueDay = selectedDueDay == -1 ? 0 : selectedDueDay;

        btnSaveBudget.setEnabled(false);

        // EDIT MODE
        if (isEditMode) {

            if (editingMonthlyBudget == null ||
                    editingParentBudget == null) {

                btnSaveBudget.setEnabled(true);

                Toast.makeText(
                        this,
                        "Budget details are still loading",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Update monthly record
            editingMonthlyBudget.budgetName = budgetName;
            editingMonthlyBudget.amount = amountPaise;
            editingMonthlyBudget.dueDay = dueDay;
            editingMonthlyBudget.reminderEnabled = reminderEnabled;

            // Update parent budget template
            editingParentBudget.budgetName = budgetName;
            editingParentBudget.amount = amountPaise;
            editingParentBudget.dueDay = dueDay;
            editingParentBudget.isRecurring = isRecurring;
            editingParentBudget.reminderEnabled = reminderEnabled;

            budgetRepository.updateBudgetAndMonthlyBudget(
                    editingParentBudget,
                    editingMonthlyBudget,
                    new BudgetRepository.OnBudgetSavedListener() {

                        @Override
                        public void onSuccess() {

                            runOnUiThread(() -> {

                                // Cancel old reminders
                                BudgetReminderScheduler.cancelReminders(
                                        CreateBudgetActivity.this,
                                        editingMonthlyBudget.id
                                );

                                // Schedule updated reminders
                                BudgetReminderScheduler.scheduleReminders(
                                        CreateBudgetActivity.this,
                                        editingMonthlyBudget.id,
                                        budgetName,
                                        editingMonthlyBudget.budgetMonth,
                                        dueDay,
                                        reminderEnabled
                                );

                                Toast.makeText(
                                        CreateBudgetActivity.this,
                                        "Budget updated successfully!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                setResult(RESULT_OK);
                                finish();
                            });
                        }

                        @Override
                        public void onError(String error) {

                            runOnUiThread(() -> {

                                btnSaveBudget.setEnabled(true);

                                Toast.makeText(
                                        CreateBudgetActivity.this,
                                        "Update failed: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );

        } else {

            // ADD MODE
            budgetRepository.addBudgetWithId(
                    budgetName,
                    "CUSTOM",
                    amountPaise,
                    dueDay,
                    isRecurring,
                    reminderEnabled,
                    selectedBudgetMonth,
                    new BudgetRepository.OnBudgetCreatedListener() {

                        @Override
                        public void onSuccess(int savedMonthlyBudgetId) {

                            runOnUiThread(() -> {

                                // Schedule budget reminders
                                BudgetReminderScheduler.scheduleReminders(
                                        CreateBudgetActivity.this,
                                        savedMonthlyBudgetId,
                                        budgetName,
                                        selectedBudgetMonth,
                                        dueDay,
                                        reminderEnabled
                                );

                                BudgetReminderScheduler.testNotification(
                                        CreateBudgetActivity.this,
                                        savedMonthlyBudgetId,
                                        budgetName
                                );


                                Toast.makeText(
                                        CreateBudgetActivity.this,
                                        "Budget saved successfully!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                setResult(RESULT_OK);
                                finish();
                            });
                        }

                        @Override
                        public void onError(String error) {

                            runOnUiThread(() -> {

                                btnSaveBudget.setEnabled(true);

                                Toast.makeText(
                                        CreateBudgetActivity.this,
                                        "Save failed: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );
        }
    }
}
