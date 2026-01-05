package com.fintrace.app.data.processor

import java.math.BigDecimal

/**
 * Result of processing a transaction through the TransactionProcessor pipeline.
 */
sealed class ProcessResult {
    /**
     * Transaction was successfully processed and saved.
     */
    data class Success(
        val transactionId: Long,
        val cashbackAmount: BigDecimal?,
        val subscriptionMatched: Boolean = false,
        val ruleApplied: String? = null
    ) : ProcessResult()

    /**
     * Transaction was blocked by a rule.
     */
    data class Blocked(
        val ruleName: String,
        val reason: String
    ) : ProcessResult()

    /**
     * Transaction is a duplicate of an existing one.
     */
    data class Duplicate(
        val existingTransactionId: Long,
        val reason: String
    ) : ProcessResult()

    /**
     * An error occurred during processing.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ProcessResult()
}

/**
 * Configuration options for transaction processing.
 */
data class ProcessConfig(
    /**
     * Skip duplicate hash check (e.g., when confirming a pending transaction).
     */
    val skipDuplicateCheck: Boolean = false,

    /**
     * Preserve the category set by user instead of applying merchant mapping.
     */
    val preserveUserCategory: Boolean = false,

    /**
     * Apply custom cashback percentage (for this transaction only, not retroactive).
     */
    val customCashbackPercent: BigDecimal? = null
)
