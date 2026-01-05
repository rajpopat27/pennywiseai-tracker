package com.fintrace.app.data.repository

import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository interface for budget operations.
 */
interface BudgetRepository {
    /**
     * Get the currently active budget as a Flow.
     */
    fun getActiveBudget(): Flow<BudgetEntity?>

    /**
     * Get the currently active budget (one-shot).
     */
    suspend fun getActiveBudgetOnce(): BudgetEntity?

    /**
     * Set a new monthly budget. Deactivates any existing budget.
     */
    suspend fun setBudget(amount: BigDecimal, currency: String)

    /**
     * Delete (deactivate) the current budget.
     */
    suspend fun deleteBudget()

    /**
     * Get the active budget with current month's spending info.
     */
    fun getBudgetWithSpending(): Flow<BudgetWithSpending?>

    /**
     * Get budget history for the last N months.
     */
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>

    /**
     * Save a snapshot of the budget and spending for a given month.
     * Called at month-end or when viewing history.
     */
    suspend fun saveBudgetSnapshot(month: Int, year: Int)
}

/**
 * Budget with current spending information for display.
 */
data class BudgetWithSpending(
    val budget: BudgetEntity,
    val spentThisMonth: BigDecimal,
    val remaining: BigDecimal,
    val percentUsed: Float
) {
    /**
     * True if spending exceeds the budget amount.
     */
    val isOverBudget: Boolean = remaining < BigDecimal.ZERO

    /**
     * Formatted progress percentage (clamped to 0-100 for progress bars).
     */
    val progressPercent: Float = percentUsed.coerceIn(0f, 100f)
}
