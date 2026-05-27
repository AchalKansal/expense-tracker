package com.offline.coders;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ExpenseDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "offline_expenses.db";
    private static final int DATABASE_VERSION = 4;
    private static final String TABLE_ENTRIES = "entries";
    private static final String TABLE_CATEGORIES = "categories";

    private static final String[] DEFAULT_EXPENSE_CATEGORIES = {
            "Food", "Transport", "Shopping", "Bills", "Health", "Rent", "Family", "Investment", "Other"
    };
    private static final String[] DEFAULT_INCOME_CATEGORIES = {
            "Salary", "Business", "Freelance", "Gift", "Refund", "Investment", "Other"
    };

    private static final Set<String> DEFAULT_EXPENSE_SET = new HashSet<>(Arrays.asList(DEFAULT_EXPENSE_CATEGORIES));
    private static final Set<String> DEFAULT_INCOME_SET = new HashSet<>(Arrays.asList(DEFAULT_INCOME_CATEGORIES));

    static boolean isDefaultCategory(String type, String name) {
        if (EntryTypes.EXPENSE.equals(type)) return DEFAULT_EXPENSE_SET.contains(name);
        if (EntryTypes.INCOME.equals(type)) return DEFAULT_INCOME_SET.contains(name);
        return false;
    }

    ExpenseDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createEntriesTable(db);
        createCategoriesTable(db);
        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createCategoriesTable(db);
            seedDefaultCategories(db);
        }
        if (oldVersion < 3) {
            insertCategory(db, EntryTypes.EXPENSE, "Investment");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_type_date ON " + TABLE_ENTRIES + "(type, created_at)");
        }
    }

    private void createEntriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ENTRIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "category TEXT NOT NULL, " +
                "note TEXT, " +
                "created_at INTEGER NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_entries_type_date ON " + TABLE_ENTRIES + "(type, created_at)");
    }

    private void createCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "UNIQUE(type, name)" +
                ")");
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        for (String category : DEFAULT_EXPENSE_CATEGORIES) {
            insertCategory(db, EntryTypes.EXPENSE, category);
        }
        for (String category : DEFAULT_INCOME_CATEGORIES) {
            insertCategory(db, EntryTypes.INCOME, category);
        }
    }

    private void insertCategory(SQLiteDatabase db, String type, String name) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("name", name);
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    long addEntry(String type, double amount, String category, String note, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("created_at", createdAt);
        return getWritableDatabase().insert(TABLE_ENTRIES, null, values);
    }

    void updateEntry(long id, String type, double amount, String category, String note, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("created_at", createdAt);
        getWritableDatabase().update(TABLE_ENTRIES, values, "id = ?", new String[]{String.valueOf(id)});
    }

    void deleteEntry(long id) {
        getWritableDatabase().delete(TABLE_ENTRIES, "id = ?", new String[]{String.valueOf(id)});
    }

    ExpenseEntry getEntry(long id) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                null,
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return readEntry(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    void addCategory(String type, String name) {
        insertCategory(getWritableDatabase(), type, name);
    }

    void renameCategory(String type, String oldName, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", newName);
        db.update(TABLE_CATEGORIES, cv, "type = ? AND name = ?", new String[]{type, oldName});
        ContentValues entryCv = new ContentValues();
        entryCv.put("category", newName);
        db.update(TABLE_ENTRIES, entryCv, "type = ? AND category = ?", new String[]{type, oldName});
    }

    List<String> getMostUsedCategories(String type, int limit) {
        List<String> categories = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category FROM " + TABLE_ENTRIES +
                " WHERE type = ? GROUP BY category ORDER BY COUNT(*) DESC LIMIT ?",
                new String[]{type, String.valueOf(limit)}
        );
        try {
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return categories;
    }

    List<String> getCategories(String type) {
        List<String> categories = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_CATEGORIES,
                new String[]{"name"},
                "type = ?",
                new String[]{type},
                null,
                null,
                "name COLLATE NOCASE"
        );

        try {
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        return categories;
    }

    List<ExpenseEntry> getRecentEntries(int limit) {
        List<ExpenseEntry> entries = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                null,
                null,
                null,
                null,
                null,
                "created_at DESC, id DESC",
                String.valueOf(limit)
        );

        try {
            while (cursor.moveToNext()) {
                entries.add(readEntry(cursor));
            }
        } finally {
            cursor.close();
        }

        return entries;
    }

    List<ExpenseEntry> getAllExpenses() {
        return getAllEntriesForType(EntryTypes.EXPENSE);
    }

    List<ExpenseEntry> getAllEntries() {
        List<ExpenseEntry> entries = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                null,
                null,
                null,
                null,
                null,
                "created_at DESC, id DESC"
        );

        try {
            while (cursor.moveToNext()) {
                entries.add(readEntry(cursor));
            }
        } finally {
            cursor.close();
        }

        return entries;
    }

    List<ExpenseEntry> getAllEntriesForType(String type) {
        List<ExpenseEntry> entries = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                null,
                "type = ?",
                new String[]{type},
                null,
                null,
                "created_at DESC, id DESC"
        );

        try {
            while (cursor.moveToNext()) {
                entries.add(readEntry(cursor));
            }
        } finally {
            cursor.close();
        }

        return entries;
    }

    double getTotalForType(String type) {
        return getTotalForTypeSince(type, 0L);
    }

    double getTotalForTypeSince(String type, long sinceMillis) {
        String selection = "type = ?";
        String[] args;
        if (sinceMillis > 0L) {
            selection += " AND created_at >= ?";
            args = new String[]{type, String.valueOf(sinceMillis)};
        } else {
            args = new String[]{type};
        }

        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                new String[]{"SUM(amount) AS total"},
                selection,
                args,
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.isNull(0) ? 0.0 : cursor.getDouble(0);
            }
            return 0.0;
        } finally {
            cursor.close();
        }
    }

    List<CategoryTotal> getCategoryTotals(String type, long startMillis, long endMillis) {
        List<CategoryTotal> totals = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                new String[]{"category", "SUM(amount) AS total"},
                "type = ? AND created_at >= ? AND created_at < ?",
                new String[]{type, String.valueOf(startMillis), String.valueOf(endMillis)},
                "category",
                null,
                "total DESC"
        );

        try {
            while (cursor.moveToNext()) {
                totals.add(new CategoryTotal(cursor.getString(0), cursor.getDouble(1)));
            }
        } finally {
            cursor.close();
        }

        return totals;
    }

    List<DayTotal> getDailyTotals(String type, long startMillis, long endMillis) {
        List<DayTotal> totals = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT (created_at / 86400000) * 86400000 AS day_bucket, SUM(amount) AS total" +
                " FROM " + TABLE_ENTRIES +
                " WHERE type = ? AND created_at >= ? AND created_at < ?" +
                " GROUP BY day_bucket ORDER BY day_bucket",
                new String[]{type, String.valueOf(startMillis), String.valueOf(endMillis)}
        );
        try {
            while (cursor.moveToNext()) {
                totals.add(new DayTotal(cursor.getLong(0), cursor.getDouble(1)));
            }
        } finally {
            cursor.close();
        }
        return totals;
    }

    List<DayTotal> getMonthlyTotals(String type, long startMillis, long endMillis) {
        List<DayTotal> totals = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT strftime('%Y-%m', datetime(created_at/1000, 'unixepoch')) AS month_key," +
                " MIN(created_at) AS month_start, SUM(amount) AS total" +
                " FROM " + TABLE_ENTRIES +
                " WHERE type = ? AND created_at >= ? AND created_at < ?" +
                " GROUP BY month_key ORDER BY month_key",
                new String[]{type, String.valueOf(startMillis), String.valueOf(endMillis)}
        );
        try {
            while (cursor.moveToNext()) {
                totals.add(new DayTotal(cursor.getLong(1), cursor.getDouble(2)));
            }
        } finally {
            cursor.close();
        }
        return totals;
    }

    private ExpenseEntry readEntry(Cursor cursor) {
        return new ExpenseEntry(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getString(cursor.getColumnIndexOrThrow("note")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        );
    }
}
