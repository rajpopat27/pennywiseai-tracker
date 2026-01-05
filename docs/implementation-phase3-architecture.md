# Implementation Phase 3: Architecture Refactoring

## Overview
This phase focuses on core architecture improvements: consolidating duplicate code, creating shared components, and improving data flow patterns.

**Estimated scope:** Refactoring, new abstractions
**Dependencies:** Phase 1 and 2 should be complete

---

## 3.1 Create TransactionProcessor (Critical)

### Task: Unify transaction processing
**Reference:** See `docs/transaction-processing-refactor.md` and `docs/transaction-processing-refactor-todo.md`

**New File:** `app/src/main/java/com/fintrace/app/data/processor/TransactionProcessor.kt`

```kotlin
@Singleton
class TransactionProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    sealed class ProcessResult {
        data class Success(
            val transactionId: Long,
            val cashbackApplied: BigDecimal?
        ) : ProcessResult()
        data class Blocked(val ruleName: String) : ProcessResult()
        data class Duplicate(val reason: String) : ProcessResult()
        data class Error(val message: String) : ProcessResult()
    }

    suspend fun processAndSave(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null,
        skipDuplicateCheck: Boolean = false,
        preserveUserCategory: Boolean = false,
        customCashbackPercent: BigDecimal? = null // For single transaction only
    ): ProcessResult {
        // 1. Duplicate check (if not skipped)
        // 2. Apply merchant mapping (if not preserveUserCategory)
        // 3. Check block rules
        // 4. Evaluate and apply rules
        // 5. Match subscription
        // 6. Apply cashback (custom OR default from account)
        // 7. Insert transaction
        // 8. Update balance
        // 9. Save rule applications
        // 10. Return result
    }
}
```

**Implementation steps:**
- [ ] Create `data/processor/` directory
- [ ] Create `TransactionProcessor.kt`
- [ ] Implement `ProcessResult` sealed class
- [ ] Implement `processAndSave()` method
- [ ] Implement `processBalanceUpdate()` helper
- [ ] Add `@Singleton` and `@Inject` annotations

**Update consumers:**
- [ ] `OptimizedSmsReaderWorker` - use TransactionProcessor for direct save
- [ ] `PendingTransactionManager.confirmTransaction()` - use TransactionProcessor
- [ ] `PendingTransactionManager.autoSaveExpiredTransactions()` - use TransactionProcessor

**Remove duplicated code:**
- [ ] Remove processing logic from `OptimizedSmsReaderWorker`
- [ ] Remove `applyDefaultCashback()` from `PendingTransactionManager`
- [ ] Remove `processBalanceUpdate()` duplicates

---

## 3.2 Create FilterStateManager

### Task: Centralize filter state management
**New File:** `app/src/main/java/com/fintrace/app/presentation/common/FilterStateManager.kt`

```kotlin
class FilterStateManager(
    private val savedStateHandle: SavedStateHandle,
    private val defaultCurrency: String = "INR"
) {
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(defaultCurrency)
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _transactionTypeFilter = MutableStateFlow<TransactionType?>(null)
    val transactionTypeFilter: StateFlow<TransactionType?> = _transactionTypeFilter.asStateFlow()

    private val _customDateRange = savedStateHandle.getStateFlow<Pair<Long, Long>?>(
        "custom_date_range", null
    )
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange

    // Combined filter state for easy observation
    val filterState: StateFlow<FilterState> = combine(
        selectedPeriod,
        selectedCurrency,
        transactionTypeFilter,
        customDateRange
    ) { period, currency, type, dateRange ->
        FilterState(period, currency, type, dateRange)
    }.stateIn(/* scope */)

    fun updatePeriod(period: TimePeriod) { ... }
    fun updateCurrency(currency: String) { ... }
    fun updateTransactionType(type: TransactionType?) { ... }
    fun setCustomDateRange(start: Long, end: Long) { ... }
    fun clearFilters() { ... }
    fun hasActiveFilters(): Boolean { ... }
    fun activeFilterCount(): Int { ... }
}

data class FilterState(
    val period: TimePeriod,
    val currency: String,
    val transactionType: TransactionType?,
    val customDateRange: Pair<Long, Long>?
)
```

**Update ViewModels:**
- [ ] Create `FilterStateManager`
- [ ] Update `TransactionsViewModel` to use it
- [ ] Update `AnalyticsViewModel` to use it
- [ ] Consider: Share instance via Hilt assisted injection or scoped to navigation graph

