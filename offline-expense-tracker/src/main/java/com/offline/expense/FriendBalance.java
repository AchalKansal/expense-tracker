package com.offline.expense;

final class FriendBalance {
    final String friendName;
    /** Positive: the friend owes the user. Negative: the user owes the friend. */
    final double netBalance;
    final int unsettledCount;

    FriendBalance(String friendName, double netBalance, int unsettledCount) {
        this.friendName = friendName;
        this.netBalance = netBalance;
        this.unsettledCount = unsettledCount;
    }
}
