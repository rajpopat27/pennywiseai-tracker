package com.fintrace.app.data.processor

import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.data.repository.AccountBalanceRepository
import com.fintrace.app.data.repository.CardRepository
import com.fintrace.app.data.repository.MerchantMappingRepository
import com.fintrace.app.data.repository.TransactionRepository
import com.fintrace.app.domain.repository.RuleRepository
import com.fintrace.app.domain.service.RuleEngine
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified transaction processing pipeline.
 *
 * This processor handles all transaction flows through a single pipeline:
 * 1. SMS Reader -> Direct save
 * 2. SMS Reader -> Pending -> Confirm
 * 3. Manual transaction entry
 *
 * The pipeline steps are:
 * 1. Duplicate check (optional)
 * 2. Apply merchant mapping (optional)
 * 3. Check block rules
 * 4. Apply transform rules
 * 5. Apply cashback
 * 6. Insert transaction
 * 7. Update balance (if from pending)
 */
@Singleton
class TransactionProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    /**
     * Process and save a transaction through the unified pipeline.
     *
     * @param entity The transaction entity to process
     * @param smsBody Original SMS body (for rule matching)
     * @param pendingEntity If converting from pending (for balance updates)
     * @param config Processing configuration options
     * @return ProcessResult indicating success, blocked, duplicate, or error
     */
    suspend fun processAndSave(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null,
        config: ProcessConfig = ProcessConfig()
    ): ProcessResult {
        try {
            // Step 1: Duplicate Check
            if (!config.skipDuplicateCheck) {
                val existing = transactionRepository.getTransactionByHash(entity.transactionHash)
                if (existing != null) {
                    return ProcessResult.Duplicate(
                        existingTransactionId = existing.id,
                        reason = if (existing.isDeleted) "Previously deleted" else "Already exists"
                    )
                }
            }

            // Step 2: Apply Merchant Mapping (if not preserving user category)
            var processedEntity = entity
            if (!config.preserveUserCategory && entity.merchantName != null) {
                val category = merchantMappingRepository.getCategoryForMerchant(entity.merchantName)
                if (category != null) {
                    processedEntity = processedEntity.copy(category = category)
                }
            }

            // Step 3: Get active rules
            val rules = ruleRepository.getActiveRules()

            // Step 4: Check Block Rules
            val blockingRule = ruleEngine.shouldBlockTransaction(processedEntity, smsBody, rules)
            if (blockingRule != null) {
                return ProcessResult.Blocked(
                    ruleName = blockingRule.name,
                    reason = "Transaction blocked by rule: ${blockingRule.name}"
                )
            }

            // Step 5: Apply Transform Rules
            val (transformedEntity, ruleApplications) = ruleEngine.evaluateRules(
                processedEntity,
                smsBody,
                rules
            )
            processedEntity = transformedEntity

            // Track which rule was applied (for result)
            val appliedRuleName = ruleApplications.firstOrNull()?.ruleName

            // Step 6: Apply Cashback
            val cashbackAmount = applyCashback(processedEntity, config)
            if (cashbackAmount != null) {
                processedEntity = processedEntity.copy(
                    cashbackPercent = cashbackAmount.percent,
                    cashbackAmount = cashbackAmount.amount
                )
            }

            // Step 7: Insert Transaction
            val transactionId = transactionRepository.insertTransaction(processedEntity)

            // Step 8: Update Balance (if from pending)
            if (pendingEntity != null) {
                updateBalanceFromPending(pendingEntity, processedEntity)
            }

            // Step 9: Save Rule Applications (if any)
            if (ruleApplications.isNotEmpty()) {
                for (application in ruleApplications) {
                    ruleRepository.saveRuleApplication(
                        application.copy(transactionId = transactionId.toString())
                    )
                }
            }

            return ProcessResult.Success(
                transactionId = transactionId,
                cashbackAmount = cashbackAmount?.amount,
                subscriptionMatched = false, // TODO: Implement subscription matching
                ruleApplied = appliedRuleName
            )

        } catch (e: Exception) {
            return ProcessResult.Error(
                message = "Failed to process transaction: ${e.message}",
                exception = e
            )
        }
    }

    private data class CashbackInfo(val percent: BigDecimal, val amount: BigDecimal)

    private suspend fun applyCashback(
        entity: TransactionEntity,
        config: ProcessConfig
    ): CashbackInfo? {
        // Only apply to EXPENSE transactions
        if (entity.transactionType != TransactionType.EXPENSE) {
            return null
        }

        // Custom cashback for this transaction only
        if (config.customCashbackPercent != null) {
            val amount = entity.amount * config.customCashbackPercent / BigDecimal(100)
            return CashbackInfo(config.customCashbackPercent, amount)
        }

        // Try to get default cashback from account
        val account = entity.bankName?.let { bankName ->
            entity.accountNumber?.let { accountNumber ->
                accountBalanceRepository.getLatestBalance(bankName, accountNumber)
            }
        }

        val cashbackPercent = account?.defaultCashbackPercent

        return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
            val amount = entity.amount * cashbackPercent / BigDecimal(100)
            CashbackInfo(cashbackPercent, amount)
        } else {
            null
        }
    }

    private suspend fun updateBalanceFromPending(
        pending: PendingTransactionEntity,
        transaction: TransactionEntity
    ) {
        // Use pending entity's bank/account info for balance update
        val bankName = pending.bankName ?: return
        val accountNumber = pending.accountNumber ?: return
        val newBalance = pending.balanceAfter ?: return

        accountBalanceRepository.insertBalanceUpdate(
            bankName = bankName,
            accountLast4 = accountNumber,
            balance = newBalance,
            timestamp = transaction.dateTime,
            smsSource = pending.smsBody,
            sourceType = "TRANSACTION"
        )
    }
}
