package com.fintrace.app.ui.components.charts

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Data class for daily spending chart data.
 */
data class DailySpending(
    val date: LocalDate,
    val amount: BigDecimal
)

/**
 * Data class for monthly budget comparison.
 */
data class MonthlyBudgetData(
    val month: Int,
    val year: Int,
    val budgetAmount: BigDecimal?,
    val spentAmount: BigDecimal
)

/**
 * Data class for day-of-week spending analysis.
 */
data class DaySpending(
    val dayOfWeek: DayOfWeek,
    val dayName: String,
    val amount: BigDecimal
)

/**
 * Data class for merchant spending summary.
 */
data class MerchantSpending(
    val merchantName: String,
    val totalAmount: BigDecimal,
    val transactionCount: Int,
    val percentage: Float = 0f
)

/**
 * Data class for cashback summary.
 */
data class CashbackSummary(
    val totalCashback: BigDecimal,
    val transactionsWithCashback: Int,
    val averagePercent: Double
)

/**
 * Data class for quick statistics.
 */
data class QuickStats(
    val avgDailySpending: BigDecimal,
    val highestSpendingDay: String,
    val lowestSpendingDay: String,
    val totalTransactions: Int,
    val avgTransactionAmount: BigDecimal
)

/**
 * Data class for category amount breakdown.
 */
data class CategoryAmount(
    val category: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)
