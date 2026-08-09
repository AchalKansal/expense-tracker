package com.offline.expense;

final class SmsSuggestion {
    final long id;
    final String type;
    final double amount;
    final String category;
    final String note;
    final String sender;
    final long smsTime;

    SmsSuggestion(long id, String type, double amount, String category, String note, String sender, long smsTime) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.sender = sender;
        this.smsTime = smsTime;
    }

    boolean isIncome() {
        return EntryTypes.INCOME.equals(type);
    }
}
