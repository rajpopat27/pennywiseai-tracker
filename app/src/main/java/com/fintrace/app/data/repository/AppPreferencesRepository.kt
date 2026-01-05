package com.fintrace.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS scan period options.
 */
enum class ScanPeriod(val days: Int?) {
    ONE_DAY(1),
    ONE_WEEK(7),
    FIFTEEN_DAYS(15),
    SINCE_INSTALL(null) // null means use install date
}

/**
 * Repository interface for app-wide preferences.
 */
interface AppPreferencesRepository {
    /**
     * Get the app install date (epoch millis).
     */
    fun getInstallDate(): Long

    /**
     * Get the current SMS scan period as a Flow.
     */
    fun getScanPeriod(): Flow<ScanPeriod>

    /**
     * Get the current SMS scan period (one-shot).
     */
    fun getScanPeriodOnce(): ScanPeriod

    /**
     * Set the SMS scan period.
     */
    suspend fun setScanPeriod(period: ScanPeriod)

    /**
     * Get the scan start date based on the current scan period.
     * Returns epoch millis.
     */
    fun getScanStartDate(): Long
}

/**
 * Implementation of AppPreferencesRepository using SharedPreferences.
 */
@Singleton
class AppPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferencesRepository {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val _scanPeriod = MutableStateFlow(loadScanPeriod())

    init {
        ensureInstallDateSet()
    }

    private fun ensureInstallDateSet() {
        if (!prefs.contains("app_install_date")) {
            prefs.edit().putLong("app_install_date", System.currentTimeMillis()).apply()
        }
    }

    override fun getInstallDate(): Long =
        prefs.getLong("app_install_date", System.currentTimeMillis())

    private fun loadScanPeriod(): ScanPeriod {
        val ordinal = prefs.getInt("scan_period", ScanPeriod.FIFTEEN_DAYS.ordinal)
        return ScanPeriod.entries.getOrElse(ordinal) { ScanPeriod.FIFTEEN_DAYS }
    }

    override fun getScanPeriod(): Flow<ScanPeriod> = _scanPeriod.asStateFlow()

    override fun getScanPeriodOnce(): ScanPeriod = _scanPeriod.value

    override suspend fun setScanPeriod(period: ScanPeriod) {
        prefs.edit().putInt("scan_period", period.ordinal).apply()
        _scanPeriod.value = period
    }

    override fun getScanStartDate(): Long {
        val period = _scanPeriod.value
        return if (period.days != null) {
            System.currentTimeMillis() - (period.days * 24 * 60 * 60 * 1000L)
        } else {
            getInstallDate()
        }
    }
}
