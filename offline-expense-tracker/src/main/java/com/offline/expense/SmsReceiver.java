package com.offline.expense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public final class SmsReceiver extends BroadcastReceiver {
    static final String PREFS_NAME = "expense_tracker_prefs";
    static final String KEY_SMS_AUTO_DETECT = "sms_auto_detect_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_SMS_AUTO_DETECT, false)) return;

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        String sender = messages[0].getOriginatingAddress();
        long timestamp = messages[0].getTimestampMillis();
        StringBuilder bodyBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message != null && message.getMessageBody() != null) {
                bodyBuilder.append(message.getMessageBody());
            }
        }

        ParsedTransaction parsed = SmsTransactionParser.parse(bodyBuilder.toString());
        if (parsed == null) return;

        ExpenseDatabaseHelper databaseHelper = new ExpenseDatabaseHelper(context);
        String dedupeKey = sender + "|" + timestamp + "|" + parsed.amount + "|" + parsed.type;
        long id = databaseHelper.addSmsSuggestion(
                parsed.type, parsed.amount, parsed.category, parsed.note, sender, timestamp, dedupeKey);

        if (id != -1) {
            SmsNotifications.showSuggestionNotification(context, databaseHelper.getPendingSmsSuggestionCount());
        }
    }
}
