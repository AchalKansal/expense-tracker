package com.offline.expense;

final class CategoryBudget {
    final String category;
    final double amount;
    final double spent;

    CategoryBudget(String category, double amount, double spent) {
        this.category = category;
        this.amount = amount;
        this.spent = spent;
    }
}
