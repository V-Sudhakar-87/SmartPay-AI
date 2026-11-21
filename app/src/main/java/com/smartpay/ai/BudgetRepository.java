
package com.smartpay.ai;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {

    private final BudgetDao budgetDao;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    public BudgetRepository(Context context) {

        SmartPayDatabase database =
                SmartPayDatabase.getDatabase(context);

        budgetDao = database.budgetDao();
    }

    // Category budget input
    public static class CategoryBudgetInput {

        public String category;
        public long amount;

        public CategoryBudgetInput(String category, long amount) {
            this.category = category;
            this.amount = amount;
        }
    }

    // Save multiple category budgets
    public void addCategoryBudgets(
            String budgetName,
            String budgetMonth,
            List<CategoryBudgetInput> categories,
            OnBudgetSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                long currentTime = System.currentTimeMillis();

                for (CategoryBudgetInput item : categories) {

                    if (item.amount <= 0) {
                        continue;
                    }

                    Budget budget = new Budget();

                    budget.budgetName = budgetName;
                    budget.category = item.category;
                    budget.amount = item.amount;
                    budget.dueDay = 0;
                    budget.isRecurring = false;
                    budget.reminderEnabled = false;
                    budget.startMonth = budgetMonth;
                    budget.isActive = true;
                    budget.createdAt = currentTime;

                    long budgetId = budgetDao.insertBudget(budget);

                    MonthlyBudget monthlyBudget = new MonthlyBudget();

                    monthlyBudget.budgetId = (int) budgetId;
                    monthlyBudget.budgetName = budgetName;
                    monthlyBudget.category = item.category;
                    monthlyBudget.amount = item.amount;
                    monthlyBudget.budgetMonth = budgetMonth;
                    monthlyBudget.dueDay = 0;
                    monthlyBudget.paymentStatus = "PENDING";
                    monthlyBudget.paidAt = 0;
                    monthlyBudget.reminderEnabled = false;
                    monthlyBudget.createdAt = currentTime;

                    budgetDao.insertMonthlyBudget(monthlyBudget);
                }

                if (listener != null) {
                    listener.onSuccess();
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Save a new single budget

    // Save a new single budget for selected month
    public void addBudget(
            String budgetName,
            String category,
            long amount,
            int dueDay,
            boolean isRecurring,
            boolean reminderEnabled,
            String budgetMonth,
            OnBudgetSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                long currentTime = System.currentTimeMillis();

                Budget budget = new Budget();

                budget.budgetName = budgetName;
                budget.category = category;
                budget.amount = amount;
                budget.dueDay = dueDay;
                budget.isRecurring = isRecurring;
                budget.reminderEnabled = reminderEnabled;
                budget.startMonth = budgetMonth;
                budget.isActive = true;
                budget.createdAt = currentTime;

                long budgetId = budgetDao.insertBudget(budget);

                MonthlyBudget monthlyBudget = new MonthlyBudget();

                monthlyBudget.budgetId = (int) budgetId;
                monthlyBudget.budgetName = budgetName;
                monthlyBudget.category = category;
                monthlyBudget.amount = amount;
                monthlyBudget.budgetMonth = budgetMonth;
                monthlyBudget.dueDay = dueDay;
                monthlyBudget.paymentStatus = "PENDING";
                monthlyBudget.paidAt = 0;
                monthlyBudget.reminderEnabled = reminderEnabled;
                monthlyBudget.createdAt = currentTime;

                budgetDao.insertMonthlyBudget(monthlyBudget);

                if (listener != null) {
                    listener.onSuccess();
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Save a new budget and return its monthly budget ID
    public void addBudgetWithId(
            String budgetName,
            String category,
            long amount,
            int dueDay,
            boolean isRecurring,
            boolean reminderEnabled,
            String budgetMonth,
            OnBudgetCreatedListener listener
    ) {

        executorService.execute(() -> {

            try {

                long currentTime = System.currentTimeMillis();

                Budget budget = new Budget();

                budget.budgetName = budgetName;
                budget.category = category;
                budget.amount = amount;
                budget.dueDay = dueDay;
                budget.isRecurring = isRecurring;
                budget.reminderEnabled = reminderEnabled;
                budget.startMonth = budgetMonth;
                budget.isActive = true;
                budget.createdAt = currentTime;

                long budgetId = budgetDao.insertBudget(budget);

                MonthlyBudget monthlyBudget = new MonthlyBudget();

                monthlyBudget.budgetId = (int) budgetId;
                monthlyBudget.budgetName = budgetName;
                monthlyBudget.category = category;
                monthlyBudget.amount = amount;
                monthlyBudget.budgetMonth = budgetMonth;
                monthlyBudget.dueDay = dueDay;
                monthlyBudget.paymentStatus = "PENDING";
                monthlyBudget.paidAt = 0;
                monthlyBudget.reminderEnabled = reminderEnabled;
                monthlyBudget.createdAt = currentTime;

                long monthlyBudgetId =
                        budgetDao.insertMonthlyBudget(monthlyBudget);

                if (listener != null) {
                    listener.onSuccess((int) monthlyBudgetId);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }


    // Fetch current month's individual budget items
    public void getMonthlyBudgets(
            String month,
            OnMonthlyBudgetsLoadedListener listener
    ) {

        executorService.execute(() -> {

            try {

                List<MonthlyBudget> budgets =
                        budgetDao.getMonthlyBudgets(month);

                if (listener != null) {
                    listener.onSuccess(budgets);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Get all monthly budget records
    public void getAllMonthlyBudgets(
            OnMonthlyBudgetsLoadedListener listener
    ) {

        executorService.execute(() -> {

            try {

                List<MonthlyBudget> budgets =
                        budgetDao.getAllMonthlyBudgets();

                if (listener != null) {
                    listener.onSuccess(budgets);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Get one monthly budget using its ID
    public void getMonthlyBudgetById(
            int id,
            OnMonthlyBudgetLoadedListener listener
    ) {

        executorService.execute(() -> {

            try {

                MonthlyBudget budget =
                        budgetDao.getMonthlyBudgetById(id);

                if (listener != null) {
                    listener.onSuccess(budget);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Get parent budget using its ID
    public void getBudgetById(
            int budgetId,
            OnBudgetLoadedListener listener
    ) {

        executorService.execute(() -> {

            try {

                Budget budget =
                        budgetDao.getBudgetById(budgetId);

                if (listener != null) {
                    listener.onSuccess(budget);
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Update monthly budget and parent budget together
    public void updateBudgetAndMonthlyBudget(
            Budget parentBudget,
            MonthlyBudget monthlyBudget,
            OnBudgetSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                budgetDao.updateBudget(parentBudget);
                budgetDao.updateMonthlyBudget(monthlyBudget);

                if (listener != null) {
                    listener.onSuccess();
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Update existing monthly budget only
    public void updateMonthlyBudget(
            MonthlyBudget monthlyBudget,
            OnBudgetSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                budgetDao.updateMonthlyBudget(monthlyBudget);

                if (listener != null) {
                    listener.onSuccess();
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Delete only the selected monthly budget
    public void deleteMonthlyBudget(
            MonthlyBudget monthlyBudget,
            OnBudgetSavedListener listener
    ) {

        executorService.execute(() -> {

            try {

                budgetDao.deleteMonthlyBudget(monthlyBudget);

                if (listener != null) {
                    listener.onSuccess();
                }

            } catch (Exception e) {

                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    // Callback for loading monthly budgets
    public interface OnMonthlyBudgetsLoadedListener {

        void onSuccess(List<MonthlyBudget> budgets);

        void onError(String error);
    }

    // Callback for loading one monthly budget
    public interface OnMonthlyBudgetLoadedListener {

        void onSuccess(MonthlyBudget budget);

        void onError(String error);
    }

    // Callback for loading parent budget
    public interface OnBudgetLoadedListener {

        void onSuccess(Budget budget);

        void onError(String error);
    }

    public interface OnBudgetCreatedListener {

        void onSuccess(int monthlyBudgetId);

        void onError(String error);
    }

    // Save callback
    public interface OnBudgetSavedListener {

        void onSuccess();

        void onError(String error);
    }
}
