package com.offline.coders;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditEntryActivity extends Activity {
    static final String EXTRA_ENTRY_ID = "entry_id";
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private long entryId;
    private long selectedDateMillis;
    private boolean darkMode;
    private SimpleDateFormat dateFormat;

    private LinearLayout editRoot;
    private LinearLayout editCard;
    private TextView editTitle;
    private Button backButton;
    private Button dateButton;
    private Button saveButton;
    private RadioGroup typeGroup;
    private RadioButton expenseRadio;
    private RadioButton incomeRadio;
    private EditText amountInput;
    private EditText noteInput;
    private Spinner categorySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Edit entry");
        setContentView(R.layout.activity_edit_entry);

        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, -1L);
        databaseHelper = new ExpenseDatabaseHelper(this);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        bindViews();
        applyWindowInsets();
        applyTheme();
        loadEntry();
        setupActions();
    }

    private void bindViews() {
        editRoot = findViewById(R.id.editRoot);
        editCard = findViewById(R.id.editCard);
        editTitle = findViewById(R.id.editTitle);
        backButton = findViewById(R.id.backButton);
        dateButton = findViewById(R.id.dateButton);
        saveButton = findViewById(R.id.saveButton);
        typeGroup = findViewById(R.id.typeGroup);
        expenseRadio = findViewById(R.id.expenseRadio);
        incomeRadio = findViewById(R.id.incomeRadio);
        amountInput = findViewById(R.id.amountInput);
        noteInput = findViewById(R.id.noteInput);
        categorySpinner = findViewById(R.id.categorySpinner);
    }

    private void loadEntry() {
        ExpenseEntry entry = databaseHelper.getEntry(entryId);
        if (entry == null) {
            Toast.makeText(this, "Entry not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedDateMillis = entry.createdAt;
        typeGroup.check(entry.isIncome() ? R.id.incomeRadio : R.id.expenseRadio);
        updateCategories(entry.isIncome() ? EntryTypes.INCOME : EntryTypes.EXPENSE);
        selectCategory(entry.category);
        amountInput.setText(String.valueOf(entry.amount));
        noteInput.setText(entry.note == null ? "" : entry.note);
        updateDateButton();
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        editRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            editRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        editRoot.requestApplyInsets();
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateCategories(checkedId == R.id.incomeRadio ? EntryTypes.INCOME : EntryTypes.EXPENSE);
            applyTheme();
        });
        dateButton.setOnClickListener(view -> showDatePicker());
        saveButton.setOnClickListener(view -> saveChanges());
    }

    private void updateCategories(String type) {
        categorySpinner.setAdapter(makeThemedAdapter(databaseHelper.getCategories(type)));
    }

    private ArrayAdapter<String> makeThemedAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(theme.colorInk());
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(theme.colorInk());
                view.setBackgroundColor(theme.colorSurface());
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(selectedDateMillis);
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDateMillis = selected.getTimeInMillis();
                    updateDateButton();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void saveChanges() {
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

        String type = expenseRadio.isChecked() ? EntryTypes.EXPENSE : EntryTypes.INCOME;
        String category = categorySpinner.getSelectedItem().toString();
        String note = noteInput.getText().toString().trim();
        databaseHelper.updateEntry(entryId, type, amount, category, note, selectedDateMillis);
        Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateDateButton() {
        dateButton.setText("Date: " + dateFormat.format(new Date(selectedDateMillis)));
    }

    private void selectCategory(String categoryName) {
        for (int i = 0; i < categorySpinner.getCount(); i++) {
            if (categoryName.equalsIgnoreCase(categorySpinner.getItemAtPosition(i).toString())) {
                categorySpinner.setSelection(i);
                return;
            }
        }
    }

    private void applyTheme() {
        editRoot.setBackgroundColor(theme.colorBackground());
        editCard.setBackground(theme.makeCardDrawable());
        editTitle.setTextColor(theme.colorInk());
        backButton.setTextColor(theme.colorInk());
        styleToggle(dateButton);
        styleToggle(expenseRadio);
        styleToggle(incomeRadio);
        amountInput.setTextColor(theme.colorInk());
        amountInput.setHintTextColor(theme.colorMuted());
        amountInput.setBackground(theme.makeInputDrawable());
        amountInput.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        noteInput.setTextColor(theme.colorInk());
        noteInput.setHintTextColor(theme.colorMuted());
        noteInput.setBackground(theme.makeInputDrawable());
        noteInput.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        categorySpinner.setBackground(theme.makeInputDrawable());
        saveButton.setBackground(theme.makePremiumButtonDrawable());
        saveButton.setTextColor(Color.WHITE);
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

    private void styleToggle(TextView view) {
        boolean checked = view instanceof RadioButton && ((RadioButton) view).isChecked();
        view.setTextColor(checked ? Color.WHITE : theme.colorInk());
        view.setBackground(checked ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
    }
}
