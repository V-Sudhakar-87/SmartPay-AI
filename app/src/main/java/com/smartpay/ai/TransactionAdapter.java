
package com.smartpay.ai;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList = new ArrayList<>();

    public void setTransactions(List<Transaction> transactions) {
        transactionList.clear();

        if (transactions != null) {
            transactionList.addAll(transactions);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TransactionViewHolder holder,
            int position) {

        Transaction transaction = transactionList.get(position);

        holder.tvTransactionTitle.setText(
                getTransactionTitle(transaction)
        );

        holder.tvTransactionDetails.setText(
                getTransactionDetails(transaction)
        );

        String type = transaction.transactionType;

        if ("INCOME".equalsIgnoreCase(type)) {

            holder.tvTransactionAmount.setText(
                    "+ " + formatAmount(transaction.amount)
            );

            holder.tvTransactionAmount.setTextColor(
                    Color.parseColor("#168653")
            );

        } else if ("EXPENSE".equalsIgnoreCase(type)) {

            holder.tvTransactionAmount.setText(
                    "- " + formatAmount(transaction.amount)
            );

            holder.tvTransactionAmount.setTextColor(
                    Color.parseColor("#E05252")
            );

        } else {

            holder.tvTransactionAmount.setText(
                    formatAmount(transaction.amount)
            );

            holder.tvTransactionAmount.setTextColor(
                    holder.itemView.getContext().getColor(
                            R.color.text_primary
                    )
            );
        }

        holder.tvTransactionDate.setText(
                formatDate(transaction.transactionDate)
        );

        if (transaction.referenceNumber != null
                && !transaction.referenceNumber.trim().isEmpty()) {

            holder.tvTransactionId.setVisibility(View.VISIBLE);

            holder.tvTransactionId.setText(
                    "Transaction ID: " + transaction.referenceNumber
            );

        } else {
            holder.tvTransactionId.setVisibility(View.GONE);
        }
    }

    private String getTransactionTitle(Transaction transaction) {

        if (transaction.merchantName != null
                && !transaction.merchantName.trim().isEmpty()) {
            return transaction.merchantName;
        }

        if (transaction.senderName != null
                && !transaction.senderName.trim().isEmpty()) {
            return transaction.senderName;
        }

        if (transaction.receiverName != null
                && !transaction.receiverName.trim().isEmpty()) {
            return transaction.receiverName;
        }

        if (transaction.transactionType != null) {
            return transaction.transactionType;
        }

        return "Transaction";
    }

    private String getTransactionDetails(Transaction transaction) {

        if (transaction.upiId != null
                && !transaction.upiId.trim().isEmpty()) {
            return transaction.upiId;
        }

        if (transaction.bankName != null
                && !transaction.bankName.trim().isEmpty()) {
            return transaction.bankName;
        }

        return "Bank details unavailable";
    }

    private String formatAmount(long amountInPaise) {

        double rupees = amountInPaise / 100.0;

        return "₹" + String.format(
                Locale.ENGLISH,
                "%,.2f",
                rupees
        );
    }

    private String formatDate(long timestamp) {

        if (timestamp <= 0) {
            return "Date unavailable";
        }

        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.ENGLISH
        );

        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {

        TextView tvTransactionTitle;
        TextView tvTransactionDetails;
        TextView tvTransactionDate;
        TextView tvTransactionId;
        TextView tvTransactionAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTransactionTitle = itemView.findViewById(
                    R.id.tvTransactionTitle
            );

            tvTransactionDetails = itemView.findViewById(
                    R.id.tvTransactionDetails
            );

            tvTransactionDate = itemView.findViewById(
                    R.id.tvTransactionDate
            );

            tvTransactionId = itemView.findViewById(
                    R.id.tvTransactionId
            );

            tvTransactionAmount = itemView.findViewById(
                    R.id.tvTransactionAmount
            );
        }
    }
}
