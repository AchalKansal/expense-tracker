package com.offline.expense;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

// ADS: import com.google.android.gms.ads.AdRequest;
// ADS: import com.google.android.gms.ads.AdSize;
// ADS: import com.google.android.gms.ads.AdView;
// ADS: import com.google.android.gms.ads.MobileAds;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    // ADS: private static final String TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741";

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private NumberFormat moneyFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat entryDateFormat;
    private SharedPreferences preferences;
    private boolean darkMode;
    private long selectedEntryDateMillis;
    private String selectedCategory = "";

    private DrawerLayout drawerLayout;
    private ScrollView drawerScroll;
    private LinearLayout drawerView;
    private LinearLayout drawerEntriesContainer;
    private TextView drawerTitleText;
    private TextView drawerOverviewItem;
    private TextView drawerHistoryItem;
    private TextView drawerManageCategoriesItem;
    private TextView drawerViewAllText;

    private LinearLayout mainRoot;
    private ScrollView rootScroll;
    private LinearLayout rootContent;
    private LinearLayout headerPanel;
    private TextView titleText;
    private TextView subtitleText;
    private TextView todayLabelText;
    private TextView todaySpentText;
    private TextView monthLabelText;
    private TextView monthSpentText;
    private TextView balanceLabelText;
    private TextView balanceStatText;
    private TextView emptyText;
    private TextView recentEntriesTitle;
    private TextView chartTitleText;
    private TextView privacyInfoText;
    private TextView viewAllText;
    private LinearLayout entriesContainer;
    private LinearLayout summaryCard;
    private LinearLayout todaySummaryCell;
    private LinearLayout monthSummaryCell;
    private LinearLayout balanceSummaryCell;

    private LinearLayout entrySection;
    private LinearLayout chartSection;
    private FlowLayout categoryChipContainer;
    private RadioGroup mainTabGroup;
    private RadioGroup typeGroup;
    private RadioButton expenseRadio;
    private RadioButton incomeRadio;
    private RadioButton entryTabRadio;
    private RadioButton chartTabRadio;
    private EditText amountInput;
    private EditText noteInput;
    private RadioButton monthlyChartRadio;
    private RadioButton yearlyChartRadio;
    private RadioButton barChartRadio;
    private RadioButton pieChartRadio;
    private TextView chartSummaryText;
    private CategoryChartView categoryChartView;
    // ADS: private FrameLayout adContainer;
    private Button addButton;
    private TextView menuButton;
    private TextView dateButton;
    private Switch themeSwitch;
    // ADS: private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);

        databaseHelper = new ExpenseDatabaseHelper(this);
        moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        dateFormat = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault());
        entryDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);
        selectedEntryDateMillis = System.currentTimeMillis();

        bindViews();
        applyWindowInsets();
        applyTheme();
        setupMainTabs();
        setupCategoryChips();
        setupSaveButton();
        setupViewAllLink();
        setupDatePicker();
        setupChartControls();
        setupThemeToggle();
        setupDrawer();
        // ADS: setupBottomBannerAd();
        refreshDashboard();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerScroll = findViewById(R.id.drawerScroll);
        drawerView = findViewById(R.id.drawerView);
        drawerEntriesContainer = findViewById(R.id.drawerEntriesContainer);
        drawerTitleText = findViewById(R.id.drawerTitleText);
        drawerOverviewItem = findViewById(R.id.drawerOverviewItem);
        drawerHistoryItem = findViewById(R.id.drawerHistoryItem);
        drawerManageCategoriesItem = findViewById(R.id.drawerManageCategoriesItem);
        drawerViewAllText = findViewById(R.id.drawerViewAllText);

        mainRoot = findViewById(R.id.mainRoot);
        rootScroll = findViewById(R.id.rootScroll);
        rootContent = findViewById(R.id.rootContent);
        headerPanel = findViewById(R.id.headerPanel);
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        todayLabelText = findViewById(R.id.todayLabelText);
        todaySpentText = findViewById(R.id.todaySpentText);
        monthLabelText = findViewById(R.id.monthLabelText);
        monthSpentText = findViewById(R.id.monthSpentText);
        balanceLabelText = findViewById(R.id.balanceLabelText);
        balanceStatText = findViewById(R.id.budgetStatusText);
        emptyText = findViewById(R.id.emptyText);
        recentEntriesTitle = findViewById(R.id.recentEntriesTitle);
        chartTitleText = findViewById(R.id.chartTitleText);
        privacyInfoText = findViewById(R.id.privacyInfoText);
        viewAllText = findViewById(R.id.viewAllText);
        entriesContainer = findViewById(R.id.entriesContainer);
        summaryCard = findViewById(R.id.summaryCard);
        todaySummaryCell = findViewById(R.id.todaySummaryCell);
        monthSummaryCell = findViewById(R.id.monthSummaryCell);
        balanceSummaryCell = findViewById(R.id.balanceSummaryCell);
        entrySection = findViewById(R.id.entrySection);
        chartSection = findViewById(R.id.chartSection);
        categoryChipContainer = findViewById(R.id.categoryChipContainer);
        mainTabGroup = findViewById(R.id.mainTabGroup);
        typeGroup = findViewById(R.id.typeGroup);
        expenseRadio = findViewById(R.id.expenseRadio);
        incomeRadio = findViewById(R.id.incomeRadio);
        entryTabRadio = findViewById(R.id.entryTabRadio);
        chartTabRadio = findViewById(R.id.chartTabRadio);
        amountInput = findViewById(R.id.amountInput);
        noteInput = findViewById(R.id.noteInput);
        monthlyChartRadio = findViewById(R.id.monthlyChartRadio);
        yearlyChartRadio = findViewById(R.id.yearlyChartRadio);
        barChartRadio = findViewById(R.id.barChartRadio);
        pieChartRadio = findViewById(R.id.pieChartRadio);
        chartSummaryText = findViewById(R.id.chartSummaryText);
        categoryChartView = findViewById(R.id.categoryChartView);
        // ADS: adContainer = findViewById(R.id.adContainer);
        addButton = findViewById(R.id.addButton);
        menuButton = findViewById(R.id.menuButton);
        dateButton = findViewById(R.id.dateButton);
        themeSwitch = findViewById(R.id.themeSwitch);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        mainRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            rootContent.setPadding(p, p + top, p, p);
            // ADS: adContainer.setPadding(0, 0, 0, bottom);
            drawerView.setPadding(
                    theme.dp(20), theme.dp(20) + top,
                    theme.dp(20), theme.dp(20)
            );
            return insets;
        });
        mainRoot.requestApplyInsets();
    }

    private void setupMainTabs() {
        mainTabGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean showCharts = checkedId == R.id.chartTabRadio;
            entrySection.setVisibility(showCharts ? View.GONE : View.VISIBLE);
            recentEntriesTitle.setVisibility(showCharts ? View.GONE : View.VISIBLE);
            viewAllText.setVisibility(showCharts ? View.GONE : View.VISIBLE);
            entriesContainer.setVisibility(showCharts ? View.GONE : View.VISIBLE);
            emptyText.setVisibility(showCharts ? View.GONE : emptyText.getVisibility());
            chartSection.setVisibility(showCharts ? View.VISIBLE : View.GONE);
            applyTheme();
            refreshDashboard();
        });
    }

    private void setupCategoryChips() {
        buildCategoryChips(EntryTypes.EXPENSE);
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isIncome = checkedId == R.id.incomeRadio;
            String type = isIncome ? EntryTypes.INCOME : EntryTypes.EXPENSE;
            selectedCategory = "";
            buildCategoryChips(type);
            addButton.setText(isIncome ? "Add Income" : "Add Expense");
            applyTheme();
        });
    }

    private void buildCategoryChips(String type) {
        List<String> all = databaseHelper.getCategories(type);
        List<String> mostUsed = databaseHelper.getMostUsedCategories(type, 5);

        List<String> top = new ArrayList<>(mostUsed);
        for (String cat : all) {
            if (top.size() >= 5) break;
            if (!top.contains(cat)) top.add(cat);
        }

        List<String> remaining = new ArrayList<>();
        for (String cat : all) {
            if (!top.contains(cat)) remaining.add(cat);
        }

        if (selectedCategory.isEmpty() || !all.contains(selectedCategory)) {
            selectedCategory = top.isEmpty() ? "" : top.get(0);
        }

        categoryChipContainer.removeAllViews();
        for (String cat : top) {
            categoryChipContainer.addView(makeChip(cat, cat.equals(selectedCategory)));
        }
        if (!remaining.isEmpty()) {
            categoryChipContainer.addView(makeMoreChip(remaining, type));
        }
    }

    private TextView makeChip(String label, boolean selected) {
        TextView chip = new TextView(this);
        chip.setTag(label);
        chip.setText(CategoryIcons.getEmoji(label) + " " + label);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setSingleLine(true);
        chip.setPadding(theme.dp(12), theme.dp(8), theme.dp(12), theme.dp(8));
        chip.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        applyChipStyle(chip, selected);
        chip.setOnClickListener(v -> {
            selectedCategory = label;
            refreshChipStyles();
        });
        return chip;
    }

    private TextView makeMoreChip(List<String> remaining, String type) {
        TextView chip = new TextView(this);
        chip.setText("+" + remaining.size() + " More");
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setSingleLine(true);
        chip.setPadding(theme.dp(12), theme.dp(8), theme.dp(12), theme.dp(8));
        chip.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        chip.setTextColor(theme.colorAccent());
        chip.setBackground(theme.makeToggleDrawable());
        chip.setOnClickListener(v -> {
            categoryChipContainer.removeView(chip);
            for (String cat : remaining) {
                categoryChipContainer.addView(makeChip(cat, cat.equals(selectedCategory)));
            }
            categoryChipContainer.addView(makeShowLessChip(type));
        });
        return chip;
    }

    private TextView makeShowLessChip(String type) {
        TextView chip = new TextView(this);
        chip.setText("Show less");
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setSingleLine(true);
        chip.setPadding(theme.dp(12), theme.dp(8), theme.dp(12), theme.dp(8));
        chip.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        chip.setTextColor(theme.colorMuted());
        chip.setBackground(theme.makeToggleDrawable());
        chip.setOnClickListener(v -> buildCategoryChips(type));
        return chip;
    }

    private void applyChipStyle(TextView chip, boolean selected) {
        chip.setTextColor(selected ? Color.WHITE : theme.colorInk());
        chip.setBackground(selected ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
    }

    private void refreshChipStyles() {
        for (int i = 0; i < categoryChipContainer.getChildCount(); i++) {
            View child = categoryChipContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                Object tag = chip.getTag();
                if (tag instanceof String) {
                    applyChipStyle(chip, tag.equals(selectedCategory));
                }
            }
        }
    }

    private void setupSaveButton() {
        addButton.setOnClickListener(view -> saveEntry());
    }

    private void setupViewAllLink() {
        viewAllText.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void setupDatePicker() {
        updateDateButtonLabel();
        dateButton.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedEntryDateMillis);
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (datePicker, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.setTimeInMillis(selectedEntryDateMillis);
                        selected.set(Calendar.YEAR, year);
                        selected.set(Calendar.MONTH, month);
                        selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        selectedEntryDateMillis = selected.getTimeInMillis();
                        updateDateButtonLabel();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void updateDateButtonLabel() {
        String label = new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(selectedEntryDateMillis));
        dateButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dateButton.setText(label);
    }

    private void setupDrawer() {
        menuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(drawerScroll)) {
                drawerLayout.closeDrawer(drawerScroll);
            } else {
                updateDrawerEntries();
                drawerLayout.openDrawer(drawerScroll);
            }
        });

        drawerOverviewItem.setOnClickListener(v -> {
            drawerLayout.closeDrawer(drawerScroll);
            mainTabGroup.check(R.id.chartTabRadio);
        });

        drawerHistoryItem.setOnClickListener(v -> {
            drawerLayout.closeDrawer(drawerScroll);
            startActivity(new Intent(this, HistoryActivity.class));
        });

        drawerManageCategoriesItem.setOnClickListener(v -> {
            drawerLayout.closeDrawer(drawerScroll);
            startActivity(new Intent(this, ManageCategoriesActivity.class));
        });

        drawerViewAllText.setOnClickListener(v -> {
            drawerLayout.closeDrawer(drawerScroll);
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    private void updateDrawerEntries() {
        drawerEntriesContainer.removeAllViews();
        List<ExpenseEntry> entries = databaseHelper.getRecentEntries(5);
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No entries yet.");
            empty.setTextColor(theme.colorMuted());
            empty.setTextSize(13);
            drawerEntriesContainer.addView(empty);
        } else {
            for (ExpenseEntry entry : entries) {
                drawerEntriesContainer.addView(createDrawerEntryRow(entry));
            }
        }
    }

    private View createDrawerEntryRow(ExpenseEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, theme.dp(7), 0, theme.dp(7));

        TextView info = new TextView(this);
        info.setText(CategoryIcons.getEmoji(entry.category) + "  " + entry.category + formatNote(entry.note));
        info.setTextColor(theme.colorInk());
        info.setTextSize(13);
        info.setSingleLine(true);
        info.setEllipsize(TextUtils.TruncateAt.END);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView amountText = new TextView(this);
        String prefix = entry.isIncome() ? "+" : "-";
        amountText.setText(prefix + moneyFormat.format(entry.amount));
        amountText.setTextColor(getColor(entry.isIncome() ? R.color.income : R.color.expense));
        amountText.setTextSize(12);
        amountText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        row.addView(info);
        row.addView(amountText);
        return row;
    }

    private void setupChartControls() {
        RadioGroup chartPeriodGroup = findViewById(R.id.chartPeriodGroup);
        chartPeriodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            applyTheme();
            refreshChart();
        });

        RadioGroup chartStyleGroup = findViewById(R.id.chartStyleGroup);
        chartStyleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            categoryChartView.setMode(checkedId == R.id.pieChartRadio
                    ? CategoryChartView.MODE_TREND
                    : CategoryChartView.MODE_DONUT);
            applyTheme();
            refreshChart();
        });
    }

    private void setupThemeToggle() {
        themeSwitch.setChecked(darkMode);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            darkMode = isChecked;
            theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);
            preferences.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
            applyTheme();
            refreshDashboard();
        });
    }

    // ADS:
    // private void setupBottomBannerAd() {
    //     MobileAds.initialize(this, initializationStatus -> runOnUiThread(this::loadBottomBannerAd));
    // }

    // private void loadBottomBannerAd() {
    //     adView = new AdView(this);
    //     adView.setAdUnitId(TEST_BANNER_AD_UNIT_ID);
    //     adView.setAdSize(AdSize.BANNER);
    //     adContainer.addView(adView);
    //     adView.loadAd(new AdRequest.Builder().build());
    // }

    private void saveEntry() {
        String amountText = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            amountInput.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException exception) {
            amountInput.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            amountInput.setError("Amount must be greater than zero");
            return;
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = expenseRadio.isChecked() ? EntryTypes.EXPENSE : EntryTypes.INCOME;
        String note = noteInput.getText().toString().trim();

        databaseHelper.addEntry(type, amount, selectedCategory, note, selectedEntryDateMillis);
        amountInput.setText("");
        noteInput.setText("");
        typeGroup.check(R.id.expenseRadio);
        hideKeyboard();
        refreshDashboard();
        Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show();
    }

    private void refreshDashboard() {
        double totalIncome = databaseHelper.getTotalForType(EntryTypes.INCOME);
        double totalExpense = databaseHelper.getTotalForType(EntryTypes.EXPENSE);
        double balance = totalIncome - totalExpense;

        long dayStart = getStartOfDay();
        long monthStart = getStartOfMonth();
        double todayExpense = databaseHelper.getTotalForTypeSince(EntryTypes.EXPENSE, dayStart);
        double monthExpense = databaseHelper.getTotalForTypeSince(EntryTypes.EXPENSE, monthStart);

        todaySpentText.setText(moneyFormat.format(todayExpense));
        monthSpentText.setText(moneyFormat.format(monthExpense));
        balanceStatText.setText(moneyFormat.format(balance));
        balanceStatText.setTextColor(balance >= 0 ? getColor(R.color.income) : getColor(R.color.expense));

        renderEntries(databaseHelper.getRecentEntries(30));
        refreshChart();
    }

    private void refreshChart() {
        long startMillis;
        long endMillis;
        String label;
        if (monthlyChartRadio.isChecked()) {
            startMillis = getStartOfMonth();
            endMillis = getStartOfNextMonth();
            label = "Monthly expense by category";
        } else {
            startMillis = getStartOfYear();
            endMillis = getStartOfNextYear();
            label = "Yearly expense by category";
        }

        List<CategoryTotal> totals = databaseHelper.getCategoryTotals(EntryTypes.EXPENSE, startMillis, endMillis);
        categoryChartView.setData(totals);

        if (monthlyChartRadio.isChecked()) {
            categoryChartView.setTrendData(databaseHelper.getDailyTotals(EntryTypes.EXPENSE, startMillis, endMillis));
        } else {
            categoryChartView.setTrendData(databaseHelper.getMonthlyTotals(EntryTypes.EXPENSE, startMillis, endMillis));
        }

        double total = 0.0;
        for (CategoryTotal categoryTotal : totals) total += categoryTotal.total;
        chartSummaryText.setText(label + ": " + moneyFormat.format(total));
    }

    private void renderEntries(List<ExpenseEntry> entries) {
        entriesContainer.removeAllViews();
        boolean showEmpty = entries.isEmpty() && entryTabRadio.isChecked();
        emptyText.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        for (ExpenseEntry entry : entries) {
            entriesContainer.addView(createEntryRow(entry));
        }
    }

    private View createEntryRow(ExpenseEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.makeCardDrawable());
        row.setPadding(theme.dp(12), theme.dp(10), theme.dp(8), theme.dp(10));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, theme.dp(8));
        row.setLayoutParams(rowParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(CategoryIcons.getEmoji(entry.category) + "  " + entry.category + formatNote(entry.note));
        title.setTextColor(theme.colorInk());
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView date = new TextView(this);
        date.setText(dateFormat.format(new Date(entry.createdAt)));
        date.setTextColor(theme.colorMuted());
        date.setTextSize(12);
        date.setPadding(0, theme.dp(3), 0, 0);

        details.addView(title);
        details.addView(date);

        TextView amount = new TextView(this);
        String prefix = entry.isIncome() ? "+" : "-";
        amount.setText(prefix + moneyFormat.format(entry.amount));
        amount.setTextColor(getColor(entry.isIncome() ? R.color.income : R.color.expense));
        amount.setTextSize(13);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        amount.setGravity(Gravity.END);
        amount.setPadding(theme.dp(6), 0, theme.dp(6), 0);

        Button editButton = new Button(this);
        editButton.setText("✏️");
        editButton.setTextSize(16);
        editButton.setAllCaps(false);
        editButton.setTextColor(theme.colorPrimary());
        editButton.setBackground(theme.makeToggleDrawable());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(theme.dp(36), theme.dp(34));
        editParams.setMargins(theme.dp(4), 0, theme.dp(4), 0);
        editButton.setLayoutParams(editParams);
        editButton.setPadding(0, 0, 0, 0);
        editButton.setMinWidth(0);
        editButton.setMinHeight(0);
        editButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, EditEntryActivity.class);
            intent.putExtra(EditEntryActivity.EXTRA_ENTRY_ID, entry.id);
            startActivity(intent);
        });

        Button deleteButton = new Button(this);
        deleteButton.setText("🗑");
        deleteButton.setTextSize(15);
        deleteButton.setAllCaps(false);
        deleteButton.setTextColor(theme.colorDanger());
        deleteButton.setBackground(theme.makeDangerButtonDrawable());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(theme.dp(36), theme.dp(34));
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setPadding(0, 0, 0, 0);
        deleteButton.setMinWidth(0);
        deleteButton.setMinHeight(0);
        deleteButton.setOnClickListener(view -> {
            databaseHelper.deleteEntry(entry.id);
            refreshDashboard();
        });

        row.addView(details);
        row.addView(amount);
        row.addView(editButton);
        row.addView(deleteButton);
        return row;
    }

    private void applyTheme() {
        int background = theme.colorBackground();
        int ink = theme.colorInk();
        int muted = theme.colorMuted();

        rootScroll.setBackgroundColor(background);
        rootContent.setBackgroundColor(background);
        headerPanel.setBackgroundColor(background);
        themeSwitch.setText(darkMode ? "Light" : "Dark");
        themeSwitch.setTextColor(ink);

        summaryCard.setBackground(theme.makeCardDrawable());
        todaySummaryCell.setBackground(theme.makeInputDrawable());
        monthSummaryCell.setBackground(theme.makeInputDrawable());
        balanceSummaryCell.setBackground(theme.makeInputDrawable());
        entrySection.setBackground(theme.makeCardDrawable());
        chartSection.setBackground(theme.makeCardDrawable());
        privacyInfoText.setBackground(theme.makeCardDrawable());
        privacyInfoText.setPadding(theme.dp(12), theme.dp(12), theme.dp(12), theme.dp(12));
        // ADS: adContainer.setBackgroundColor(theme.colorSurface());

        // Drawer theming
        drawerScroll.setBackgroundColor(background);
        drawerView.setBackgroundColor(background);
        drawerTitleText.setTextColor(ink);
        drawerOverviewItem.setTextColor(ink);
        drawerHistoryItem.setTextColor(ink);
        drawerManageCategoriesItem.setTextColor(ink);
        drawerViewAllText.setTextColor(theme.colorAccent());

        TextView[] inkViews = {
                titleText, todaySpentText, monthSpentText, recentEntriesTitle, chartTitleText
        };
        for (TextView v : inkViews) v.setTextColor(ink);

        TextView[] mutedViews = {
                subtitleText, chartSummaryText, emptyText, privacyInfoText,
                todayLabelText, monthLabelText, balanceLabelText
        };
        for (TextView v : mutedViews) v.setTextColor(muted);

        EditText[] inputs = {amountInput, noteInput};
        for (EditText input : inputs) {
            input.setTextColor(ink);
            input.setHintTextColor(muted);
            input.setBackground(theme.makeInputDrawable());
            input.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        }

        addButton.setTextColor(Color.WHITE);
        addButton.setBackground(theme.makePremiumButtonDrawable());

        // View all link styling
        viewAllText.setTextColor(theme.colorAccent());

        // Menu button
        menuButton.setTextColor(theme.colorInk());
        menuButton.setBackground(theme.makeToggleDrawable());

        // Date button — icon only, no active gradient
        dateButton.setTextColor(theme.colorInk());
        dateButton.setBackground(theme.makeToggleDrawable());
        updateDateButtonLabel();

        // Chip styles refresh
        refreshChipStyles();

        styleToggle(entryTabRadio);
        styleToggle(chartTabRadio);
        styleToggle(expenseRadio);
        styleToggle(incomeRadio);
        styleToggle(monthlyChartRadio);
        styleToggle(yearlyChartRadio);
        styleToggle(barChartRadio);
        styleToggle(pieChartRadio);
        categoryChartView.setThemeColors(theme.colorAccent(), ink, muted, theme.colorSurface());

        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
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

    private void styleToggle(TextView view) {
        boolean checked = view instanceof RadioButton && ((RadioButton) view).isChecked();
        view.setTextColor(checked ? Color.WHITE : theme.colorInk());
        view.setBackground(checked ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
    }

    private String formatNote(String note) {
        if (TextUtils.isEmpty(note)) return "";
        return " — " + note;
    }

    private long getStartOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getStartOfNextMonth() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(getStartOfMonth());
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    private long getStartOfYear() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getStartOfNextYear() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(getStartOfYear());
        c.add(Calendar.YEAR, 1);
        return c.getTimeInMillis();
    }

    private long getStartOfDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void hideKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (mgr != null && focused != null) {
            mgr.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    @Override
    protected void onPause() {
        // ADS: if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ADS: if (adView != null) adView.resume();
        String type = expenseRadio.isChecked() ? EntryTypes.EXPENSE : EntryTypes.INCOME;
        buildCategoryChips(type);
        applyTheme();
        refreshDashboard();
    }

    @Override
    protected void onDestroy() {
        // ADS: if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
