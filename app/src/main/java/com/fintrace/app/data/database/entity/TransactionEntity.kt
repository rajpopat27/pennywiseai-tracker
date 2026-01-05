package com.fintrace.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_hash"], unique = true),
        Index(value = ["merchant_name"]),
        Index(value = ["category"]),
        Index(value = ["bank_name"]),
        Index(value = ["account_number"]),
        Index(value = ["bank_name", "account_number"]),
        Index(value = ["date_time"]),
        Index(value = ["is_deleted"]),
        Index(value = ["transaction_type"]),
        Index(value = ["currency"])
    ]
)
data class TransactionEntity(
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
    
    @ColumnInfo(name = "transaction_hash", defaultValue = "")
    val transactionHash: String,
    
    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,
    
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = Currency.DEFAULT,

    @ColumnInfo(name = "from_account")
    val fromAccount: String? = null,

    @ColumnInfo(name = "to_account")
    val toAccount: String? = null,

    @ColumnInfo(name = "cashback_percent")
    val cashbackPercent: BigDecimal? = null,

    @ColumnInfo(name = "cashback_amount")
    val cashbackAmount: BigDecimal? = null
)

enum class TransactionType {
    INCOME,     // Money received
    EXPENSE,    // Money spent (including credit card purchases and investments)
    TRANSFER    // Between own accounts
}