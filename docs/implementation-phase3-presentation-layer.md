# Phase 3: Presentation Layer Architecture

## Overview
Simplify ViewModels by extracting shared patterns, creating reusable delegates, and standardizing state management.

**Goal:** Thin ViewModels that delegate to domain layer, with consistent patterns across the app.

---

## 3.1 FilterStateManager

### 3.1.1 Create FilterStateManager
**New File:** `presentation/common/FilterStateManager.kt`

```kotlin
package com.fintrace.app.presentation.common

import androidx.lifecycle.SavedStateHandle
import com.fintrace.app.data.database.entity.Currency
import com.fintrace.app.data.database.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.time.LocalDate

enum class TimePeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

data class FilterState(
    val period: TimePeriod = TimePeriod.THIS_MONTH,
    val currency: String = Currency.DEFAULT,
    val transactionType: TransactionType? = null,
    val customDateRange: Pair<LocalDate, LocalDate>? = null,
    val merchants: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val accounts: Set<String> = emptySet(),
    val amountRange: Pair<Double, Double>? = null
) {
    val hasActiveFilters: Boolean
        get() = transactionType != null ||
                merchants.isNotEmpty() ||
                categories.isNotEmpty() ||
                accounts.isNotEmpty() ||
                amountRange != null

    val activeFilterCount: Int
        get() = listOf(
            transactionType != null,
            merchants.isNotEmpty(),
            categories.isNotEmpty(),
            accounts.isNotEmpty(),
            amountRange != null
        ).count { it }
}

class FilterStateManager(
    private val savedStateHandle: SavedStateHandle,
    private val scope: CoroutineScope,
    private val defaultCurrency: String = Currency.DEFAULT
) {
    private val _period = MutableStateFlow(
        savedStateHandle.get<Int>("filter_period")?.let { TimePeriod.entries[it] }
            ?: TimePeriod.THIS_MONTH
    )
    val period: StateFlow<TimePeriod> = _period.asStateFlow()

    private val _currency = MutableStateFlow(
        savedStateHandle.get<String>("filter_currency") ?: defaultCurrency
    )
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _transactionType = MutableStateFlow<TransactionType?>(
        savedStateHandle.get<String>("filter_type")?.let { TransactionType.valueOf(it) }
    )
    val transactionType: StateFlow<TransactionType?> = _transactionType.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRange.asStateFlow()

    private val _merchants = MutableStateFlow<Set<String>>(emptySet())
    val merchants: StateFlow<Set<String>> = _merchants.asStateFlow()

    private val _categories = MutableStateFlow<Set<String>>(emptySet())
    val categories: StateFlow<Set<String>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<Set<String>>(emptySet())
    val accounts: StateFlow<Set<String>> = _accounts.asStateFlow()

    private val _amountRange = MutableStateFlow<Pair<Double, Double>?>(null)
    val amountRange: StateFlow<Pair<Double, Double>?> = _amountRange.asStateFlow()

    // Combined state for observers
    val filterState: StateFlow<FilterState> = combine(
        period,
        currency,
        transactionType,
        customDateRange,
        merchants,
        categories,
        accounts,
        amountRange
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        FilterState(
            period = values[0] as TimePeriod,
            currency = values[1] as String,
            transactionType = values[2] as TransactionType?,
            customDateRange = values[3] as Pair<LocalDate, LocalDate>?,
            merchants = values[4] as Set<String>,
            categories = values[5] as Set<String>,
            accounts = values[6] as Set<String>,
            amountRange = values[7] as Pair<Double, Double>?
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), FilterState())

    // Update methods
    fun updatePeriod(newPeriod: TimePeriod) {
        _period.value = newPeriod
        savedStateHandle["filter_period"] = newPeriod.ordinal
        if (newPeriod != TimePeriod.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun updateCurrency(newCurrency: String) {
        _currency.value = newCurrency
        savedStateHandle["filter_currency"] = newCurrency
    }

    fun updateTransactionType(type: TransactionType?) {
        _transactionType.value = type
        savedStateHandle["filter_type"] = type?.name
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        _period.value = TimePeriod.CUSTOM
        _customDateRange.value = Pair(start, end)
    }

    fun updateMerchants(newMerchants: Set<String>) {
        _merchants.value = newMerchants
    }

    fun updateCategories(newCategories: Set<String>) {
        _categories.value = newCategories
    }

    fun updateAccounts(newAccounts: Set<String>) {
        _accounts.value = newAccounts
    }

    fun updateAmountRange(range: Pair<Double, Double>?) {
        _amountRange.value = range
    }

    fun clearAllFilters() {
        _transactionType.value = null
        _merchants.value = emptySet()
        _categories.value = emptySet()
        _accounts.value = emptySet()
        _amountRange.value = null
        savedStateHandle["filter_type"] = null
    }

    fun clearFilter(filterType: FilterType) {
        when (filterType) {
            FilterType.TRANSACTION_TYPE -> _transactionType.value = null
            FilterType.MERCHANTS -> _merchants.value = emptySet()
            FilterType.CATEGORIES -> _categories.value = emptySet()
            FilterType.ACCOUNTS -> _accounts.value = emptySet()
            FilterType.AMOUNT -> _amountRange.value = null
        }
    }

    // Helper to get date range based on period
    fun getDateRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (_period.value) {
            TimePeriod.TODAY -> Pair(now, now)
            TimePeriod.THIS_WEEK -> Pair(now.minusDays(now.dayOfWeek.value.toLong() - 1), now)
            TimePeriod.THIS_MONTH -> Pair(now.withDayOfMonth(1), now)
            TimePeriod.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                Pair(lastMonth.withDayOfMonth(1), lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()))
            }
            TimePeriod.LAST_3_MONTHS -> Pair(now.minusMonths(3), now)
            TimePeriod.LAST_6_MONTHS -> Pair(now.minusMonths(6), now)
            TimePeriod.THIS_YEAR -> Pair(now.withDayOfYear(1), now)
            TimePeriod.ALL_TIME -> Pair(LocalDate.of(2000, 1, 1), now)
            TimePeriod.CUSTOM -> _customDateRange.value ?: Pair(now.withDayOfMonth(1), now)
        }
    }
}

enum class FilterType {
    TRANSACTION_TYPE,
    MERCHANTS,
    CATEGORIES,
    ACCOUNTS,
    AMOUNT
}
```

