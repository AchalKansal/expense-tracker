package com.offline.expense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final int MAX_CATEGORIES = 15;

    private ExpenseDatabaseHelper db;
    private ThemeHelper theme;
    private String currentType = EntryTypes.EXPENSE;

    private LinearLayout rootContent;
    private LinearLayout categoriesContainer;
    private EditText newCategoryInput;
    private TextView countText;
    private Button expTypeBtn;
    private Button incTypeBtn;
    private boolean darkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        theme = new ThemeHelper(darkMode, getResources().getDisplayMetrics().density);
        db = new ExpenseDatabaseHelper(this);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.colorBackground());
        scroll.setFillViewport(true);

        rootContent = new LinearLayout(this);
        rootContent.setOrientation(LinearLayout.VERTICAL);
        rootContent.setBackgroundColor(theme.colorBackground());
        int p = theme.dp(18);
        rootContent.setPadding(p, p, p, p);

        // Title bar
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("Back");
        backBtn.setTextSize(15);
        backBtn.setTextColor(theme.colorPrimary());
        backBtn.setPaintFlags(backBtn.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        backBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        backBtn.setOnClickListener(v -> finish());

        TextView titleTv = new TextView(this);
        titleTv.setText("Manage Categories");
        titleTv.setTextColor(theme.colorInk());
        titleTv.setTextSize(21);
        titleTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMarginStart(theme.dp(12));
        titleTv.setLayoutParams(titleParams);

        titleRow.addView(backBtn);
        titleRow.addView(titleTv);
        rootContent.addView(titleRow);

        // Expense / Income toggle
        LinearLayout typeRow = new LinearLayout(this);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams typeRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(44));
        typeRowParams.topMargin = theme.dp(16);
        typeRow.setLayoutParams(typeRowParams);

        expTypeBtn = new Button(this);
        expTypeBtn.setText("Expense");
        expTypeBtn.setAllCaps(false);
        expTypeBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        expTypeBtn.setMinWidth(0);
        expTypeBtn.setMinHeight(0);
        expTypeBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams expBtnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        expBtnParams.setMarginEnd(theme.dp(8));
        expTypeBtn.setLayoutParams(expBtnParams);

        incTypeBtn = new Button(this);
        incTypeBtn.setText("Income");
        incTypeBtn.setAllCaps(false);
        incTypeBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        incTypeBtn.setMinWidth(0);
        incTypeBtn.setMinHeight(0);
        incTypeBtn.setPadding(0, 0, 0, 0);
        incTypeBtn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        expTypeBtn.setOnClickListener(v -> switchType(EntryTypes.EXPENSE));
        incTypeBtn.setOnClickListener(v -> switchType(EntryTypes.INCOME));

        typeRow.addView(expTypeBtn);
        typeRow.addView(incTypeBtn);
        rootContent.addView(typeRow);

        // Add category row
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams addRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(50));
        addRowParams.topMargin = theme.dp(16);
        addRow.setLayoutParams(addRowParams);

        newCategoryInput = new EditText(this);
        newCategoryInput.setHint("New category name");
        newCategoryInput.setTextColor(theme.colorInk());
        newCategoryInput.setHintTextColor(theme.colorMuted());
        newCategoryInput.setBackground(theme.makeInputDrawable());
        newCategoryInput.setPadding(theme.dp(14), 0, theme.dp(14), 0);
        newCategoryInput.setSingleLine(true);
        newCategoryInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        LinearLayout.LayoutParams inputP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        inputP.setMarginEnd(theme.dp(8));
        newCategoryInput.setLayoutParams(inputP);

        Button addBtn = new Button(this);
        addBtn.setText("Add");
        addBtn.setAllCaps(false);
        addBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setBackground(theme.makePremiumButtonDrawable());
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(theme.dp(80), LinearLayout.LayoutParams.MATCH_PARENT));
        addBtn.setMinWidth(0);
        addBtn.setMinHeight(0);
        addBtn.setPadding(0, 0, 0, 0);
        addBtn.setOnClickListener(v -> addCategory());

        addRow.addView(newCategoryInput);
        addRow.addView(addBtn);
        rootContent.addView(addRow);

        // Count indicator
        countText = new TextView(this);
        countText.setTextSize(12);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countParams.topMargin = theme.dp(6);
        countText.setLayoutParams(countParams);
        rootContent.addView(countText);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(theme.colorBorder());
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, theme.dp(1));
        divParams.topMargin = theme.dp(14);
        divParams.bottomMargin = theme.dp(8);
        divider.setLayoutParams(divParams);
        rootContent.addView(divider);

        // Category list
        categoriesContainer = new LinearLayout(this);
        categoriesContainer.setOrientation(LinearLayout.VERTICAL);
        rootContent.addView(categoriesContainer);

        scroll.addView(rootContent);
        setContentView(scroll);

        rootContent.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            rootContent.setPadding(theme.dp(18), theme.dp(18) + top, theme.dp(18), theme.dp(18));
            return insets;
        });
        rootContent.requestApplyInsets();

        getWindow().setStatusBarColor(theme.colorBackground());
        getWindow().setNavigationBarColor(theme.colorBackground());
        applyStatusBarAppearance();

        applyTypeButtonStyles();
        refreshList();
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

    private void switchType(String type) {
        currentType = type;
        applyTypeButtonStyles();
        refreshList();
    }

    private void applyTypeButtonStyles() {
        boolean expActive = currentType.equals(EntryTypes.EXPENSE);
        expTypeBtn.setBackground(expActive ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
        expTypeBtn.setTextColor(expActive ? Color.WHITE : theme.colorInk());
        incTypeBtn.setBackground(!expActive ? theme.makeActiveToggleDrawable() : theme.makeToggleDrawable());
        incTypeBtn.setTextColor(!expActive ? Color.WHITE : theme.colorInk());
    }

    private void refreshList() {
        categoriesContainer.removeAllViews();
        List<String> all = db.getCategories(currentType);
        int totalCount = all.size();
        countText.setText(totalCount + " / " + MAX_CATEGORIES + " categories");
        countText.setTextColor(totalCount >= MAX_CATEGORIES ? theme.colorDanger() : theme.colorMuted());

        List<String> userCategories = new ArrayList<>();
        for (String cat : all) {
            if (!ExpenseDatabaseHelper.isDefaultCategory(currentType, cat)) {
                userCategories.add(cat);
            }
        }

        if (userCategories.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No categories added by you. Nothing to modify.\nPlease add categories to modify.");
            emptyTv.setTextColor(theme.colorMuted());
            emptyTv.setTextSize(14);
            emptyTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.topMargin = theme.dp(32);
            emptyTv.setLayoutParams(p);
            categoriesContainer.addView(emptyTv);
        } else {
            for (String cat : userCategories) {
                categoriesContainer.addView(makeCategoryRow(cat));
            }
        }
    }

    private View makeCategoryRow(String categoryName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.makeCardDrawable());
        row.setPadding(theme.dp(12), theme.dp(10), theme.dp(8), theme.dp(10));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = theme.dp(8);
        row.setLayoutParams(rowParams);

        TextView nameTv = new TextView(this);
        nameTv.setText(CategoryIcons.getEmoji(categoryName) + "  " + categoryName);
        nameTv.setTextColor(theme.colorInk());
        nameTv.setTextSize(15);
        nameTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button editBtn = new Button(this);
        editBtn.setText("✏️");
        editBtn.setTextSize(15);
        editBtn.setAllCaps(false);
        editBtn.setTextColor(theme.colorPrimary());
        editBtn.setBackground(theme.makeToggleDrawable());
        editBtn.setMinWidth(0);
        editBtn.setMinHeight(0);
        editBtn.setPadding(0, 0, 0, 0);
        editBtn.setLayoutParams(new LinearLayout.LayoutParams(theme.dp(40), theme.dp(36)));

        if (ExpenseDatabaseHelper.isDefaultCategory(currentType, categoryName)) {
            editBtn.setVisibility(View.GONE);
        } else {
            editBtn.setOnClickListener(v -> showRenameInline(row, nameTv, editBtn, categoryName));
        }

        row.addView(nameTv);
        row.addView(editBtn);
        return row;
    }

    private void showRenameInline(LinearLayout row, TextView nameTv, Button editBtn, String originalName) {
        nameTv.setVisibility(View.GONE);
        editBtn.setVisibility(View.GONE);

        EditText renameInput = new EditText(this);
        renameInput.setText(originalName);
        renameInput.setTextColor(theme.colorInk());
        renameInput.setHintTextColor(theme.colorMuted());
        renameInput.setBackground(theme.makeInputDrawable());
        renameInput.setPadding(theme.dp(10), theme.dp(4), theme.dp(10), theme.dp(4));
        renameInput.setSingleLine(true);
        renameInput.selectAll();
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, theme.dp(40), 1f);
        inputParams.setMarginEnd(theme.dp(6));
        renameInput.setLayoutParams(inputParams);

        Button confirmBtn = new Button(this);
        confirmBtn.setText("✓");
        confirmBtn.setTextSize(16);
        confirmBtn.setAllCaps(false);
        confirmBtn.setTextColor(Color.WHITE);
        confirmBtn.setBackground(theme.makePremiumButtonDrawable());
        confirmBtn.setMinWidth(0);
        confirmBtn.setMinHeight(0);
        confirmBtn.setPadding(0, 0, 0, 0);
        confirmBtn.setLayoutParams(new LinearLayout.LayoutParams(theme.dp(40), theme.dp(40)));

        Button cancelBtn = new Button(this);
        cancelBtn.setText("✕");
        cancelBtn.setTextSize(14);
        cancelBtn.setAllCaps(false);
        cancelBtn.setTextColor(theme.colorMuted());
        cancelBtn.setBackground(theme.makeToggleDrawable());
        cancelBtn.setMinWidth(0);
        cancelBtn.setMinHeight(0);
        cancelBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(theme.dp(40), theme.dp(40));
        cancelParams.setMarginStart(theme.dp(4));
        cancelBtn.setLayoutParams(cancelParams);

        row.addView(renameInput);
        row.addView(confirmBtn);
        row.addView(cancelBtn);

        renameInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(renameInput, InputMethodManager.SHOW_IMPLICIT);

        confirmBtn.setOnClickListener(v -> {
            String newName = renameInput.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                renameInput.setError("Enter a name");
                return;
            }
            if (newName.equals(originalName)) {
                collapseRow(row, nameTv, editBtn, renameInput, confirmBtn, cancelBtn);
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Rename Category")
                    .setMessage("Renaming \"" + originalName + "\" to \"" + newName + "\" will update all existing expenses recorded under this category. This cannot be undone. Continue?")
                    .setPositiveButton("Rename", (dialog, which) -> {
                        db.renameCategory(currentType, originalName, newName);
                        hideKeyboard();
                        refreshList();
                        Toast.makeText(this, "Category renamed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        cancelBtn.setOnClickListener(v -> {
            collapseRow(row, nameTv, editBtn, renameInput, confirmBtn, cancelBtn);
            hideKeyboard();
        });
    }

    private void collapseRow(LinearLayout row, TextView nameTv, Button editBtn,
                              EditText renameInput, Button confirmBtn, Button cancelBtn) {
        row.removeView(renameInput);
        row.removeView(confirmBtn);
        row.removeView(cancelBtn);
        nameTv.setVisibility(View.VISIBLE);
        editBtn.setVisibility(View.VISIBLE);
    }

    private void addCategory() {
        String name = newCategoryInput.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            newCategoryInput.setError("Enter a category name");
            return;
        }
        List<String> existing = db.getCategories(currentType);
        if (existing.size() >= MAX_CATEGORIES) {
            Toast.makeText(this, "You cannot have more than " + MAX_CATEGORIES + " categories", Toast.LENGTH_LONG).show();
            return;
        }
        if (existing.contains(name)) {
            newCategoryInput.setError("Category already exists");
            return;
        }
        db.addCategory(currentType, name);
        newCategoryInput.setText("");
        hideKeyboard();
        refreshList();
        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (mgr != null && focused != null) {
            mgr.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }
}
