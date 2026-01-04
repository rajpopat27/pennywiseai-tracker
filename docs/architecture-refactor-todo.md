# Architecture Refactor - TODO Checklist

## Overview
This checklist tracks the refactoring tasks identified in `docs/architecture-refactor-analysis.md`.

**Reference:** See `docs/architecture-refactor-analysis.md` for full analysis and rationale.

---

## Phase 1: Critical Issues (Fix Immediately)

### 1.1 Memory Leak in HomeViewModel
**File:** `presentation/home/HomeViewModel.kt:258-277`

- [ ] Replace `observeForever` with Flow-based observation
- [ ] OR store observer reference and remove in `onCleared()`
- [ ] Test that WorkManager observation still works correctly
- [ ] Verify no memory leaks with LeakCanary

### 1.2 Delete Duplicate Worker
**Files:**
- `worker/SmsReaderWorker.kt` (DELETE)
- `worker/OptimizedSmsReaderWorker.kt` (KEEP)

- [ ] Verify `SmsReaderWorker.kt` is not used anywhere
- [ ] Search for references to `SmsReaderWorker` in codebase
- [ ] Delete `SmsReaderWorker.kt`
- [ ] Update any WorkManager scheduling that references old worker

### 1.3 Transaction Processing Unification
**See:** `docs/transaction-processing-refactor-todo.md` for detailed steps

- [ ] Complete Phase 1-6 from transaction-processing-refactor-todo.md
- [ ] Verify cashback is applied in ALL flows

---

## Phase 2: Data Layer Cleanup

### 2.1 Add Missing Database Indexes
**File:** `data/database/entity/TransactionEntity.kt`

- [ ] Add index on `merchant_name`
- [ ] Add index on `category`
- [ ] Add index on `bank_name`
- [ ] Add index on `account_number`
- [ ] Add composite index on `(bank_name, account_number)` for account queries
- [ ] Run performance benchmarks before/after

### 2.2 Create Database VIEW for Latest Balances
**File:** `data/database/AppDatabase.kt`

- [ ] Create SQL VIEW: `latest_account_balances`
```sql
CREATE VIEW latest_account_balances AS
SELECT ab1.*
FROM account_balances ab1
INNER JOIN (
    SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
    FROM account_balances
    GROUP BY bank_name, account_last4
) ab2 ON ab1.bank_name = ab2.bank_name
    AND ab1.account_last4 = ab2.account_last4
    AND ab1.timestamp = ab2.max_timestamp;
```
- [ ] Update `AccountBalanceDao` to use the VIEW
- [ ] Remove duplicated join queries

### 2.3 Standardize DAO Return Types
**Pattern:** Flow for reactive, suspend List for one-shot

- [ ] `AccountBalanceDao`: Keep `getLatestBalanceFlow()`, remove duplicate suspend version OR keep both with clear naming
- [ ] `PendingTransactionDao`: Standardize `getAllPending()` naming
- [ ] `SubscriptionDao`: Standardize `getSubscriptionsByState()` naming
- [ ] Document naming convention in code comments

### 2.4 Extract Monthly Breakdown Helper
**File:** `data/repository/TransactionRepository.kt:152-257`

- [ ] Create private helper function for shared calculation logic
- [ ] Refactor `getCurrentMonthBreakdown()` to use helper
- [ ] Refactor `getLastMonthBreakdown()` to use helper
- [ ] Refactor `getCurrentMonthBreakdownByCurrency()` to use helper
- [ ] Refactor `getLastMonthBreakdownByCurrency()` to use helper

### 2.5 Create CashbackRepository
**New File:** `data/repository/CashbackRepository.kt`

- [ ] Create `CashbackRepository` interface
- [ ] Implement `CashbackRepositoryImpl`
- [ ] Consolidate cashback logic from:
  - [ ] `CardDao.kt:81-88`
  - [ ] `AccountBalanceDao.kt:161-170`
  - [ ] `TransactionDao.kt:194-208`
  - [ ] `CardRepository.kt:167-177`
  - [ ] `AccountBalanceRepository.kt:140-146`
- [ ] Add Hilt binding
- [ ] Update ViewModels to use new repository

### 2.6 Delete Duplicate Currency Utils
**Files:**
- `utils/CurrencyFormatter.kt` (KEEP)
- `utils/CurrencyUtils.kt` (DELETE)

