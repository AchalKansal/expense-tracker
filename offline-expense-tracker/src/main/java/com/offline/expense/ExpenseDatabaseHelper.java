package com.offline.expense;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

final class ExpenseDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "offline_expenses.db";
    private static final int DATABASE_VERSION = 7;
    private static final String TABLE_ENTRIES = "entries";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_SMS_SUGGESTIONS = "sms_suggestions";
    private static final String TABLE_MERCHANT_CATEGORIES = "merchant_categories";
    private static final String TABLE_SHARED_EXPENSES = "shared_expenses";

    static final String PAID_BY_ME = "me";
    static final String PAID_BY_FRIEND = "friend";
    static final String SHARED_ORIGIN_LOCAL = "local";
    static final String SHARED_ORIGIN_RECEIVED = "received";

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
        createSmsSuggestionsTable(db);
        createMerchantCategoriesTable(db);
        createSharedExpensesTable(db);
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
        if (oldVersion < 5) {
            createSmsSuggestionsTable(db);
        }
        if (oldVersion < 6) {
            createMerchantCategoriesTable(db);
        }
        if (oldVersion < 7) {
            createSharedExpensesTable(db);
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

    private void createSmsSuggestionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SMS_SUGGESTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "category TEXT NOT NULL, " +
                "note TEXT, " +
                "sender TEXT, " +
                "sms_time INTEGER NOT NULL, " +
                "dedupe_key TEXT UNIQUE" +
                ")");
    }

    private void createMerchantCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MERCHANT_CATEGORIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "merchant_key TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "UNIQUE(type, merchant_key)" +
                ")");
    }

    private static String normalizeMerchantKey(String merchant) {
        if (merchant == null) return null;
        String key = merchant.trim().toLowerCase(Locale.US);
        return key.isEmpty() ? null : key;
    }

    /** Returns the category the user last picked for this merchant/type, or null if none is known yet. */
    String getCategoryForMerchant(String type, String merchant) {
        String key = normalizeMerchantKey(merchant);
        if (key == null) return null;

        Cursor cursor = getReadableDatabase().query(
                TABLE_MERCHANT_CATEGORIES,
                new String[]{"category"},
                "type = ? AND merchant_key = ?",
                new String[]{type, key},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        } finally {
            cursor.close();
        }
    }

    /** Remembers the category the user picked for this merchant/type so future SMS from them suggest it. */
    void saveMerchantCategory(String type, String merchant, String category) {
        String key = normalizeMerchantKey(merchant);
        if (key == null) return;

        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("merchant_key", key);
        values.put("category", category);
        getWritableDatabase().insertWithOnConflict(
                TABLE_MERCHANT_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void createSharedExpensesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SHARED_EXPENSES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "entry_id INTEGER NOT NULL, " +
                "friend_name TEXT NOT NULL, " +
                "total_amount REAL NOT NULL, " +
                "my_share REAL NOT NULL, " +
                "friend_share REAL NOT NULL, " +
                "paid_by TEXT NOT NULL, " +
                "settled INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL, " +
                "origin TEXT NOT NULL DEFAULT 'local'" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shared_expenses_friend ON " +
                TABLE_SHARED_EXPENSES + "(friend_name, settled)");
    }

    /** Adds a split expense: a normal entry for the user's own share, plus the shared-expense bookkeeping row. */
    long addSharedExpense(String friendName, double totalAmount, double myShare, double friendShare,
                           String paidBy, String category, String note, long createdAt, String origin) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long entryId = addEntryInternal(db, EntryTypes.EXPENSE, myShare, category, note, createdAt);
            if (entryId == -1) return -1;

            ContentValues values = new ContentValues();
            values.put("entry_id", entryId);
            values.put("friend_name", friendName);
            values.put("total_amount", totalAmount);
            values.put("my_share", myShare);
            values.put("friend_share", friendShare);
            values.put("paid_by", paidBy);
            values.put("settled", 0);
            values.put("created_at", createdAt);
            values.put("origin", origin);
            long id = db.insert(TABLE_SHARED_EXPENSES, null, values);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    List<FriendBalance> getFriendBalances() {
        List<FriendBalance> balances = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT friend_name, " +
                "SUM(CASE WHEN paid_by = ? THEN friend_share ELSE -my_share END) AS net, " +
                "COUNT(*) AS unsettled_count " +
                "FROM " + TABLE_SHARED_EXPENSES +
                " WHERE settled = 0 GROUP BY friend_name ORDER BY friend_name COLLATE NOCASE",
                new String[]{PAID_BY_ME}
        );
        try {
            while (cursor.moveToNext()) {
                balances.add(new FriendBalance(cursor.getString(0), cursor.getDouble(1), cursor.getInt(2)));
            }
        } finally {
            cursor.close();
        }
        return balances;
    }

    List<SharedExpense> getSharedExpensesForFriend(String friendName) {
        List<SharedExpense> items = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_SHARED_EXPENSES,
                null,
                "friend_name = ?",
                new String[]{friendName},
                null,
                null,
                "created_at DESC, id DESC"
        );
        try {
            while (cursor.moveToNext()) {
                items.add(readSharedExpense(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    void settleFriend(String friendName) {
        ContentValues values = new ContentValues();
        values.put("settled", 1);
        getWritableDatabase().update(TABLE_SHARED_EXPENSES, values,
                "friend_name = ? AND settled = 0", new String[]{friendName});
    }

    SharedExpense getSharedExpenseById(long id) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_SHARED_EXPENSES,
                null,
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? readSharedExpense(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    List<String> getDistinctFriendNames() {
        List<String> names = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT DISTINCT friend_name FROM " + TABLE_SHARED_EXPENSES +
                " ORDER BY friend_name COLLATE NOCASE", null);
        try {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return names;
    }

    private SharedExpense readSharedExpense(Cursor cursor) {
        return new SharedExpense(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("entry_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("friend_name")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("my_share")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("friend_share")),
                cursor.getString(cursor.getColumnIndexOrThrow("paid_by")),
                cursor.getInt(cursor.getColumnIndexOrThrow("settled")) != 0,
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("origin"))
        );
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
        return addEntryInternal(getWritableDatabase(), type, amount, category, note, createdAt);
    }

    private long addEntryInternal(SQLiteDatabase db, String type, double amount, String category, String note, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("created_at", createdAt);
        return db.insert(TABLE_ENTRIES, null, values);
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

    int getEntryCountBetween(long startMillis, long endMillis) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                new String[]{"COUNT(*) AS count"},
                "created_at >= ? AND created_at < ?",
                new String[]{String.valueOf(startMillis), String.valueOf(endMillis)},
                null,
                null,
                null
        );

        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    void deleteEntriesBetween(long startMillis, long endMillis) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ENTRIES, "created_at >= ? AND created_at < ?",
                new String[]{String.valueOf(startMillis), String.valueOf(endMillis)});
        db.execSQL("VACUUM");
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

    long addSmsSuggestion(String type, double amount, String category, String note, String sender, long smsTime, String dedupeKey) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("sender", sender);
        values.put("sms_time", smsTime);
        values.put("dedupe_key", dedupeKey);
        return getWritableDatabase().insertWithOnConflict(
                TABLE_SMS_SUGGESTIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void deleteSmsSuggestion(long id) {
        getWritableDatabase().delete(TABLE_SMS_SUGGESTIONS, "id = ?", new String[]{String.valueOf(id)});
    }

    int getPendingSmsSuggestionCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SMS_SUGGESTIONS, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    List<SmsSuggestion> getPendingSmsSuggestions() {
        List<SmsSuggestion> suggestions = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_SMS_SUGGESTIONS,
                null,
                null,
                null,
                null,
                null,
                "sms_time DESC, id DESC"
        );

        try {
            while (cursor.moveToNext()) {
                suggestions.add(new SmsSuggestion(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                        cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        cursor.getString(cursor.getColumnIndexOrThrow("note")),
                        cursor.getString(cursor.getColumnIndexOrThrow("sender")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("sms_time"))
                ));
            }
        } finally {
            cursor.close();
        }

        return suggestions;
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

    double getTotalForTypeBetween(String type, long startMillis, long endMillis) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                new String[]{"SUM(amount) AS total"},
                "type = ? AND created_at >= ? AND created_at < ?",
                new String[]{type, String.valueOf(startMillis), String.valueOf(endMillis)},
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

    Long getFirstEntryTimestamp() {
        Cursor cursor = getReadableDatabase().query(
                TABLE_ENTRIES,
                new String[]{"MIN(created_at) AS first_at"},
                null,
                null,
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
            return null;
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
        long tzOffsetMs = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        List<DayTotal> totals = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT ((created_at + " + tzOffsetMs + ") / 86400000) * 86400000 - " + tzOffsetMs + " AS day_bucket, SUM(amount) AS total" +
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
