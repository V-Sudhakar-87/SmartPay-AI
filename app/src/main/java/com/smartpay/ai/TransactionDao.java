
package com.smartpay.ai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    // --------------------------------
    // INSERT TRANSACTION
    // --------------------------------

    @Insert
    void insertTransaction(Transaction transaction);


    // --------------------------------
    // GET ALL TRANSACTIONS
    // --------------------------------

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    List<Transaction> getAllTransactions();


    // --------------------------------
    // GET INCOME
    // --------------------------------

    @Query("SELECT * FROM transactions WHERE transactionType = 'INCOME' ORDER BY transactionDate DESC")
    List<Transaction> getIncomeTransactions();


    // --------------------------------
    // GET EXPENSE
    // --------------------------------

    @Query("SELECT * FROM transactions WHERE transactionType = 'EXPENSE' ORDER BY transactionDate DESC")
    List<Transaction> getExpenseTransactions();


    // --------------------------------
    // GET TRANSACTIONS BY BANK
    // --------------------------------

    @Query("SELECT * FROM transactions WHERE bankName = :bankName ORDER BY transactionDate DESC")
    List<Transaction> getTransactionsByBank(String bankName);


    // --------------------------------
    // GET TRANSACTIONS BY ACCOUNT
    // --------------------------------

    @Query("SELECT * FROM transactions WHERE accountLast4 = :accountLast4 ORDER BY transactionDate DESC")
    List<Transaction> getTransactionsByAccount(String accountLast4);


    // --------------------------------
    // DUPLICATE CHECK USING REFERENCE
    // --------------------------------

    @Query("SELECT EXISTS(" +
            "SELECT 1 FROM transactions " +
            "WHERE referenceNumber = :referenceNumber " +
            "AND referenceNumber IS NOT NULL " +
            "AND referenceNumber != '' " +
            "AND bankName = :bankName)")
    boolean isDuplicateTransaction(
            String referenceNumber,
            String bankName
    );


    // --------------------------------
    // DUPLICATE CHECK WITHOUT REFERENCE
    // Same bank, account, type, amount and
    // transaction time within 60 seconds.
    // --------------------------------

    @Query("SELECT EXISTS(" +
            "SELECT 1 FROM transactions " +
            "WHERE bankName = :bankName " +
            "AND transactionType = :transactionType " +
            "AND amount = :amount " +
            "AND transactionDate > 0 " +
            "AND ABS(transactionDate - :transactionDate) <= 60000 " +
            "AND (" +
            "accountLast4 = :accountLast4 " +
            "OR (accountLast4 IS NULL AND :accountLast4 IS NULL)" +
            "))")
    boolean isDuplicateByDetails(
            String bankName,
            String accountLast4,
            String transactionType,
            long amount,
            long transactionDate
    );


    // --------------------------------
    // FIND INCOMPLETE OLD RECORDS
    // Do not delete automatically.
    // --------------------------------

    @Query("SELECT * FROM transactions " +
            "WHERE transactionType IS NULL " +
            "OR transactionType = '' " +
            "OR amount <= 0 " +
            "OR bankName IS NULL " +
            "OR bankName = '' " +
            "OR transactionStatus IS NULL " +
            "OR transactionStatus = '' " +
            "OR transactionDate <= 0")
    List<Transaction> getIncompleteTransactions();


    // --------------------------------
    // DELETE ONE TRANSACTION
    // --------------------------------

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    void deleteTransaction(int transactionId);


    // --------------------------------
    // DELETE ALL TRANSACTIONS
    // --------------------------------

    @Query("DELETE FROM transactions")
    void deleteAllTransactions();

}
