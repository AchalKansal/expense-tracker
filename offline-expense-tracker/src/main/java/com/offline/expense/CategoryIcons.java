package com.offline.expense;

final class CategoryIcons {
    private CategoryIcons() {}

    static String getEmoji(String category) {
        if (category == null) return "📌";
        switch (category.toLowerCase(java.util.Locale.US)) {
            case "food":       return "🍔";
            case "transport":  return "🚗";
            case "shopping":   return "🛍";
            case "bills":      return "💡";
            case "health":     return "🏥";
            case "rent":       return "🏠";
            case "family":     return "👨‍👩‍👧";
            case "investment":  return "📈";
            case "salary":     return "💵";
            case "business":   return "💼";
            case "freelance":  return "💻";
            case "gift":       return "🎁";
            case "refund":     return "🔄";
            default:           return "📌";
        }
    }
}
