package com.pennywiseai.tracker.presentation.pending

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.PendingTransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.manager.PendingTransactionManager
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

@HiltViewModel
class PendingTransactionsViewModel @Inject constructor(
    private val pendingTransactionManager: PendingTransactionManager,
    private val categoryRepository: CategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PendingTransactionsVM"
    }

    // All pending transactions
    val pendingTransactions: StateFlow<List<PendingTransactionEntity>> =
        pendingTransactionManager.getAllPending()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Pending count for badge
    val pendingCount: StateFlow<Int> =
        pendingTransactionManager.getPendingCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    // Categories for the dropdown
    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.getAllCategories()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Accounts for the dropdown (unique bank + account combinations)
    val accounts: StateFlow<List<AccountBalanceEntity>> =
        accountBalanceRepository.getAllLatestBalances()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Currently selected pending transaction for editing
    private val _selectedPending = MutableStateFlow<PendingTransactionEntity?>(null)
    val selectedPending: StateFlow<PendingTransactionEntity?> = _selectedPending.asStateFlow()

    // Edited state of the selected pending transaction
    private val _editedPending = MutableStateFlow<PendingTransactionEntity?>(null)
    val editedPending: StateFlow<PendingTransactionEntity?> = _editedPending.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estimated cashback based on selected pending transaction and account's default cashback rate
    val estimatedCashback: StateFlow<BigDecimal?> = combine(
        _editedPending,
        accounts
    ) { pending, accountsList ->
        if (pending == null) return@combine null

        // Only calculate for expense/credit transactions
        if (pending.transactionType != TransactionType.EXPENSE &&
            pending.transactionType != TransactionType.CREDIT) {
            return@combine null
        }

        // Find the matching account to get cashback rate
        // Try exact match first
        var matchingAccount = accountsList.find { account ->
            account.bankName == pending.bankName &&
            account.accountLast4 == pending.accountNumber
        }

        // If no exact match and accountNumber is not null, try partial match
        if (matchingAccount == null && pending.accountNumber != null) {
            matchingAccount = accountsList.find { account ->
                account.bankName == pending.bankName &&
                (account.accountLast4.contains(pending.accountNumber!!) ||
                 pending.accountNumber!!.contains(account.accountLast4))
            }
        }

        // If still no match, try bank name only (if single account for that bank)
        if (matchingAccount == null) {
            val bankAccounts = accountsList.filter { it.bankName == pending.bankName }
            if (bankAccounts.size == 1) {
                matchingAccount = bankAccounts.first()
            }
        }

        val cashbackPercent = matchingAccount?.defaultCashbackPercent
        if (cashbackPercent == null || cashbackPercent <= BigDecimal.ZERO) {
            return@combine null
        }

        // Calculate estimated cashback
        pending.amount.multiply(cashbackPercent)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Events
    private val _events = MutableSharedFlow<PendingTransactionEvent>()
    val events = _events.asSharedFlow()

    sealed class PendingTransactionEvent {
        data class TransactionConfirmed(val transactionId: Long) : PendingTransactionEvent()
        data class TransactionRejected(val pendingId: Long) : PendingTransactionEvent()
        data class AllConfirmed(val count: Int) : PendingTransactionEvent()
        data class Error(val message: String) : PendingTransactionEvent()
    }

    /**
     * Selects a pending transaction for editing/confirmation
     */
    fun selectPending(pending: PendingTransactionEntity) {
        _selectedPending.value = pending

        // Try to auto-match an account if accountNumber is missing
        val updatedPending = if (pending.accountNumber == null && pending.bankName != null) {
            val matchingAccounts = accounts.value.filter { it.bankName == pending.bankName }
            if (matchingAccounts.size == 1) {
                // If there's exactly one account for this bank, auto-select it
                pending.copy(accountNumber = matchingAccounts.first().accountLast4)
            } else {
                pending
            }
        } else {
            pending
        }

        _editedPending.value = updatedPending
    }

    /**
     * Clears the selected pending transaction
     */
    fun clearSelection() {
        _selectedPending.value = null
        _editedPending.value = null
    }

    /**
     * Updates a field in the edited pending transaction
     */
    fun updateAmount(amount: BigDecimal) {
        _editedPending.value = _editedPending.value?.copy(amount = amount)
    }

    fun updateMerchant(merchant: String) {
        _editedPending.value = _editedPending.value?.copy(merchantName = merchant)
    }

    fun updateCategory(category: String) {
        _editedPending.value = _editedPending.value?.copy(category = category)
    }

    fun updateTransactionType(type: TransactionType) {
        _editedPending.value = _editedPending.value?.copy(transactionType = type)
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _editedPending.value = _editedPending.value?.copy(dateTime = dateTime)
    }

    fun updateDescription(description: String?) {
        _editedPending.value = _editedPending.value?.copy(description = description)
    }

    fun updateCurrency(currency: String) {
        _editedPending.value = _editedPending.value?.copy(currency = currency)
    }

    fun updateAccount(bankName: String?, accountNumber: String?) {
        _editedPending.value = _editedPending.value?.copy(
            bankName = bankName,
            accountNumber = accountNumber
        )
    }

    /**
     * Confirms the currently selected pending transaction
     */
    fun confirmSelected() {
        val original = _selectedPending.value ?: return
        val edited = _editedPending.value ?: original

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transactionId = pendingTransactionManager.confirmTransaction(original, edited)
                if (transactionId != -1L) {
                    Log.d(TAG, "Transaction confirmed: $transactionId")
                    _events.emit(PendingTransactionEvent.TransactionConfirmed(transactionId))
                    clearSelection()
                } else {
                    _events.emit(PendingTransactionEvent.Error("Failed to confirm transaction"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming transaction", e)
                _events.emit(PendingTransactionEvent.Error(e.message ?: "Unknown error"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Confirms a specific pending transaction directly (without editing)
     */
    fun confirmPending(pending: PendingTransactionEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transactionId = pendingTransactionManager.confirmTransaction(pending)
                if (transactionId != -1L) {
                    Log.d(TAG, "Transaction confirmed: $transactionId")
                    _events.emit(PendingTransactionEvent.TransactionConfirmed(transactionId))
                } else {
                    _events.emit(PendingTransactionEvent.Error("Failed to confirm transaction"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming transaction", e)
                _events.emit(PendingTransactionEvent.Error(e.message ?: "Unknown error"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rejects the currently selected pending transaction
     */
    fun rejectSelected() {
        val pending = _selectedPending.value ?: return
        rejectPending(pending.id)
        clearSelection()
    }

    /**
     * Rejects a pending transaction by ID
     */
    fun rejectPending(pendingId: Long) {
        viewModelScope.launch {
            try {
                pendingTransactionManager.rejectTransaction(pendingId)
                Log.d(TAG, "Transaction rejected: $pendingId")
                _events.emit(PendingTransactionEvent.TransactionRejected(pendingId))
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting transaction", e)
                _events.emit(PendingTransactionEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Confirms all pending transactions
     */
    fun confirmAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val count = pendingTransactionManager.confirmAllPending()
                Log.d(TAG, "Confirmed all: $count transactions")
                _events.emit(PendingTransactionEvent.AllConfirmed(count))
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming all transactions", e)
                _events.emit(PendingTransactionEvent.Error(e.message ?: "Unknown error"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads a pending transaction by ID (used when coming from notification)
     */
    fun loadPendingById(id: Long) {
        viewModelScope.launch {
            try {
                val pending = pendingTransactionManager.getPendingById(id)
                if (pending != null) {
                    selectPending(pending)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pending transaction", e)
            }
        }
    }

    /**
     * Gets the current cashback rate for the selected account
     */
    val currentAccountCashback: StateFlow<BigDecimal?> = combine(
        _editedPending,
        accounts
    ) { pending, accountsList ->
        if (pending == null) return@combine null

        // Find the matching account
        val matchingAccount = findMatchingAccount(pending, accountsList)
        matchingAccount?.defaultCashbackPercent
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Updates the cashback rate for the currently selected account.
     * Also applies the cashback retroactively to existing transactions
     * that don't already have cashback set.
     */
    fun updateAccountCashback(cashbackPercent: BigDecimal?) {
        val pending = _editedPending.value ?: return
        val bankName = pending.bankName ?: return
        val accountNumber = pending.accountNumber ?: return

        viewModelScope.launch {
            try {
                // Update the account's default cashback
                accountBalanceRepository.updateDefaultCashback(bankName, accountNumber, cashbackPercent)
                Log.d(TAG, "Updated cashback for $bankName $accountNumber to $cashbackPercent%")

                // Apply retroactively to existing transactions that don't have cashback set
                if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
                    val updatedCount = transactionRepository.applyRetroactiveCashback(
                        bankName,
                        accountNumber,
                        cashbackPercent.toDouble()
                    )
                    if (updatedCount > 0) {
                        Log.d(TAG, "Applied $cashbackPercent% cashback retroactively to $updatedCount transactions")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating cashback", e)
            }
        }
    }

    /**
     * Helper function to find matching account for a pending transaction
     */
    private fun findMatchingAccount(
        pending: PendingTransactionEntity,
        accountsList: List<AccountBalanceEntity>
    ): AccountBalanceEntity? {
        // Try exact match first
        var matchingAccount = accountsList.find { account ->
            account.bankName == pending.bankName &&
            account.accountLast4 == pending.accountNumber
        }

        // If no exact match and accountNumber is not null, try partial match
        if (matchingAccount == null && pending.accountNumber != null) {
            matchingAccount = accountsList.find { account ->
                account.bankName == pending.bankName &&
                (account.accountLast4.contains(pending.accountNumber!!) ||
                 pending.accountNumber!!.contains(account.accountLast4))
            }
        }

        // If still no match, try bank name only (if single account for that bank)
        if (matchingAccount == null) {
            val bankAccounts = accountsList.filter { it.bankName == pending.bankName }
            if (bankAccounts.size == 1) {
                matchingAccount = bankAccounts.first()
            }
        }

        return matchingAccount
    }

    /**
     * Creates a new account from the pending transaction confirmation dialog.
     * Also applies cashback retroactively to existing transactions for this account.
     */
    fun createAccount(
        bankName: String,
        accountLast4: String,
        balance: BigDecimal,
        isCreditCard: Boolean,
        creditLimit: BigDecimal?,
        cashbackPercent: BigDecimal?
    ) {
        viewModelScope.launch {
            try {
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = bankName,
                        accountLast4 = accountLast4,
                        balance = balance,
                        creditLimit = creditLimit,
                        timestamp = LocalDateTime.now(),
                        isCreditCard = isCreditCard,
                        sourceType = "MANUAL",
                        defaultCashbackPercent = cashbackPercent
                    )
                )
                Log.d(TAG, "Created new account: $bankName ••$accountLast4")

                // Apply retroactive cashback to existing transactions for this account
                if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
                    val updatedCount = transactionRepository.applyRetroactiveCashback(
                        bankName,
                        accountLast4,
                        cashbackPercent.toDouble()
                    )
                    if (updatedCount > 0) {
                        Log.d(TAG, "Applied $cashbackPercent% cashback retroactively to $updatedCount transactions")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating account", e)
                _events.emit(PendingTransactionEvent.Error("Failed to create account: ${e.message}"))
            }
        }
    }
}
