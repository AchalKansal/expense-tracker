package com.offline.coders;

final class ExpenseEntry {
    final long id;
    final String type;
    final double amount;
    final String category;
    final String note;
    final long createdAt;

    ExpenseEntry(long id, String type, double amount, String category, String note, long createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.createdAt = createdAt;
    }

    boolean isIncome() {
        return EntryTypes.INCOME.equals(type);
    }
}
