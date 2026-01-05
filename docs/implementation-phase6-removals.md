# Phase 6: Feature Removals

## Overview
Remove features that are being dropped to simplify the app.

**Goal:** Cleaner, simpler codebase with fewer features to maintain.

---

## 6.1 Simplify Transaction Types

### 6.1.1 Update TransactionType Enum
**Files:**
- `parser-core/src/main/kotlin/com/fintrace/parser/core/TransactionType.kt`
- `app/src/main/java/com/fintrace/app/data/database/entity/TransactionType.kt`

```kotlin
// BEFORE
enum class TransactionType {
    INCOME, EXPENSE, CREDIT, INVESTMENT, TRANSFER
}

// AFTER
enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}
```

- [ ] Update enum in parser-core
- [ ] Update enum in app module
- [ ] Build and find all compilation errors

### 6.1.2 Update Bank Parsers
**Search for:** `TransactionType.CREDIT` and `TransactionType.INVESTMENT`

For each parser that returns CREDIT:
```kotlin
// BEFORE
return TransactionType.CREDIT

// AFTER
return TransactionType.EXPENSE  // Credit card spends are expenses
```

For each parser that returns INVESTMENT:
```kotlin
// BEFORE
return TransactionType.INVESTMENT

// AFTER
return TransactionType.EXPENSE  // Or TRANSFER if moving to investment account
```

- [ ] List all parsers using CREDIT
- [ ] List all parsers using INVESTMENT
- [ ] Update each parser
- [ ] Run parser tests

### 6.1.3 Update UI Filters
**File:** Find transaction type filter in TransactionsScreen

```kotlin
// BEFORE
val typeOptions = listOf(
    null to "All",
    TransactionType.INCOME to "Income",
    TransactionType.EXPENSE to "Expense",
    TransactionType.CREDIT to "Credit",
    TransactionType.INVESTMENT to "Investment",
    TransactionType.TRANSFER to "Transfer"
)

// AFTER
val typeOptions = listOf(
    null to "All",
    TransactionType.INCOME to "Income",
    TransactionType.EXPENSE to "Expense",
    TransactionType.TRANSFER to "Transfer"
)
```

- [ ] Update filter options
- [ ] Update any icons associated with types
- [ ] Test filter functionality

### 6.1.4 Update Analytics
- [ ] Update category breakdown logic if it groups by type
- [ ] Update any charts that show transaction types
- [ ] Remove CREDIT/INVESTMENT colors/icons

---

## 6.2 Remove App Lock Feature

### 6.2.1 Find All App Lock Code
**Search for:** `lock`, `biometric`, `authenticate`, `AppLock`, `fingerprint`

### 6.2.2 Remove from Settings
**File:** `presentation/settings/SettingsScreen.kt`

- [ ] Remove app lock toggle/section
- [ ] Remove biometric preference
- [ ] Remove PIN/pattern settings

### 6.2.3 Remove from SettingsViewModel
**File:** `presentation/settings/SettingsViewModel.kt`

- [ ] Remove lock-related state
- [ ] Remove lock-related functions
- [ ] Remove biometric-related code

### 6.2.4 Remove Lock Screen
- [ ] Find lock screen composable
- [ ] Delete the file
- [ ] Remove from navigation graph

### 6.2.5 Remove from App Startup
**File:** `MainActivity.kt` or `App.kt`

- [ ] Remove lock check on app launch
- [ ] Remove biometric prompt on resume

### 6.2.6 Remove Dependencies
**File:** `app/build.gradle.kts`

```kotlin
// Remove if only used for app lock
// implementation("androidx.biometric:biometric:1.x.x")
```

- [ ] Check if biometric library used elsewhere
- [ ] Remove dependency if unused
- [ ] Clean up unused imports

### 6.2.7 Remove SharedPreferences
- [ ] Remove lock-related keys from preferences
- [ ] Remove any migration code for lock settings

---

## 6.3 Remove Developer Options

### 6.3.1 Find Developer Options Code
**Search for:** `developer`, `debug`, `DevOptions`, `debugMode`

### 6.3.2 Remove from Settings
**File:** `presentation/settings/SettingsScreen.kt`

- [ ] Remove developer options section
- [ ] Remove debug toggles
- [ ] Remove any "shake to report" features

### 6.3.3 Remove from ViewModel
**File:** `presentation/settings/SettingsViewModel.kt`

- [ ] Remove developer-related state
- [ ] Remove debug functions

### 6.3.4 Clean Up
- [ ] Remove developer-only screens
- [ ] Remove from navigation if applicable
- [ ] Remove debug logging toggles

---

## 6.4 Remove Support & Community Features

### 6.4.1 Find Support/Community Code
**Search for:** `support`, `community`, `discord`, `github`, `feedback`, `report`

