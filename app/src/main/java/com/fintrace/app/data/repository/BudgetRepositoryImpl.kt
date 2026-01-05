package com.fintrace.app.data.repository

import com.fintrace.app.data.database.dao.BudgetDao
import com.fintrace.app.data.database.dao.TransactionDao
import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import com.fintrace.app.data.database.entity.Currency
import com.fintrace.app.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BudgetRepository.
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getActiveBudget(): Flow<BudgetEntity?> =
        budgetDao.getActiveBudget()

    override suspend fun getActiveBudgetOnce(): BudgetEntity? =
        budgetDao.getActiveBudgetOnce()

    override suspend fun setBudget(amount: BigDecimal, currency: String) {
        // Deactivate any existing budgets first
        budgetDao.deactivateAllBudgets()

        // Insert new active budget
        budgetDao.insertBudget(
            BudgetEntity(
                amount = amount,
                currency = currency,
                isActive = true
            )
        )
    }

    override suspend fun deleteBudget() {
        budgetDao.deactivateAllBudgets()
    }

    override fun getBudgetWithSpending(): Flow<BudgetWithSpending?> {
        return budgetDao.getActiveBudget().map { budget ->
            budget?.let { calculateBudgetWithSpending(it) }
        }
    }

    private suspend fun calculateBudgetWithSpending(budget: BudgetEntity): BudgetWithSpending {
        val now = LocalDateTime.now()
        val startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
            .toLocalDate().atTime(23, 59, 59)

        // Get total expenses for the current month
        val spent = transactionDao.getTotalAmountByTypeAndPeriod(
            type = TransactionType.EXPENSE,
            startDate = startOfMonth,
            endDate = endOfMonth
        )?.toBigDecimal() ?: BigDecimal.ZERO

        val remaining = budget.amount - spent
        val percentUsed = if (budget.amount > BigDecimal.ZERO) {
            (spent.toFloat() / budget.amount.toFloat() * 100)
        } else 0f

        return BudgetWithSpending(
            budget = budget,
            spentThisMonth = spent,
            remaining = remaining,
            percentUsed = percentUsed
        )
    }

    override fun getBudgetHistory(limit: Int): Flow<List<BudgetHistoryEntity>> =
        budgetDao.getBudgetHistory(limit)

    override suspend fun saveBudgetSnapshot(month: Int, year: Int) {
        val budget = getActiveBudgetOnce() ?: return

        val startOfMonth = LocalDateTime.of(year, month, 1, 0, 0)
        val daysInMonth = startOfMonth.toLocalDate().lengthOfMonth()
        val endOfMonth = startOfMonth.withDayOfMonth(daysInMonth)
            .toLocalDate().atTime(23, 59, 59)

        // Get total expenses for the specified month
        val spent = transactionDao.getTotalAmountByTypeAndPeriod(
            type = TransactionType.EXPENSE,
            startDate = startOfMonth,
            endDate = endOfMonth
        )?.toBigDecimal() ?: BigDecimal.ZERO

        // Check if we already have a history entry for this month
        val existing = budgetDao.getBudgetHistoryForMonth(month, year)

        if (existing != null) {
            // Update existing entry
            budgetDao.updateBudgetHistory(
                existing.copy(
                    budgetAmount = budget.amount,
                    spentAmount = spent,
                    currency = budget.currency
                )
            )
        } else {
            // Insert new history entry
            budgetDao.insertBudgetHistory(
                BudgetHistoryEntity(
                    month = month,
                    year = year,
                    budgetAmount = budget.amount,
                    spentAmount = spent,
                    currency = budget.currency
                )
            )
        }
    }
}