- [ ] Migrate any unique functionality from `CurrencyUtils.kt` to `CurrencyFormatter.kt`
- [ ] Update all imports to use `CurrencyFormatter`
- [ ] Delete `CurrencyUtils.kt`
- [ ] Use `CurrencyFormatter.sortCurrencies()` everywhere currency sorting is needed

---

## Phase 3: ViewModel Layer Cleanup

### 3.1 Create FilterStateManager
**New File:** `presentation/common/FilterStateManager.kt`

- [ ] Create `FilterStateManager` class with:
  - [ ] `selectedPeriod: StateFlow<TimePeriod>`
  - [ ] `selectedCurrency: StateFlow<String>`
  - [ ] `transactionTypeFilter: StateFlow<...>`
  - [ ] `customDateRangeEpochDays: StateFlow<...>`
- [ ] Update `TransactionsViewModel` to use `FilterStateManager`
- [ ] Update `AnalyticsViewModel` to use `FilterStateManager`
- [ ] Consider sharing instance via Hilt scope

### 3.2 Create UndoableDeleteDelegate
**New File:** `presentation/common/UndoableDeleteDelegate.kt`

- [ ] Create `UndoableDeleteDelegate` class with:
  - [ ] `deletedItem: StateFlow<T?>`
  - [ ] `delete(item: T)`
  - [ ] `undoDelete(): T?`
  - [ ] `clearDeleted()`
- [ ] Update `HomeViewModel` to use delegate
- [ ] Update `TransactionsViewModel` to use delegate

### 3.3 Create HiddenAccountsRepository
**New File:** `data/repository/HiddenAccountsRepository.kt`

- [ ] Create repository to wrap SharedPreferences access
- [ ] Expose as Flow for reactive updates
- [ ] Update `HomeViewModel` to use repository
- [ ] Update `ManageAccountsViewModel` to use repository
- [ ] Remove direct SharedPreferences access from ViewModels

### 3.4 Split HomeViewModel
**File:** `presentation/home/HomeViewModel.kt` (535 lines → ~200-250)

- [ ] Extract `AccountBalancesViewModel` or use case
- [ ] Extract `TransactionBreakdownViewModel` or use case
- [ ] Extract `RecentTransactionsViewModel` or use case
- [ ] Extract `SubscriptionsOverviewViewModel` or use case
- [ ] Keep `HomeViewModel` as coordinator

### 3.5 Standardize Loading/Error States
**Pattern:** `isLoading = true` default, separate error StateFlow

- [ ] Audit all ViewModels for loading state defaults
- [ ] Standardize on `isLoading = true` as default
- [ ] Add error handling to `SubscriptionsViewModel`
- [ ] Consider creating `BaseViewModel` with common patterns

---

## Phase 4: UI Layer Cleanup

### 4.1 Consolidate Transaction Item Implementations
**File:** `presentation/home/HomeScreen.kt`

- [ ] Analyze differences between 4 implementations:
  - [ ] `ReferenceTransactionItem` (986-1115)
  - [ ] `ExpenzioTransactionItem` (1390-1470)
  - [ ] `SimpleTransactionItem` (1474-1503)
  - [ ] `TransactionItem` (1579-1662)
- [ ] Create single configurable `TransactionListItem` with parameters:
  - [ ] `showIcon: Boolean`
  - [ ] `showCategory: Boolean`
  - [ ] `showAccount: Boolean`
  - [ ] `compact: Boolean`
- [ ] Replace all usages with new component
- [ ] Delete old implementations

### 4.2 Create Reusable EmptyStateCard
**New File:** `ui/components/states/EmptyStateCard.kt`

- [ ] Create `EmptyStateCard` composable with:
  - [ ] `icon: ImageVector`
  - [ ] `title: String`
  - [ ] `message: String`
  - [ ] `actionButton: @Composable (() -> Unit)?`
- [ ] Replace `ExpenzioEmptyTransactions` in HomeScreen
- [ ] Replace `EmptyTransactionsState` in TransactionsScreen
- [ ] Replace `EmptySubscriptionsState` in SubscriptionsScreen

### 4.3 Create Generic FilterDialog
**New File:** `ui/components/dialogs/FilterDialog.kt`

- [ ] Create `FilterDialog` composable with:
  - [ ] `title: String`
  - [ ] `items: List<T>`
  - [ ] `selectedItems: Set<T>`
  - [ ] `itemLabel: (T) -> String`
  - [ ] `onConfirm: (Set<T>) -> Unit`
  - [ ] `onDismiss: () -> Unit`
