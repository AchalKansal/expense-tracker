package com.offline.expense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbySyncActivity extends Activity {
    static final String EXTRA_MODE = "mode";
    static final String EXTRA_SHARED_EXPENSE_ID = "shared_expense_id";
    static final String MODE_SEND = "send";
    static final String MODE_RECEIVE = "receive";

    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ACCENT_THEME = "accent_theme";
    private static final String KEY_DISPLAY_NAME = "sync_display_name";
    private static final int REQUEST_PERMISSIONS = 2001;

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private SharedPreferences preferences;
    private NumberFormat moneyFormat;
    private boolean darkMode;
    private String mode;
    private long sharedExpenseId;
    private NearbySyncManager syncManager;

    private LinearLayout syncRoot;
    private LinearLayout syncCard;
    private TextView syncTitle;
    private TextView syncStatusText;
    private TextView syncDetailText;
    private Button backButton;
    private FrameLayout adContainer;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Sync with friend");
        setContentView(R.layout.activity_nearby_sync);

        databaseHelper = new ExpenseDatabaseHelper(this);
        moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        AccentTheme accentTheme = AccentTheme.fromPrefsValue(preferences.getString(KEY_ACCENT_THEME, null));
        theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        sharedExpenseId = getIntent().getLongExtra(EXTRA_SHARED_EXPENSE_ID, -1L);

        bindViews();
        applyWindowInsets();
        applyTheme();
        syncTitle.setText(MODE_SEND.equals(mode) ? "Send to friend" : "Receive from friend");
        backButton.setOnClickListener(v -> finish());
        syncManager = new NearbySyncManager(this, listener);
        adView = BannerAds.attach(this, adContainer);

        ensureReadyThenStart();
    }

    private void bindViews() {
        syncRoot = findViewById(R.id.syncRoot);
        syncCard = findViewById(R.id.syncCard);
        syncTitle = findViewById(R.id.syncTitle);
        syncStatusText = findViewById(R.id.syncStatusText);
        syncDetailText = findViewById(R.id.syncDetailText);
        backButton = findViewById(R.id.backButton);
        adContainer = findViewById(R.id.adContainer);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        syncRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            syncRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        syncRoot.requestApplyInsets();
    }

    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        return new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private void ensureReadyThenStart() {
        List<String> missing = new ArrayList<>();
        for (String permission : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), REQUEST_PERMISSIONS);
            return;
        }
        ensureDisplayNameThenStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) return;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                syncStatusText.setText("Permission needed");
                syncDetailText.setText("Bluetooth/nearby-device permission is required to sync with a friend's phone.");
                return;
            }
        }
        ensureDisplayNameThenStart();
    }

    private void ensureDisplayNameThenStart() {
        String name = preferences.getString(KEY_DISPLAY_NAME, "");
        if (!TextUtils.isEmpty(name)) {
            startSync(name);
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Your name");
        new AlertDialog.Builder(this)
                .setTitle("What should your friend see?")
                .setMessage("This name is only shown to a nearby device during sync — nothing else is shared.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    String entered = input.getText().toString().trim();
                    if (TextUtils.isEmpty(entered)) entered = "A friend";
                    preferences.edit().putString(KEY_DISPLAY_NAME, entered).apply();
                    startSync(entered);
                })
                .show();
    }

    private void startSync(String localName) {
        if (MODE_SEND.equals(mode)) {
            SharedExpense expense = databaseHelper.getSharedExpenseById(sharedExpenseId);
            if (expense == null) {
                syncStatusText.setText("Nothing to send");
                return;
            }
            syncManager.startDiscovery(localName);
        } else {
            syncManager.startAdvertising(localName);
        }
    }

    private final NearbySyncManager.Listener listener = new NearbySyncManager.Listener() {
        @Override
        public void onStatus(String message) {
            runOnUiThread(() -> syncStatusText.setText(message));
        }

        @Override
        public void onConnected(String endpointId) {
            runOnUiThread(() -> {
                if (MODE_SEND.equals(mode)) sendSharedExpense();
            });
        }

        @Override
        public void onPayloadReceived(byte[] bytes) {
            runOnUiThread(() -> handleReceivedPayload(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
        }

        @Override
        public void onError(String message) {
            runOnUiThread(() -> {
                syncStatusText.setText("Couldn't sync");
                syncDetailText.setText(message);
            });
        }
    };

    private void sendSharedExpense() {
        SharedExpense expense = databaseHelper.getSharedExpenseById(sharedExpenseId);
        if (expense == null) return;
        ExpenseEntry entry = databaseHelper.getEntry(expense.entryId);
        String category = entry != null ? entry.category : "Other";
        String note = entry != null ? entry.note : "";

        try {
            JSONObject json = new JSONObject();
            json.put("totalAmount", expense.totalAmount);
            json.put("senderShare", expense.myShare);
            json.put("receiverShare", expense.friendShare);
            json.put("paidBySender", expense.iPaid());
            json.put("category", category);
            json.put("note", note == null ? "" : note);
            json.put("createdAt", expense.createdAt);
            json.put("senderDisplayName", preferences.getString(KEY_DISPLAY_NAME, "A friend"));
            syncManager.sendJson(json.toString());
            syncDetailText.setText("Sent " + moneyFormat.format(expense.totalAmount) + " split to your friend's device.");
        } catch (JSONException e) {
            syncStatusText.setText("Couldn't prepare data");
        }
    }

    private void handleReceivedPayload(String json) {
        try {
            JSONObject payload = new JSONObject(json);
            double totalAmount = payload.getDouble("totalAmount");
            double senderShare = payload.getDouble("senderShare");
            double receiverShare = payload.getDouble("receiverShare");
            boolean paidBySender = payload.getBoolean("paidBySender");
            String category = payload.optString("category", "Other");
            String note = payload.optString("note", "");
            long createdAt = payload.optLong("createdAt", System.currentTimeMillis());
            String senderName = payload.optString("senderDisplayName", "A friend");

            syncStatusText.setText("Split expense received");
            syncDetailText.setText(senderName + " sent " + moneyFormat.format(totalAmount)
                    + " total — your share " + moneyFormat.format(receiverShare));

            new AlertDialog.Builder(this)
                    .setTitle("Add this split expense?")
                    .setMessage(senderName + "\n" + category + (TextUtils.isEmpty(note) ? "" : " — " + note)
                            + "\nTotal: " + moneyFormat.format(totalAmount)
                            + "\nYour share: " + moneyFormat.format(receiverShare)
                            + "\nPaid by: " + (paidBySender ? senderName : "you"))
                    .setPositiveButton("Add", (dialog, which) -> {
                        String paidBy = paidBySender ? ExpenseDatabaseHelper.PAID_BY_FRIEND : ExpenseDatabaseHelper.PAID_BY_ME;
                        databaseHelper.addSharedExpense(senderName, totalAmount, receiverShare, senderShare,
                                paidBy, category, note, createdAt, ExpenseDatabaseHelper.SHARED_ORIGIN_RECEIVED);
                        Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } catch (JSONException e) {
            syncStatusText.setText("Received something we couldn't read");
        }
    }

    private void applyTheme() {
        syncRoot.setBackgroundColor(theme.colorBackground());
        syncCard.setBackground(theme.makeCardDrawable());
        syncTitle.setTextColor(theme.colorInk());
        syncStatusText.setTextColor(theme.colorInk());
        syncDetailText.setTextColor(theme.colorMuted());
        backButton.setTextColor(theme.colorInk());
        adContainer.setBackgroundColor(theme.colorSurface());
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
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        if (syncManager != null) syncManager.stop();
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
