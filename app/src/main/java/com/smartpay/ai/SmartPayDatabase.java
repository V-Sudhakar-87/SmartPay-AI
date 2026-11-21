
package com.smartpay.ai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                Transaction.class,
                Budget.class,
                MonthlyBudget.class,
                AppNotification.class
        },
        version = 5,
        exportSchema = false
)
public abstract class SmartPayDatabase extends RoomDatabase {

    private static volatile SmartPayDatabase INSTANCE;

    public abstract TransactionDao transactionDao();

    public abstract BudgetDao budgetDao();

    public abstract AppNotificationDao appNotificationDao();


    // VERSION 1 TO VERSION 2

    private static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL("ALTER TABLE transactions ADD COLUMN transactionSubtype TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN transactionStatus TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN senderName TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN receiverName TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN bankName TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN accountLast4 TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN upiId TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN referenceNumber TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN transactionDateText TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN transactionTime TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN balanceBefore INTEGER");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN balanceAfter INTEGER");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN reportedBalance INTEGER");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN calculatedBalance INTEGER");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN balanceDifference INTEGER");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN balanceStatus TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN sourceBank TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN destinationBank TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN sourceAccountLast4 TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN destinationAccountLast4 TEXT");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN isDuplicate INTEGER NOT NULL DEFAULT 0");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN isValid INTEGER NOT NULL DEFAULT 0");
                    database.execSQL("ALTER TABLE transactions ADD COLUMN confidence REAL NOT NULL DEFAULT 0");
                }
            };


    // VERSION 2 TO VERSION 3

    private static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS `budgets` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`budgetName` TEXT, " +
                                    "`category` TEXT, " +
                                    "`amount` INTEGER NOT NULL, " +
                                    "`dueDay` INTEGER NOT NULL, " +
                                    "`isRecurring` INTEGER NOT NULL DEFAULT 1, " +
                                    "`reminderEnabled` INTEGER NOT NULL DEFAULT 0, " +
                                    "`startMonth` TEXT, " +
                                    "`isActive` INTEGER NOT NULL DEFAULT 1, " +
                                    "`createdAt` INTEGER NOT NULL)"
                    );

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS `monthly_budgets` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`budgetId` INTEGER NOT NULL, " +
                                    "`budgetName` TEXT, " +
                                    "`category` TEXT, " +
                                    "`amount` INTEGER NOT NULL, " +
                                    "`budgetMonth` TEXT, " +
                                    "`dueDay` INTEGER NOT NULL, " +
                                    "`paymentStatus` TEXT DEFAULT 'PENDING', " +
                                    "`paidAt` INTEGER NOT NULL, " +
                                    "`reminderEnabled` INTEGER NOT NULL DEFAULT 0, " +
                                    "`createdAt` INTEGER NOT NULL)"
                    );
                }
            };


    // VERSION 3 TO VERSION 4
    // Create notification history table

    private static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL(
                            "CREATE TABLE IF NOT EXISTS `app_notifications` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`title` TEXT, " +
                                    "`message` TEXT, " +
                                    "`monthlyBudgetId` INTEGER NOT NULL, " +
                                    "`notificationType` TEXT, " +
                                    "`isRead` INTEGER NOT NULL DEFAULT 0, " +
                                    "`createdAt` INTEGER NOT NULL)"
                    );
                }
            };


    // VERSION 4 TO VERSION 5
    // Add actionEnabled column to notification history

    private static final Migration MIGRATION_4_5 =
            new Migration(4, 5) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {

                    database.execSQL(
                            "ALTER TABLE app_notifications " +
                                    "ADD COLUMN actionEnabled INTEGER NOT NULL DEFAULT 0"
                    );
                }
            };


    // DATABASE INSTANCE

    public static SmartPayDatabase getDatabase(Context context) {

        if (INSTANCE == null) {

            synchronized (SmartPayDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SmartPayDatabase.class,
                                    "smartpay_database"
                            )
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5
                            )
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
