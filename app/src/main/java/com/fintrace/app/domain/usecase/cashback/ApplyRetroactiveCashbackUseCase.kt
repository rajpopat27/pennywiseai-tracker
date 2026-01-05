package com.fintrace.app.domain.usecase.cashback

import com.fintrace.app.domain.service.CashbackService
import com.fintrace.app.domain.service.RetroactiveResult
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case for applying retroactive cashback to all transactions for an account.
 *
 * This is typically called from Manage Accounts when the user sets or updates
 * the default cashback percentage for an account.
 */
class ApplyRetroactiveCashbackUseCase @Inject constructor(
    private val cashbackService: CashbackService
) {
    /**
     * Apply retroactive cashback to all past transactions for an account.
     *
     * @param bankName The bank name
     * @param accountLast4 The last 4 digits of the account number
     * @param cashbackPercent The cashback percentage to apply
     * @return RetroactiveResult indicating success or failure
     */
    suspend operator fun invoke(
        bankName: String,
        accountLast4: String,
        cashbackPercent: BigDecimal
    ): RetroactiveResult {
        return cashbackService.applyRetroactiveCashback(
            bankName = bankName,
            accountLast4 = accountLast4,
            cashbackPercent = cashbackPercent
        )
    }
}
