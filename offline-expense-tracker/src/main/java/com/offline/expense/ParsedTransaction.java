package com.offline.expense;

final class ParsedTransaction {
    final String type;
    final double amount;
    final String category;
    final String note;

    ParsedTransaction(String type, double amount, String category, String note) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
    }
}
