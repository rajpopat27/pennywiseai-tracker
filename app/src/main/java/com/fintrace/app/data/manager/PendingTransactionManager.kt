package com.fintrace.app.data.manager

import android.app.Application
import android.util.Log
import com.fintrace.parser.core.ParsedTransaction
import com.fintrace.app.FintraceApplication
import com.fintrace.app.data.database.entity.AccountBalanceEntity
import com.fintrace.app.data.database.entity.CardType
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.PendingTransactionStatus
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.data.mapper.toPendingEntity
import com.fintrace.app.data.mapper.toTransactionEntity
import com.fintrace.app.data.preferences.UserPreferencesRepository
import com.fintrace.app.data.repository.AccountBalanceRepository
import com.fintrace.app.data.repository.CardRepository
import com.fintrace.app.data.repository.MerchantMappingRepository
import com.fintrace.app.data.repository.PendingTransactionRepository
import com.fintrace.app.data.repository.SubscriptionRepository
import com.fintrace.app.data.repository.TransactionRepository
import com.fintrace.app.domain.repository.RuleRepository
import com.fintrace.app.domain.service.RuleEngine
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages pending transactions that require user confirmation before saving.
 *
 * When transaction confirmation is enabled, parsed SMS transactions are stored
 * in a pending state and users must review/confirm before they're saved to
 * the main transactions table.
 */
