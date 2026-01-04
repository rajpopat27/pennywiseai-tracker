# PennyWise Architecture Refactor Analysis

## Executive Summary

A comprehensive code review identified significant opportunities to reduce duplication, improve consistency, and follow best practices. The codebase has good architectural foundations but has accumulated technical debt from organic growth.

**Key Statistics:**
- ~1500-2000 lines of duplicated code identified
- 3 major areas of transaction processing duplication
- 4 duplicated transaction list item implementations
- Multiple filter state duplications across ViewModels
- 1 critical memory leak identified

---

## 1. CRITICAL ISSUES

### 1.1 Memory Leak in HomeViewModel (CRITICAL)
**File:** `presentation/home/HomeViewModel.kt:258-277`

```kotlin
// MEMORY LEAK: observeForever without removeObserver
workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME).observeForever { workInfos ->
    // Never removed in onCleared()
}
```

**Fix:** Use Flow instead of LiveData.observeForever or store observer and remove in onCleared().

---

### 1.2 Transaction Processing - 3 Duplicated Flows (CRITICAL)
**Files:**
- `worker/OptimizedSmsReaderWorker.kt:1041-1147` (Direct Save)
- `data/manager/PendingTransactionManager.kt:173-252` (Confirm Pending)
- `data/manager/PendingTransactionManager.kt:305-379` (Auto-save Expired)

**Problem:** Same processing logic duplicated 3 times:
- Merchant mapping
- Rule evaluation
- Subscription matching
- Balance updates
- **Cashback only applied in ONE path!**

**Impact:** Auto-synced transactions don't get cashback applied.

**Solution:** Create unified `TransactionProcessor` class. See `docs/transaction-processing-refactor.md`.

---

### 1.3 Duplicate Workers (HIGH)
**Files:**
- `worker/SmsReaderWorker.kt` (916 lines)
- `worker/OptimizedSmsReaderWorker.kt` (1753 lines)

**Problem:** Two workers doing the same thing with 90% code overlap.

**Solution:** Delete `SmsReaderWorker.kt`, keep only `OptimizedSmsReaderWorker.kt`.

---

## 2. DATA LAYER ISSUES

### 2.1 Duplicate DAO Queries
**File:** `data/database/dao/AccountBalanceDao.kt:31-93`

**Problem:** Same complex join query repeated 3 times:
```sql
SELECT DISTINCT ab1.*
FROM account_balances ab1
INNER JOIN (
    SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
    FROM account_balances
    GROUP BY bank_name, account_last4
) ab2
```

**Solution:** Create a database VIEW for "latest balances per account".

---

### 2.2 Inconsistent Flow vs Suspend Returns
**Files:** Multiple DAOs

| DAO | Issue |
|-----|-------|
| AccountBalanceDao | `getLatestBalance()` suspend AND `getLatestBalanceFlow()` Flow - same query! |
| PendingTransactionDao | `getAllPending()` Flow AND `getAllPendingList()` List - same query! |
| SubscriptionDao | `getSubscriptionsByState()` Flow AND `getSubscriptionsByStateList()` List |

**Solution:** Standardize: Use Flow for reactive queries, suspend List for one-shot.

---

### 2.3 Missing Database Indexes
**File:** `data/database/entity/TransactionEntity.kt`

**Missing indexes on frequently queried columns:**
- `merchant_name` (used in search, grouping)
- `category` (used in filtering, analytics)
- `bank_name` (used in account matching)
- `account_number` (used in account matching)

**Current:** Only index on `transaction_hash`.

---

### 2.4 Duplicate Monthly Breakdown Logic
**File:** `data/repository/TransactionRepository.kt:152-257`

**4 nearly identical methods:**
- `getCurrentMonthBreakdown()`
- `getLastMonthBreakdown()`
- `getCurrentMonthBreakdownByCurrency()`
- `getLastMonthBreakdownByCurrency()`

**Solution:** Extract shared calculation to helper function.

---

### 2.5 Cashback Configuration Fragmentation
**Files:**
- `CardDao.kt:81-88` - Card cashback
- `AccountBalanceDao.kt:161-170` - Account cashback
- `TransactionDao.kt:194-208` - Apply retroactive
- `CardRepository.kt:167-177` - Card operations
- `AccountBalanceRepository.kt:140-146` - Account operations

**Problem:** No single source of truth. Card has default, Account has default, Transaction has cashback_percent and cashback_amount.

**Solution:** Create `CashbackRepository` to centralize all cashback logic.

---

### 2.6 Missing Foreign Key Constraints
**File:** `data/database/entity/CardEntity.kt:38-39`

```kotlin
val accountLast4: String? = null, // Links to AccountBalanceEntity - NO FK CONSTRAINT!
```

**Problem:** No cascade rules, orphaned references possible.

---

## 3. VIEWMODEL LAYER ISSUES

