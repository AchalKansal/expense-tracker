package com.offline.expense;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SplitEntryActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ACCENT_THEME = "accent_theme";

    private ExpenseDatabaseHelper databaseHelper;
    private ThemeHelper theme;
    private long selectedDateMillis;
    private boolean darkMode;
    private SimpleDateFormat dateFormat;
    private boolean updatingShares;

    private LinearLayout splitRoot;
    private LinearLayout splitCard;
    private TextView splitTitle;
    private Button backButton;
    private EditText totalAmountInput;
    private AutoCompleteTextView friendNameInput;
    private TextView paidByLabel;
    private RadioGroup paidByGroup;
    private RadioButton paidByMeRadio;
    private RadioButton paidByFriendRadio;
    private TextView splitModeLabel;
    private RadioGroup splitModeGroup;
    private RadioButton equalSplitRadio;
    private RadioButton customSplitRadio;
    private EditText myShareInput;
    private EditText friendShareInput;
    private Spinner categorySpinner;
    private EditText noteInput;
    private Button dateButton;
    private Button saveButton;
    private FrameLayout adContainer;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Split expense");
        setContentView(R.layout.activity_split_entry);

        databaseHelper = new ExpenseDatabaseHelper(this);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        AccentTheme accentTheme = AccentTheme.fromPrefsValue(preferences.getString(KEY_ACCENT_THEME, null));
        theme = new ThemeHelper(darkMode, accentTheme, getResources().getDisplayMetrics().density);
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        selectedDateMillis = System.currentTimeMillis();

        bindViews();
        applyWindowInsets();
        applyTheme();
        setupFriendNameSuggestions();
        setupCategorySpinner();
        setupSplitModeToggle();
        setupShareAutoFill();
        setupDatePicker();
        setupActions();
        adView = BannerAds.attach(this, adContainer);
    }

    private void bindViews() {
        splitRoot = findViewById(R.id.splitRoot);
        splitCard = findViewById(R.id.splitCard);
        splitTitle = findViewById(R.id.splitTitle);
        backButton = findViewById(R.id.backButton);
        totalAmountInput = findViewById(R.id.totalAmountInput);
        friendNameInput = findViewById(R.id.friendNameInput);
        paidByLabel = findViewById(R.id.paidByLabel);
        paidByGroup = findViewById(R.id.paidByGroup);
        paidByMeRadio = findViewById(R.id.paidByMeRadio);
        paidByFriendRadio = findViewById(R.id.paidByFriendRadio);
        splitModeLabel = findViewById(R.id.splitModeLabel);
        splitModeGroup = findViewById(R.id.splitModeGroup);
        equalSplitRadio = findViewById(R.id.equalSplitRadio);
        customSplitRadio = findViewById(R.id.customSplitRadio);
        myShareInput = findViewById(R.id.myShareInput);
        friendShareInput = findViewById(R.id.friendShareInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        noteInput = findViewById(R.id.noteInput);
        dateButton = findViewById(R.id.dateButton);
        saveButton = findViewById(R.id.saveButton);
        adContainer = findViewById(R.id.adContainer);
    }

    private void applyWindowInsets() {
        int p = Math.round(18 * getResources().getDisplayMetrics().density);
        splitRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            splitRoot.setPadding(p, p + top, p, p + bottom);
            return insets;
        });
        splitRoot.requestApplyInsets();
    }

    private void setupFriendNameSuggestions() {
        List<String> names = databaseHelper.getDistinctFriendNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        friendNameInput.setAdapter(adapter);
    }

    private void setupCategorySpinner() {
        categorySpinner.setAdapter(makeThemedAdapter(databaseHelper.getCategories(EntryTypes.EXPENSE)));
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

    private void setupSplitModeToggle() {
        splitModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean equal = checkedId == R.id.equalSplitRadio;
            myShareInput.setEnabled(!equal);
            friendShareInput.setEnabled(!equal);
            if (equal) fillEqualShares();
            applyTheme();
        });
        myShareInput.setEnabled(false);
        friendShareInput.setEnabled(false);
    }

    private void setupShareAutoFill() {
        totalAmountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (equalSplitRadio.isChecked()) fillEqualShares();
            }
        });
    }

    private void fillEqualShares() {
        if (updatingShares) return;
        updatingShares = true;
        double total = parseOrZero(totalAmountInput.getText().toString());
        String half = EditEntryActivity.formatAmountForEditing(total / 2.0);
        myShareInput.setText(half);
        friendShareInput.setText(half);
        updatingShares = false;
    }

    private double parseOrZero(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void setupDatePicker() {
        updateDateButton();
        dateButton.setOnClickListener(view -> {
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
        });
    }

    private void updateDateButton() {
        dateButton.setText("Date: " + dateFormat.format(new Date(selectedDateMillis)));
    }

    private void setupActions() {
        backButton.setOnClickListener(view -> finish());
        saveButton.setOnClickListener(view -> save());
    }

    private void save() {
        double total;
        try {
            total = Double.parseDouble(totalAmountInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            totalAmountInput.setError("Invalid amount");
            return;
        }
        if (total <= 0) {
            totalAmountInput.setError("Amount must be greater than zero");
            return;
        }

        String friendName = friendNameInput.getText().toString().trim();
        if (TextUtils.isEmpty(friendName)) {
            friendNameInput.setError("Enter a name");
            return;
        }

        double myShare;
        double friendShare;
        try {
            myShare = Double.parseDouble(myShareInput.getText().toString().trim());
            friendShare = Double.parseDouble(friendShareInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter both shares", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myShare < 0 || friendShare < 0) {
            Toast.makeText(this, "Shares can't be negative", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Math.abs((myShare + friendShare) - total) > 0.01) {
            Toast.makeText(this, "Shares must add up to the total amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categorySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        String category = categorySpinner.getSelectedItem().toString();
        String note = noteInput.getText().toString().trim();
        String paidBy = paidByMeRadio.isChecked() ? ExpenseDatabaseHelper.PAID_BY_ME : ExpenseDatabaseHelper.PAID_BY_FRIEND;

        databaseHelper.addSharedExpense(friendName, total, myShare, friendShare, paidBy, category, note,
                selectedDateMillis, ExpenseDatabaseHelper.SHARED_ORIGIN_LOCAL);
        Toast.makeText(this, "Split expense saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applyTheme() {
        splitRoot.setBackgroundColor(theme.colorBackground());
        splitCard.setBackground(theme.makeCardDrawable());
        splitTitle.setTextColor(theme.colorInk());
        backButton.setTextColor(theme.colorInk());
        paidByLabel.setTextColor(theme.colorInk());
        splitModeLabel.setTextColor(theme.colorInk());

        EditText[] inputs = {totalAmountInput, myShareInput, friendShareInput, noteInput};
        for (EditText input : inputs) {
            input.setTextColor(theme.colorInk());
            input.setHintTextColor(theme.colorMuted());
            input.setBackground(theme.makeInputDrawable());
            input.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        }
        friendNameInput.setTextColor(theme.colorInk());
        friendNameInput.setHintTextColor(theme.colorMuted());
        friendNameInput.setBackground(theme.makeInputDrawable());
        friendNameInput.setPadding(theme.dp(14), 0, theme.dp(14), 0);

        categorySpinner.setBackground(theme.makeInputDrawable());
        styleToggle(dateButton);
        styleToggle(paidByMeRadio);
        styleToggle(paidByFriendRadio);
        styleToggle(equalSplitRadio);
        styleToggle(customSplitRadio);

        saveButton.setBackground(theme.makePremiumButtonDrawable());
        saveButton.setTextColor(theme.colorOnAccentFill());
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

    private void styleToggle(TextView view) {
        boolean checked = view instanceof RadioButton && ((RadioButton) view).isChecked();
        view.setTextColor(checked ? theme.colorOnAccentFill() : theme.colorInk());
        view.setBackground(checked ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
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