---

## 3.3 Create HiddenAccountsRepository

### Task: Move SharedPreferences access out of ViewModels
**New File:** `app/src/main/java/com/fintrace/app/data/repository/HiddenAccountsRepository.kt`

```kotlin
interface HiddenAccountsRepository {
    fun getHiddenAccounts(): Flow<Set<String>>
    suspend fun toggleAccountVisibility(bankName: String, accountLast4: String)
    suspend fun isAccountHidden(bankName: String, accountLast4: String): Boolean
    suspend fun hideAccount(bankName: String, accountLast4: String)
    suspend fun showAccount(bankName: String, accountLast4: String)
}

@Singleton
class HiddenAccountsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HiddenAccountsRepository {

    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    private val _hiddenAccounts = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadHiddenAccounts()
    }

    private fun loadHiddenAccounts() {
        val hidden = prefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
        _hiddenAccounts.value = hidden
    }

    override fun getHiddenAccounts(): Flow<Set<String>> = _hiddenAccounts.asStateFlow()

    override suspend fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        val current = _hiddenAccounts.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        saveAndUpdate(current)
    }

    private fun saveAndUpdate(hidden: Set<String>) {
        prefs.edit().putStringSet("hidden_accounts", hidden).apply()
        _hiddenAccounts.value = hidden
    }

    // ... other methods
}
```

**Add DI binding:**
```kotlin
// In AppModule.kt
@Binds
@Singleton
abstract fun bindHiddenAccountsRepository(
    impl: HiddenAccountsRepositoryImpl
): HiddenAccountsRepository
```

**Update ViewModels:**
- [ ] Remove SharedPreferences from `HomeViewModel`
- [ ] Remove SharedPreferences from `ManageAccountsViewModel`
- [ ] Inject `HiddenAccountsRepository` instead
- [ ] Update to use repository methods

---

## 3.4 Create UndoableDeleteDelegate

### Task: Centralize delete with undo pattern
**New File:** `app/src/main/java/com/fintrace/app/presentation/common/UndoableDeleteDelegate.kt`

```kotlin
class UndoableDeleteDelegate<T>(
    private val coroutineScope: CoroutineScope,
    private val onDelete: suspend (T) -> Unit,
    private val onRestore: suspend (T) -> Unit,
    private val undoTimeoutMs: Long = 5000
) {
    private val _deletedItem = MutableStateFlow<T?>(null)
    val deletedItem: StateFlow<T?> = _deletedItem.asStateFlow()

    private var undoJob: Job? = null

    fun delete(item: T) {
        // Cancel any pending undo timeout
        undoJob?.cancel()

        // Store for potential undo
        _deletedItem.value = item

        // Perform delete
        coroutineScope.launch {
            onDelete(item)

            // Start undo timeout
            undoJob = launch {
                delay(undoTimeoutMs)
                _deletedItem.value = null // Clear after timeout
            }
        }
    }

    fun undo() {
        undoJob?.cancel()
        val item = _deletedItem.value ?: return
        _deletedItem.value = null

        coroutineScope.launch {
            onRestore(item)
        }
    }

    fun clearPending() {
        undoJob?.cancel()
        _deletedItem.value = null
    }
}
```

**Update ViewModels:**
- [ ] Update `HomeViewModel` to use delegate
- [ ] Update `TransactionsViewModel` to use delegate
- [ ] Remove duplicated undo logic from both ViewModels

---

## 3.5 Add Missing Database Indexes

