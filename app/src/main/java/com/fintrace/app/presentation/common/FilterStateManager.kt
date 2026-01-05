package com.fintrace.app.presentation.common

import androidx.lifecycle.SavedStateHandle
import com.fintrace.app.data.database.entity.Currency
import com.fintrace.app.data.database.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Combined filter state for transaction filtering.
 */
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
    /**
     * True if any filters beyond period and currency are active.
     */
    val hasActiveFilters: Boolean
        get() = transactionType != null ||
                merchants.isNotEmpty() ||
                categories.isNotEmpty() ||
                accounts.isNotEmpty() ||
                amountRange != null

    /**
     * Count of active filters.
     */
    val activeFilterCount: Int
        get() = listOf(
            transactionType != null,
            merchants.isNotEmpty(),
            categories.isNotEmpty(),
            accounts.isNotEmpty(),
            amountRange != null
        ).count { it }
}

/**
 * Types of filters that can be cleared individually.
 */
enum class FilterType {
    TRANSACTION_TYPE,
    MERCHANTS,
    CATEGORIES,
    ACCOUNTS,
    AMOUNT
}

/**
 * Manages filter state with SavedStateHandle persistence.
 *
 * Usage:
 * ```
 * class MyViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *     val filterStateManager = FilterStateManager(savedStateHandle, viewModelScope)
 *
 *     init {
 *         filterStateManager.filterState.collect { state ->
 *             // React to filter changes
 *         }
 *     }
 * }
 * ```
 */
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

    /**
     * Combined state for observers.
     */
    val filterState: StateFlow<FilterState> = combine(
        period,
        currency,
        transactionType,
        customDateRange
    ) { period, currency, type, dateRange ->
        FilterState(
            period = period,
            currency = currency,
            transactionType = type,
            customDateRange = dateRange,
            merchants = _merchants.value,
            categories = _categories.value,
            accounts = _accounts.value,
            amountRange = _amountRange.value
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
            FilterType.TRANSACTION_TYPE -> {
                _transactionType.value = null
                savedStateHandle["filter_type"] = null
            }
            FilterType.MERCHANTS -> _merchants.value = emptySet()
            FilterType.CATEGORIES -> _categories.value = emptySet()
            FilterType.ACCOUNTS -> _accounts.value = emptySet()
            FilterType.AMOUNT -> _amountRange.value = null
        }
    }

    /**
     * Get the date range based on the current period.
     */
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
