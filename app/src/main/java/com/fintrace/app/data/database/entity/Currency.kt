package com.fintrace.app.data.database.entity

/**
 * Currency constants for the app.
 * Centralizes default currency and list of supported currencies.
 */
object Currency {
    const val DEFAULT = "INR"

    val SUPPORTED = listOf(
        "INR",  // Indian Rupee
        "USD",  // US Dollar
        "EUR",  // Euro
        "GBP",  // British Pound
        "AED",  // UAE Dirham
        "SAR",  // Saudi Riyal
        "KES",  // Kenyan Shilling
        "NPR",  // Nepalese Rupee
        "BYN",  // Belarusian Ruble
        "COP",  // Colombian Peso
        "EGP"   // Egyptian Pound
    )

    fun isSupported(currency: String): Boolean = currency.uppercase() in SUPPORTED
}