- [ ] Create `presentation/common/` directory if not exists
- [ ] Create `FilterStateManager.kt`
- [ ] Create `TimePeriod` enum
- [ ] Create `FilterState` data class
- [ ] Create `FilterType` enum
- [ ] Test state persistence with SavedStateHandle

---

## 3.2 UndoableDeleteDelegate

### 3.2.1 Create UndoableDeleteDelegate
**New File:** `presentation/common/UndoableDeleteDelegate.kt`

```kotlin
package com.fintrace.app.presentation.common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Delegate for handling delete-with-undo pattern.
 *
 * Usage:
 * ```
 * class MyViewModel : ViewModel() {
 *     private val deleteDelegate = UndoableDeleteDelegate<MyItem>(
 *         scope = viewModelScope,
 *         onDelete = { item -> repository.delete(item) },
 *         onRestore = { item -> repository.insert(item) }
 *     )
 *
 *     val deletedItem = deleteDelegate.deletedItem
 *
 *     fun delete(item: MyItem) = deleteDelegate.delete(item)
 *     fun undoDelete() = deleteDelegate.undo()
 *     fun clearDeletedItem() = deleteDelegate.clear()
 * }
 * ```
 */
class UndoableDeleteDelegate<T>(
    private val scope: CoroutineScope,
    private val onDelete: suspend (T) -> Unit,
    private val onRestore: suspend (T) -> Unit,
    private val undoTimeoutMs: Long = 5000L,
    private val onTimeout: (() -> Unit)? = null
) {
    private val _deletedItem = MutableStateFlow<T?>(null)
    val deletedItem: StateFlow<T?> = _deletedItem.asStateFlow()

    private var timeoutJob: Job? = null
    private var isPermanentlyDeleted = false

    /**
     * Mark item for deletion. Shows undo option for [undoTimeoutMs].
     * If not undone, deletion becomes permanent.
     */
    fun delete(item: T) {
        // Cancel any previous timeout
        timeoutJob?.cancel()
        isPermanentlyDeleted = false

        // Store item for potential undo
        _deletedItem.value = item

        // Perform soft delete immediately
        scope.launch {
            try {
                onDelete(item)
            } catch (e: Exception) {
                // Restore on failure
                _deletedItem.value = null
                throw e
            }
        }

        // Start timeout for permanent deletion
        timeoutJob = scope.launch {
            delay(undoTimeoutMs)
            if (_deletedItem.value == item) {
                isPermanentlyDeleted = true
                _deletedItem.value = null
                onTimeout?.invoke()
            }
        }
    }

    /**
     * Undo the last deletion if still within timeout.
     * @return true if undo was successful, false if too late
     */
    fun undo(): Boolean {
        timeoutJob?.cancel()

        val item = _deletedItem.value
        if (item == null || isPermanentlyDeleted) {
            return false
        }

        _deletedItem.value = null

        scope.launch {
            onRestore(item)
        }

        return true
    }

    /**
     * Clear the deleted item without restoring.
     * Call this when user dismisses the undo snackbar.
     */
    fun clear() {
        timeoutJob?.cancel()
        isPermanentlyDeleted = true
        _deletedItem.value = null
    }

    /**
     * Check if there's a pending deletion that can be undone.
     */
    fun hasPendingUndo(): Boolean = _deletedItem.value != null && !isPermanentlyDeleted
}
```

