package com.offline.expense;

final class SharedExpense {
    final long id;
    final long entryId;
    final String friendName;
    final double totalAmount;
    final double myShare;
    final double friendShare;
    final String paidBy;
    final boolean settled;
    final long createdAt;
    final String origin;

    SharedExpense(long id, long entryId, String friendName, double totalAmount, double myShare,
                  double friendShare, String paidBy, boolean settled, long createdAt, String origin) {
        this.id = id;
        this.entryId = entryId;
        this.friendName = friendName;
        this.totalAmount = totalAmount;
        this.myShare = myShare;
        this.friendShare = friendShare;
        this.paidBy = paidBy;
        this.settled = settled;
        this.createdAt = createdAt;
        this.origin = origin;
    }

    boolean iPaid() {
        return ExpenseDatabaseHelper.PAID_BY_ME.equals(paidBy);
    }
}
