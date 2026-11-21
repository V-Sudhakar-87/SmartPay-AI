
package com.smartpay.ai;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private static final String TAG = "SMARTPAY_DB";
    private static final String VERIFY_TAG = "SMARTPAY_VERIFY";

    private final TransactionDao transactionDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    public TransactionRepository(Context context) {

        SmartPayDatabase database =
                SmartPayDatabase.getDatabase(context);

        transactionDao = database.transactionDao();
    }

    // ---------------------------------
    // INSERT NORMAL TRANSACTION
    // ---------------------------------

    public void insertTransaction(Transaction transaction) {

        executorService.execute(() -> {

            try {
                if (transaction == null ||
                        !hasRequiredFields(transaction)) {

                    Log.d(TAG, "INVALID NORMAL TRANSACTION - SKIPPED");
                    return;
                }

                transactionDao.insertTransaction(transaction);

                Log.d(TAG, "NORMAL TRANSACTION SAVED");

            } catch (Exception e) {
                Log.e(TAG, "NORMAL TRANSACTION SAVE FAILED", e);
            }
        });
    }

    // ---------------------------------
    // INSERT TRANSACTION FROM AI JSON
    // ---------------------------------

    public void insertTransactionFromJson(JSONObject json) {

        executorService.execute(() -> {

            try {

                if (json == null) {
                    Log.d(TAG, "EMPTY JSON - SKIPPED");
                    return;
                }

                boolean isValid = json.optBoolean("is_valid", false);

                if (!isValid) {
                    Log.d(TAG, "AI INVALID TRANSACTION - SKIPPED");
                    return;
                }

                // Extract important fields

                String transactionType =
                        getString(json, "transaction_type");

                String transactionStatus =
                        getString(json, "transaction_status");

                String bankName =
                        getString(json, "bank_name");

                String accountLast4 =
                        getString(json, "account_last4");

                String referenceNumber =
                        getString(json, "reference_number");

                long amount =
                        convertToPaise(json, "amount");

                String transactionDateText =
                        getString(json, "transaction_date");

                String transactionTime =
                        getString(json, "transaction_time");

                long transactionDate =
                        convertToTimestamp(
                                transactionDateText,
                                transactionTime
                        );

                // ---------------------------------
                // REQUIRED FIELD VALIDATION
                // ---------------------------------

                if (transactionType == null ||
                        transactionStatus == null ||
                        bankName == null ||
                        amount <= 0 ||
                        transactionDate <= 0) {

                    Log.d(TAG,
                            "TRANSACTION REJECTED - REQUIRED FIELD MISSING");

                    Log.d(TAG,
                            "TYPE: " + transactionType);

                    Log.d(TAG,
                            "BANK: " + bankName);

                    Log.d(TAG,
                            "STATUS: " + transactionStatus);

                    Log.d(TAG,
                            "AMOUNT: " + amount);

                    Log.d(TAG,
                            "DATE TIMESTAMP: " + transactionDate);

                    return;
                }

                // ---------------------------------
                // REFERENCE NUMBER DUPLICATE CHECK
                // ---------------------------------

                if (referenceNumber != null &&
                        transactionDao.isDuplicateTransaction(
                                referenceNumber,
                                bankName)) {

                    Log.d(TAG,
                            "DUPLICATE REFERENCE TRANSACTION - SKIPPED");

                    return;
                }

                // ---------------------------------
                // FALLBACK DUPLICATE CHECK
                // ---------------------------------

                if (referenceNumber == null) {

                    if (transactionDao.isDuplicateByDetails(
                            bankName,
                            accountLast4,
                            transactionType,
                            amount,
                            transactionDate)) {

                        Log.d(TAG,
                                "DUPLICATE DETAILS TRANSACTION - SKIPPED");

                        return;
                    }
                }

                // ---------------------------------
                // CREATE TRANSACTION
                // ---------------------------------

                Transaction transaction = new Transaction();

                transaction.transactionType = transactionType;

                transaction.transactionSubtype =
                        getString(json, "transaction_subtype");

                transaction.transactionStatus = transactionStatus;

                transaction.amount = amount;

                transaction.currency =
                        getString(json, "currency");

                transaction.merchantName =
                        getString(json, "merchant_name");

                transaction.senderName =
                        getString(json, "sender_name");

                transaction.receiverName =
                        getString(json, "receiver_name");

                transaction.bankName = bankName;

                transaction.accountLast4 = accountLast4;

                transaction.paymentMethod =
                        getString(json, "payment_method");

                transaction.paymentApp =
                        getString(json, "payment_app");

                transaction.upiId =
                        getString(json, "upi_id");

                transaction.referenceNumber = referenceNumber;

                transaction.transactionDateText =
                        transactionDateText;

                transaction.transactionTime =
                        transactionTime;

                transaction.transactionDate = transactionDate;

                transaction.category =
                        getString(json, "category");

                transaction.balanceBefore =
                        convertNullablePaise(json, "balance_before");

                transaction.balanceAfter =
                        convertNullablePaise(json, "balance_after");

                transaction.reportedBalance =
                        convertNullablePaise(json, "reported_balance");

                transaction.calculatedBalance =
                        convertNullablePaise(json, "calculated_balance");

                transaction.balanceDifference =
                        convertNullablePaise(json, "balance_difference");

                transaction.balanceStatus =
                        getString(json, "balance_status");

                transaction.sourceBank =
                        getString(json, "source_bank");

                transaction.destinationBank =
                        getString(json, "destination_bank");

                transaction.sourceAccountLast4 =
                        getString(json, "source_account_last4");

                transaction.destinationAccountLast4 =
                        getString(json, "destination_account_last4");

                transaction.isDuplicate = false;

                transaction.isValid = true;

                transaction.confidence =
                        json.optDouble("confidence", 0.0);

                // ---------------------------------
                // SAVE TO ROOM
                // ---------------------------------

                transactionDao.insertTransaction(transaction);

                Log.d(TAG, "AI TRANSACTION SAVED SUCCESSFULLY");

                Log.d(TAG,
                        "SAVED TRANSACTION ID: " + transaction.id);

                verifySavedTransactionsInternal();

            } catch (Exception e) {

                Log.e(TAG, "TRANSACTION SAVE FAILED", e);
            }
        });
    }

    // ---------------------------------
    // REQUIRED FIELD VALIDATION
    // ---------------------------------

    private boolean hasRequiredFields(Transaction transaction) {

        return transaction.transactionType != null &&
                !transaction.transactionType.trim().isEmpty() &&
                transaction.transactionStatus != null &&
                !transaction.transactionStatus.trim().isEmpty() &&
                transaction.bankName != null &&
                !transaction.bankName.trim().isEmpty() &&
                transaction.amount > 0 &&
                transaction.transactionDate > 0;
    }

    // ---------------------------------
    // VERIFY DATABASE RECORDS
    // ---------------------------------

    public void verifySavedTransactions() {

        executorService.execute(() -> {
            verifySavedTransactionsInternal();
        });
    }

    private void verifySavedTransactionsInternal() {

        try {

            List<Transaction> transactions =
                    transactionDao.getAllTransactions();

            Log.d(VERIFY_TAG,
                    "TOTAL TRANSACTIONS: " + transactions.size());

            for (Transaction transaction : transactions) {

                Log.d(VERIFY_TAG, "------------------------");

                Log.d(VERIFY_TAG,
                        "ID: " + transaction.id);

                Log.d(VERIFY_TAG,
                        "TYPE: " + transaction.transactionType);

                Log.d(VERIFY_TAG,
                        "AMOUNT: Rs. " +
                                (transaction.amount / 100.0));

                Log.d(VERIFY_TAG,
                        "BANK: " + transaction.bankName);

                Log.d(VERIFY_TAG,
                        "STATUS: " + transaction.transactionStatus);
            }

            Log.d(VERIFY_TAG, "DATABASE READ SUCCESS");

        } catch (Exception e) {

            Log.e(VERIFY_TAG, "DATABASE READ FAILED", e);
        }
    }

    // ---------------------------------
    // JSON STRING HELPER
    // ---------------------------------

    private String getString(JSONObject json, String key) {

        if (json.isNull(key)) {
            return null;
        }

        String value = json.optString(key, null);

        if (value == null ||
                value.trim().isEmpty() ||
                value.equalsIgnoreCase("null")) {

            return null;
        }

        return value.trim();
    }

    // ---------------------------------
    // RUPEES TO PAISE
    // ---------------------------------

    private long convertToPaise(JSONObject json, String key) {

        double amount = json.optDouble(key, 0.0);

        if (Double.isNaN(amount) ||
                Double.isInfinite(amount)) {
            return 0;
        }

        return Math.round(amount * 100);
    }

    private Long convertNullablePaise(JSONObject json, String key) {

        if (json.isNull(key)) {
            return null;
        }

        double amount = json.optDouble(key, Double.NaN);

        if (Double.isNaN(amount) ||
                Double.isInfinite(amount)) {

            return null;
        }

        return Math.round(amount * 100);
    }

    // ---------------------------------
    // DATE TO TIMESTAMP
    // ---------------------------------

    private long convertToTimestamp(String date, String time) {

        if (date == null) {
            return 0;
        }

        String dateTime = date;

        if (time != null) {
            dateTime = date + " " + time;
        }

        String[] formats = {
                "dd-MM-yyyy hh:mm a",
                "dd-MM-yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm",
                "dd-MM-yyyy"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(format, Locale.ENGLISH);

                sdf.setLenient(false);

                Date parsedDate = sdf.parse(dateTime);

                if (parsedDate != null) {
                    return parsedDate.getTime();
                }

            } catch (ParseException ignored) {
                // Try next format
            }
        }

        return 0;
    }

    // ---------------------------------
    // GET ALL TRANSACTIONS
    // ---------------------------------

    public void getAllTransactions(TransactionCallback callback) {

        executorService.execute(() -> {

            List<Transaction> transactions =
                    transactionDao.getAllTransactions();

            if (callback != null) {
                callback.onTransactionsLoaded(transactions);
            }
        });
    }

    // ---------------------------------
    // GET INCOME
    // ---------------------------------

    public void getIncomeTransactions(TransactionCallback callback) {

        executorService.execute(() -> {

            List<Transaction> transactions =
                    transactionDao.getIncomeTransactions();

            if (callback != null) {
                callback.onTransactionsLoaded(transactions);
            }
        });
    }

    // ---------------------------------
    // GET EXPENSE
    // ---------------------------------

    public void getExpenseTransactions(TransactionCallback callback) {

        executorService.execute(() -> {

            List<Transaction> transactions =
                    transactionDao.getExpenseTransactions();

            if (callback != null) {
                callback.onTransactionsLoaded(transactions);
            }
        });
    }

    // ---------------------------------
    // DELETE TRANSACTION
    // ---------------------------------

    public void deleteTransaction(int transactionId) {

        executorService.execute(() -> {
            transactionDao.deleteTransaction(transactionId);
        });
    }

    // ---------------------------------
    // CALLBACK
    // ---------------------------------

    public interface TransactionCallback {

        void onTransactionsLoaded(List<Transaction> transactions);
    }
}
