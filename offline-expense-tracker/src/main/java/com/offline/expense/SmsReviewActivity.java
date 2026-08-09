package com.offline.expense;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsReviewActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final int REQUEST_READ_SMS = 2001;
    private static final int SCAN_LIMIT = 500;

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private NumberFormat moneyFormat;
    private SimpleDateFormat dateFormat;
    private boolean darkMode;

    private LinearLayout smsReviewRoot;
    private TextView smsReviewTitle;
    private TextView smsReviewSubtitle;
    private TextView suggestionsEmptyText;
    private LinearLayout suggestionsContainer;
    private Button backButton;
    private Button scanInboxButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SMS suggestions");
        setContentView(R.layout.activity_sms_review);

        databaseHelper = new ExpenseDatabaseHelper(this);
        moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        dateFormat = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault());
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);

        bindViews();
        applyWindowInsets();
        setupActions();
        applyTheme();
        renderSuggestions();
    }

    private void bindViews() {
        smsReviewRoot = findViewById(R.id.smsReviewRoot);
        smsReviewTitle = findViewById(R.id.smsReviewTitle);
        smsReviewSubtitle = findViewById(R.id.smsReviewSubtitle);
        suggestionsEmptyText = findViewById(R.id.suggestionsEmptyText);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        backButton = findViewById(R.id.backButton);
        scanInboxButton = findViewById(R.id.scanInboxButton);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        smsReviewRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            smsReviewRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        smsReviewRoot.requestApplyInsets();
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
        scanInboxButton.setOnClickListener(view -> requestReadSmsThenScan());
    }

    private void requestReadSmsThenScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            scanInbox();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, REQUEST_READ_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_SMS
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanInbox();
        } else if (requestCode == REQUEST_READ_SMS) {
            Toast.makeText(this, "SMS permission is needed to scan the inbox", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanInbox() {
        Uri uri = Telephony.Sms.Inbox.CONTENT_URI;
        String[] projection = {Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE};
        int scanned = 0;
        int imported = 0;

        try (Cursor cursor = getContentResolver().query(
                uri, projection, null, null, Telephony.Sms.DATE + " DESC LIMIT " + SCAN_LIMIT)) {
            if (cursor != null) {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);

                while (cursor.moveToNext()) {
                    scanned++;
                    String sender = addressIndex >= 0 ? cursor.getString(addressIndex) : null;
                    String body = bodyIndex >= 0 ? cursor.getString(bodyIndex) : null;
                    long time = dateIndex >= 0 ? cursor.getLong(dateIndex) : System.currentTimeMillis();

                    ParsedTransaction parsed = SmsTransactionParser.parse(body);
                    if (parsed == null) continue;

                    String dedupeKey = sender + "|" + time + "|" + parsed.amount + "|" + parsed.type;
                    long id = databaseHelper.addSmsSuggestion(
                            parsed.type, parsed.amount, parsed.category, parsed.note, sender, time, dedupeKey);
                    if (id != -1) imported++;
                }
            }
        } catch (SecurityException exception) {
            Toast.makeText(this, "SMS permission is needed to scan the inbox", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, imported + " new suggestion(s) found from " + scanned + " messages", Toast.LENGTH_LONG).show();
        renderSuggestions();
    }

    private void renderSuggestions() {
        List<SmsSuggestion> suggestions = databaseHelper.getPendingSmsSuggestions();
        suggestionsContainer.removeAllViews();
        suggestionsEmptyText.setVisibility(suggestions.isEmpty() ? View.VISIBLE : View.GONE);
        for (SmsSuggestion suggestion : suggestions) {
            suggestionsContainer.addView(createSuggestionRow(suggestion));
        }
    }

    private View createSuggestionRow(SmsSuggestion suggestion) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.makeCardDrawable());
        row.setPadding(theme.dp(12), theme.dp(10), theme.dp(8), theme.dp(10));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, theme.dp(8));
        row.setLayoutParams(rowParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        String noteSuffix = TextUtils.isEmpty(suggestion.note) ? "" : " — " + suggestion.note;
        title.setText(CategoryIcons.getEmoji(suggestion.category) + "  " + suggestion.category + noteSuffix);
        title.setTextColor(theme.colorInk());
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView subtitle = new TextView(this);
        String sender = TextUtils.isEmpty(suggestion.sender) ? "Unknown sender" : suggestion.sender;
        subtitle.setText(sender + "  ·  " + dateFormat.format(new Date(suggestion.smsTime)));
        subtitle.setTextColor(theme.colorMuted());
        subtitle.setTextSize(12);
        subtitle.setPadding(0, theme.dp(3), 0, 0);

        details.addView(title);
        details.addView(subtitle);

        TextView amount = new TextView(this);
        String prefix = suggestion.isIncome() ? "+" : "-";
        amount.setText(prefix + moneyFormat.format(suggestion.amount));
        amount.setTextColor(getColor(suggestion.isIncome() ? R.color.income : R.color.expense));
        amount.setTextSize(13);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setGravity(Gravity.END);
        amount.setPadding(theme.dp(6), 0, theme.dp(6), 0);

        Button confirmButton = makeSmallButton("✓", theme.colorPrimary(), theme.makeToggleDrawable());
        confirmButton.setOnClickListener(view -> confirmSuggestion(suggestion));

        Button editButton = makeSmallButton("✏️", theme.colorPrimary(), theme.makeToggleDrawable());
        editButton.setOnClickListener(view -> showEditDialog(suggestion));

        Button discardButton = makeSmallButton("🗑", theme.colorDanger(), theme.makeDangerButtonDrawable());
        discardButton.setOnClickListener(view -> {
            databaseHelper.deleteSmsSuggestion(suggestion.id);
            renderSuggestions();
        });

        row.addView(details);
        row.addView(amount);
        row.addView(confirmButton);
        row.addView(editButton);
        row.addView(discardButton);
        return row;
    }

    private Button makeSmallButton(String label, int textColor, android.graphics.drawable.Drawable background) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(theme.dp(36), theme.dp(34));
        params.setMargins(theme.dp(4), 0, 0, 0);
        button.setLayoutParams(params);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        return button;
    }

    private void confirmSuggestion(SmsSuggestion suggestion) {
        databaseHelper.addEntry(
                suggestion.type, suggestion.amount, suggestion.category, suggestion.note, suggestion.smsTime);
        databaseHelper.deleteSmsSuggestion(suggestion.id);
        renderSuggestions();
        Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show();
    }

    private void showEditDialog(SmsSuggestion suggestion) {
        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        int pad = theme.dp(20);
        dialogRoot.setPadding(pad, theme.dp(8), pad, 0);

        EditText amountInput = new EditText(this);
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setText(String.valueOf(suggestion.amount));
        amountInput.setHint("Amount");

        Spinner categorySpinner = new Spinner(this);
        List<String> categories = databaseHelper.getCategories(suggestion.type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equalsIgnoreCase(suggestion.category)) {
                categorySpinner.setSelection(i);
                break;
            }
        }

        EditText noteInput = new EditText(this);
        noteInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteInput.setText(suggestion.note == null ? "" : suggestion.note);
        noteInput.setHint("Description");

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = theme.dp(10);
        amountInput.setLayoutParams(fieldParams);
        categorySpinner.setLayoutParams(fieldParams);
        noteInput.setLayoutParams(fieldParams);

        dialogRoot.addView(amountInput);
        dialogRoot.addView(categorySpinner);
        dialogRoot.addView(noteInput);

        String typeLabel = EntryTypes.INCOME.equals(suggestion.type) ? "Income" : "Expense";
        new AlertDialog.Builder(this)
                .setTitle("Confirm " + typeLabel)
                .setView(dialogRoot)
                .setPositiveButton("Add", (dialog, which) -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(amountInput.getText().toString().trim());
                    } catch (NumberFormatException exception) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount <= 0 || categorySpinner.getSelectedItem() == null) {
                        Toast.makeText(this, "Invalid amount or category", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String category = categorySpinner.getSelectedItem().toString();
                    String note = noteInput.getText().toString().trim();
                    databaseHelper.addEntry(suggestion.type, amount, category, note, suggestion.smsTime);
                    databaseHelper.deleteSmsSuggestion(suggestion.id);
                    renderSuggestions();
                    Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Discard", (dialog, which) -> {
                    databaseHelper.deleteSmsSuggestion(suggestion.id);
                    renderSuggestions();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyTheme() {
        smsReviewRoot.setBackgroundColor(theme.colorBackground());
        smsReviewTitle.setTextColor(theme.colorInk());
        smsReviewSubtitle.setTextColor(theme.colorMuted());
        suggestionsEmptyText.setTextColor(theme.colorMuted());
        backButton.setTextColor(theme.colorInk());
        scanInboxButton.setTextColor(Color.WHITE);
        scanInboxButton.setBackground(theme.makePremiumButtonDrawable());
        getWindow().setStatusBarColor(theme.colorBackground());
        getWindow().setNavigationBarColor(theme.colorBackground());
        applyStatusBarAppearance();
    }

    private void applyStatusBarAppearance() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int lightFlags = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(
                    darkMode ? 0 : lightFlags, lightFlags);
        } else {
            android.view.View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (darkMode) {
                flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        renderSuggestions();
    }
}
