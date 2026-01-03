package com.pennywiseai.tracker.data.currency

import java.math.BigDecimal

/**
 * Interface for fetching exchange rates from external APIs.
 */
interface ExchangeRateProvider {
    /**
     * Fetch all exchange rates with metadata including update times.
     *
     * @param baseCurrency The base currency for the rates (e.g., "USD")
     * @return ExchangeRateResponse with rates and metadata, or null if failed
     */
    suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String): ExchangeRateResponse?
}

/**
 * Response from the exchange rate API.
 */
data class ExchangeRateResponse(
    val rates: Map<String, BigDecimal>,
    val lastUpdateTimeUnix: Long,
    val nextUpdateTimeUnix: Long,
    val provider: String
)