@Singleton
class PendingTransactionManager @Inject constructor(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
) {
    private val pennyWiseApplication = application as FintraceApplication

    companion object {
        private const val TAG = "PendingTransactionManager"
    }

    /**
     * SharedFlow to emit pending transactions when app is in foreground.
     * UI components observe this to show confirmation dialog.
     */
    private val _pendingTransactionFlow = MutableSharedFlow<PendingTransactionEntity>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pendingTransactionFlow: SharedFlow<PendingTransactionEntity> = _pendingTransactionFlow.asSharedFlow()

    /**
     * Flow of pending transactions count for badge display
     */
    fun getPendingCount(): Flow<Int> = pendingTransactionRepository.getPendingCount()

    /**
     * Flow of all pending transactions
     */
    fun getAllPending(): Flow<List<PendingTransactionEntity>> = pendingTransactionRepository.getAllPending()

    /**
     * Check if transaction confirmation feature is enabled
     */
    suspend fun isConfirmationEnabled(): Boolean {
        return userPreferencesRepository.getTransactionConfirmationEnabled()
    }

    /**
     * Check if the app is currently in the foreground
     */
    fun isAppInForeground(): Boolean = pennyWiseApplication.isAppInForeground

    /**
     * Result of adding a pending transaction
     */
    sealed class AddPendingResult {
        data class ShowDialog(val pending: PendingTransactionEntity) : AddPendingResult()
        data class SavedAsPending(val id: Long, val pending: PendingTransactionEntity) : AddPendingResult()
        data class DirectSaved(val transactionId: Long) : AddPendingResult()
        data class Duplicate(val reason: String) : AddPendingResult()
        data class Error(val reason: String) : AddPendingResult()
    }

    /**
     * Adds a parsed transaction to the pending queue.
     *
     * @param parsedTransaction The parsed transaction from SMS
     * @return Result indicating whether to show dialog, saved as pending, or error
     */
    suspend fun addPendingTransaction(parsedTransaction: ParsedTransaction): AddPendingResult {
        return try {
            val pendingEntity = parsedTransaction.toPendingEntity()

            // Check for existing transaction (duplicate check)
            val existingTransaction = transactionRepository.getTransactionByHash(pendingEntity.transactionHash)
            if (existingTransaction != null) {
                if (existingTransaction.isDeleted) {
                    Log.d(TAG, "Skipping previously deleted transaction")
                    return AddPendingResult.Duplicate("Transaction was previously deleted")
                }
                Log.d(TAG, "Transaction already exists")
                return AddPendingResult.Duplicate("Duplicate transaction")
            }

            // Check if already pending
            val existingPending = pendingTransactionRepository.getByHash(pendingEntity.transactionHash)
            if (existingPending != null && existingPending.status == PendingTransactionStatus.PENDING) {
                Log.d(TAG, "Transaction already pending")
                return AddPendingResult.Duplicate("Already pending review")
            }

            // Apply merchant mapping to get better category
            val customCategory = merchantMappingRepository.getCategoryForMerchant(pendingEntity.merchantName)
            val entityWithMapping = if (customCategory != null) {
                pendingEntity.copy(category = customCategory)
            } else {
                pendingEntity
            }

            val isAppInForeground = pennyWiseApplication.isAppInForeground

            // Save to pending table
            val pendingId = pendingTransactionRepository.insert(entityWithMapping)
            if (pendingId == -1L) {
                Log.e(TAG, "Failed to insert pending transaction")
                return AddPendingResult.Error("Failed to save pending transaction")
            }

            val savedPending = entityWithMapping.copy(id = pendingId)
            Log.d(TAG, "Saved pending transaction with ID: $pendingId, foreground: $isAppInForeground")

            if (isAppInForeground) {
                // Emit to flow for immediate dialog display
                _pendingTransactionFlow.emit(savedPending)
                AddPendingResult.ShowDialog(savedPending)
            } else {
                // Return saved pending for notification display
                AddPendingResult.SavedAsPending(pendingId, savedPending)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding pending transaction", e)
            AddPendingResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Confirms a pending transaction, saving it to the main transactions table.
     * Applies merchant mapping, rules, and subscription matching like auto-save.
     *
     * @param pending The pending transaction to confirm
     * @param editedEntity Optional edited version (if user made changes)
     * @return The ID of the saved transaction, or -1 if failed
     */
    suspend fun confirmTransaction(
        pending: PendingTransactionEntity,
        editedEntity: PendingTransactionEntity = pending
    ): Long {
        return try {
            // Convert to TransactionEntity
            val transactionEntity = editedEntity.toTransactionEntity()

            // Apply merchant mapping (if user didn't change the category)
            val customCategory = merchantMappingRepository.getCategoryForMerchant(editedEntity.merchantName)
            val entityWithMapping = if (customCategory != null && editedEntity.category == pending.category) {
                Log.d(TAG, "Found custom category mapping: ${editedEntity.merchantName} -> $customCategory")
                transactionEntity.copy(category = customCategory)
            } else {
                transactionEntity
            }

            // Apply rules
            val activeRules = ruleRepository.getActiveRulesByType(entityWithMapping.transactionType)

            // Check if this transaction should be blocked by a rule
            val blockingRule = ruleEngine.shouldBlockTransaction(
                entityWithMapping,
                editedEntity.smsBody ?: "",
                activeRules
            )

            if (blockingRule != null) {
                Log.d(TAG, "Transaction blocked by rule: ${blockingRule.name}")
                // Reject the pending transaction since it's blocked
                pendingTransactionRepository.reject(pending.id)
                return -1L
            }

            val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                entityWithMapping,
                editedEntity.smsBody ?: "",
                activeRules
            )

            // Check if this transaction matches an active subscription
            val matchedSubscription = subscriptionRepository.matchTransactionToSubscription(
                entityWithRules.merchantName,
                entityWithRules.amount
            )

            val finalEntity = if (matchedSubscription != null) {
                Log.d(TAG, "Transaction matched to active subscription: ${matchedSubscription.merchantName}")
                subscriptionRepository.updateNextPaymentDateAfterCharge(
                    matchedSubscription.id,
                    entityWithRules.dateTime.toLocalDate()
                )
                entityWithRules.copy(isRecurring = true)
            } else {
                entityWithRules
            }

            // Apply cashback for expense/credit transactions
            val entityWithCashback = applyDefaultCashback(finalEntity, editedEntity)

            // Save to main transactions table
            val transactionId = pendingTransactionRepository.confirm(pending, entityWithCashback)

            if (transactionId != -1L) {
                Log.d(TAG, "Confirmed transaction with ID: $transactionId${if (entityWithCashback.isRecurring) " (Recurring)" else ""}${if (entityWithCashback.cashbackAmount != null && entityWithCashback.cashbackAmount > BigDecimal.ZERO) " (Cashback: ${entityWithCashback.cashbackAmount})" else ""}")
                // Save rule applications if any
                if (ruleApplications.isNotEmpty()) {
                    ruleRepository.saveRuleApplications(ruleApplications)
                }

                // Process balance update (create/update account)
                processBalanceUpdate(editedEntity, entityWithCashback, transactionId)
            }

            transactionId
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming transaction", e)
            -1L
        }
    }

    /**
     * Quick confirms a pending transaction using parsed values with mappings applied.
     * Used for "Quick Confirm" action from notification.
     */
    suspend fun quickConfirmTransaction(pendingId: Long): Long {
        val pending = pendingTransactionRepository.getById(pendingId)
            ?: return -1L

        return confirmTransaction(pending)
    }

    /**
     * Rejects a pending transaction (user dismissed it).
     */
    suspend fun rejectTransaction(pendingId: Long) {
        pendingTransactionRepository.reject(pendingId)
        Log.d(TAG, "Rejected pending transaction: $pendingId")
    }

    /**
     * Gets a pending transaction by ID.
     */
    suspend fun getPendingById(id: Long): PendingTransactionEntity? {
        return pendingTransactionRepository.getById(id)
    }

    /**
     * Confirms all pending transactions at once.
     *
     * @return Number of successfully confirmed transactions
     */
    suspend fun confirmAllPending(): Int {
        val pendingList = pendingTransactionRepository.getAllPendingList()
        var confirmedCount = 0

        pendingList.forEach { pending ->
            val id = confirmTransaction(pending)
            if (id != -1L) {
                confirmedCount++
            }
        }

        Log.d(TAG, "Confirmed $confirmedCount of ${pendingList.size} pending transactions")
        return confirmedCount
    }

    /**
     * Auto-saves expired pending transactions.
     * Called by the auto-save worker.
     * Applies the same processing as confirmTransaction: merchant mapping, rules, subscription matching.
     */
    suspend fun autoSaveExpiredTransactions(): Int {
        val expired = pendingTransactionRepository.getExpiredPending()
        var savedCount = 0

        expired.forEach { pending ->
            try {
                val transactionEntity = pending.toTransactionEntity()

                // Apply merchant mapping
                val customCategory = merchantMappingRepository.getCategoryForMerchant(pending.merchantName)
                val entityWithMapping = if (customCategory != null) {
                    transactionEntity.copy(category = customCategory)
                } else {
                    transactionEntity
                }

                // Apply rules
                val activeRules = ruleRepository.getActiveRulesByType(entityWithMapping.transactionType)

                // Check if this transaction should be blocked by a rule
                val blockingRule = ruleEngine.shouldBlockTransaction(
                    entityWithMapping,
                    pending.smsBody ?: "",
                    activeRules
                )

                if (blockingRule != null) {
                    Log.d(TAG, "Expired transaction blocked by rule: ${blockingRule.name}")
                    pendingTransactionRepository.reject(pending.id)
                    return@forEach
                }

                val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                    entityWithMapping,
                    pending.smsBody ?: "",
                    activeRules
                )

                // Check if this transaction matches an active subscription
                val matchedSubscription = subscriptionRepository.matchTransactionToSubscription(
                    entityWithRules.merchantName,
                    entityWithRules.amount
                )

                val finalEntity = if (matchedSubscription != null) {
                    Log.d(TAG, "Expired transaction matched to subscription: ${matchedSubscription.merchantName}")
                    subscriptionRepository.updateNextPaymentDateAfterCharge(
                        matchedSubscription.id,
                        entityWithRules.dateTime.toLocalDate()
                    )
                    entityWithRules.copy(isRecurring = true)
                } else {
                    entityWithRules
                }

                val transactionId = transactionRepository.insertTransaction(finalEntity)
                if (transactionId != -1L) {
                    pendingTransactionRepository.markAutoSaved(pending.id)
                    savedCount++
                    if (ruleApplications.isNotEmpty()) {
                        ruleRepository.saveRuleApplications(ruleApplications)
                    }

                    // Process balance update (create/update account)
                    processBalanceUpdate(pending, finalEntity, transactionId)
                    Log.d(TAG, "Auto-saved transaction $transactionId${if (finalEntity.isRecurring) " (Recurring)" else ""}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-saving expired transaction ${pending.id}", e)
            }
        }

        Log.d(TAG, "Auto-saved $savedCount expired transactions")
        return savedCount
    }

    /**
     * Processes balance update when confirming a pending transaction.
     * Creates or updates account balance records similar to SMS transaction processing.
     */
    private suspend fun processBalanceUpdate(
        pending: PendingTransactionEntity,
        entity: TransactionEntity,
        transactionId: Long
    ) {
        val accountLast4 = pending.accountNumber ?: return
        val bankName = pending.bankName ?: return

        try {
            // Check if this is a card transaction
            val existingCard = cardRepository.getCard(bankName, accountLast4)
            val isCreditCard = entity.transactionType == TransactionType.EXPENSE ||
                    existingCard?.cardType == CardType.CREDIT

            // Create card if doesn't exist
            if (existingCard == null) {
                cardRepository.findOrCreateCard(
                    cardLast4 = accountLast4,
                    bankName = bankName,
                    isCredit = isCreditCard
                )
            }

            // Get the target account for balance update
            val targetAccountLast4 = if (existingCard?.cardType == CardType.DEBIT && existingCard.accountLast4 != null) {
                existingCard.accountLast4
            } else {
                accountLast4
            }

            // Get existing account balance
            val existingAccount = accountBalanceRepository.getLatestBalance(bankName, targetAccountLast4)

            // Calculate new balance based on transaction type and existing balance
            val newBalance = when {
                pending.balanceAfter != null -> pending.balanceAfter
                isCreditCard -> {
                    val currentBalance = existingAccount?.balance ?: BigDecimal.ZERO
                    currentBalance + pending.amount
                }
                existingAccount?.isCreditCard == true && entity.transactionType == TransactionType.INCOME -> {
                    val currentBalance = existingAccount.balance
                    (currentBalance - pending.amount).max(BigDecimal.ZERO)
                }
                else -> {
                    val currentBalance = existingAccount?.balance ?: BigDecimal.ZERO
                    when (entity.transactionType) {
                        TransactionType.INCOME -> currentBalance + pending.amount
                        TransactionType.EXPENSE, TransactionType.EXPENSE ->
                            (currentBalance - pending.amount).max(BigDecimal.ZERO)
                        TransactionType.EXPENSE, TransactionType.TRANSFER -> currentBalance
                    }
                }
            }

            val balanceEntity = AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = targetAccountLast4,
                balance = newBalance,
                timestamp = entity.dateTime,
                transactionId = transactionId,
                creditLimit = existingAccount?.creditLimit,
                isCreditCard = isCreditCard || (existingAccount?.isCreditCard ?: false),
                smsSource = pending.smsBody?.take(500),
                sourceType = "TRANSACTION",
                currency = pending.currency
            )

            accountBalanceRepository.insertBalance(balanceEntity)
            Log.d(TAG, "Saved balance update for $bankName **$targetAccountLast4")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing balance update", e)
        }
    }

    /**
     * Applies default cashback rate from the account to the transaction.
     * Only applies to EXPENSE and CREDIT transaction types.
     */
    private suspend fun applyDefaultCashback(
        entity: TransactionEntity,
        pending: PendingTransactionEntity
    ): TransactionEntity {
        // Only calculate cashback for expense/credit transactions
        if (entity.transactionType != TransactionType.EXPENSE &&
            entity.transactionType != TransactionType.EXPENSE) {
            return entity
        }

        val bankName = pending.bankName ?: return entity
        val accountLast4 = pending.accountNumber ?: return entity

        // Get cashback rate from account
        val cashbackPercent = accountBalanceRepository.getDefaultCashback(bankName, accountLast4)

        // If we have a cashback rate, apply it
        return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
            val cashbackAmount = entity.amount.multiply(cashbackPercent)
                .divide(BigDecimal(100), 2, java.math.RoundingMode.HALF_UP)
            Log.d(TAG, "Applied ${cashbackPercent}% cashback: $cashbackAmount")
            entity.copy(
                cashbackPercent = cashbackPercent,
                cashbackAmount = cashbackAmount
            )
        } else {
            entity
        }
    }

    /**
     * Cleans up old processed pending transactions.
     */
    suspend fun cleanup() {
        pendingTransactionRepository.cleanupOldProcessed()
    }
}
