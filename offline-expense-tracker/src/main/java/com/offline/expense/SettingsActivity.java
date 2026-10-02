package com.offline.expense;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ACCENT_THEME = "accent_theme";

    private SharedPreferences preferences;
    private ThemeHelper theme;
    private boolean darkMode;
    private AccentTheme accentTheme;

    private LinearLayout settingsRoot;
    private LinearLayout settingsCard;
    private TextView settingsTitle;
    private TextView appearanceHeading;
    private View appearanceDivider;
    private TextView darkModeLabel;
    private Switch darkModeSwitch;
    private TextView themeColorLabel;
    private LinearLayout themeSwatchRow;
    private Button backButton;
    private FrameLayout adContainer;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Settings");
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        accentTheme = AccentTheme.fromPrefsValue(preferences.getString(KEY_ACCENT_THEME, null));
        theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);

        bindViews();
        applyWindowInsets();
        setupActions();
        setupDarkModeSwitch();
        renderThemeSwatches();
        applyTheme();
        adView = BannerAds.attach(this, adContainer);
    }

    private void bindViews() {
        settingsRoot = findViewById(R.id.settingsRoot);
        settingsCard = findViewById(R.id.settingsCard);
        settingsTitle = findViewById(R.id.settingsTitle);
        appearanceHeading = findViewById(R.id.appearanceHeading);
        appearanceDivider = findViewById(R.id.appearanceDivider);
        darkModeLabel = findViewById(R.id.darkModeLabel);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        themeColorLabel = findViewById(R.id.themeColorLabel);
        themeSwatchRow = findViewById(R.id.themeSwatchRow);
        backButton = findViewById(R.id.backButton);
        adContainer = findViewById(R.id.adContainer);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        settingsRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            settingsRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        settingsRoot.requestApplyInsets();
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
    }

    private void setupDarkModeSwitch() {
        darkModeSwitch.setChecked(darkMode);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            darkMode = isChecked;
            preferences.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
            theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);
            applyTheme();
            renderThemeSwatches();
        });
    }

    private void renderThemeSwatches() {
        themeSwatchRow.removeAllViews();
        for (AccentTheme option : AccentTheme.values()) {
            themeSwatchRow.addView(makeThemeSwatch(option));
        }
    }

    private View makeThemeSwatch(AccentTheme option) {
        View swatch = new View(this);
        boolean selected = option == accentTheme;
        swatch.setBackground(theme.makeSwatchDrawable(option.swatchColor(), selected));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(theme.dp(32), theme.dp(32));
        params.setMarginEnd(theme.dp(12));
        swatch.setLayoutParams(params);
        swatch.setOnClickListener(v -> {
            if (option == accentTheme) return;
            accentTheme = option;
            preferences.edit().putString(KEY_ACCENT_THEME, accentTheme.name()).apply();
            theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);
            applyTheme();
            renderThemeSwatches();
        });
        return swatch;
    }

    private void applyTheme() {
        settingsRoot.setBackgroundColor(theme.colorBackground());
        settingsCard.setBackground(theme.makeCardDrawable());
        settingsTitle.setTextColor(theme.colorInk());
        appearanceHeading.setTextColor(theme.colorInk());
        appearanceDivider.setBackgroundColor(theme.colorBorder());
        darkModeLabel.setTextColor(theme.colorInk());
        themeColorLabel.setTextColor(theme.colorInk());
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
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
