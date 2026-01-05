package com.fintrace.app.data.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

/**
 * Factory for creating ExchangeRateProvider instances.
 */
object ExchangeRateProviderFactory {
    /**
     * Creates the default exchange rate provider.
     * Uses the free exchangerate-api.com service.
     */
    fun createProvider(): ExchangeRateProvider {
        return ExchangeRateApiProvider()
    }
}

/**
 * Exchange rate provider using exchangerate-api.com (free tier).
 * This API provides free exchange rates that update daily.
 */
private class ExchangeRateApiProvider : ExchangeRateProvider {

    companion object {
        private const val BASE_URL = "https://open.er-api.com/v6/latest"
        private const val PROVIDER_NAME = "exchangerate-api"
    }

    override suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String): ExchangeRateResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/$baseCurrency")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResponse(response)
                } else {
                    null
                }
            } catch (e: Exception) {
                println("Failed to fetch exchange rates: ${e.message}")
                null
            }
        }
    }

    private fun parseResponse(jsonString: String): ExchangeRateResponse? {
        return try {
            val json = JSONObject(jsonString)

            if (json.optString("result") != "success") {
                return null
            }

            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, BigDecimal>()

            ratesJson.keys().forEach { currency ->
                val rate = ratesJson.getDouble(currency)
                rates[currency] = BigDecimal.valueOf(rate)
            }

            // The API provides time_last_update_unix and time_next_update_unix
            val lastUpdateTimeUnix = json.optLong("time_last_update_unix", System.currentTimeMillis() / 1000)
            val nextUpdateTimeUnix = json.optLong("time_next_update_unix", lastUpdateTimeUnix + 86400) // Default to 24h

            ExchangeRateResponse(
                rates = rates,
                lastUpdateTimeUnix = lastUpdateTimeUnix,
                nextUpdateTimeUnix = nextUpdateTimeUnix,
                provider = PROVIDER_NAME
            )
        } catch (e: Exception) {
            println("Failed to parse exchange rate response: ${e.message}")
            null
        }
    }
}
