package com.offline.expense;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationManagerCompat;

public final class SmsNotificationListenerService extends NotificationListenerService {
    static final String PREFS_NAME = "expense_tracker_prefs";
    static final String KEY_NOTIFICATION_DETECT_ENABLED = "notification_detect_enabled";

    static boolean isListenerAccessGranted(Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.getPackageName());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_NOTIFICATION_DETECT_ENABLED, false)) return;

        String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this);
        if (defaultSmsPackage == null || !defaultSmsPackage.equals(sbn.getPackageName())) return;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        CharSequence sender = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (body == null) body = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (body == null) return;

        ParsedTransaction parsed = SmsTransactionParser.parse(body.toString());
        if (parsed == null) return;

        ExpenseDatabaseHelper databaseHelper = new ExpenseDatabaseHelper(this);
        String senderStr = sender == null ? "" : sender.toString();
        String remembered = databaseHelper.getCategoryForMerchant(parsed.type, parsed.note);
        String category = remembered != null ? remembered : parsed.category;
        // Keyed on message content (not sbn.getPostTime()): the SMS app can repost/update the
        // same notification (e.g. marking it read, regrouping it) which changes postTime but not
        // the underlying message, so a time-based key let the same SMS create duplicate suggestions.
        String dedupeKey = "notif|" + sbn.getPackageName() + "|" + senderStr + "|" + body.toString().trim().hashCode();
        long id = databaseHelper.addSmsSuggestion(
                parsed.type, parsed.amount, category, parsed.note, senderStr, sbn.getPostTime(), dedupeKey);

        if (id != -1) {
            SmsNotifications.showSuggestionNotification(this, databaseHelper.getPendingSmsSuggestionCount());
        }
    }
}