- [ ] Create `UndoableDeleteDelegate.kt`
- [ ] Test with sample ViewModel

---

## 3.3 BaseUiState Pattern

### 3.3.1 Create BaseUiState
**New File:** `presentation/common/UiState.kt`

```kotlin
package com.fintrace.app.presentation.common

/**
 * Standard wrapper for screen UI state.
 * All ViewModels should follow this pattern.
 */
data class UiStateWrapper<T>(
    val data: T,
    val isLoading: Boolean = true, // Default to true
    val error: UiError? = null
)

sealed class UiError {
    data class Message(val message: String) : UiError()
    data class Resource(val resId: Int) : UiError()
    data class Exception(val throwable: Throwable) : UiError()
}

/**
 * Extension to create loading state
 */
fun <T> T.toLoadingState() = UiStateWrapper(
    data = this,
    isLoading = true,
    error = null
)

/**
 * Extension to create success state
 */
fun <T> T.toSuccessState() = UiStateWrapper(
    data = this,
    isLoading = false,
    error = null
)

/**
 * Extension to create error state
 */
fun <T> T.toErrorState(error: UiError) = UiStateWrapper(
    data = this,
    isLoading = false,
    error = error
)
```

- [ ] Create `UiState.kt`
- [ ] Define consistent loading/error pattern

---

## 3.4 Update TransactionsViewModel

### 3.4.1 Refactor to use FilterStateManager
**File:** `presentation/transactions/TransactionsViewModel.kt`

```kotlin
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val processTransactionUseCase: ProcessTransactionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Use FilterStateManager instead of individual StateFlows
    val filterStateManager = FilterStateManager(
        savedStateHandle = savedStateHandle,
        scope = viewModelScope
    )

    // Use UndoableDeleteDelegate
    private val deleteDelegate = UndoableDeleteDelegate<TransactionEntity>(
        scope = viewModelScope,
        onDelete = { transaction ->
            transactionRepository.softDelete(transaction.id)
        },
        onRestore = { transaction ->
            transactionRepository.restore(transaction.id)
        }
    )
    val deletedTransaction = deleteDelegate.deletedItem

    // Transactions based on filter state
    val transactions: StateFlow<List<TransactionEntity>> =
        filterStateManager.filterState
            .flatMapLatest { filter ->
                val (startDate, endDate) = filterStateManager.getDateRange()
                transactionRepository.getTransactionsFiltered(
                    startDate = startDate.atStartOfDay(),
                    endDate = endDate.atTime(23, 59, 59),
                    currency = filter.currency,
                    transactionType = filter.transactionType
                ).map { transactions ->
                    // Apply additional filters in memory
                    transactions.filter { tx ->
                        (filter.merchants.isEmpty() || tx.merchantName in filter.merchants) &&
                        (filter.categories.isEmpty() || tx.category in filter.categories) &&
                        (filter.accounts.isEmpty() || "${tx.bankName}_${tx.accountNumber}" in filter.accounts) &&
                        (filter.amountRange == null ||
                            (tx.amount.toDouble() >= filter.amountRange.first &&
                             tx.amount.toDouble() <= filter.amountRange.second))
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simplified public API
    fun deleteTransaction(transaction: TransactionEntity) = deleteDelegate.delete(transaction)
    fun undoDelete() = deleteDelegate.undo()
    fun clearDeletedTransaction() = deleteDelegate.clear()

    // ... rest of ViewModel
}
```

