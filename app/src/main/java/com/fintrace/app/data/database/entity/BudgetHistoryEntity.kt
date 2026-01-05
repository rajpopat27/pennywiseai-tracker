package com.fintrace.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing historical budget data for a specific month.
 * Used to track spending vs budget over time.
 */
@Entity(
    tableName = "budget_history",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "month")
    val month: Int,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "budget_amount")
    val budgetAmount: BigDecimal,

    @ColumnInfo(name = "spent_amount")
    val spentAmount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = Currency.DEFAULT,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val remainingAmount: BigDecimal
        get() = budgetAmount - spentAmount

    val percentUsed: Float
        get() = if (budgetAmount > BigDecimal.ZERO) {
            (spentAmount.toFloat() / budgetAmount.toFloat() * 100)
        } else 0f

    val isOverBudget: Boolean
        get() = spentAmount > budgetAmount
}
