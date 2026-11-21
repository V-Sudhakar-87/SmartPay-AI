
package com.smartpay.ai;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartPayJsonBuilder {

    private static final String TAG = "SMARTPAY_JSON";

    public static JSONObject build(
            String sms,
            SmartPayAI.Prediction prediction
    ) {

        try {

            if (sms == null || sms.trim().isEmpty()
                    || prediction == null) {
                return null;
            }

            String text = sms.toLowerCase(Locale.ROOT);

            JSONObject json = new JSONObject();

            // 1. Transaction type
            String transactionType =
                    extractTransactionType(text, prediction.transactionType);

            json.put("transaction_type", transactionType);
            json.put("transaction_subtype", JSONObject.NULL);

            // 2. Transaction status
            String status = extractStatus(text);

            json.put("transaction_status", status);

            // 3. Transaction amount
            Double amount = extractNumber(
                    "(?:debited\\s+with|credited\\s+by|transferred\\s+by|"
                            + "paid\\s+|received\\s+|sent\\s+|"
                            + "rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)",
                    sms
            );

            putNullable(json, "amount", amount);
            json.put("currency", "INR");

            // 4. Merchant / sender / receiver
            String merchant = extractText(
                    "(?i)\\bat\\s+(.+?)(?=\\s+(?:via|using|on|ref|"
                            + "available|avbl|avl)\\b|[.,]|$)",
                    sms
            );

            String sender = extractText(
                    "(?i)\\bfrom\\s+(.+?)(?=\\s+(?:via|ref|"
                            + "upi\\s+ref|utr)\\b|[.,]|$)",
                    sms
            );

            String receiver = extractText(
                    "(?i)\\bto\\s+([A-Za-z][A-Za-z .'-]*?)"
                            + "(?=\\s+(?:via|using|ref|upi|on)\\b|[.,]|$)",
                    sms
            );

            if (sender != null && sender.contains("@")) {
                sender = null;
            }

            if (receiver != null && receiver.contains("@")) {
                receiver = null;
            }

            putNullable(json, "merchant_name", merchant);
            putNullable(json, "sender_name", sender);
            putNullable(json, "receiver_name", receiver);

            // 5. Bank name
            String bank = extractBank(sms);

            putNullable(json, "bank_name", bank);

            // 6. Account last four digits
            String accountLast4 = extractText(
                    "(?i)(?:a/c|account)\\s*"
                            + "(?:no\\.?\\s*)?"
                            + "(?:x{2,}|\\*{2,})\\s*(\\d{4})",
                    sms
            );

            putNullable(json, "account_last4", accountLast4);

            // 7. Payment method
            String paymentMethod = extractPaymentMethod(sms);

            putNullable(json, "payment_method", paymentMethod);

            // 8. UPI ID
            String upiId = extractText(
                    "(?i)\\b([A-Za-z0-9._-]+@[A-Za-z0-9.-]+)\\b",
                    sms
            );

            putNullable(json, "upi_id", upiId);

            // 9. Reference number
            String reference = extractText(
                    "(?i)\\b(?:UPI\\s*)?"
                            + "(?:Ref(?:erence)?\\s*(?:No\\.?|Number)?"
                            + "|UTR|Txn\\s*ID)"
                            + "\\s*[:#-]?\\s*([A-Za-z0-9]+)",
                    sms
            );

            putNullable(json, "reference_number", reference);

            // 10. Transaction date
            String rawDate = extractText(
                    "(?i)\\b(\\d{1,2}-\\d{1,2}-\\d{4}"
                            + "|\\d{8}"
                            + "|\\d{4}-\\d{2}-\\d{2}"
                            + "|\\d{1,2}/\\d{1,2}/\\d{4})\\b",
                    sms
            );

            String date = normalizeDate(rawDate);

            putNullable(json, "transaction_date", date);

            // 11. Transaction time
            String time = extractText(
                    "(?i)\\b(\\d{1,2}:\\d{2}"
                            + "(?::\\d{2})?\\s*(?:AM|PM)?)(?!\\d)",
                    sms
            );

            putNullable(json, "transaction_time", time);

            // 12. Category
            String category = extractCategory(text);

            putNullable(json, "category", category);

            // 13. Reported / available balance
            Double reportedBalance = extractNumber(
                    "(?i)(?:current\\s+)?"
                            + "(?:available\\s+balance"
                            + "|avbl?\\.?\\s*bal(?:ance)?"
                            + "|avl\\.?\\s*bal(?:ance)?"
                            + "|balance(?:\\s+is)?)"
                            + "\\s*(?:is\\s*)?[:=-]?\\s*"
                            + "(?:rs\\.?|inr|₹)?\\s*"
                            + "([0-9,]+(?:\\.\\d{1,2})?)",
                    sms
            );

            json.put("balance_before", JSONObject.NULL);
            json.put("balance_after", JSONObject.NULL);

            putNullable(json, "reported_balance", reportedBalance);

            json.put("calculated_balance", JSONObject.NULL);
            json.put("balance_difference", JSONObject.NULL);

            json.put(
                    "balance_status",
                    reportedBalance != null ? "PENDING" : "UNKNOWN"
            );

            // 14. Transfer details
            json.put("source_bank", JSONObject.NULL);
            json.put("destination_bank", JSONObject.NULL);
            json.put("source_account_last4", JSONObject.NULL);
            json.put("destination_account_last4", JSONObject.NULL);

            // 15. Validation
            json.put("is_duplicate", false);

            boolean valid =
                    amount != null
                            && amount > 0
                            && prediction.confidence > 0;

            json.put("is_valid", valid);

            JSONArray missingFields = new JSONArray();

            if (amount == null || amount <= 0) {
                missingFields.put("amount");
            }

            if (bank == null) {
                missingFields.put("bank_name");
            }

            if (accountLast4 == null) {
                missingFields.put("account_last4");
            }

            if (reference == null) {
                missingFields.put("reference_number");
            }

            if (date == null) {
                missingFields.put("transaction_date");
            }

            json.put("missing_fields", missingFields);

            // 16. AI confidence
            json.put("confidence", prediction.confidence);

            Log.d(TAG, "JSON CREATED SUCCESSFULLY");
            Log.d(TAG, "Transaction Type: " + transactionType);
            Log.d(TAG, "Status: " + status);
            Log.d(TAG, "Amount: " + amount);
            Log.d(TAG, "Bank: " + bank);
            Log.d(TAG, "Account: " + accountLast4);
            Log.d(TAG, "Reference: " + reference);
            Log.d(TAG, "Reported Balance: " + reportedBalance);
            Log.d(TAG, "Date: " + date);
            Log.d(TAG, "Time: " + time);
            Log.d(TAG, "Valid: " + valid);
            Log.d(TAG, "JSON fields: " + json.length());

            return json;

        } catch (Exception e) {

            Log.e(TAG, "JSON CREATION FAILED", e);

            return null;
        }
    }

    // Determine transaction type using SMS wording

    private static String extractTransactionType(
            String text,
            String aiType
    ) {

        // 1. First identify our own account's transaction direction

        boolean ownAccountCredited = Pattern.compile(
                "(?i)your\\s+a/c\\s+.*?\\bis\\s+credited\\b"
        ).matcher(text).find();

        boolean ownAccountDebited = Pattern.compile(
                "(?i)your\\s+a/c\\s+.*?\\bis\\s+debited\\b"
        ).matcher(text).find();

        if (ownAccountCredited) {
            return "INCOME";
        }

        if (ownAccountDebited) {
            return "EXPENSE";
        }

        // 2. Fallback for other notification formats

        boolean debit = containsAny(
                text,
                "debited",
                "debit",
                "withdrawn",
                "withdrawal",
                "spent",
                "paid",
                "sent"
        );

        boolean credit = containsAny(
                text,
                "credited",
                "credit",
                "received",
                "deposited"
        );

        boolean transfer = containsAny(
                text,
                "transferred",
                "transfer"
        );

        if (debit && credit) {
            return aiType;
        }

        if (transfer) {
            return "TRANSFER";
        }

        if (debit) {
            return "EXPENSE";
        }

        if (credit) {
            return "INCOME";
        }

        return aiType;
    }


    // Extract a numeric value using the first matching regex group
    private static Double extractNumber(
            String regex,
            String text
    ) {

        try {

            Matcher matcher = Pattern.compile(
                    regex,
                    Pattern.CASE_INSENSITIVE
            ).matcher(text);

            if (matcher.find()) {

                return Double.parseDouble(
                        matcher.group(1).replace(",", "")
                );
            }

        } catch (Exception e) {

            Log.e(TAG, "NUMBER EXTRACTION ERROR", e);
        }

        return null;
    }

    // Extract text from the first matching group
    private static String extractText(
            String regex,
            String text
    ) {

        Matcher matcher = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE
        ).matcher(text);

        if (matcher.find()) {

            String value = matcher.group(1).trim();

            return value.isEmpty() ? null : value;
        }

        return null;
    }

    // Normalize supported date formats to dd-MM-yyyy
    private static String normalizeDate(String date) {

        if (date == null) {
            return null;
        }

        String[] formats = {
                "dd-MM-yyyy",
                "ddMMyyyy",
                "yyyy-MM-dd",
                "dd/MM/yyyy"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat input =
                        new SimpleDateFormat(format, Locale.ENGLISH);

                input.setLenient(false);

                Date parsed = input.parse(date);

                if (parsed != null) {

                    SimpleDateFormat output =
                            new SimpleDateFormat(
                                    "dd-MM-yyyy",
                                    Locale.ENGLISH
                            );

                    return output.format(parsed);
                }

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    // Detect bank from notification suffix or known bank names
    private static String extractBank(String sms) {

        String suffix = extractText(
                "(?i)[-\\s]+(TMB|CBoI|CBIN)\\s*$",
                sms
        );

        if (suffix != null) {

            if (suffix.equalsIgnoreCase("TMB")) {
                return "TMB";
            }

            if (suffix.equalsIgnoreCase("CBoI")) {
                return "CBoI";
            }

            if (suffix.equalsIgnoreCase("CBIN")) {
                return "CBIN";
            }
        }

        String[][] banks = {
                {"HDFC Bank", "HDFC"},
                {"State Bank of India", "SBI"},
                {"ICICI Bank", "ICICI"},
                {"Axis Bank", "Axis"},
                {"Kotak Mahindra Bank", "Kotak"},
                {"Canara Bank", "Canara"},
                {"Indian Bank", "Indian Bank"},
                {"Bank of Baroda", "Bank of Baroda"},
                {"Tamilnad Mercantile Bank", "TMB"},
                {"Central Bank of India", "CBoI"}
        };

        for (String[] bank : banks) {

            Pattern pattern = Pattern.compile(
                    "\\b" + Pattern.quote(bank[0]) + "\\b",
                    Pattern.CASE_INSENSITIVE
            );

            if (pattern.matcher(sms).find()) {
                return bank[1];
            }
        }

        // Identify bank from UPI handle
        String[][] upiBanks = {
                {"@okhdfcbank", "HDFC"},
                {"@oksbi", "SBI"},
                {"@okicici", "ICICI"},
                {"@okaxis", "Axis"},
                {"@okbizaxis", "Axis"},
                {"@okkotak", "Kotak"}
        };

        String lowerSms = sms.toLowerCase(Locale.ROOT);

        for (String[] item : upiBanks) {

            if (lowerSms.contains(item[0])) {
                return item[1];
            }
        }

        return null;
    }

    // Detect payment method
    private static String extractPaymentMethod(String sms) {

        String[] methods = {
                "UPI", "IMPS", "NEFT", "RTGS",
                "CARD", "ATM", "POS", "NACH"
        };

        for (String method : methods) {

            Pattern pattern = Pattern.compile(
                    "\\b" + method + "\\b",
                    Pattern.CASE_INSENSITIVE
            );

            if (pattern.matcher(sms).find()) {
                return method;
            }
        }

        return null;
    }

    // Detect transaction status
    private static String extractStatus(String text) {

        if (containsAny(
                text,
                "failed",
                "declined",
                "unsuccessful"
        )) {
            return "FAILED";
        }

        if (containsAny(text, "pending", "processing")) {
            return "PENDING";
        }

        if (containsAny(text, "reversed", "reversal")) {
            return "REVERSED";
        }

        if (containsAny(
                text,
                "debited",
                "credited",
                "successfully",
                "successful"
        )) {
            return "SUCCESS";
        }

        return "UNKNOWN";
    }

    // Detect basic transaction category
    private static String extractCategory(String text) {

        if (text.contains("salary")) return "SALARY";
        if (text.contains("refund")) return "REFUND";

        if (containsAny(
                text,
                "swiggy",
                "zomato",
                "restaurant"
        )) {
            return "FOOD";
        }

        if (containsAny(text, "uber", "ola")) {
            return "TRAVEL";
        }

        if (text.contains("amazon")) return "SHOPPING";
        if (text.contains("electricity")) return "BILLS";
        if (text.contains("recharge")) return "RECHARGE";

        return null;
    }

    private static boolean containsAny(
            String text,
            String... words
    ) {

        for (String word : words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private static void putNullable(
            JSONObject json,
            String key,
            Object value
    ) throws Exception {

        json.put(
                key,
                value == null ? JSONObject.NULL : value
        );
    }
}
