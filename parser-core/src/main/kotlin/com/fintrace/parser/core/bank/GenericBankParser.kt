package com.fintrace.parser.core.bank

import com.fintrace.parser.core.Constants
import com.fintrace.parser.core.ParsedTransaction

/**
 * Generic fallback parser for transaction SMS messages.
 *
 * This parser handles two scenarios:
 * 1. SMS from unknown bank senders (no specific parser available)
 * 2. SMS from known banks where the specific parser failed to parse
 *
 * It relies heavily on the base class BankParser patterns which already
 * handle 90% of common transaction SMS formats across banks.
 *
 * Design principles:
 * - Minimal custom logic - leverage base class patterns
 * - Currency detection from SMS content (Rs/INR/AED/USD/etc.)
 * - Extract sender as bank name (e.g., "HDFCBK" becomes "HDFC Bank")
 * - Lower confidence score to distinguish from specific parsers
 */
class GenericBankParser : BankParser() {

    private var detectedCurrency: String? = null
    private var cleanedBankName: String? = null

    override fun getBankName(): String {
        return cleanedBankName ?: "Unknown Bank"
    }

    override fun getCurrency(): String {
        return detectedCurrency ?: "INR"
    }

    override fun canHandle(sender: String): Boolean {
        // Generic parser accepts all senders
        // But should only be used as a fallback when no specific parser matches
        return true
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Extract and cache currency for this parse
        detectedCurrency = detectCurrencyFromMessage(smsBody)

        // Clean sender to create bank name
        cleanedBankName = cleanSenderToBankName(sender)

        // Use base class parse logic which already handles:
        // - isTransactionMessage() filtering
        // - extractAmount()
        // - extractTransactionType()
        // - extractMerchant()
        // - extractReference()
        // - extractAccountLast4()
        // - extractBalance()
        val result = super.parse(smsBody, sender, timestamp)

        // Add confidence score if successfully parsed
        return result?.copy(
            parserConfidence = Constants.Parsing.CONFIDENCE_PATTERN_BASED
        )
    }

    /**
     * Detects currency from SMS content.
     * Checks for currency codes (INR, USD, AED, etc.) and symbols (₹, $, etc.)
     */
    private fun detectCurrencyFromMessage(message: String): String {
        // Priority 1: Explicit currency codes before amounts
        val currencyCodePattern = Regex("""([A-Z]{3})\s*[0-9,]+""")
        currencyCodePattern.find(message)?.let { match ->
            val code = match.groupValues[1]
            if (isValidCurrencyCode(code)) {
                return code
            }
        }

        // Priority 2: Currency symbols and text patterns
        return when {
            message.contains("₹") -> "INR"
            message.contains("Rs.", ignoreCase = true) -> "INR"
            message.contains("INR", ignoreCase = false) -> "INR"
            message.contains("$") && !message.contains("Rs") -> "USD"
            message.contains("€") -> "EUR"
            message.contains("£") -> "GBP"
            message.contains("AED", ignoreCase = true) -> "AED"
            message.contains("SAR", ignoreCase = true) -> "SAR"
            message.contains("KES", ignoreCase = true) -> "KES"
            message.contains("NPR", ignoreCase = true) -> "NPR"
            message.contains("BDT", ignoreCase = true) -> "BDT"
            message.contains("LKR", ignoreCase = true) -> "LKR"
            message.contains("PKR", ignoreCase = true) -> "PKR"
            else -> "INR" // Default to INR
        }
    }

    /**
     * Validates if a 3-letter code is a known currency.
     */
    private fun isValidCurrencyCode(code: String): Boolean {
        val knownCurrencies = setOf(
            "INR", "USD", "EUR", "GBP", "AED", "SAR", "KES", "NPR",
            "BDT", "LKR", "PKR", "MYR", "SGD", "THB", "IDR", "PHP",
            "AUD", "CAD", "NZD", "ZAR", "BRL", "MXN", "CNY", "JPY",
            "KRW", "RUB", "TRY", "EGP", "NGN", "KWD", "QAR", "OMR",
            "BHD", "JOD", "ILS", "CHF", "SEK", "NOK", "DKK", "PLN"
        )
        return code.uppercase() in knownCurrencies
    }

