
package com.smartpay.ai;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Transaction details
    public String transactionType;
    public String transactionSubtype;
    public String transactionStatus;

    // Amount stored in paise
    public long amount;
    public String currency;

    // People and merchant
    public String merchantName;
    public String senderName;
    public String receiverName;

    // Bank and account
    public String bankName;
    public String accountLast4;

    // Payment details
    public String paymentMethod;
    public String paymentApp;
    public String upiId;
    public String referenceNumber;

    // Date and time
    public String transactionDateText;
    public String transactionTime;

    // Epoch timestamp for sorting
    public long transactionDate;

    // Category
    public String category;

    // Balance details stored in paise
    public Long balanceBefore;
    public Long balanceAfter;
    public Long reportedBalance;
    public Long calculatedBalance;
    public Long balanceDifference;

    public String balanceStatus;

    // Transfer details
    public String sourceBank;
    public String destinationBank;
    public String sourceAccountLast4;
    public String destinationAccountLast4;

    // Validation
    @ColumnInfo(defaultValue = "0")
    public boolean isDuplicate;

    @ColumnInfo(defaultValue = "0")
    public boolean isValid;

    @ColumnInfo(defaultValue = "0")
    public double confidence;

    // Room requires a constructor matching the entity fields
    public Transaction() {
    }
}
