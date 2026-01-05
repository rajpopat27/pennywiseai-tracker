package com.fintrace.app.data.database.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Database view that returns only the latest balance for each account.
 * This eliminates the need for complex join queries throughout the codebase.
 */
@DatabaseView(
    viewName = "latest_account_balances",
    value = """
        SELECT ab1.*
        FROM account_balances ab1
        INNER JOIN (
            SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
            FROM account_balances
            GROUP BY bank_name, account_last4
        ) ab2 ON ab1.bank_name = ab2.bank_name
            AND ab1.account_last4 = ab2.account_last4
            AND ab1.timestamp = ab2.max_timestamp
    """
)
data class LatestAccountBalanceView(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "account_last4")
    val accountLast4: String,

    @ColumnInfo(name = "balance")
    val balance: BigDecimal,

    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long?,

    @ColumnInfo(name = "credit_limit")
    val creditLimit: BigDecimal?,

    @ColumnInfo(name = "is_credit_card")
    val isCreditCard: Boolean,

    @ColumnInfo(name = "sms_source")
    val smsSource: String?,

    @ColumnInfo(name = "source_type")
    val sourceType: String?,

    @ColumnInfo(name = "account_type")
    val accountType: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime?,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "default_cashback_percent")
    val defaultCashbackPercent: BigDecimal?
)