    /**
     * Converts sender ID to a readable bank name.
     * Examples:
     * - "HDFCBK" -> "HDFC Bank"
     * - "VM-ICICIB" -> "ICICI Bank"
     * - "SBMSMS" -> "SBM Bank"
     */
    private fun cleanSenderToBankName(sender: String): String {
        // Remove DLT prefix (e.g., "VM-", "AD-", "BZ-")
        var cleaned = sender.replace(Regex("^[A-Z]{2}-"), "")

        // Map common bank name patterns
        val upperCleaned = cleaned.uppercase()

        return when {
            upperCleaned.contains("HDFC") -> "HDFC Bank"
            upperCleaned.contains("ICICI") -> "ICICI Bank"
            upperCleaned.contains("SBI") && !upperCleaned.contains("SBIM") -> "State Bank of India"
            upperCleaned.contains("AXIS") -> "Axis Bank"
            upperCleaned.contains("KOTAK") -> "Kotak Bank"
            upperCleaned.contains("PNB") -> "Punjab National Bank"
            upperCleaned.contains("BOB") -> "Bank of Baroda"
            upperCleaned.contains("BOI") -> "Bank of India"
            upperCleaned.contains("CANARA") -> "Canara Bank"
            upperCleaned.contains("UNION") -> "Union Bank"
            upperCleaned.contains("IDFC") -> "IDFC First Bank"
            upperCleaned.contains("IDBI") -> "IDBI Bank"
            upperCleaned.contains("FEDERAL") -> "Federal Bank"
            upperCleaned.contains("INDUS") -> "IndusInd Bank"
            upperCleaned.contains("YES") -> "Yes Bank"
            else -> {
                // Generic cleanup: remove SMS/BK suffix and format
                cleaned = cleaned
                    .replace(Regex("SMS$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("BK$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("BANK$", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (cleaned.isNotEmpty()) {
                    // Add "Bank" suffix if not already present
                    if (!cleaned.contains("Bank", ignoreCase = true)) {
                        "$cleaned Bank"
                    } else {
                        cleaned
                    }
                } else {
                    "Unknown Bank"
                }
            }
        }
    }

    /**
     * Enhanced merchant extraction with additional patterns for generic SMS.
     * Falls back to base class patterns first, then tries additional generic patterns.
     */
    override fun extractMerchant(message: String, sender: String): String? {
        // Try base class patterns first
        super.extractMerchant(message, sender)?.let { return it }

        // Additional generic patterns for common formats not covered by base class
        val additionalPatterns = listOf(
            // "spent at MERCHANT on DATE" - common card transaction format
            Regex(
                """spent\s+(?:on\s+[^\s]+\s+(?:card\s+)?)?at\s+([A-Z0-9][A-Z0-9\s]+?)(?:\s+on\s+\d|\s*\.|$)""",
                RegexOption.IGNORE_CASE
            ),

            // "paid to MERCHANT" - UPI/transfer format
            Regex(
                """paid\s+to\s+([^\n.]+?)(?:\s+on|\s+ref|\s+upi|\s*\.|$)""",
                RegexOption.IGNORE_CASE
            ),

            // "payment to MERCHANT successful"
            Regex(
                """payment\s+to\s+([^\n.]+?)(?:\s+successful|\s+completed|\s+done|\s*\.|$)""",
                RegexOption.IGNORE_CASE
            ),

            // "transferred to MERCHANT"
            Regex(
                """transferred\s+to\s+([^\n.]+?)(?:\s+on|\s+ref|\s*\.|$)""",
                RegexOption.IGNORE_CASE
            ),

            // "purchase at MERCHANT"
            Regex(
                """purchase\s+at\s+([^\n.]+?)(?:\s+on|\s+ref|\s*\.|$)""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in additionalPatterns) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        return null
    }
}
