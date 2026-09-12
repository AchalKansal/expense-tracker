# Offline Expense Tracker

A fully offline Android expense tracker. All data — entries, categories, SMS-derived
suggestions — lives in a local SQLite database on the device. Nothing is sent to a server.

## Features

- **Add / edit / delete entries** for expenses and income, each with an amount, category,
  optional note, and date.
- **Custom categories** — create, rename, or remove categories per entry type
  (`ManageCategoriesActivity`), with sensible defaults seeded on first run.
- **History view** with daily/monthly totals and a category breakdown chart.
- **SMS transaction suggestions** — bank/UPI SMS are parsed on-device (regex/keyword based,
  no network calls) to detect amount, type (debit/credit), and merchant, then offered as a
  one-tap entry suggestion. Two ways to feed it:
  - Share a message into the app from Messages via the system share sheet.
  - Enable Notification Access so `SmsNotificationListenerService` reads notifications from
    your default SMS app and auto-detects transactions.
- **Merchant category memory** — once you confirm or edit a category for a merchant, the app
  remembers it and auto-suggests the same category next time that merchant appears in an SMS.
- **Dark mode** support (`ThemeHelper`).
- **Banner ads** via Google AdMob (adaptive banner size, with retry-on-failure) to support
  the app; ads are personalized (uses the device advertising ID).

## Project structure

```
offline-expense-tracker/
  src/main/java/com/offline/expense/   Activities, services, DB helper, parsers, views
  src/main/res/                        Layouts, drawables, strings
  build.gradle                         Module build config (applicationId: com.offline.expense)
```

Key classes:
- `ExpenseDatabaseHelper` — SQLite schema (entries, categories, sms_suggestions,
  merchant_categories) and all data access.
- `SmsTransactionParser` — on-device parsing of bank/UPI SMS text into a `ParsedTransaction`.
- `SmsReviewActivity` / `SmsNotificationListenerService` — the two entry points that turn a
  parsed SMS into a pending suggestion.
- `MainActivity`, `HistoryActivity`, `EditEntryActivity`, `ManageCategoriesActivity` — core UI.

## Requirements

- Android Studio (or a JDK 21 toolchain + Android SDK for CLI builds)
- `compileSdk` / `targetSdk` 36, `minSdk` 24

## Building

```bash
# from the repo root
./gradlew :offline-expense-tracker:assembleDebug
```

A release build requires a `keystore.properties` file at the repo root (gitignored) with:

```properties
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

```bash
./gradlew :offline-expense-tracker:assembleRelease
```

## Privacy

- No SMS/Call Log permissions are requested. SMS content is only read when the user shares a
  message in, or when Notification Access is explicitly granted (reads notification text from
  the default SMS app only, on-device parsing only).
- The app shows ads via Google AdMob and uses the advertising ID for personalization — see the
  in-app privacy policy for details.
