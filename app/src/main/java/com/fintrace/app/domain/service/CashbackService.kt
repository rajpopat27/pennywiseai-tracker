package com.fintrace.app.domain.service

import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.data.repository.AccountBalanceRepository
import com.fintrace.app.data.repository.CardRepository
import com.fintrace.app.data.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for cashback-related operations.
 */
@Singleton
class CashbackService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    /**
     * Calculate cashback for a single transaction based on account/card settings.
     */
    suspend fun calculateCashback(
        transaction: TransactionEntity
    ): CashbackResult {
        if (transaction.transactionType != TransactionType.EXPENSE) {
            return CashbackResult.NotApplicable("Only EXPENSE transactions earn cashback")
        }

        val account = transaction.bankName?.let { bankName ->
            transaction.accountNumber?.let { accountNumber ->
                accountBalanceRepository.getLatestBalance(bankName, accountNumber)
            }
        }

        val cashbackPercent = account?.defaultCashbackPercent

        return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
            val amount = transaction.amount * cashbackPercent / BigDecimal(100)
            CashbackResult.Calculated(cashbackPercent, amount)
        } else {
            CashbackResult.NoCashbackConfigured
        }
    }

    /**
     * Apply retroactive cashback to all transactions for an account.
     * Only updates transactions that don't already have cashback.
     */
    suspend fun applyRetroactiveCashback(
        bankName: String,
        accountLast4: String,
        cashbackPercent: BigDecimal
    ): RetroactiveResult {
        if (cashbackPercent <= BigDecimal.ZERO) {
            return RetroactiveResult.InvalidPercent
        }

        val updatedCount = transactionRepository.applyRetroactiveCashback(
            bankName = bankName,
            accountLast4 = accountLast4,
            cashbackPercent = cashbackPercent.toDouble()
        )

        return RetroactiveResult.Success(updatedCount)
    }
}

/**
 * Result of a cashback calculation.
 */
sealed class CashbackResult {
    /**
     * Cashback was calculated successfully.
     */
    data class Calculated(val percent: BigDecimal, val amount: BigDecimal) : CashbackResult()

    /**
     * No cashback is configured for this account/card.
     */
    data object NoCashbackConfigured : CashbackResult()

    /**
     * Cashback is not applicable (e.g., not an EXPENSE transaction).
     */
    data class NotApplicable(val reason: String) : CashbackResult()
}

/**
 * Result of applying retroactive cashback.
 */
sealed class RetroactiveResult {
    /**
     * Successfully updated transactions with retroactive cashback.
     */
    data class Success(val updatedCount: Int) : RetroactiveResult()

    /**
     * The provided cashback percent was invalid (<=0).
     */
    data object InvalidPercent : RetroactiveResult()
}
