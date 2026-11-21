
package com.smartpay.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private final List<MonthlyBudget> budgetList = new ArrayList<>();

    private OnBudgetActionListener listener;

    // Edit and Delete callback interface
    public interface OnBudgetActionListener {
        void onEdit(MonthlyBudget budget);
        void onDelete(MonthlyBudget budget);
    }

    // Set listener from BudgetActivity
    public void setOnBudgetActionListener(OnBudgetActionListener listener) {
        this.listener = listener;
    }

    public void setBudgets(List<MonthlyBudget> budgets) {
        budgetList.clear();

        if (budgets != null) {
            budgetList.addAll(budgets);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);

        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BudgetViewHolder holder,
            int position
    ) {

        MonthlyBudget budget = budgetList.get(position);

        holder.tvBudgetName.setText(budget.budgetName);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(
                new Locale("en", "IN")
        );

        holder.tvBudgetAmount.setText(
                formatter.format(budget.amount / 100.0)
        );

        holder.tvBudgetDueDate.setText(
                budget.dueDay == 0
                        ? "Due date: Not set"
                        : "Due date: " + budget.dueDay
        );

        String status = budget.paymentStatus == null
                ? "PENDING"
                : budget.paymentStatus;

        holder.tvBudgetStatus.setText(status);

        // Edit button click
        holder.btnEditBudget.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(budget);
            }
        });

        // Delete button click
        holder.btnDeleteBudget.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(budget);
            }
        });
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {

        TextView tvBudgetName;
        TextView tvBudgetAmount;
        TextView tvBudgetDueDate;
        TextView tvBudgetStatus;

        TextView btnEditBudget;
        TextView btnDeleteBudget;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBudgetName = itemView.findViewById(R.id.tvBudgetName);
            tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);
            tvBudgetDueDate = itemView.findViewById(R.id.tvBudgetDueDate);
            tvBudgetStatus = itemView.findViewById(R.id.tvBudgetStatus);

            btnEditBudget = itemView.findViewById(R.id.btnEditBudget);
            btnDeleteBudget = itemView.findViewById(R.id.btnDeleteBudget);
        }
    }
}