### 3.1 Filter State Duplication (HIGH)
**Files:**
- `presentation/transactions/TransactionsViewModel.kt:41-88`
- `ui/screens/analytics/AnalyticsViewModel.kt:24-74`

**Duplicated:**
```kotlin
private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
private val _selectedCurrency = MutableStateFlow("INR")
private val _transactionTypeFilter = MutableStateFlow(...)
private val _customDateRangeEpochDays = savedStateHandle.getStateFlow(...)
```

**Solution:** Create `FilterStateManager` class.

---

### 3.2 HomeViewModel Too Large (HIGH)
**File:** `presentation/home/HomeViewModel.kt` (535 lines)

**Manages 6+ concerns:**
- Account balances
- Transaction breakdown
- Recent transactions
- Subscriptions
- SMS scanning
- In-app updates/reviews
- Currency conversion

**Solution:** Split into focused ViewModels.

---

### 3.3 SharedPreferences in ViewModels
**Files:**
- `presentation/home/HomeViewModel.kt:45`
- `presentation/accounts/ManageAccountsViewModel.kt:59`

**Problem:** Direct SharedPreferences access in ViewModels.

**Solution:** Create `HiddenAccountsRepository`.

---

### 3.4 Business Logic in ViewModels
**Files:**
- `AnalyticsViewModel.kt:148-220` - Complex aggregations
- `HomeViewModel.kt:100-111, 328-353` - Currency conversion
- `PendingTransactionsViewModel.kt:90-139` - Cashback calculation
- `TransactionsViewModel.kt:675-728` - Currency totals

**Solution:** Extract to domain layer use cases.

---

### 3.5 Delete with Undo Pattern Duplicated
**Files:**
- `HomeViewModel.kt:389-413`
- `TransactionsViewModel.kt:370-394`

**Identical pattern:**
```kotlin
private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
fun deleteTransaction(transaction: TransactionEntity) { ... }
fun undoDelete() { ... }
fun clearDeletedTransaction() { ... }
```

**Solution:** Create `UndoableDeleteDelegate` class.

---

### 3.6 Inconsistent Error/Loading State Patterns

| ViewModel | Loading Default | Error Pattern |
|-----------|-----------------|---------------|
| HomeViewModel | `true` | In UiState |
| TransactionsViewModel | `true` | In UiState |
| CategoriesViewModel | `false` | In UiState |
| SubscriptionsViewModel | `true` | None! |
| ManageAccountsViewModel | `false` | In UiState |

**Solution:** Standardize on `isLoading = true` default and separate error StateFlow.

---

## 4. UI LAYER ISSUES

### 4.1 Transaction Item Implementations x4 (HIGH)
**File:** `presentation/home/HomeScreen.kt`

**4 different implementations:**
- `ReferenceTransactionItem` (lines 986-1115)
- `ExpenzioTransactionItem` (lines 1390-1470)
- `SimpleTransactionItem` (lines 1474-1503)
- `TransactionItem` (lines 1579-1662)

**Solution:** Consolidate to single configurable `TransactionListItem`.

---

### 4.2 Empty State Pattern x3
**Files:**
- `HomeScreen.kt:1331-1364` - `ExpenzioEmptyTransactions`
- `TransactionsScreen.kt:646-692` - `EmptyTransactionsState`
- `SubscriptionsScreen.kt:357-387` - `EmptySubscriptionsState`

**Solution:** Create reusable `EmptyStateCard` component.

---

### 4.3 Filter Dialogs x4 with Same Pattern
**File:** `TransactionsScreen.kt:1012-1327`

4 dialogs with identical structure:
- MerchantFilterDialog
- AmountFilterDialog
- CategoryFilterDialog
- AccountFilterDialog

**Solution:** Create generic `FilterDialog` composable.

---

### 4.4 Inconsistent Card Usage
**Problem:** Mix of `PennyWiseCard` (standardized) and raw `Card` with manual styling.

**Files:** Found 20+ instances of manual Card creation in HomeScreen.kt.

**Solution:** Always use `PennyWiseCard`.

---

### 4.5 Hardcoded Dimensions
**Examples:**
- `HomeScreen.kt:180` - `Arrangement.spacedBy(12.dp)`
- `HomeScreen.kt:231` - `contentPadding = PaddingValues(bottom = 80.dp)`
- Various `16.dp`, `8.dp` values

**Solution:** Use `Spacing.sm`, `Spacing.md`, `Dimensions.Component` consistently.

---

### 4.6 HomeScreen Too Large
**File:** `presentation/home/HomeScreen.kt` (2104 lines!)

**Problem:** Contains multiple unused/experimental implementations.

**Solution:**
- Extract dialogs to separate files
- Extract complex composables to components
- Remove dead code

---

## 5. DOMAIN LAYER ISSUES

### 5.1 Missing Use Cases
**Currently only 5 use cases:**
- AddTransactionUseCase
- AddSubscriptionUseCase
- ApplyRulesToPastTransactionsUseCase
- GetCategoriesUseCase
- InitializeRuleTemplatesUseCase

