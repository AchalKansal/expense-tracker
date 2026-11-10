package com.offline.expense;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Monthly, per-expense-category budgets. Amounts recur each calendar month. */
public class BudgetActivity extends Activity {
    private static final String PREFS_NAME = "expense_tracker_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ACCENT_THEME = "accent_theme";

    private ExpenseDatabaseHelper db;
    private ThemeHelper theme;
    private NumberFormat money;
    private LinearLayout budgetList;
    private TextView emptyText;
    private TextView monthText;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        theme = new ThemeHelper(prefs.getBoolean(KEY_DARK_MODE, false),
                AccentTheme.fromPrefsValue(prefs.getString(KEY_ACCENT_THEME, null)),
                getResources().getDisplayMetrics().density);
        db = new ExpenseDatabaseHelper(this);
        money = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (budgetList != null) refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(theme.makeBackgroundDrawable());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = theme.dp(18);
        content.setPadding(p, p, p, p);
        scroll.addView(content);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("←", theme.dp(48), theme.dp(44));
        back.setTextSize(26);
        back.setTextColor(theme.colorInk());
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> finish());
        TextView title = text("Budget Planner", 24, theme.colorInk(), true);
        title.setPadding(theme.dp(12), 0, 0, 0);
        titleRow.addView(back);
        titleRow.addView(title);
        content.addView(titleRow);

        monthText = text("", 14, theme.colorMuted(), false);
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(-1, -2);
        monthParams.topMargin = theme.dp(8);
        monthText.setLayoutParams(monthParams);
        content.addView(monthText);

        TextView help = text("Set a monthly limit for any expense category. Your budget resets automatically at the start of each month.", 14, theme.colorMuted(), false);
        help.setBackground(theme.makeCardDrawable());
        help.setPadding(theme.dp(14), theme.dp(14), theme.dp(14), theme.dp(14));
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = theme.dp(16);
        help.setLayoutParams(helpParams);
        content.addView(help);

        Button add = button("Add category budget", -1, theme.dp(48));
        add.setTextColor(theme.colorOnAccentFill());
        add.setBackground(theme.makePremiumButtonDrawable());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, theme.dp(48));
        addParams.topMargin = theme.dp(16);
        add.setLayoutParams(addParams);
        add.setOnClickListener(v -> showBudgetDialog(null));
        content.addView(add);

        budgetList = new LinearLayout(this);
        budgetList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, -2);
        listParams.topMargin = theme.dp(16);
        budgetList.setLayoutParams(listParams);
        content.addView(budgetList);

        emptyText = text("No category budgets yet. Add one to start tracking this month’s spending.", 14, theme.colorMuted(), false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(theme.dp(16), theme.dp(36), theme.dp(16), theme.dp(16));
        content.addView(emptyText);
        root.addView(scroll);
        setContentView(root);
        refresh();
    }

    private void refresh() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
        monthText.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()) + " budgets");
        List<CategoryBudget> budgets = db.getCategoryBudgets(start.getTimeInMillis(), end.getTimeInMillis());
        budgetList.removeAllViews();
        emptyText.setVisibility(budgets.isEmpty() ? View.VISIBLE : View.GONE);
        for (CategoryBudget budget : budgets) budgetList.addView(makeBudgetRow(budget));
    }

    private View makeBudgetRow(CategoryBudget budget) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(theme.makeCardDrawable());
        card.setPadding(theme.dp(14), theme.dp(12), theme.dp(14), theme.dp(12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = theme.dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(CategoryIcons.getEmoji(budget.category) + "  " + budget.category, 16, theme.colorInk(), true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        Button edit = button("Edit", theme.dp(58), theme.dp(36));
        edit.setTextColor(theme.colorPrimary()); edit.setBackground(theme.makeToggleDrawable());
        edit.setOnClickListener(v -> showBudgetDialog(budget));
        top.addView(name); top.addView(edit); card.addView(top);

        double ratio = budget.amount == 0 ? 0 : budget.spent / budget.amount;
        int status = ratio >= 1 ? theme.colorDanger() : ratio >= .8 ? Color.rgb(205, 135, 20) : theme.colorPrimary();
        TextView amounts = text(money.format(budget.spent) + " spent of " + money.format(budget.amount), 14, status, false);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, -2); amountParams.topMargin = theme.dp(9);
        amounts.setLayoutParams(amountParams); card.addView(amounts);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress((int) Math.min(100, Math.round(ratio * 100)));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(status));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, theme.dp(8)); barParams.topMargin = theme.dp(8);
        bar.setLayoutParams(barParams); card.addView(bar);
        String remaining = ratio > 1 ? money.format(budget.spent - budget.amount) + " over budget" : money.format(budget.amount - budget.spent) + " remaining";
        TextView detail = text(Math.round(ratio * 100) + "% used  ·  " + remaining, 12, theme.colorMuted(), false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2); detailParams.topMargin = theme.dp(7);
        detail.setLayoutParams(detailParams); card.addView(detail);
        return card;
    }

    private void showBudgetDialog(CategoryBudget existing) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        int p = theme.dp(20); form.setPadding(p, 0, p, 0);
        final android.widget.Spinner categories = new android.widget.Spinner(this);
        List<String> names = db.getCategories(EntryTypes.EXPENSE);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        categories.setAdapter(adapter);
        if (existing != null) categories.setSelection(Math.max(0, names.indexOf(existing.category)));
        categories.setEnabled(existing == null);
        form.addView(categories);
        EditText amount = new EditText(this); amount.setHint("Monthly budget amount"); amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(existing == null ? "" : String.valueOf(existing.amount)); amount.setSingleLine();
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, -2); amountParams.topMargin = theme.dp(12); amount.setLayoutParams(amountParams); form.addView(amount);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "Set category budget" : "Edit budget").setView(form)
                .setNegativeButton("Cancel", null).setNeutralButton(existing == null ? null : "Remove", null)
                .setPositiveButton("Save", null).create();
        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(save -> {
                String raw = amount.getText().toString().trim();
                if (TextUtils.isEmpty(raw)) { amount.setError("Enter an amount"); return; }
                try {
                    double value = Double.parseDouble(raw);
                    if (value <= 0) { amount.setError("Amount must be greater than zero"); return; }
                    db.saveBudget(categories.getSelectedItem().toString(), value); dialog.dismiss(); refresh();
                } catch (NumberFormatException e) { amount.setError("Enter a valid amount"); }
            });
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(remove -> {
                db.deleteBudget(existing.category); dialog.dismiss(); refresh(); Toast.makeText(this, "Budget removed", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private Button button(String value, int width, int height) {
        Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setMinWidth(0); button.setMinHeight(0); button.setPadding(0, 0, 0, 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(width, height)); return button;
    }
}
