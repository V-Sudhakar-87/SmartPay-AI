
package com.smartpay.ai;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class DatabaseTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TransactionRepository repository =
                new TransactionRepository(getApplicationContext());


        Transaction testTransaction = new Transaction();

        testTransaction.merchantName = "Swiggy Test";
        testTransaction.amount = 25000; // ₹250 in paise
        testTransaction.transactionType = "EXPENSE";
        testTransaction.category = "Food";
        testTransaction.paymentApp = "GPay";
        testTransaction.transactionDate = System.currentTimeMillis();

// Save to Room Database


        repository.insertTransaction(testTransaction);

        repository.getAllTransactions(transactions -> {

            for (Transaction transaction : transactions) {

                Log.d(
                        "SMARTPAY_DB",
                        "Merchant: " + transaction.merchantName
                                + " | Amount: " + transaction.amount
                                + " | Type: " + transaction.transactionType
                );
            }
        });
    }
}
