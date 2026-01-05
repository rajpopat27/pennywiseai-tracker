package com.fintrace.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for managing hidden accounts.
 * Hidden accounts are excluded from balance totals and home screen display.
 */
interface HiddenAccountsRepository {
    /**
     * Get the set of hidden account keys as a Flow.
     */
    fun getHiddenAccounts(): Flow<Set<String>>

    /**
     * Toggle the visibility of an account.
     */
    suspend fun toggleAccountVisibility(bankName: String, accountLast4: String)

    /**
     * Check if an account is currently hidden.
     */
    fun isAccountHidden(bankName: String, accountLast4: String): Boolean

    /**
     * Hide an account.
     */
    suspend fun hideAccount(bankName: String, accountLast4: String)

    /**
     * Show (unhide) an account.
     */
    suspend fun showAccount(bankName: String, accountLast4: String)

    /**
     * Remove an account from the hidden list.
     */
    suspend fun removeAccount(bankName: String, accountLast4: String)
}

/**
 * Implementation of HiddenAccountsRepository using SharedPreferences.
 */
@Singleton
class HiddenAccountsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HiddenAccountsRepository {

    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    private val _hiddenAccounts = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadHiddenAccounts()
    }

    private fun loadHiddenAccounts() {
        val hidden = prefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
        _hiddenAccounts.value = hidden
    }

    private fun createKey(bankName: String, accountLast4: String): String =
        "${bankName}_${accountLast4}"

    override fun getHiddenAccounts(): Flow<Set<String>> = _hiddenAccounts.asStateFlow()

    override suspend fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        val key = createKey(bankName, accountLast4)
        val current = _hiddenAccounts.value.toMutableSet()

        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }

        saveAndUpdate(current)
    }

    override fun isAccountHidden(bankName: String, accountLast4: String): Boolean {
        val key = createKey(bankName, accountLast4)
        return _hiddenAccounts.value.contains(key)
    }

    override suspend fun hideAccount(bankName: String, accountLast4: String) {
        val key = createKey(bankName, accountLast4)
        val current = _hiddenAccounts.value.toMutableSet()
        current.add(key)
        saveAndUpdate(current)
    }

    override suspend fun showAccount(bankName: String, accountLast4: String) {
        val key = createKey(bankName, accountLast4)
        val current = _hiddenAccounts.value.toMutableSet()
        current.remove(key)
        saveAndUpdate(current)
    }

    override suspend fun removeAccount(bankName: String, accountLast4: String) {
        showAccount(bankName, accountLast4) // Just ensure it's not in hidden list
    }

    private fun saveAndUpdate(hidden: Set<String>) {
        prefs.edit().putStringSet("hidden_accounts", hidden).apply()
        _hiddenAccounts.value = hidden
    }
}
