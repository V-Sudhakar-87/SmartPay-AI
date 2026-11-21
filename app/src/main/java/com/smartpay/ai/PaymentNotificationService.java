
package com.smartpay.ai;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class PaymentNotificationService extends NotificationListenerService {

    private static final String TAG = "SMARTPAY_NOTIFY";

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile(
                    "(?i)(?:rs\\.?|inr|₹)\\s*[0-9,]+(?:\\.\\d{1,2})?"
            );

    private SmartPayAI smartPayAI;
    private SmartPayTfidf smartPayTfidf;

    // Room Database Repository
    private TransactionRepository transactionRepository;

    private ExecutorService aiExecutor;

    private volatile boolean aiReady = false;

    // --------------------------------
    // SERVICE CREATE
    // --------------------------------

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(TAG, "SERVICE CREATED");

        smartPayAI = new SmartPayAI();
        smartPayTfidf = new SmartPayTfidf();

        // Initialize Room Repository
        transactionRepository = new TransactionRepository(
                getApplicationContext()
        );

        aiExecutor = Executors.newSingleThreadExecutor();

        // Initialize AI in background
        aiExecutor.execute(() -> {

            try {

                Log.d(TAG, "INITIALIZING TF-IDF");

                boolean tfidfLoaded =
                        smartPayTfidf.initialize(
                                getApplicationContext()
                        );

                if (!tfidfLoaded) {

                    Log.e(TAG, "TF-IDF INITIALIZATION FAILED");
                    return;
                }

                Log.d(TAG, "TF-IDF INITIALIZED");

                Log.d(TAG, "INITIALIZING ONNX MODEL");

                boolean modelLoaded =
                        smartPayAI.initialize(
                                getApplicationContext()
                        );

                if (!modelLoaded) {

                    Log.e(TAG, "AI MODEL INITIALIZATION FAILED");
                    return;
                }

                aiReady = true;

                Log.d(TAG, "SMARTPAY AI READY");

            } catch (Exception e) {

                Log.e(TAG, "AI INITIALIZATION ERROR", e);
            }
        });
    }

    // --------------------------------
    // NOTIFICATION RECEIVED
    // --------------------------------

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (sbn == null || sbn.getNotification() == null) {
            return;
        }

        Notification notification = sbn.getNotification();

        Bundle extras = notification.extras;

        if (extras == null) {
            return;
        }

        Set<String> messageParts = new LinkedHashSet<>();

        // Notification title
        addText(
                messageParts,
                extras.getCharSequence(Notification.EXTRA_TITLE)
        );

        // Normal notification text
        addText(
                messageParts,
                extras.getCharSequence(Notification.EXTRA_TEXT)
        );

        // Expanded notification text
        addText(
                messageParts,
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        );

        // Sub text
        addText(
                messageParts,
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        );

        // Summary text
        addText(
                messageParts,
                extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        );

        // Expanded notification lines
        CharSequence[] lines =
                extras.getCharSequenceArray(
                        Notification.EXTRA_TEXT_LINES
                );

        if (lines != null) {

            for (CharSequence line : lines) {
                addText(messageParts, line);
            }
        }

        // MessagingStyle messages
        Parcelable[] messages =
                extras.getParcelableArray(
                        Notification.EXTRA_MESSAGES
                );

        if (messages != null) {

            for (Parcelable parcelable : messages) {

                if (parcelable instanceof Bundle) {

                    Bundle messageBundle = (Bundle) parcelable;

                    CharSequence messageText =
                            messageBundle.getCharSequence("text");

                    addText(messageParts, messageText);
                }
            }
        }

        // Combine notification text
        StringBuilder fullMessage = new StringBuilder();

        for (String part : messageParts) {

            if (fullMessage.length() > 0) {
                fullMessage.append("\n");
            }

            fullMessage.append(part);
        }

        String notificationMessage = fullMessage.toString();

        if (notificationMessage.trim().isEmpty()) {
            return;
        }

        // Ignore sensitive messages
        if (isSensitiveMessage(notificationMessage)) {

            Log.d(TAG, "Sensitive notification ignored");
            return;
        }

        // Transaction filter
        if (!isTransactionNotification(notificationMessage)) {
            return;
        }

        Log.d(TAG, "TRANSACTION NOTIFICATION DETECTED");

        // Process AI in background
        if (aiExecutor != null && !aiExecutor.isShutdown()) {

            aiExecutor.execute(() -> {

                processTransaction(notificationMessage);
            });
        }
    }

    // --------------------------------
    // AI + JSON + DATABASE PROCESSING
    // --------------------------------

    private void processTransaction(String message) {

        try {

            if (!aiReady) {

                Log.w(TAG, "AI NOT READY - NOTIFICATION SKIPPED");
                return;
            }

            Log.d(TAG, "STEP 1: CONVERTING SMS TO FEATURES");

            float[] features =
                    smartPayTfidf.convert(message);

            if (features == null || features.length != 245) {

                Log.e(TAG, "INVALID TF-IDF FEATURES");
                return;
            }

            Log.d(TAG, "STEP 2: RUNNING AI PREDICTION");

            SmartPayAI.Prediction prediction =
                    smartPayAI.predict(features);

            if (prediction == null) {

                Log.e(TAG, "AI PREDICTION FAILED");
                return;
            }

            Log.d(
                    TAG,
                    "AI TYPE: " + prediction.transactionType
            );

            Log.d(
                    TAG,
                    "AI CONFIDENCE: " + prediction.confidence
            );

            Log.d(TAG, "STEP 3: GENERATING JSON");

            JSONObject transactionJson =
                    SmartPayJsonBuilder.build(
                            message,
                            prediction
                    );

            if (transactionJson == null) {

                Log.e(TAG, "JSON GENERATION FAILED");
                return;
            }

            Log.d(TAG, "JSON GENERATED SUCCESSFULLY");

            // --------------------------------
            // JSON VALIDATION
            // --------------------------------

            boolean isValid =
                    transactionJson.optBoolean("is_valid", false);

            Log.d(TAG, "----- JSON VALIDATION -----");

            Log.d(
                    TAG,
                    "TYPE: " +
                            transactionJson.optString("transaction_type")
            );

            Log.d(
                    TAG,
                    "STATUS: " +
                            transactionJson.optString("transaction_status")
            );

            Log.d(
                    TAG,
                    "VALID: " + isValid
            );

            Log.d(
                    TAG,
                    "JSON FIELD COUNT: " + transactionJson.length()
            );

            // --------------------------------
            // SAVE TO ROOM DATABASE
            // --------------------------------

            if (isValid) {

                Log.d(TAG, "STEP 4: SENDING TRANSACTION TO DATABASE");

                if (transactionRepository != null) {

                    transactionRepository.insertTransactionFromJson(
                            transactionJson
                    );

                    Log.d(
                            TAG,
                            "TRANSACTION SENT TO ROOM DATABASE"
                    );

                } else {

                    Log.e(
                            TAG,
                            "DATABASE REPOSITORY NOT INITIALIZED"
                    );
                }

            } else {

                Log.w(
                        TAG,
                        "INVALID TRANSACTION - NOT SAVED"
                );
            }

            Log.d(TAG, "SMARTPAY AI PROCESS COMPLETED");

        } catch (Exception e) {

            Log.e(TAG, "TRANSACTION PROCESSING ERROR", e);
        }
    }

    // --------------------------------
    // ADD NOTIFICATION TEXT
    // --------------------------------

    private void addText(
            Set<String> parts,
            CharSequence value
    ) {

        if (value == null) {
            return;
        }

        String text = value.toString().trim();

        if (!text.isEmpty()) {
            parts.add(text);
        }
    }

    // --------------------------------
    // SENSITIVE MESSAGE FILTER
    // --------------------------------

    private boolean isSensitiveMessage(String message) {

        String text = message.toLowerCase(Locale.ROOT);

        String[] keywords = {

                "one time password",
                "verification code",
                "security code",
                "login code",
                "your pin",
                "upi pin",
                "password reset",
                "authentication code"
        };

        for (String keyword : keywords) {

            if (text.contains(keyword)) {
                return true;
            }
        }

        return Pattern.compile(
                "\\botp\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(text).find();
    }

    // --------------------------------
    // TRANSACTION NOTIFICATION FILTER
    // --------------------------------

    private boolean isTransactionNotification(String message) {

        String text = message.toLowerCase(Locale.ROOT);

        boolean hasAmount =
                AMOUNT_PATTERN.matcher(message).find();

        if (!hasAmount) {
            return false;
        }

        String[] transactionKeywords = {

                "debited",
                "debit",
                "credited",
                "credit",
                "withdrawn",
                "withdrawal",
                "spent",
                "paid",
                "payment",
                "received",
                "transferred",
                "transfer",
                "sent",
                "failed",
                "declined",
                "unsuccessful",
                "reversed",
                "reversal",
                "refund",
                "deposited",
                "transaction"
        };

        for (String keyword : transactionKeywords) {

            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // --------------------------------
    // SERVICE DESTROY
    // --------------------------------

    @Override
    public void onDestroy() {

        Log.d(TAG, "SERVICE DESTROYED");

        aiReady = false;

        if (aiExecutor != null) {

            aiExecutor.shutdown();
        }

        if (smartPayAI != null) {

            smartPayAI.close();
        }

        super.onDestroy();
    }
}
