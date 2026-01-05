package com.fintrace.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for budget-related database operations.
 */
@Dao
interface BudgetDao {

    // ========== Budget Entity ==========

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    fun getActiveBudget(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveBudgetOnce(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET is_active = 0")
    suspend fun deactivateAllBudgets()

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    // ========== Budget History Entity ==========

    @Query("SELECT * FROM budget_history ORDER BY year DESC, month DESC LIMIT :limit")
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>

    @Query("SELECT * FROM budget_history ORDER BY year DESC, month DESC")
    suspend fun getBudgetHistoryOnce(): List<BudgetHistoryEntity>

    @Query("SELECT * FROM budget_history WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetHistoryForMonth(month: Int, year: Int): BudgetHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetHistory(history: BudgetHistoryEntity): Long

    @Update
    suspend fun updateBudgetHistory(history: BudgetHistoryEntity)

    @Query("DELETE FROM budget_history WHERE id = :id")
    suspend fun deleteBudgetHistory(id: Long)
}
