
package com.smartpay.ai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddBudgetActivity extends AppCompatActivity {

    private EditText etBudgetName;
    private EditText etBudgetAmount;
    private EditText etDueDay;

    private CheckBox cbRecurring;
    private CheckBox cbReminder;

    private Button btnSaveBudget;

    private BudgetRepository budgetRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        etBudgetName = findViewById(R.id.etBudgetName);
        etBudgetAmount = findViewById(R.id.etBudgetAmount);
        etDueDay = findViewById(R.id.etDueDay);

        cbRecurring = findViewById(R.id.cbRecurring);
        cbReminder = findViewById(R.id.cbReminder);

        btnSaveBudget = findViewById(R.id.btnSaveBudget);

        budgetRepository = new BudgetRepository(this);

        btnSaveBudget.setOnClickListener(v -> saveBudget());
    }

    private void saveBudget() {

        String budgetName = etBudgetName.getText().toString().trim();
        String amountText = etBudgetAmount.getText().toString().trim();
        String dueDayText = etDueDay.getText().toString().trim();

        boolean isRecurring = cbRecurring.isChecked();
        boolean reminderEnabled = cbReminder.isChecked();

        if (budgetName.isEmpty()) {
            etBudgetName.setError("Enter budget name");
            etBudgetName.requestFocus();
            return;
        }

        if (amountText.isEmpty()) {
            etBudgetAmount.setError("Enter budget amount");
            etBudgetAmount.requestFocus();
            return;
        }

        double amountRupees;

        try {
            amountRupees = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etBudgetAmount.setError("Enter a valid amount");
            return;
        }

        if (amountRupees <= 0 || amountRupees > 999999999999.0) {
            etBudgetAmount.setError("Enter a valid amount");
            return;
        }

        long amountPaise = Math.round(amountRupees * 100);

        int dueDay = 0;

        if (!dueDayText.isEmpty()) {

            try {
                dueDay = Integer.parseInt(dueDayText);
            } catch (NumberFormatException e) {
                etDueDay.setError("Enter a valid due day");
                return;
            }

            if (dueDay < 1 || dueDay > 31) {
                etDueDay.setError("Due day must be between 1 and 31");
                return;
            }
        }

        btnSaveBudget.setEnabled(false);

        String budgetMonth = new SimpleDateFormat(
                "yyyy-MM",
                Locale.getDefault()
        ).format(Calendar.getInstance().getTime());

        budgetRepository.addBudget(
                budgetName,
                "CUSTOM",
                amountPaise,
                dueDay,
                isRecurring,
                reminderEnabled,
                budgetMonth,
                new BudgetRepository.OnBudgetSavedListener() {

                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    AddBudgetActivity.this,
                                    "Budget saved successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnSaveBudget.setEnabled(true);

                            Toast.makeText(
                                    AddBudgetActivity.this,
                                    "Failed to save: " + error,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }
}
