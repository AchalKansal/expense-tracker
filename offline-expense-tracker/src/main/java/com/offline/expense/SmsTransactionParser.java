package com.offline.expense;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort, fully on-device parser for bank/UPI transaction SMS (common Indian bank formats).
 * Never network-backed: everything here is regex/keyword matching against the SMS text.
 */
final class SmsTransactionParser {
    private SmsTransactionParser() {}

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);

    private static final String[] DEBIT_KEYWORDS = {
            "debited", "debit", "spent", "paid", "withdrawn", "purchase", "deducted", "sent"
    };
    private static final String[] CREDIT_KEYWORDS = {
            "credited", "credit", "received", "deposited", "refunded"
    };
    private static final String[] EXCLUDE_KEYWORDS = {
            "otp", "one time password", "verification code"
    };

    private static final Pattern MERCHANT_TO = Pattern.compile(
            "\\b(?:to|towards)\\s+([A-Za-z0-9 &._'-]{2,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_AT = Pattern.compile(
            "\\bat\\s+([A-Za-z0-9 &._'-]{2,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_FROM = Pattern.compile(
            "\\bfrom\\s+([A-Za-z0-9 &._'-]{2,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_VPA = Pattern.compile(
            "\\bvpa\\s+([\\w.\\-]+@[\\w]+)", Pattern.CASE_INSENSITIVE);
    private static final String[] MERCHANT_STOP_WORDS = {
            " on ", " ref", " dated", " info", " via", " using", " avl", " bal", " a/c", " acct",
            " ac ", " upi", " imps", " neft", " rrn", ".", ","
    };

    static ParsedTransaction parse(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        String lower = body.toLowerCase(Locale.US);
        if (containsAny(lower, EXCLUDE_KEYWORDS)) return null;

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(body);
        if (!amountMatcher.find()) return null;
        double amount;
        try {
            amount = Double.parseDouble(amountMatcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) return null;

        String type = detectType(lower);
        if (type == null) return null;

        String merchant = extractMerchant(body);
        String category = guessCategory(type, merchant, lower);
        String note = merchant == null ? "" : merchant;

        return new ParsedTransaction(type, amount, category, note);
    }

    private static String detectType(String lower) {
        int debitIndex = earliestIndexOf(lower, DEBIT_KEYWORDS);
        int creditIndex = earliestIndexOf(lower, CREDIT_KEYWORDS);
        if (debitIndex < 0 && creditIndex < 0) return null;
        if (creditIndex >= 0 && (debitIndex < 0 || creditIndex < debitIndex)) return EntryTypes.INCOME;
        return EntryTypes.EXPENSE;
    }

    private static int earliestIndexOf(String haystack, String[] needles) {
        int earliest = -1;
        for (String needle : needles) {
            int index = haystack.indexOf(needle);
            if (index >= 0 && (earliest < 0 || index < earliest)) earliest = index;
        }
        return earliest;
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String extractMerchant(String body) {
        Matcher matcher = MERCHANT_VPA.matcher(body);
        if (matcher.find()) return cleanMerchant(matcher.group(1));

        matcher = MERCHANT_TO.matcher(body);
        if (matcher.find()) return cleanMerchant(matcher.group(1));

        matcher = MERCHANT_AT.matcher(body);
        if (matcher.find()) return cleanMerchant(matcher.group(1));

        matcher = MERCHANT_FROM.matcher(body);
        if (matcher.find()) return cleanMerchant(matcher.group(1));

        return null;
    }

    private static String cleanMerchant(String raw) {
        String lower = " " + raw.toLowerCase(Locale.US) + " ";
        int cut = raw.length();
        for (String stop : MERCHANT_STOP_WORDS) {
            int index = lower.indexOf(stop);
            if (index >= 0 && index < cut) cut = index;
        }
        String cleaned = raw.substring(0, Math.min(cut, raw.length())).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String guessCategory(String type, String merchant, String lowerBody) {
        String haystack = (merchant == null ? "" : merchant.toLowerCase(Locale.US)) + " " + lowerBody;

        if (EntryTypes.INCOME.equals(type)) {
            if (containsAny(haystack, new String[]{"salary", "sal credit", "payroll"})) return "Salary";
            if (containsAny(haystack, new String[]{"refund", "reversed", "reversal"})) return "Refund";
            if (containsAny(haystack, new String[]{"dividend", "mutual fund", "zerodha", "groww", "upstox", "interest"})) return "Investment";
            return "Other";
        }

        if (containsAny(haystack, new String[]{"swiggy", "zomato", "restaurant", "cafe", "domino", "pizza", "mcdonald", "kfc", "starbucks", "eatery", "food"})) return "Food";
        if (containsAny(haystack, new String[]{"uber", "ola", "rapido", "irctc", "indianoil", "petrol", "diesel", "fuel", "metro", "redbus", "fastag", "cab"})) return "Transport";
        if (containsAny(haystack, new String[]{"amazon", "flipkart", "myntra", "ajio", "meesho", "mall", "store", "shop"})) return "Shopping";
        if (containsAny(haystack, new String[]{"electricity", "recharge", "broadband", "airtel", "jio", "vodafone", "vi ", "dth", "gas bill", "water bill", "wifi", "postpaid", "prepaid"})) return "Bills";
        if (containsAny(haystack, new String[]{"hospital", "pharmacy", "apollo", "medplus", "clinic", "diagnostic", "medical", "medicine"})) return "Health";
        if (containsAny(haystack, new String[]{"rent", "landlord"})) return "Rent";
        if (containsAny(haystack, new String[]{"mutual fund", "zerodha", "groww", "upstox", "sip ", " nps", "stocks"})) return "Investment";

        return "Other";
    }
}
