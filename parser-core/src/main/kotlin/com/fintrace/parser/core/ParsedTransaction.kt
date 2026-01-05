package com.fintrace.parser.core

import java.math.BigDecimal
import java.security.MessageDigest

data class ParsedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val merchant: String?,
    val reference: String?,
    val accountLast4: String?,
    val balance: BigDecimal?,
    val creditLimit: BigDecimal? = null,
    val smsBody: String,
    val sender: String,
    val timestamp: Long,
    val bankName: String,
    val transactionHash: String? = null,
    val isFromCard: Boolean = false,
    val currency: String = "INR",
    val fromAccount: String? = null,
    val toAccount: String? = null,
    /**
     * Confidence score indicating parser reliability.
     * 1.0 = Specific bank parser (e.g., HDFCBankParser)
     * 0.7 = Generic fallback parser
     * 0.9 = AI-based parsing (future)
     */
    val parserConfidence: Float = 1.0f
) {
    fun generateTransactionId(): String {
        val normalizedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP)
        // Include timestamp for reliable deduplication - same amount on different days should be different transactions
        // Normalize timestamp to minute precision to handle minor time differences between sources
        val normalizedTimestamp = (timestamp / 60000) * 60000 // Round to nearest minute
        val data = "$sender|$normalizedAmount|$normalizedTimestamp|$smsBody"
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}


