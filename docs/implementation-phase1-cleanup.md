# Implementation Phase 1: Cleanup & Critical Fixes

## Overview
This phase focuses on removing features, fixing critical bugs, and cleaning up the codebase before adding new features.

**Estimated scope:** Remove code, fix bugs, simplify
**Dependencies:** None - this phase comes first

---

## 1.1 Remove Transaction Types

### Task: Simplify TransactionType enum
**Files to modify:**
- [ ] `parser-core/src/main/kotlin/com/pennywiseai/parser/core/TransactionType.kt`
- [ ] `app/src/main/java/com/pennywiseai/tracker/data/database/entity/TransactionType.kt`

**Changes:**
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

- [ ] Remove `CREDIT` from enum
- [ ] Remove `INVESTMENT` from enum
- [ ] Search codebase for `TransactionType.CREDIT` - update all references
- [ ] Search codebase for `TransactionType.INVESTMENT` - update all references
- [ ] Update bank parsers that return CREDIT/INVESTMENT types
- [ ] Update UI filters to show only 3 options
- [ ] Build and verify no compilation errors

---

## 1.2 Remove App Lock Feature

### Task: Remove app lock completely
**Search for:** `lock`, `biometric`, `authenticate`, `AppLock`

**Files to check/modify:**
- [ ] Settings screen - remove app lock toggle
- [ ] SettingsViewModel - remove lock state/functions
- [ ] Lock screen composable - DELETE if exists
- [ ] SharedPreferences - remove lock-related keys
- [ ] App startup - remove lock check
- [ ] `build.gradle` - remove biometric library if unused elsewhere

**Cleanup:**
- [ ] Remove unused imports
- [ ] Remove unused string resources
- [ ] Build and verify

---

## 1.3 Remove Developer Options

### Task: Remove developer options section
**Search for:** `developer`, `debug`, `DevOptions`

**Files to modify:**
- [ ] SettingsScreen - remove developer options section
- [ ] SettingsViewModel - remove related state/functions
- [ ] Remove any debug-only UI elements
- [ ] Clean up unused code

---

## 1.4 Remove Support & Community Features

### Task: Remove support and community sections
**Search for:** `support`, `community`, `discord`, `github`, `feedback`

**Files to modify:**
- [ ] SettingsScreen - remove support section
- [ ] SettingsScreen - remove community section
- [ ] Remove Discord links
- [ ] Remove GitHub issue template references
- [ ] Clean up unused string resources

---

## 1.5 Fix Critical Bug: Custom Cashback Retroactive

### Task: Fix cashback applying to all transactions
**File:** `data/manager/PendingTransactionManager.kt`

**Problem:** When confirming pending transaction with custom cashback, it applies to ALL transactions.

**Root cause analysis:**
- [ ] Find where `applyRetroactiveCashback()` is called during confirm
- [ ] Trace the flow from UI → ViewModel → Manager

**Fix approach:**
- [ ] Custom cashback should ONLY apply to the single transaction being confirmed
- [ ] Do NOT call `applyRetroactiveCashback()` with custom percentage
- [ ] Only apply retroactive when updating account default cashback in Manage Accounts

**Implementation:**
```kotlin
// In confirmTransaction():
// Set cashback on THIS transaction only
val transactionWithCashback = transaction.copy(
    cashbackPercent = customCashbackPercent,
    cashbackAmount = transaction.amount * customCashbackPercent / 100
)
// Do NOT call applyRetroactiveCashback here!
```

- [ ] Test: Confirm pending with custom 5% cashback
- [ ] Verify: Other transactions NOT affected
- [ ] Verify: Only confirmed transaction has 5% cashback

---

## 1.6 Fix Memory Leak in HomeViewModel

### Task: Fix observeForever without removal
**File:** `presentation/home/HomeViewModel.kt:258-277`

**Current (LEAKY):**
```kotlin
workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME)
    .observeForever { workInfos -> ... }
```

**Fix Option 1 - Use Flow (Preferred):**
```kotlin
workManager.getWorkInfosByTagFlow(OptimizedSmsReaderWorker.WORK_NAME)
    .onEach { workInfos -> ... }
    .launchIn(viewModelScope)
```

**Fix Option 2 - Store and remove observer:**
```kotlin
private var workObserver: Observer<List<WorkInfo>>? = null

init {
    workObserver = Observer { workInfos -> ... }
    workManager.getWorkInfosByTagLiveData(...).observeForever(workObserver!!)
}

override fun onCleared() {
    workObserver?.let {
        workManager.getWorkInfosByTagLiveData(...).removeObserver(it)
    }
    super.onCleared()
}
```

- [ ] Implement fix (prefer Option 1)
- [ ] Test WorkManager observation still works
- [ ] Verify no memory leak with LeakCanary

---

## 1.7 Delete Duplicate Worker

### Task: Remove SmsReaderWorker
**Files:**
- `worker/SmsReaderWorker.kt` - DELETE
- `worker/OptimizedSmsReaderWorker.kt` - KEEP

**Steps:**
- [ ] Search for references to `SmsReaderWorker` in codebase
- [ ] Update any WorkManager scheduling using old worker
- [ ] Delete `SmsReaderWorker.kt`
- [ ] Build and verify

---

## 1.8 Update SMS Scan Period Options

### Task: Change scan period options
**Files to modify:**
- [ ] Scan period enum/options definition
- [ ] Settings UI - update picker options
- [ ] `OptimizedSmsReaderWorker` - update date calculation

**New options:**
```kotlin
enum class ScanPeriod {
    ONE_DAY,      // 1 day
    ONE_WEEK,     // 7 days
    FIFTEEN_DAYS, // 15 days
    SINCE_INSTALL // From app install date
}
```

**Install date tracking:**
- [ ] Store install date on first launch
- [ ] Add to SharedPreferences or AppPreferencesRepository
- [ ] Use in worker when `SINCE_INSTALL` selected

```kotlin
// On first launch (in Application or MainActivity)
if (!prefs.contains("app_install_date")) {
    prefs.edit().putLong("app_install_date", System.currentTimeMillis()).apply()
}
```

---

## Verification Checklist

After completing Phase 1:

- [ ] App builds without errors
- [ ] App launches without crashes
- [ ] Transaction types show only: Income, Expense, Transfer
- [ ] App lock feature completely removed
- [ ] Developer options removed from Settings
- [ ] Support/Community sections removed
- [ ] Custom cashback on pending only affects that transaction
- [ ] No memory leaks (test with LeakCanary)
- [ ] Only OptimizedSmsReaderWorker exists
- [ ] SMS scan period has new options (1d, 1w, 15d, install date)

---

## Files Deleted in This Phase

| File | Reason |
|------|--------|
| `worker/SmsReaderWorker.kt` | Duplicate of OptimizedSmsReaderWorker |
| Lock screen composable (if exists) | App lock removed |

---

## Next Phase
→ Proceed to `implementation-phase2-ui-fixes.md`