- [ ] Add `FilterStateManager` to TransactionsViewModel
- [ ] Replace individual filter StateFlows
- [ ] Add `UndoableDeleteDelegate`
- [ ] Remove duplicate filter logic
- [ ] Remove duplicate delete/undo logic

---

## 3.5 Update AnalyticsViewModel

### 3.5.1 Refactor to use FilterStateManager
**File:** `ui/screens/analytics/AnalyticsViewModel.kt`

```kotlin
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val cashbackService: CashbackService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Shared filter state
    val filterStateManager = FilterStateManager(
        savedStateHandle = savedStateHandle,
        scope = viewModelScope
    )

    // Tab state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Budget state (from repository)
    val budgetWithSpending: StateFlow<BudgetWithSpending?> =
        budgetRepository.getBudgetWithSpending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Category breakdown based on filters
    val categoryBreakdown: StateFlow<List<CategoryAmount>> =
        filterStateManager.filterState
            .flatMapLatest { filter ->
                val (startDate, endDate) = filterStateManager.getDateRange()
                transactionRepository.getCategoryBreakdown(
                    startDate = startDate.atStartOfDay(),
                    endDate = endDate.atTime(23, 59, 59),
                    currency = filter.currency
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ... rest of analytics-specific state
}
```

- [ ] Add `FilterStateManager` to AnalyticsViewModel
- [ ] Inject `BudgetRepository` and `CashbackService`
- [ ] Remove duplicate filter logic

---

## 3.6 Simplify HomeViewModel

### 3.6.1 Extract Concerns into Delegates/Services
**File:** `presentation/home/HomeViewModel.kt`

