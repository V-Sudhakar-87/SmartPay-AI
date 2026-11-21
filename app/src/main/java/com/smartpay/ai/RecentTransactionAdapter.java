
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

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    private final List<Transaction> transactions = new ArrayList<>();

    public void setTransactions(List<Transaction> newTransactions) {
        transactions.clear();

        if (newTransactions != null) {
            transactions.addAll(newTransactions);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Transaction transaction = transactions.get(position);

        // Merchant / Sender / Receiver
        String title = getTitle(transaction);
        holder.tvTitle.setText(title);

        // UPI ID or Bank
        String details = getDetails(transaction);
        holder.tvDetails.setText(details);

        // Date and time
        holder.tvDate.setText(getDate(transaction));

        // Amount and color
        String amount = String.format(
                Locale.getDefault(),
                "₹ %.2f",
                transaction.amount / 100.0
        );

        if ("INCOME".equalsIgnoreCase(transaction.transactionType)) {

            holder.tvAmount.setText("+ " + amount);
            holder.tvAmount.setTextColor(Color.parseColor("#168653"));

        } else if ("EXPENSE".equalsIgnoreCase(transaction.transactionType)) {

            holder.tvAmount.setText("- " + amount);
            holder.tvAmount.setTextColor(Color.parseColor("#E05252"));

        } else {

            holder.tvAmount.setText(amount);
            holder.tvAmount.setTextColor(Color.DKGRAY);
        }
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

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvDetails;
        TextView tvDate;
        TextView tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvRecentTitle);
            tvDetails = itemView.findViewById(R.id.tvRecentDetails);
            tvDate = itemView.findViewById(R.id.tvRecentDate);
            tvAmount = itemView.findViewById(R.id.tvRecentAmount);
        }
    }
}