**Missing:**
- GetAnalyticsUseCase
- CalculateMonthlyBreakdownUseCase
- CalculateCategoryBreakdownUseCase
- GroupTransactionsByDateUseCase
- CalculateCashbackUseCase
- ApplyRetroactiveCashbackUseCase
- ExportBackupUseCase
- ImportBackupUseCase
- ManageUnrecognizedSmsUseCase

---

### 5.2 ViewModels Calling Repositories Directly
**Files:**
- `AnalyticsViewModel.kt` - Injects `TransactionRepository`
- `SettingsViewModel.kt` - Injects 4 repositories directly
- `UnrecognizedSmsViewModel.kt` - Injects `UnrecognizedSmsRepository`

---

## 6. UTILITIES ISSUES

### 6.1 Duplicate Currency Utilities
**Files:**
- `utils/CurrencyFormatter.kt` (137 lines) - Multi-currency
- `utils/CurrencyUtils.kt` (86 lines) - INR-only

**Solution:** Delete `CurrencyUtils.kt`, use only `CurrencyFormatter.kt`.

---

### 6.2 Currency Sorting Duplicated
**Files:** HomeViewModel, TransactionsViewModel, AnalyticsViewModel

**Same logic:**
```kotlin
currencies.sortedWith { a, b ->
    when {
        a == "INR" -> -1
        b == "INR" -> 1
        else -> a.compareTo(b)
    }
}
```

**Note:** `CurrencyUtils.sortCurrencies()` exists but not used everywhere.

---

## 7. ENTITY ISSUES

### 7.1 Currency Default Duplicated
**5 entities** with hardcoded `val currency: String = "INR"`:
- TransactionEntity
- AccountBalanceEntity
- CardEntity
- SubscriptionEntity
- PendingTransactionEntity

---

### 7.2 Timestamp Inconsistency
| Entity | created_at | updated_at |
|--------|------------|------------|
| TransactionEntity | ✅ | ✅ |
| CategoryEntity | ✅ | ✅ |
| AccountBalanceEntity | ✅ | ❌ Missing! |
| SubscriptionEntity | ✅ | ✅ |
| CardEntity | ✅ | ✅ |

---

## 8. PRIORITY MATRIX

### Critical (Fix Immediately)
1. Memory leak in HomeViewModel (observeForever)
2. Transaction processing duplication (causes cashback bug)
3. Delete SmsReaderWorker.kt (duplicate worker)

### High Priority
4. Create FilterStateManager (reduce ViewModel duplication)
5. Consolidate transaction item implementations (4 → 1)
6. Add missing database indexes
7. Split HomeViewModel into smaller ViewModels
8. Move SharedPreferences to repository

### Medium Priority
9. Create TransactionProcessor (unify all processing)
10. Create CashbackRepository (centralize cashback)
11. Create reusable UI components (EmptyState, FilterDialog)
12. Standardize DAO return types (Flow vs suspend)
13. Delete CurrencyUtils.kt (keep CurrencyFormatter)
14. Extract business logic to use cases

### Low Priority
15. Create database VIEW for latest balances
16. Add foreign key constraints
17. Fix hardcoded dimensions
18. Add missing updatedAt to AccountBalanceEntity
19. Move category seeding to use case

---

## 9. ESTIMATED IMPACT

| Metric | Before | After |
|--------|--------|-------|
| Duplicated Lines | ~1500-2000 | ~200-300 |
| Transaction Item Impls | 4 | 1 |
| Filter State Copies | 2 | 1 (shared) |
| Workers | 2 | 1 |
| Transaction Processing Paths | 3 | 1 |
| HomeScreen Lines | 2104 | ~800-1000 |
| HomeViewModel Lines | 535 | ~200-250 |

---

## 10. ARCHITECTURE RECOMMENDATIONS

### Proposed Structure
```
domain/
├── model/           # Domain models (separate from entities)
├── repository/      # Repository interfaces
├── usecase/         # All business logic use cases
│   ├── transaction/
│   ├── analytics/
│   ├── account/
│   └── settings/
└── service/         # Domain services (RuleEngine, etc.)

data/
├── database/
│   ├── dao/
│   ├── entity/
│   └── view/        # NEW: Database views
├── repository/      # Repository implementations
├── mapper/          # Entity ↔ Domain mappers
├── processor/       # NEW: TransactionProcessor
└── manager/         # Simplified managers

presentation/
├── common/
│   ├── FilterStateManager.kt    # NEW: Shared filter state
│   ├── UndoableDeleteDelegate.kt # NEW: Shared undo logic
│   └── ...
├── home/
├── transactions/
└── ...

ui/
├── components/
│   ├── cards/
│   ├── dialogs/     # NEW: Extracted dialogs
│   ├── items/       # NEW: List items (TransactionListItem, etc.)
│   └── states/      # NEW: EmptyState, LoadingState, ErrorState
└── theme/
```