### 6.4.2 Remove from Settings
**File:** `presentation/settings/SettingsScreen.kt`

```kotlin
// REMOVE sections like:
// - "Join our Discord"
// - "Report an issue"
// - "Rate the app"
// - "Share feedback"
// - "Community"
// - "Support"
```

- [ ] Remove support section
- [ ] Remove community section
- [ ] Remove Discord links
- [ ] Remove GitHub links
- [ ] Remove feedback options

### 6.4.3 Clean Up Resources
**File:** `res/values/strings.xml`

- [ ] Remove unused string resources
- [ ] Remove Discord/GitHub URLs
- [ ] Remove support-related strings

---

## 6.5 Delete Duplicate Worker

### 6.5.1 Verify SmsReaderWorker is Unused
**Search for:** `SmsReaderWorker` (not `OptimizedSmsReaderWorker`)

- [ ] Search entire codebase for references
- [ ] Check WorkManager scheduling code
- [ ] Check any tests

### 6.5.2 Delete the File
**File:** `worker/SmsReaderWorker.kt`

- [ ] Delete `SmsReaderWorker.kt`
- [ ] Remove from any Hilt modules if registered
- [ ] Build and verify

---

## 6.6 Update SMS Scan Period Options

### 6.6.1 Update ScanPeriod Enum
**File:** Already created in Phase 2 (`AppPreferencesRepository.kt`)

```kotlin
enum class ScanPeriod(val days: Int?) {
    ONE_DAY(1),
    ONE_WEEK(7),
    FIFTEEN_DAYS(15),
    SINCE_INSTALL(null)
}
```

### 6.6.2 Update Settings UI
**File:** `presentation/settings/SettingsScreen.kt`

```kotlin
// Update scan period picker options
val scanPeriodOptions = listOf(
    ScanPeriod.ONE_DAY to "1 Day",
    ScanPeriod.ONE_WEEK to "1 Week",
    ScanPeriod.FIFTEEN_DAYS to "15 Days",
    ScanPeriod.SINCE_INSTALL to "Since Install"
)

// In UI
SettingsDropdown(
    title = "SMS Scan Period",
    selectedValue = currentScanPeriod,
    options = scanPeriodOptions,
    onSelect = { viewModel.updateScanPeriod(it) }
)
```

- [ ] Find scan period setting in Settings
- [ ] Update options to new 4 options
- [ ] Connect to `AppPreferencesRepository`

### 6.6.3 Update Worker
**File:** `worker/OptimizedSmsReaderWorker.kt`

```kotlin
// Use AppPreferencesRepository for scan start date
private suspend fun getScanStartDate(): Long {
    return appPreferencesRepository.getScanStartDate()
}
```

- [ ] Inject `AppPreferencesRepository` into worker
- [ ] Use `getScanStartDate()` for date filtering
- [ ] Remove old scan period logic

---

## 6.7 Clean Up Unused Code

### 6.7.1 Find Dead Code
- [ ] Run Android Studio's "Analyze > Inspect Code"
- [ ] Look for unused functions
- [ ] Look for unused variables
- [ ] Look for unused imports

### 6.7.2 Remove Dead Code
- [ ] Remove unused private functions
- [ ] Remove commented-out code blocks
- [ ] Remove TODO comments for removed features
- [ ] Clean up unused resources (strings, drawables, etc.)

---

## Verification Checklist

After completing Phase 6:

- [ ] TransactionType only has: INCOME, EXPENSE, TRANSFER
- [ ] All parsers updated to new types
- [ ] UI filters show only 3 types
- [ ] App lock completely removed
- [ ] No biometric prompts on app start
- [ ] Developer options removed from Settings
- [ ] Support/Community sections removed
- [ ] `SmsReaderWorker.kt` deleted
- [ ] Only `OptimizedSmsReaderWorker.kt` exists
- [ ] SMS scan period has 4 options (1d, 1w, 15d, install)
- [ ] Install date tracked for "Since Install" option
- [ ] App builds without errors
- [ ] App runs without crashes
- [ ] All tests pass

---

## Files Deleted

| File | Reason |
|------|--------|
| `worker/SmsReaderWorker.kt` | Duplicate of OptimizedSmsReaderWorker |
| Lock screen composable (if exists) | App lock removed |
| Developer options screen (if exists) | Feature removed |

---

## Lines Removed Estimate

| Area | Estimated Lines |
|------|-----------------|
| SmsReaderWorker.kt | ~916 lines |
| App lock code | ~200-400 lines |
| Developer options | ~100-200 lines |
| Support/Community | ~50-100 lines |
| Dead code cleanup | ~100-300 lines |
| **Total** | **~1300-1900 lines** |

---

## Next Phase
→ Proceed to `implementation-phase7-new-features.md`