- [ ] Replace `MerchantFilterDialog`
- [ ] Replace `AmountFilterDialog` (may need custom variant)
- [ ] Replace `CategoryFilterDialog`
- [ ] Replace `AccountFilterDialog`

### 4.4 Replace Raw Card with PennyWiseCard
**File:** `presentation/home/HomeScreen.kt`

- [ ] Find all raw `Card` usages with manual styling
- [ ] Replace with `PennyWiseCard`
- [ ] Ensure consistent styling across app

### 4.5 Use Design System Dimensions
**Files:** Multiple UI files

- [ ] Create `Dimensions` object with common values if not exists
- [ ] Replace hardcoded `12.dp`, `16.dp`, `8.dp` etc with `Spacing.*`
- [ ] Replace `PaddingValues(bottom = 80.dp)` with named constant
- [ ] Audit `HomeScreen.kt` for hardcoded dimensions

### 4.6 Split HomeScreen
**File:** `presentation/home/HomeScreen.kt` (2104 lines → ~800-1000)

- [ ] Extract dialogs to `ui/components/dialogs/`
- [ ] Extract complex composables to `ui/components/`
- [ ] Remove unused/experimental implementations
- [ ] Keep main screen composition logic

---

## Phase 5: Domain Layer Improvements

### 5.1 Create Missing Use Cases
**Directory:** `domain/usecase/`

Priority order:
- [ ] `GetAnalyticsUseCase`
- [ ] `CalculateMonthlyBreakdownUseCase`
- [ ] `CalculateCategoryBreakdownUseCase`
- [ ] `GroupTransactionsByDateUseCase`
- [ ] `CalculateCashbackUseCase`
- [ ] `ApplyRetroactiveCashbackUseCase`
- [ ] `ExportBackupUseCase`
- [ ] `ImportBackupUseCase`
- [ ] `ManageUnrecognizedSmsUseCase`

### 5.2 Refactor ViewModels to Use Use Cases
**Pattern:** ViewModel → UseCase → Repository

- [ ] `AnalyticsViewModel`: Use `GetAnalyticsUseCase`
- [ ] `SettingsViewModel`: Reduce direct repository injections
- [ ] `UnrecognizedSmsViewModel`: Use `ManageUnrecognizedSmsUseCase`
- [ ] Move business logic from ViewModels to use cases

---

## Phase 6: Entity/Database Cleanup

### 6.1 Create Currency Constants
**New File:** `domain/model/Currency.kt`

- [ ] Create `Currency` object with `DEFAULT = "INR"`
- [ ] Update all entities to use `Currency.DEFAULT`
- [ ] Remove hardcoded "INR" strings

### 6.2 Add Missing updatedAt
**File:** `data/database/entity/AccountBalanceEntity.kt`

- [ ] Add `updatedAt: LocalDateTime` column
- [ ] Create database migration
- [ ] Update repository to set `updatedAt` on updates

### 6.3 Add Foreign Key Constraints (Optional)
**File:** `data/database/entity/CardEntity.kt`

- [ ] Consider adding FK constraint for `accountLast4`
- [ ] Define cascade behavior
- [ ] Create database migration
- [ ] Test cascade deletes

---

## Verification Checklist

After completing refactoring:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual testing of key flows:
  - [ ] Auto-sync transactions
  - [ ] Pending transaction confirmation
  - [ ] Cashback application (all paths)
  - [ ] Account management
  - [ ] Analytics display
- [ ] No memory leaks (LeakCanary)
- [ ] Build successful with no warnings
- [ ] App performance acceptable

---

## Estimated Line Count Impact

| Area | Before | After (Est.) |
|------|--------|--------------|
| Duplicated code | ~1500-2000 | ~200-300 |
| Transaction items | 4 impls | 1 impl |
| HomeScreen.kt | 2104 lines | ~800-1000 |
| HomeViewModel.kt | 535 lines | ~200-250 |
| Workers | 2 | 1 |
| Transaction processing paths | 3 | 1 |

---

## Priority Summary

1. **Critical** (Do first):
   - Memory leak fix
   - Delete duplicate worker
   - Transaction processing unification

2. **High** (Do soon):
   - Database indexes
   - FilterStateManager
   - Transaction item consolidation
   - Split HomeViewModel

3. **Medium** (Do when time permits):
   - CashbackRepository
   - Use case creation
   - UI component extraction
   - HiddenAccountsRepository

4. **Low** (Nice to have):
   - Database VIEW
   - Foreign key constraints
   - Design system dimensions
   - Currency constants
