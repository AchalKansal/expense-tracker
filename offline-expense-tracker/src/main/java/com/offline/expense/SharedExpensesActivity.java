package com.offline.expense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SharedExpensesActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ACCENT_THEME = "accent_theme";

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private NumberFormat moneyFormat;
    private SimpleDateFormat dateFormat;
    private boolean darkMode;

    private LinearLayout sharedRoot;
    private TextView sharedTitle;
    private TextView sharedSubtitle;
    private Button backButton;
    private Button receiveButton;
    private Button addSplitButton;
    private LinearLayout friendsContainer;
    private TextView friendsEmptyText;
    private FrameLayout adContainer;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Shared expenses");
        setContentView(R.layout.activity_shared_expenses);

        databaseHelper = new ExpenseDatabaseHelper(this);
        moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        AccentTheme accentTheme = AccentTheme.fromPrefsValue(preferences.getString(KEY_ACCENT_THEME, null));
        theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);

        bindViews();
        applyWindowInsets();
        setupActions();
        applyTheme();
        renderFriends();
        adView = BannerAds.attach(this, adContainer);
    }

    private void bindViews() {
        sharedRoot = findViewById(R.id.sharedRoot);
        sharedTitle = findViewById(R.id.sharedTitle);
        sharedSubtitle = findViewById(R.id.sharedSubtitle);
        backButton = findViewById(R.id.backButton);
        receiveButton = findViewById(R.id.receiveButton);
        addSplitButton = findViewById(R.id.addSplitButton);
        friendsContainer = findViewById(R.id.friendsContainer);
        friendsEmptyText = findViewById(R.id.friendsEmptyText);
        adContainer = findViewById(R.id.adContainer);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        sharedRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            sharedRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        sharedRoot.requestApplyInsets();
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
        addSplitButton.setOnClickListener(view -> startActivity(new Intent(this, SplitEntryActivity.class)));
        receiveButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, NearbySyncActivity.class);
            intent.putExtra(NearbySyncActivity.EXTRA_MODE, NearbySyncActivity.MODE_RECEIVE);
            startActivity(intent);
        });
    }

    private void renderFriends() {
        List<FriendBalance> balances = databaseHelper.getFriendBalances();
        friendsContainer.removeAllViews();
        friendsEmptyText.setVisibility(balances.isEmpty() ? View.VISIBLE : View.GONE);
        for (FriendBalance balance : balances) {
            friendsContainer.addView(createFriendCard(balance));
        }
    }

    private View createFriendCard(FriendBalance balance) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(theme.makeCardDrawable());
        card.setPadding(theme.dp(12), theme.dp(10), theme.dp(12), theme.dp(10));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, theme.dp(8));
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText("🧑 " + balance.friendName);
        name.setTextColor(theme.colorInk());
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView balanceText = new TextView(this);
        double net = balance.netBalance;
        int color;
        String label;
        if (net > 0.01) {
            color = getColor(R.color.income);
            label = "Owes you " + moneyFormat.format(net);
        } else if (net < -0.01) {
            color = getColor(R.color.expense);
            label = "You owe " + moneyFormat.format(-net);
        } else {
            color = theme.colorMuted();
            label = "Settled net";
        }
        balanceText.setText(label);
        balanceText.setTextColor(color);
        balanceText.setTextSize(13);
        balanceText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        header.addView(name);
        header.addView(balanceText);
        card.addView(header);

        LinearLayout detail = new LinearLayout(this);
        detail.setOrientation(LinearLayout.VERTICAL);
        detail.setVisibility(View.GONE);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = theme.dp(10);
        detail.setLayoutParams(detailParams);
        card.addView(detail);

        header.setOnClickListener(v -> {
            boolean expanding = detail.getVisibility() != View.VISIBLE;
            if (expanding && detail.getChildCount() == 0) {
                populateFriendDetail(detail, balance.friendName);
            }
            detail.setVisibility(expanding ? View.VISIBLE : View.GONE);
        });

        return card;
    }

    private void populateFriendDetail(LinearLayout detail, String friendName) {
        List<SharedExpense> items = databaseHelper.getSharedExpensesForFriend(friendName);
        for (SharedExpense item : items) {
            detail.addView(createSharedExpenseRow(item));
        }

        boolean hasUnsettled = false;
        for (SharedExpense item : items) {
            if (!item.settled) hasUnsettled = true;
        }

        if (hasUnsettled) {
            Button settleButton = new Button(this);
            settleButton.setText("Settle up");
            settleButton.setAllCaps(false);
            settleButton.setTextColor(theme.colorPrimary());
            settleButton.setBackground(theme.makeToggleDrawable());
            settleButton.setTextSize(13);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(40));
            params.topMargin = theme.dp(6);
            settleButton.setLayoutParams(params);
            settleButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Settle up with " + friendName + "?")
                        .setMessage("This marks all outstanding split expenses with " + friendName + " as settled.")
                        .setPositiveButton("Settle", (dialog, which) -> {
                            databaseHelper.settleFriend(friendName);
                            renderFriends();
                            Toast.makeText(this, "Settled up with " + friendName, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            detail.addView(settleButton);
        }
    }

    private View createSharedExpenseRow(SharedExpense item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.makeInputDrawable());
        row.setPadding(theme.dp(10), theme.dp(8), theme.dp(8), theme.dp(8));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = theme.dp(6);
        row.setLayoutParams(rowParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        String paidLabel = item.iPaid() ? "You paid" : item.friendName + " paid";
        String originTag = ExpenseDatabaseHelper.SHARED_ORIGIN_RECEIVED.equals(item.origin) ? " (synced)" : "";
        title.setText(CategoryIcons.getEmoji(getEntryCategory(item)) + "  " + moneyFormat.format(item.totalAmount)
                + " total — " + paidLabel + originTag);
        title.setTextColor(theme.colorInk());
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView subtitle = new TextView(this);
        subtitle.setText(dateFormat.format(new Date(item.createdAt)) + " · your share " + moneyFormat.format(item.myShare)
                + (item.settled ? " · settled" : ""));
        subtitle.setTextColor(theme.colorMuted());
        subtitle.setTextSize(11);

        details.addView(title);
        details.addView(subtitle);

        Button shareButton = new Button(this);
        shareButton.setText("📶");
        shareButton.setTextSize(15);
        shareButton.setAllCaps(false);
        shareButton.setTextColor(theme.colorPrimary());
        shareButton.setBackground(theme.makeToggleDrawable());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(theme.dp(36), theme.dp(34));
        shareParams.setMargins(theme.dp(6), 0, 0, 0);
        shareButton.setLayoutParams(shareParams);
        shareButton.setPadding(0, 0, 0, 0);
        shareButton.setMinWidth(0);
        shareButton.setMinHeight(0);
        shareButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NearbySyncActivity.class);
            intent.putExtra(NearbySyncActivity.EXTRA_MODE, NearbySyncActivity.MODE_SEND);
            intent.putExtra(NearbySyncActivity.EXTRA_SHARED_EXPENSE_ID, item.id);
            startActivity(intent);
        });

        row.addView(details);
        row.addView(shareButton);
        return row;
    }

    // The shared_expenses row itself doesn't store category/note (that lives on the linked
    // entries row) — for the emoji we fall back to a generic tag since looking up the entry
    // per row would mean an extra query per item; good enough for this summary view.
    private String getEntryCategory(SharedExpense item) {
        return "Other";
    }

    private void applyTheme() {
        sharedRoot.setBackgroundColor(theme.colorBackground());
        sharedTitle.setTextColor(theme.colorInk());
        sharedSubtitle.setTextColor(theme.colorMuted());
        friendsEmptyText.setTextColor(theme.colorMuted());
        backButton.setTextColor(theme.colorInk());
        receiveButton.setTextColor(theme.colorInk());
        receiveButton.setBackground(theme.makeToggleDrawable());
        addSplitButton.setBackground(theme.makePremiumButtonDrawable());
        addSplitButton.setTextColor(theme.colorOnAccentFill());
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
    protected void onResume() {
        super.onResume();
        applyTheme();
        renderFriends();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