### Task: Add indexes for query performance
**File:** `app/src/main/java/com/fintrace/app/data/database/entity/TransactionEntity.kt`

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_hash"], unique = true),
        Index(value = ["merchant_name"]),
        Index(value = ["category"]),
        Index(value = ["bank_name"]),
        Index(value = ["account_number"]),
        Index(value = ["bank_name", "account_number"]), // Composite
        Index(value = ["date_time"]),
        Index(value = ["is_deleted"])
    ]
)
data class TransactionEntity(...)
```

**Implementation:**
- [ ] Add index on `merchant_name`
- [ ] Add index on `category`
- [ ] Add index on `bank_name`
- [ ] Add index on `account_number`
- [ ] Add composite index on `(bank_name, account_number)`
- [ ] Add index on `date_time`
- [ ] Add index on `is_deleted`
- [ ] Increment database version
- [ ] Add migration (Room handles index creation automatically on migration)

---

## 3.6 Consolidate Currency Utils

### Task: Remove duplicate currency utilities
**Files:**
- `utils/CurrencyFormatter.kt` - KEEP
- `utils/CurrencyUtils.kt` - DELETE

**Steps:**
- [ ] Review `CurrencyUtils.kt` for any unique functionality
- [ ] Migrate unique functions to `CurrencyFormatter.kt`
- [ ] Search for all `CurrencyUtils` imports
- [ ] Update imports to use `CurrencyFormatter`
- [ ] Delete `CurrencyUtils.kt`
- [ ] Build and verify

**Ensure `CurrencyFormatter` has:**
```kotlin
object CurrencyFormatter {
    fun format(amount: BigDecimal, currency: String = "INR"): String
    fun formatCompact(amount: BigDecimal, currency: String = "INR"): String
    fun getCurrencySymbol(currency: String): String
    fun sortCurrencies(currencies: List<String>, priorityCurrency: String = "INR"): List<String>
}
```

---

## 3.7 Consolidate Transaction Item UI

### Task: Create single configurable TransactionListItem
**New File:** `app/src/main/java/com/fintrace/app/ui/components/items/TransactionListItem.kt`

```kotlin
@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    config: TransactionItemConfig = TransactionItemConfig()
) {
    // Single implementation that handles all cases
}

data class TransactionItemConfig(
    val showIcon: Boolean = true,
    val showCategory: Boolean = true,
    val showAccount: Boolean = false,
    val showCashback: Boolean = false,
    val showDate: Boolean = true,
    val showTime: Boolean = false,
    val compact: Boolean = false,
    val swipeToDelete: Boolean = false
)
```

**Implementation:**
- [ ] Create `TransactionListItem.kt`
- [ ] Create `TransactionItemConfig` data class
- [ ] Implement unified composable
- [ ] Update HomeScreen to use new component
- [ ] Update TransactionsScreen to use new component
- [ ] Update PendingTransactionsScreen to use new component
- [ ] Delete old implementations:
  - [ ] `ReferenceTransactionItem`
  - [ ] `ExpenzioTransactionItem`
  - [ ] `SimpleTransactionItem`
  - [ ] `TransactionItem`

---

## 3.8 Create Reusable EmptyStateCard

### Task: Consolidate empty state patterns
**New File:** `app/src/main/java/com/fintrace/app/ui/components/states/EmptyStateCard.kt`

```kotlin
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    FintraceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}
```

**Update screens:**
- [ ] Replace `ExpenzioEmptyTransactions` in HomeScreen
- [ ] Replace `EmptyTransactionsState` in TransactionsScreen
- [ ] Replace `EmptySubscriptionsState` in SubscriptionsScreen
- [ ] Delete old implementations

---

## Verification Checklist

After completing Phase 3:

- [ ] TransactionProcessor handles all transaction saving
- [ ] Cashback applied consistently in all flows
- [ ] FilterStateManager used by Transactions and Analytics ViewModels
- [ ] HiddenAccountsRepository replaces SharedPreferences in ViewModels
- [ ] UndoableDeleteDelegate used for delete operations
- [ ] Database has proper indexes
- [ ] Only CurrencyFormatter exists (CurrencyUtils deleted)
- [ ] Single TransactionListItem component
- [ ] Single EmptyStateCard component
- [ ] All tests pass
- [ ] No performance regressions

---

## New Files Created

| File | Purpose |
|------|---------|
| `data/processor/TransactionProcessor.kt` | Unified transaction processing |
| `presentation/common/FilterStateManager.kt` | Shared filter state |
| `data/repository/HiddenAccountsRepository.kt` | Hidden accounts abstraction |
| `presentation/common/UndoableDeleteDelegate.kt` | Delete with undo pattern |
| `ui/components/items/TransactionListItem.kt` | Unified transaction item |
| `ui/components/states/EmptyStateCard.kt` | Reusable empty state |

## Files Deleted

| File | Reason |
|------|--------|
| `utils/CurrencyUtils.kt` | Duplicate of CurrencyFormatter |
| Multiple transaction item composables | Consolidated to one |
| Multiple empty state composables | Consolidated to one |

---

## Next Phase
→ Proceed to `implementation-phase4-budget-feature.md`
