package com.offline.expense;

final class DayTotal {
    final long dayMillis;
    final double total;

    DayTotal(long dayMillis, double total) {
        this.dayMillis = dayMillis;
        this.total = total;
    }
}
