package com.fintrace.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing a pending transaction awaiting user confirmation.
 * When an SMS is parsed, the transaction is stored here first instead of
 * being saved directly to the transactions table.
 */
@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["transaction_hash"], unique = true)]
)
data class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "merchant_name")
    val merchantName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "transaction_type")
    val transactionType: TransactionType,

    @ColumnInfo(name = "date_time")
    val dateTime: LocalDateTime,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "sms_body")
    val smsBody: String? = null,

    @ColumnInfo(name = "bank_name")
    val bankName: String? = null,

    @ColumnInfo(name = "sms_sender")
    val smsSender: String? = null,

    @ColumnInfo(name = "account_number")
    val accountNumber: String? = null,

    @ColumnInfo(name = "balance_after")
    val balanceAfter: BigDecimal? = null,

    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,

    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = Currency.DEFAULT,

    @ColumnInfo(name = "from_account")
    val fromAccount: String? = null,

    @ColumnInfo(name = "to_account")
    val toAccount: String? = null,

    @ColumnInfo(name = "status", defaultValue = "PENDING")
    val status: PendingTransactionStatus = PendingTransactionStatus.PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "expires_at")
    val expiresAt: LocalDateTime = LocalDateTime.now().plusHours(24)
)

enum class PendingTransactionStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    AUTO_SAVED
}
