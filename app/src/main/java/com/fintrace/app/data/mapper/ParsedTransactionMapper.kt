package com.fintrace.app.data.mapper

import com.fintrace.parser.core.ParsedTransaction
import com.fintrace.app.core.Constants
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.ui.icons.CategoryMapping
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Maps ParsedTransaction from parser-core to TransactionEntity
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )

    // Normalize merchant name to proper case
    val normalizedMerchant = merchant?.let { normalizeMerchantName(it) }

    // Map TransactionType from parser-core to database entity
    val entityType = when (type) {
        com.fintrace.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.fintrace.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.fintrace.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
    }

    return TransactionEntity(
        id = 0, // Auto-generated
        amount = amount,
        merchantName = normalizedMerchant ?: "Unknown Merchant",
        category = determineCategory(merchant, entityType),
        transactionType = entityType,
        dateTime = dateTime,
        description = null,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = sender,
        accountNumber = accountLast4,
        balanceAfter = balance,
        transactionHash = transactionHash?.takeIf { it.isNotBlank() } ?: generateTransactionId(),
        isRecurring = false, // Will be determined later
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount
    )
}

/**
 * Normalizes merchant name to consistent format.
 * Converts all-caps to proper case, preserves already mixed case.
 */
private fun normalizeMerchantName(name: String): String {
    val trimmed = name.trim()

    // If it's all uppercase, convert to proper case
    return if (trimmed == trimmed.uppercase()) {
        trimmed.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    } else {
        // Already has mixed case, keep as is
        trimmed
    }
}

/**
 * Determines the category based on merchant name and transaction type.
 */
private fun determineCategory(merchant: String?, type: TransactionType): String {
    val merchantName = merchant ?: return "Others"

    // Special handling for income transactions
    if (type == TransactionType.INCOME) {
        val merchantLower = merchantName.lowercase()
        return when {
            merchantLower.contains("salary") -> "Salary"
            merchantLower.contains("refund") -> "Refunds"
            merchantLower.contains("cashback") -> "Cashback"
            merchantLower.contains("interest") -> "Interest"
            merchantLower.contains("dividend") -> "Dividends"
            else -> "Income"
        }
    }

    // Use unified category mapping for expenses
    return CategoryMapping.getCategory(merchantName)
}

/**
 * Extension to map parser-core TransactionType to database entity TransactionType
 */
fun com.fintrace.parser.core.TransactionType.toEntityType(): TransactionType {
    return when (this) {
        com.fintrace.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.fintrace.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.fintrace.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
    }
}

/**
 * Maps ParsedTransaction from parser-core to PendingTransactionEntity
 */
fun ParsedTransaction.toPendingEntity(): PendingTransactionEntity {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )

    val normalizedMerchant = merchant?.let { normalizeMerchantName(it) }
    val entityType = type.toEntityType()

    return PendingTransactionEntity(
        id = 0,
        amount = amount,
        merchantName = normalizedMerchant ?: "Unknown Merchant",
        category = determineCategory(merchant, entityType),
        transactionType = entityType,
        dateTime = dateTime,
        description = null,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = sender,
        accountNumber = accountLast4,
        balanceAfter = balance,
        transactionHash = transactionHash?.takeIf { it.isNotBlank() } ?: generateTransactionId(),
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount
    )
}

/**
 * Converts a PendingTransactionEntity to TransactionEntity for final save
 */
fun PendingTransactionEntity.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        id = 0,
        amount = amount,
        merchantName = merchantName,
        category = category,
        transactionType = transactionType,
        dateTime = dateTime,
        description = description,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = smsSender,
        accountNumber = accountNumber,
        balanceAfter = balanceAfter,
        transactionHash = transactionHash,
        isRecurring = false,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount
    )
}