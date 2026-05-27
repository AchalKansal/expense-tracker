package com.offline.coders;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final int REQUEST_EXPORT_CSV = 1001;
    private static final int REQUEST_BACKUP = 1002;
    private static final int REQUEST_RESTORE = 1003;
    private static final String[] PERIODS = {
            "All time", "This month", "Last month", "This year", "Custom range"
    };
    private static final int PERIOD_CUSTOM = 4;

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private NumberFormat moneyFormat;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat buttonDateFormat;
    private boolean darkMode;
    private long customStartMillis;
    private long customEndMillis;

    private LinearLayout historyRoot;
    private LinearLayout historyContainer;
    private TextView historyTitle;
    private TextView historySubtitle;
    private TextView historyEmptyText;
    private EditText searchInput;
    private Spinner periodSpinner;
    private Spinner categoryFilterSpinner;
    private LinearLayout customDateRow;
    private Button customStartDateButton;
    private Button customEndDateButton;
    private Button backButton;
    private Button exportButton;
    private Button backupButton;
    private Button restoreButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Expense history");
        setContentView(R.layout.activity_history);

        databaseHelper = new ExpenseDatabaseHelper(this);
        moneyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateFormat = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault());
        buttonDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);

        // Default custom range = this month
        customStartMillis = getStartOfMonth();
        customEndMillis = getStartOfNextMonth();

        bindViews();
        applyWindowInsets();
        setupFilters();
        setupActions();
        applyTheme();
        renderHistory();
    }

    private void bindViews() {
        historyRoot = findViewById(R.id.historyRoot);
        historyContainer = findViewById(R.id.historyContainer);
        historyTitle = findViewById(R.id.historyTitle);
        historySubtitle = findViewById(R.id.historySubtitle);
        historyEmptyText = findViewById(R.id.historyEmptyText);
        searchInput = findViewById(R.id.searchInput);
        periodSpinner = findViewById(R.id.periodSpinner);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        customDateRow = findViewById(R.id.customDateRow);
        customStartDateButton = findViewById(R.id.customStartDateButton);
        customEndDateButton = findViewById(R.id.customEndDateButton);
        backButton = findViewById(R.id.backButton);
        exportButton = findViewById(R.id.exportButton);
        backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        historyRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            historyRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        historyRoot.requestApplyInsets();
    }

    private void setupFilters() {
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PERIODS);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);

        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        categories.addAll(databaseHelper.getCategories(EntryTypes.EXPENSE));
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);

        updateCustomDateButtons();
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
        exportButton.setOnClickListener(view -> createDocument(REQUEST_EXPORT_CSV, "expense-history.csv"));
        backupButton.setOnClickListener(view -> createDocument(REQUEST_BACKUP, "expense-tracker-backup.csv"));
        restoreButton.setOnClickListener(view -> openBackupDocument());

        periodSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> {
            boolean isCustom = periodSpinner.getSelectedItemPosition() == PERIOD_CUSTOM;
            customDateRow.setVisibility(isCustom ? View.VISIBLE : View.GONE);
            applyTheme();
            renderHistory();
        }));

        categoryFilterSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(this::renderHistory));

        customStartDateButton.setOnClickListener(view -> showStartDatePicker());
        customEndDateButton.setOnClickListener(view -> showEndDatePicker());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderHistory(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(customStartMillis);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            customStartMillis = selected.getTimeInMillis();
            updateCustomDateButtons();
            renderHistory();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(customEndMillis);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 23, 59, 59);
            selected.set(Calendar.MILLISECOND, 999);
            customEndMillis = selected.getTimeInMillis();
            updateCustomDateButtons();
            renderHistory();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateCustomDateButtons() {
        customStartDateButton.setText("From: " + buttonDateFormat.format(new Date(customStartMillis)));
        customEndDateButton.setText("To: " + buttonDateFormat.format(new Date(customEndMillis)));
    }

    private void renderHistory() {
        List<ExpenseEntry> expenses = getFilteredExpenses();
        historyContainer.removeAllViews();
        historyEmptyText.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);

        String currentMonth = "";
        LinearLayout monthGroup = null;
        double monthTotal = 0.0;
        TextView monthTitle = null;

        for (ExpenseEntry entry : expenses) {
            String month = monthFormat.format(new Date(entry.createdAt));
            if (!month.equals(currentMonth)) {
                currentMonth = month;
                monthTotal = 0.0;
                monthGroup = createMonthGroup();
                monthTitle = createMonthTitle(month, monthTotal);
                monthGroup.addView(monthTitle);
                historyContainer.addView(monthGroup);
            }

            monthTotal += entry.amount;
            if (monthTitle != null) {
                monthTitle.setText(currentMonth + "  ·  " + moneyFormat.format(monthTotal));
            }
            if (monthGroup != null) {
                monthGroup.addView(createEntryRow(entry));
            }
        }
    }

    private LinearLayout createMonthGroup() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(theme.makeCardDrawable());
        group.setPadding(theme.dp(12), theme.dp(12), theme.dp(12), theme.dp(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, theme.dp(12));
        group.setLayoutParams(params);
        return group;
    }

    private TextView createMonthTitle(String month, double total) {
        TextView title = new TextView(this);
        title.setText(month + "  ·  " + moneyFormat.format(total));
        title.setTextColor(theme.colorInk());
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, 0, 0, theme.dp(10));
        return title;
    }

    private View createEntryRow(ExpenseEntry entry) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        View divider = new View(this);
        divider.setBackgroundColor(theme.colorBorder());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, theme.dp(10), 0, theme.dp(10));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(CategoryIcons.getEmoji(entry.category) + "  " + entry.category + formatNote(entry.note));
        title.setTextColor(theme.colorInk());
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView date = new TextView(this);
        date.setText(dateFormat.format(new Date(entry.createdAt)));
        date.setTextColor(theme.colorMuted());
        date.setTextSize(12);
        date.setPadding(0, theme.dp(4), 0, 0);

        boolean isIncome = entry.isIncome();
        TextView amount = new TextView(this);
        amount.setText((isIncome ? "+" : "-") + moneyFormat.format(entry.amount));
        amount.setTextColor(getColor(isIncome ? R.color.income : R.color.expense));
        amount.setTextSize(15);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        amountParams.setMarginStart(theme.dp(8));
        amount.setLayoutParams(amountParams);

        details.addView(title);
        details.addView(date);
        row.addView(details);
        row.addView(amount);
        row.setOnClickListener(view -> {
            Intent intent = new Intent(this, EditEntryActivity.class);
            intent.putExtra(EditEntryActivity.EXTRA_ENTRY_ID, entry.id);
            startActivity(intent);
        });

        wrapper.addView(divider);
        wrapper.addView(row);
        return wrapper;
    }

    private List<ExpenseEntry> getFilteredExpenses() {
        List<ExpenseEntry> filtered = new ArrayList<>();
        List<ExpenseEntry> allExpenses = databaseHelper.getAllExpenses();
        String search = searchInput.getText().toString().trim().toLowerCase(Locale.US);
        String category = categoryFilterSpinner.getSelectedItem() == null
                ? "All categories"
                : categoryFilterSpinner.getSelectedItem().toString();
        long[] period = getSelectedPeriodRange();

        for (ExpenseEntry entry : allExpenses) {
            if (period[0] > 0 && (entry.createdAt < period[0] || entry.createdAt > period[1])) {
                continue;
            }
            if (!"All categories".equals(category) && !category.equals(entry.category)) {
                continue;
            }
            String haystack = (entry.category + " " + (entry.note == null ? "" : entry.note)).toLowerCase(Locale.US);
            if (!TextUtils.isEmpty(search) && !haystack.contains(search)) {
                continue;
            }
            filtered.add(entry);
        }
        return filtered;
    }

    private long[] getSelectedPeriodRange() {
        int selected = periodSpinner.getSelectedItemPosition();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        clearTime(start);
        clearTime(end);

        switch (selected) {
            case 1: // This month
                start.set(Calendar.DAY_OF_MONTH, 1);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.MONTH, 1);
                return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
            case 2: // Last month
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.add(Calendar.MONTH, -1);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.MONTH, 1);
                return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
            case 3: // This year
                start.set(Calendar.MONTH, Calendar.JANUARY);
                start.set(Calendar.DAY_OF_MONTH, 1);
                end.setTimeInMillis(start.getTimeInMillis());
                end.add(Calendar.YEAR, 1);
                return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
            case PERIOD_CUSTOM: // Custom range
                return new long[]{customStartMillis, customEndMillis};
            default: // All time
                return new long[]{0L, Long.MAX_VALUE};
        }
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private long getStartOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getStartOfNextMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getStartOfMonth());
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTimeInMillis();
    }

    private void createDocument(int requestCode, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, requestCode);
    }

    private void openBackupDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, REQUEST_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_CSV) {
            writeEntries(uri, getFilteredExpenses());
        } else if (requestCode == REQUEST_BACKUP) {
            writeEntries(uri, databaseHelper.getAllEntries());
        } else if (requestCode == REQUEST_RESTORE) {
            restoreEntries(uri);
        }
    }

    private void writeEntries(Uri uri, List<ExpenseEntry> entries) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(uri)))) {
            writer.write("type,amount,category,note,created_at\n");
            for (ExpenseEntry entry : entries) {
                writer.write(csv(entry.type) + "," + entry.amount + "," +
                        csv(entry.category) + "," + csv(entry.note) + "," + entry.createdAt + "\n");
            }
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Could not save file", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreEntries(Uri uri) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            List<String[]> validRows = new ArrayList<>();
            int skipped = 0;
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] columns = parseCsvLine(line);
                if (columns.length >= 5) {
                    validRows.add(columns);
                } else {
                    skipped++;
                }
            }

            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (String[] columns : validRows) {
                    databaseHelper.addEntry(
                            columns[0],
                            Double.parseDouble(columns[1]),
                            columns[2],
                            columns[3],
                            Long.parseLong(columns[4])
                    );
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            renderHistory();
            String msg = validRows.size() + " entries restored";
            if (skipped > 0) msg += ", " + skipped + " skipped";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, "Could not restore backup", Toast.LENGTH_SHORT).show();
        }
    }

    private String csv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    private void applyTheme() {
        historyRoot.setBackgroundColor(theme.colorBackground());
        historyTitle.setTextColor(theme.colorInk());
        historySubtitle.setTextColor(theme.colorMuted());
        historyEmptyText.setTextColor(theme.colorMuted());
        searchInput.setTextColor(theme.colorInk());
        searchInput.setHintTextColor(theme.colorMuted());
        searchInput.setBackground(theme.makeInputDrawable());
        searchInput.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        periodSpinner.setBackground(theme.makeInputDrawable());
        categoryFilterSpinner.setBackground(theme.makeInputDrawable());
        customStartDateButton.setTextColor(theme.colorInk());
        customStartDateButton.setBackground(theme.makeToggleDrawable());
        customEndDateButton.setTextColor(theme.colorInk());
        customEndDateButton.setBackground(theme.makeToggleDrawable());
        backButton.setTextColor(theme.colorInk());
        Button[] primaryButtons = {exportButton, backupButton, restoreButton};
        for (Button button : primaryButtons) {
            button.setTextColor(Color.WHITE);
            button.setBackground(theme.makePremiumButtonDrawable());
        }
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

    private String formatNote(String note) {
        if (TextUtils.isEmpty(note)) return "";
        return " — " + note;
    }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;

        SimpleItemSelectedListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            callback.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }
}
