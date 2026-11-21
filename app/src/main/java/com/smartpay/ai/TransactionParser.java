
package com.smartpay.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionParser {

    public static class ParsedTransaction {

        public long amount;
        public String transactionType;
        public String bankName;
        public String accountLastFour;
        public String paymentMethod;
        public String merchantName;
        public String transactionReference;

        public boolean isValid;
    }

    public static ParsedTransaction parse(String message) {

        ParsedTransaction result = new ParsedTransaction();

        result.isValid = false;

        if (message == null || message.trim().isEmpty()) {
            return result;
        }

        String text = message.toLowerCase(Locale.ROOT);

        // Ignore OTP messages
        if (text.contains("otp")
                || text.contains("one time password")
                || text.contains("verification code")
                || text.contains("upi pin")) {
            return result;
        }

        // Identify transaction type
        if (text.matches("(?s).*\\b(debited|spent|withdrawn|paid)\\b.*")) {

            result.transactionType = "EXPENSE";

        } else if (text.matches("(?s).*\\b(credited|received|deposited)\\b.*")) {

            result.transactionType = "INCOME";

        } else {

            return result;
        }

        // Extract amount
        Pattern amountPattern = Pattern.compile(
                "(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)"
                        + "|([0-9,]+(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|inr)"
        );

        Matcher amountMatcher = amountPattern.matcher(message);

        if (amountMatcher.find()) {

            String amountText = amountMatcher.group(1);

            if (amountText == null) {
                amountText = amountMatcher.group(2);
            }

            try {

                double rupees = Double.parseDouble(
                        amountText.replace(",", "")
                );

                result.amount = Math.round(rupees * 100);

            } catch (NumberFormatException e) {

                return result;
            }

        } else {

            return result;
        }

        // Identify payment method
        if (text.contains("upi")) {

            result.paymentMethod = "UPI";

        } else if (text.contains("imps")) {

            result.paymentMethod = "IMPS";

        } else if (text.contains("neft")) {

            result.paymentMethod = "NEFT";

        } else if (text.contains("rtgs")) {

            result.paymentMethod = "RTGS";

        } else if (text.contains("card")) {

            result.paymentMethod = "CARD";

        } else if (text.contains("atm")) {

            result.paymentMethod = "ATM";

        } else {

            result.paymentMethod = "UNKNOWN";
        }

        // Identify bank name
        String[] banks = {
                "SBI",
                "HDFC",
                "ICICI",
                "AXIS",
                "KOTAK",
                "CANARA",
                "INDIAN BANK",
                "BANK OF BARODA"
        };

        result.bankName = "UNKNOWN";

        for (String bank : banks) {

            if (text.contains(bank.toLowerCase(Locale.ROOT))) {

                result.bankName = bank;
                break;
            }
        }

        // Extract masked account last four digits
        Pattern accountPattern = Pattern.compile(
                "(?:XX|X{2,}|\\*{2,})\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher accountMatcher = accountPattern.matcher(message);

        if (accountMatcher.find()) {

            result.accountLastFour = accountMatcher.group(1);

        } else {

            result.accountLastFour = "UNKNOWN";
        }

        // Extract transaction reference
        Pattern referencePattern = Pattern.compile(
                "(?:UPI Ref(?:erence)?|UTR|Ref(?:erence)? No)"
                        + "\\s*[:#-]?\\s*([A-Za-z0-9]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher referenceMatcher = referencePattern.matcher(message);

        if (referenceMatcher.find()) {

            result.transactionReference = referenceMatcher.group(1);

        } else {

            result.transactionReference = "UNKNOWN";
        }

        result.merchantName = "UNKNOWN";

        result.isValid = true;

        return result;
    }
}
