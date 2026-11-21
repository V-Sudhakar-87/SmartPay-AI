
package com.smartpay.ai;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class TransactionsActivity extends AppCompatActivity {

    private TextView btnBack;

    private TextView filterAll;
    private TextView filterIncome;
    private TextView filterExpense;

    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private TextView tvExpenseAmount;
    private TextView tvEmptyTransactions;
    private TextView tvOverviewMonth;

    private EditText etSearchTransaction;

    private RecyclerView rvTransactions;

    private Spinner spinnerMonthFilter;

    private TransactionAdapter transactionAdapter;
    private TransactionRepository transactionRepository;

    private List<Transaction> allTransactions = new ArrayList<>();

    private List<Long> monthKeys = new ArrayList<>();

    private String selectedFilter = "ALL";
    private String searchQuery = "";

    // Null means All History
    private Long selectedMonthKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transactions);

        initializeViews();

        setupRecyclerView();

        setupBackButton();

        setupFilters();

        setupSearch();

        transactionRepository = new TransactionRepository(
                getApplicationContext()
        );

        loadTransactions();
    }

    // ---------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        filterAll = findViewById(R.id.filterAll);
        filterIncome = findViewById(R.id.filterIncome);
        filterExpense = findViewById(R.id.filterExpense);

        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvExpenseAmount = findViewById(R.id.tvExpenseAmount);

        tvOverviewMonth = findViewById(R.id.tvOverviewMonth);

        etSearchTransaction = findViewById(
                R.id.etSearchTransaction
        );

        rvTransactions = findViewById(
                R.id.rvTransactions
        );

        tvEmptyTransactions = findViewById(
                R.id.tvEmptyTransactions
        );

        spinnerMonthFilter = findViewById(
                R.id.spinnerMonthFilter
        );
    }

    // ---------------------------------
    // RECYCLER VIEW
    // ---------------------------------

    private void setupRecyclerView() {

        rvTransactions.setLayoutManager(
                new LinearLayoutManager(this)
        );

        transactionAdapter = new TransactionAdapter();

        rvTransactions.setAdapter(transactionAdapter);

        rvTransactions.setNestedScrollingEnabled(true);
    }

    // ---------------------------------
    // BACK BUTTON
    // ---------------------------------

    private void setupBackButton() {

        btnBack.setOnClickListener(v -> finish());
    }

    // ---------------------------------
    // FILTER BUTTONS
    // ---------------------------------

    private void setupFilters() {

        filterAll.setOnClickListener(v -> {

            selectedFilter = "ALL";

            selectFilter(filterAll);

            applyFilters();
        });

        filterIncome.setOnClickListener(v -> {

            selectedFilter = "INCOME";

            selectFilter(filterIncome);

            applyFilters();
        });

        filterExpense.setOnClickListener(v -> {

            selectedFilter = "EXPENSE";

            selectFilter(filterExpense);

            applyFilters();
        });

        selectFilter(filterAll);
    }

    private void selectFilter(TextView selectedFilterView) {

        resetFilter(filterAll);
        resetFilter(filterIncome);
        resetFilter(filterExpense);

        selectedFilterView.setBackgroundResource(
                R.drawable.bg_balance_card
        );

        selectedFilterView.setTextColor(
                getColor(R.color.white)
        );
    }

    private void resetFilter(TextView filter) {

        filter.setBackgroundResource(
                R.drawable.bg_white_card
        );

        filter.setTextColor(
                getColor(R.color.text_primary)
        );
    }

    // ---------------------------------
    // SEARCH
    // ---------------------------------

    private void setupSearch() {

        etSearchTransaction.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        searchQuery = s.toString()
                                .trim()
                                .toLowerCase(Locale.ENGLISH);

                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                }
        );
    }

    // ---------------------------------
    // LOAD ROOM TRANSACTIONS
    // ---------------------------------

    private void loadTransactions() {

        transactionRepository.getAllTransactions(
                transactions -> {

                    runOnUiThread(() -> {

                        allTransactions = transactions != null
                                ? transactions
                                : new ArrayList<>();

                        // Default: All History
                        selectedMonthKey = null;

                        setupMonthSpinner();

                        updateMonthlySummary();

                        applyFilters();
                    });
                }
        );
    }

    // ---------------------------------
    // MONTH SPINNER
    // ---------------------------------

    private void setupMonthSpinner() {

        TreeSet<Long> sortedMonths = new TreeSet<>(
                Collections.reverseOrder()
        );

        // Always include current mobile month
        sortedMonths.add(getCurrentMonthKey());

        // Add available transaction months
        for (Transaction transaction : allTransactions) {

            if (transaction == null ||
                    transaction.transactionDate <= 0) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(
                    transaction.transactionDate
            );

            sortedMonths.add(getMonthKey(calendar));
        }

        monthKeys.clear();

        monthKeys.add(null);

        List<String> monthNames = new ArrayList<>();

        monthNames.add("All History");

        for (Long key : sortedMonths) {

            monthKeys.add(key);

            monthNames.add(formatMonthName(key));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                monthNames
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMonthFilter.setAdapter(adapter);

        spinnerMonthFilter.setSelection(0);

        spinnerMonthFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position < 0 ||
                                position >= monthKeys.size()) {
                            return;
                        }

                        selectedMonthKey = monthKeys.get(position);

                        updateMonthlySummary();

                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    // ---------------------------------
    // CURRENT MONTH KEY
    // ---------------------------------

    private long getCurrentMonthKey() {

        Calendar calendar = Calendar.getInstance();

        return getMonthKey(calendar);
    }

    // ---------------------------------
    // MONTH KEY
    // ---------------------------------

    private long getMonthKey(Calendar calendar) {

        int year = calendar.get(Calendar.YEAR);

        int month = calendar.get(Calendar.MONTH);

        return (long) year * 12 + month;
    }

    // ---------------------------------
    // MONTH NAME
    // ---------------------------------

    private String formatMonthName(long monthKey) {

        int year = (int) (monthKey / 12);

        int month = (int) (monthKey % 12);

        Calendar calendar = Calendar.getInstance();

        calendar.clear();

        calendar.set(Calendar.YEAR, year);

        calendar.set(Calendar.MONTH, month);

        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return String.format(
                Locale.ENGLISH,
                "%tB %tY",
                calendar,
                calendar
        );
    }

    // ---------------------------------
    // APPLY FILTERS AND SEARCH
    // ---------------------------------

    private void applyFilters() {

        List<Transaction> filteredTransactions =
                new ArrayList<>();

        for (Transaction transaction : allTransactions) {

            if (transaction == null) {
                continue;
            }

            // Month filter
            // Null means All History

            if (selectedMonthKey != null) {

                if (transaction.transactionDate <= 0) {
                    continue;
                }

                Calendar calendar = Calendar.getInstance();

                calendar.setTimeInMillis(
                        transaction.transactionDate
                );

                long transactionMonthKey = getMonthKey(calendar);

                if (transactionMonthKey != selectedMonthKey) {
                    continue;
                }
            }

            // Income / Expense filter

            if (!selectedFilter.equals("ALL")) {

                if (transaction.transactionType == null ||
                        !transaction.transactionType.equalsIgnoreCase(
                                selectedFilter
                        )) {

                    continue;
                }
            }

            // Search filter

            if (!matchesSearch(transaction)) {
                continue;
            }

            filteredTransactions.add(transaction);
        }

        transactionAdapter.setTransactions(
                filteredTransactions
        );

        if (filteredTransactions.isEmpty()) {

            tvEmptyTransactions.setVisibility(
                    View.VISIBLE
            );

            rvTransactions.setVisibility(
                    View.GONE
            );

        } else {

            tvEmptyTransactions.setVisibility(
                    View.GONE
            );

            rvTransactions.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ---------------------------------
    // SEARCH MATCHING
    // ---------------------------------

    private boolean matchesSearch(Transaction transaction) {

        if (searchQuery.isEmpty()) {
            return true;
        }

        String amount = String.valueOf(
                transaction.amount / 100.0
        );

        String amountWithoutDecimal = String.valueOf(
                transaction.amount / 100
        );

        return contains(transaction.merchantName)
                || contains(transaction.senderName)
                || contains(transaction.receiverName)
                || contains(transaction.bankName)
                || contains(transaction.upiId)
                || contains(transaction.accountLast4)
                || contains(transaction.paymentMethod)
                || contains(transaction.paymentApp)
                || contains(transaction.transactionType)
                || contains(transaction.category)
                || amount.contains(searchQuery)
                || amountWithoutDecimal.contains(searchQuery);
    }

    private boolean contains(String value) {

        return value != null &&
                value.toLowerCase(Locale.ENGLISH)
                        .contains(searchQuery);
    }

    // ---------------------------------
    // MONTHLY SUMMARY
    // ---------------------------------

    private void updateMonthlySummary() {

        long summaryMonthKey;

        // All History -> Current mobile month overview

        if (selectedMonthKey == null) {

            summaryMonthKey = getCurrentMonthKey();

        } else {

            summaryMonthKey = selectedMonthKey;
        }

        // Update heading

        tvOverviewMonth.setText(
                formatMonthName(summaryMonthKey) + " Overview"
        );

        long totalIncome = 0;

        long totalExpense = 0;

        for (Transaction transaction : allTransactions) {

            if (transaction == null ||
                    transaction.transactionDate <= 0) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(
                    transaction.transactionDate
            );

            long transactionMonthKey = getMonthKey(calendar);

            if (transactionMonthKey != summaryMonthKey) {
                continue;
            }

            if ("INCOME".equalsIgnoreCase(
                    transaction.transactionType
            )) {

                totalIncome += transaction.amount;

            } else if ("EXPENSE".equalsIgnoreCase(
                    transaction.transactionType
            )) {

                totalExpense += transaction.amount;
            }
        }

        tvTotalIncome.setText(
                formatAmount(totalIncome)
        );

        tvTotalExpense.setText(
                formatAmount(totalExpense)
        );

        tvExpenseAmount.setText(
                formatAmount(totalExpense)
        );
    }

    // ---------------------------------
    // FORMAT RUPEES
    // ---------------------------------

    private String formatAmount(long amountInPaise) {

        double rupees = amountInPaise / 100.0;

        return "₹" + String.format(
                Locale.ENGLISH,
                "%,.2f",
                rupees
        );
    }
}