**Before:** 535 lines managing 6+ concerns
**After:** ~200-250 lines coordinating delegates

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val hiddenAccountsRepository: HiddenAccountsRepository,
    private val budgetRepository: BudgetRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Delete delegate
    private val deleteDelegate = UndoableDeleteDelegate<TransactionEntity>(
        scope = viewModelScope,
        onDelete = { transactionRepository.softDelete(it.id) },
        onRestore = { transactionRepository.restore(it.id) }
    )
    val deletedTransaction = deleteDelegate.deletedItem

    // Hidden accounts from repository (not SharedPreferences directly)
    val hiddenAccounts: StateFlow<Set<String>> =
        hiddenAccountsRepository.getHiddenAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Account balances (filtered by hidden)
    val visibleAccounts: StateFlow<List<AccountBalanceEntity>> =
        combine(
            accountBalanceRepository.getAllLatestBalances(),
            hiddenAccounts
        ) { accounts, hidden ->
            accounts.filter { account ->
                val key = "${account.bankName}_${account.accountLast4}"
                key !in hidden
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget summary
    val budgetSummary: StateFlow<BudgetWithSpending?> =
        budgetRepository.getBudgetWithSpending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Recent transactions
    val recentTransactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.getRecentTransactions(limit = 10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total balance calculation
    val totalBalance: StateFlow<Map<String, BigDecimal>> =
        visibleAccounts.map { accounts ->
            accounts.groupBy { it.currency }
                .mapValues { (_, accountsInCurrency) ->
                    accountsInCurrency.sumOf { it.balance }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Public methods - delegate to appropriate handler
    fun deleteTransaction(transaction: TransactionEntity) = deleteDelegate.delete(transaction)
    fun undoDelete() = deleteDelegate.undo()
    fun clearDeletedTransaction() = deleteDelegate.clear()

    fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            hiddenAccountsRepository.toggleAccountVisibility(bankName, accountLast4)
        }
    }

    // ... remaining simplified methods
}
```

- [ ] Inject `HiddenAccountsRepository` instead of SharedPreferences
- [ ] Add `UndoableDeleteDelegate`
- [ ] Inject `BudgetRepository`
- [ ] Remove duplicated logic
- [ ] Reduce ViewModel to ~200-250 lines

---

## 3.7 Update ManageAccountsViewModel

### 3.7.1 Use HiddenAccountsRepository
**File:** `presentation/accounts/ManageAccountsViewModel.kt`

```kotlin
@HiltViewModel
class ManageAccountsViewModel @Inject constructor(
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val hiddenAccountsRepository: HiddenAccountsRepository, // NEW
    private val applyRetroactiveCashbackUseCase: ApplyRetroactiveCashbackUseCase // NEW
) : ViewModel() {

    // Remove: private val sharedPrefs = context.getSharedPreferences(...)

    // Use repository instead
    val hiddenAccounts: StateFlow<Set<String>> =
        hiddenAccountsRepository.getHiddenAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            hiddenAccountsRepository.toggleAccountVisibility(bankName, accountLast4)
        }
    }

    fun isAccountHidden(bankName: String, accountLast4: String): Boolean =
        hiddenAccountsRepository.isAccountHidden(bankName, accountLast4)

    // Use cashback use case
    fun updateAccountCashback(bankName: String, accountLast4: String, cashbackPercent: BigDecimal?) {
        viewModelScope.launch {
            try {
                accountBalanceRepository.updateDefaultCashback(bankName, accountLast4, cashbackPercent)

                if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
                    val result = applyRetroactiveCashbackUseCase(
                        bankName = bankName,
                        accountLast4 = accountLast4,
                        cashbackPercent = cashbackPercent
                    )

                    val message = when (result) {
                        is RetroactiveResult.Success ->
                            if (result.updatedCount > 0)
                                "Cashback applied to ${result.updatedCount} transactions"
                            else
                                "Cashback rate updated"
                        is RetroactiveResult.InvalidPercent ->
                            "Invalid cashback percentage"
                    }
                    _uiState.update { it.copy(successMessage = message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // ... rest of ViewModel
}
```

- [ ] Replace SharedPreferences with `HiddenAccountsRepository`
- [ ] Use `ApplyRetroactiveCashbackUseCase`
- [ ] Remove direct context/SharedPreferences access

---

## 3.8 Standardize Error/Loading Patterns

### 3.8.1 Audit and Standardize
**Pattern:** All ViewModels should use `isLoading = true` as default

| ViewModel | Current Default | Target |
|-----------|-----------------|--------|
| HomeViewModel | true | true ✅ |
| TransactionsViewModel | true | true ✅ |
| CategoriesViewModel | false | true |
| SubscriptionsViewModel | true | true ✅ |
| ManageAccountsViewModel | false | true |
| AnalyticsViewModel | varies | true |

- [ ] Update `CategoriesViewModel` to default `isLoading = true`
- [ ] Update `ManageAccountsViewModel` to default `isLoading = true`
- [ ] Ensure all ViewModels have error handling
- [ ] Add error handling to `SubscriptionsViewModel` (currently missing!)

---

## Verification Checklist

After completing Phase 3:

- [ ] `FilterStateManager` used by TransactionsViewModel
- [ ] `FilterStateManager` used by AnalyticsViewModel
- [ ] `UndoableDeleteDelegate` used by HomeViewModel
- [ ] `UndoableDeleteDelegate` used by TransactionsViewModel
- [ ] No direct SharedPreferences in ViewModels
- [ ] `HiddenAccountsRepository` used everywhere
- [ ] HomeViewModel reduced to ~200-250 lines
- [ ] All ViewModels have consistent loading/error patterns
- [ ] Use cases used instead of direct repository access where appropriate
- [ ] All tests pass

---

## New Files Created

| File | Purpose |
|------|---------|
| `presentation/common/FilterStateManager.kt` | Shared filter state |
| `presentation/common/UndoableDeleteDelegate.kt` | Delete with undo |
| `presentation/common/UiState.kt` | Standard UI state pattern |

---

## Files Modified

| File | Changes |
|------|---------|
| `TransactionsViewModel.kt` | Use FilterStateManager, UndoableDeleteDelegate |
| `AnalyticsViewModel.kt` | Use FilterStateManager |
| `HomeViewModel.kt` | Use UndoableDeleteDelegate, HiddenAccountsRepository |
| `ManageAccountsViewModel.kt` | Use HiddenAccountsRepository, use cases |
| `CategoriesViewModel.kt` | Standardize loading state |
| `SubscriptionsViewModel.kt` | Add error handling |

---

## Next Phase
→ Proceed to `implementation-phase4-ui-layer.md`
