package com.fintrace.app.data.database.dao.result

import java.math.BigDecimal

/**
 * Result classes for aggregation queries in TransactionDao.
 * These are used for analytics and reporting.
 */

/**
 * Daily spending aggregation result.
 */
data class DailySpendingResult(
    val date: String,
    val total: BigDecimal
)

/**
 * Spending aggregated by day of week.
 * dayOfWeek: 0 = Sunday, 6 = Saturday (SQLite strftime('%w') format)
 */
data class DayOfWeekSpendingResult(
    val dayOfWeek: Int,
    val total: BigDecimal
)

/**
 * Top merchant by spending amount.
 */
data class TopMerchantResult(
    val merchantName: String,
    val total: BigDecimal,
    val transactionCount: Int
)

/**
 * Cashback aggregated by account.
 */
data class AccountCashbackResult(
    val bankName: String?,
    val accountNumber: String?,
    val total: BigDecimal
)

/**
 * Category spending breakdown.
 */
data class CategorySpendingResult(
    val category: String,
    val total: BigDecimal,
    val transactionCount: Int
)

/**
 * Monthly summary result.
 */
data class MonthlySummaryResult(
    val month: String,
    val income: BigDecimal,
    val expense: BigDecimal
)
