# Phase 2: Domain Layer Architecture

## Overview
Centralize business logic into use cases and services. Create the TransactionProcessor to unify all transaction flows.

**Goal:** Clean separation of business logic from ViewModels and consistent processing across all paths.

---

## 2.1 TransactionProcessor (Critical)

### 2.1.1 Create ProcessResult
**New File:** `data/processor/ProcessResult.kt`

```kotlin
package com.fintrace.app.data.processor

import java.math.BigDecimal

sealed class ProcessResult {
    data class Success(
        val transactionId: Long,
        val cashbackAmount: BigDecimal?,
        val subscriptionMatched: Boolean = false,
        val ruleApplied: String? = null
    ) : ProcessResult()

    data class Blocked(
        val ruleName: String,
        val reason: String
    ) : ProcessResult()

    data class Duplicate(
        val existingTransactionId: Long,
        val reason: String
    ) : ProcessResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ProcessResult()
}
```

### 2.1.2 Create TransactionProcessor
**New File:** `data/processor/TransactionProcessor.kt`

```kotlin
package com.fintrace.app.data.processor

import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.repository.*
import com.fintrace.app.domain.service.RuleEngine
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    /**
     * Process and save a transaction through the unified pipeline.
     *
     * @param entity The transaction entity to process
     * @param smsBody Original SMS body (for rule matching)
     * @param pendingEntity If converting from pending (for balance updates)
     * @param config Processing configuration options
     * @return ProcessResult indicating success, blocked, duplicate, or error
     */
    suspend fun processAndSave(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null,
        config: ProcessConfig = ProcessConfig()
    ): ProcessResult {
        try {
            // Step 1: Duplicate Check
            if (!config.skipDuplicateCheck) {
                val existing = transactionRepository.getByHash(entity.transactionHash)
                if (existing != null) {
                    return ProcessResult.Duplicate(
                        existingTransactionId = existing.id,
                        reason = if (existing.isDeleted) "Previously deleted" else "Already exists"
                    )
                }
            }

            // Step 2: Apply Merchant Mapping
            var processedEntity = entity
            if (!config.preserveUserCategory && entity.merchantName != null) {
                val mapping = merchantMappingRepository.getMappingForMerchant(entity.merchantName)
                if (mapping != null) {
                    processedEntity = processedEntity.copy(category = mapping.category)
                }
            }

            // Step 3: Check Block Rules
            val rules = ruleRepository.getActiveRules()
            for (rule in rules) {
                if (rule.action == "BLOCK" && ruleEngine.matches(processedEntity, smsBody, rule)) {
                    return ProcessResult.Blocked(
                        ruleName = rule.name,
                        reason = "Transaction blocked by rule: ${rule.name}"
                    )
                }
            }

            // Step 4: Apply Transform Rules
            for (rule in rules) {
                if (rule.action != "BLOCK" && ruleEngine.matches(processedEntity, smsBody, rule)) {
                    processedEntity = ruleEngine.apply(processedEntity, rule)
                }
            }

            // Step 5: Match Subscription
            var subscriptionMatched = false
            val subscription = subscriptionRepository.findMatchingSubscription(
                merchantName = processedEntity.merchantName,
                amount = processedEntity.amount,
                category = processedEntity.category
            )
            if (subscription != null) {
                processedEntity = processedEntity.copy(subscriptionId = subscription.id)
                subscriptionMatched = true
            }

            // Step 6: Apply Cashback
            val cashbackAmount = if (config.customCashbackPercent != null) {
                // Custom cashback for this transaction only
                val amount = processedEntity.amount * config.customCashbackPercent / BigDecimal(100)
                processedEntity = processedEntity.copy(
                    cashbackPercent = config.customCashbackPercent.toDouble(),
                    cashbackAmount = amount
                )
                amount
            } else {
                // Apply default cashback from account/card
                applyDefaultCashback(processedEntity)
            }

            // Step 7: Insert Transaction
            val transactionId = transactionRepository.insertTransaction(processedEntity)

            // Step 8: Update Balance
            if (pendingEntity != null) {
                updateBalanceFromPending(pendingEntity, processedEntity)
            }

            // Step 9: Save Rule Applications (for analytics)
            // TODO: Track which rules were applied

            return ProcessResult.Success(
                transactionId = transactionId,
                cashbackAmount = cashbackAmount,
                subscriptionMatched = subscriptionMatched
            )

        } catch (e: Exception) {
            return ProcessResult.Error(
                message = "Failed to process transaction: ${e.message}",
                exception = e
            )
        }
    }

    private suspend fun applyDefaultCashback(entity: TransactionEntity): BigDecimal? {
        // Only apply to EXPENSE transactions
        if (entity.transactionType != TransactionType.EXPENSE) {
            return null
        }

        // Try to get cashback from account
        val account = entity.bankName?.let { bankName ->
            entity.accountNumber?.let { accountNumber ->
                accountBalanceRepository.getLatestBalance(bankName, accountNumber)
            }
        }

        val cashbackPercent = account?.defaultCashbackPercent

        return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
            val amount = entity.amount * cashbackPercent / BigDecimal(100)
            // Note: This should update the entity before insert
            // The caller should handle this
            amount
        } else {
            null
        }
    }

    private suspend fun updateBalanceFromPending(
        pending: PendingTransactionEntity,
        transaction: TransactionEntity
    ) {
        // Use pending entity's bank/account info for balance update
        val bankName = pending.bankName ?: return
        val accountLast4 = pending.accountLast4 ?: return
        val newBalance = pending.balanceAfter ?: return

        accountBalanceRepository.insertBalanceUpdate(
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = newBalance,
            timestamp = transaction.dateTime,
            smsSource = pending.smsBody,
            sourceType = "TRANSACTION"
        )
    }
}

data class ProcessConfig(
    val skipDuplicateCheck: Boolean = false,
    val preserveUserCategory: Boolean = false,
    val customCashbackPercent: BigDecimal? = null
)
```

- [ ] Create `data/processor/` directory
- [ ] Create `ProcessResult.kt`
- [ ] Create `ProcessConfig.kt` (or include in TransactionProcessor)
- [ ] Create `TransactionProcessor.kt`
- [ ] Add `@Singleton` and `@Inject` annotations
- [ ] Verify all dependencies are injectable

---

## 2.2 CashbackService

### 2.2.1 Create CashbackService
**New File:** `domain/service/CashbackService.kt`

```kotlin
package com.fintrace.app.domain.service

import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.repository.AccountBalanceRepository
import com.fintrace.app.data.repository.CardRepository
import com.fintrace.app.data.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashbackService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    /**
     * Calculate cashback for a single transaction based on account/card settings.
     */
    suspend fun calculateCashback(
        transaction: TransactionEntity
    ): CashbackResult {
        if (transaction.transactionType != TransactionType.EXPENSE) {
            return CashbackResult.NotApplicable("Only EXPENSE transactions earn cashback")
        }

        val account = transaction.bankName?.let { bankName ->
            transaction.accountNumber?.let { accountNumber ->
                accountBalanceRepository.getLatestBalance(bankName, accountNumber)
            }
        }

        val cashbackPercent = account?.defaultCashbackPercent

        return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
            val amount = transaction.amount * cashbackPercent / BigDecimal(100)
            CashbackResult.Calculated(cashbackPercent, amount)
        } else {
            CashbackResult.NoCashbackConfigured
        }
    }

    /**
     * Apply retroactive cashback to all transactions for an account.
     * Only updates transactions that don't already have cashback.
     */
    suspend fun applyRetroactiveCashback(
        bankName: String,
        accountLast4: String,
        cashbackPercent: BigDecimal
    ): RetroactiveResult {
        if (cashbackPercent <= BigDecimal.ZERO) {
            return RetroactiveResult.InvalidPercent
        }

        val updatedCount = transactionRepository.applyRetroactiveCashback(
            bankName = bankName,
            accountLast4 = accountLast4,
            cashbackPercent = cashbackPercent.toDouble()
        )

        return RetroactiveResult.Success(updatedCount)
    }

    /**
     * Get cashback summary for analytics.
     */
    suspend fun getCashbackSummary(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): CashbackSummary {
        val total = transactionRepository.getTotalCashback(startDate, endDate) ?: BigDecimal.ZERO
        val byAccount = transactionRepository.getCashbackByAccount(startDate, endDate)

        return CashbackSummary(
            total = total,
            byAccount = byAccount.map {
                AccountCashback(it.bankName ?: "", it.accountNumber ?: "", it.total)
            }
        )
    }
}

sealed class CashbackResult {
    data class Calculated(val percent: BigDecimal, val amount: BigDecimal) : CashbackResult()
    object NoCashbackConfigured : CashbackResult()
    data class NotApplicable(val reason: String) : CashbackResult()
}

sealed class RetroactiveResult {
    data class Success(val updatedCount: Int) : RetroactiveResult()
    object InvalidPercent : RetroactiveResult()
}

data class CashbackSummary(
    val total: BigDecimal,
    val byAccount: List<AccountCashback>
)

data class AccountCashback(
    val bankName: String,
    val accountLast4: String,
    val amount: BigDecimal
)
```

- [ ] Create `CashbackService.kt`
- [ ] Create result sealed classes
- [ ] Add to Hilt module if needed

---

## 2.3 BudgetRepository

### 2.3.1 Create BudgetRepository Interface
**New File:** `data/repository/BudgetRepository.kt`

```kotlin
package com.fintrace.app.data.repository

import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface BudgetRepository {
    // Current Budget
    fun getActiveBudget(): Flow<BudgetEntity?>
    suspend fun getActiveBudgetOnce(): BudgetEntity?
    suspend fun setBudget(amount: BigDecimal, currency: String)
    suspend fun deleteBudget()

    // Budget with current spending
    fun getBudgetWithSpending(): Flow<BudgetWithSpending?>

    // History
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>
    suspend fun saveBudgetSnapshot(month: Int, year: Int)
}

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val spentThisMonth: BigDecimal,
    val remaining: BigDecimal,
    val percentUsed: Float
) {
    val isOverBudget: Boolean = remaining < BigDecimal.ZERO
}
```

### 2.3.2 Create BudgetRepositoryImpl
**New File:** `data/repository/BudgetRepositoryImpl.kt`

```kotlin
package com.fintrace.app.data.repository

import com.fintrace.app.data.database.dao.BudgetDao
import com.fintrace.app.data.database.dao.TransactionDao
import com.fintrace.app.data.database.entity.BudgetEntity
import com.fintrace.app.data.database.entity.BudgetHistoryEntity
import com.fintrace.app.data.database.entity.Currency
import com.fintrace.app.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getActiveBudget(): Flow<BudgetEntity?> =
        budgetDao.getActiveBudget()

    override suspend fun getActiveBudgetOnce(): BudgetEntity? =
        budgetDao.getActiveBudgetOnce()

    override suspend fun setBudget(amount: BigDecimal, currency: String) {
        budgetDao.deactivateAllBudgets()
        budgetDao.insertBudget(
            BudgetEntity(
                amount = amount,
                currency = currency,
                isActive = true
            )
        )
    }

    override suspend fun deleteBudget() {
        budgetDao.deactivateAllBudgets()
    }

    override fun getBudgetWithSpending(): Flow<BudgetWithSpending?> {
        return budgetDao.getActiveBudget().map { budget ->
            budget?.let { calculateBudgetWithSpending(it) }
        }
    }

    private suspend fun calculateBudgetWithSpending(budget: BudgetEntity): BudgetWithSpending {
        val now = LocalDateTime.now()
        val startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay()
        val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
            .toLocalDate().atTime(23, 59, 59)

        val spent = transactionDao.getTotalAmountByTypeAndPeriod(
            type = TransactionType.EXPENSE,
            startDate = startOfMonth,
            endDate = endOfMonth
        )?.toBigDecimal() ?: BigDecimal.ZERO

        val remaining = budget.amount - spent
        val percentUsed = if (budget.amount > BigDecimal.ZERO) {
            (spent.toFloat() / budget.amount.toFloat() * 100)
        } else 0f

        return BudgetWithSpending(
            budget = budget,
            spentThisMonth = spent,
            remaining = remaining,
            percentUsed = percentUsed
        )
    }

    override fun getBudgetHistory(limit: Int): Flow<List<BudgetHistoryEntity>> =
        budgetDao.getBudgetHistory(limit)

    override suspend fun saveBudgetSnapshot(month: Int, year: Int) {
        val budget = getActiveBudgetOnce() ?: return

        val startOfMonth = LocalDateTime.of(year, month, 1, 0, 0)
        val endOfMonth = startOfMonth.withDayOfMonth(
            startOfMonth.toLocalDate().lengthOfMonth()
        ).toLocalDate().atTime(23, 59, 59)

        val spent = transactionDao.getTotalAmountByTypeAndPeriod(
            type = TransactionType.EXPENSE,
            startDate = startOfMonth,
            endDate = endOfMonth
        )?.toBigDecimal() ?: BigDecimal.ZERO

        budgetDao.insertBudgetHistory(
            BudgetHistoryEntity(
                month = month,
                year = year,
                budgetAmount = budget.amount,
                spentAmount = spent,
                currency = budget.currency
            )
        )
    }
}
```

### 2.3.3 Add DI Binding
**File:** `di/AppModule.kt`

```kotlin
@Binds
@Singleton
abstract fun bindBudgetRepository(
    impl: BudgetRepositoryImpl
): BudgetRepository
```

- [ ] Create `BudgetRepository.kt` interface
- [ ] Create `BudgetWithSpending` data class
- [ ] Create `BudgetRepositoryImpl.kt`
- [ ] Add Hilt binding

---

## 2.4 HiddenAccountsRepository

### 2.4.1 Create Interface and Implementation
**New File:** `data/repository/HiddenAccountsRepository.kt`

```kotlin
package com.fintrace.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface HiddenAccountsRepository {
    fun getHiddenAccounts(): Flow<Set<String>>
    suspend fun toggleAccountVisibility(bankName: String, accountLast4: String)
    fun isAccountHidden(bankName: String, accountLast4: String): Boolean
    suspend fun hideAccount(bankName: String, accountLast4: String)
    suspend fun showAccount(bankName: String, accountLast4: String)
    suspend fun removeAccount(bankName: String, accountLast4: String)
}

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
```

- [ ] Create `HiddenAccountsRepository.kt`
- [ ] Add Hilt binding

---

## 2.5 AppPreferencesRepository

### 2.5.1 Create for Install Date and Scan Period
**New File:** `data/repository/AppPreferencesRepository.kt`

```kotlin
package com.fintrace.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ScanPeriod(val days: Int?) {
    ONE_DAY(1),
    ONE_WEEK(7),
    FIFTEEN_DAYS(15),
    SINCE_INSTALL(null) // null means use install date
}

interface AppPreferencesRepository {
    fun getInstallDate(): Long
    fun getScanPeriod(): Flow<ScanPeriod>
    suspend fun setScanPeriod(period: ScanPeriod)
    fun getScanStartDate(): Long // Returns epoch millis
}

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
```

- [ ] Create `AppPreferencesRepository.kt`
- [ ] Add `ScanPeriod` enum
- [ ] Add Hilt binding

---

## 2.6 Use Cases (Essential Ones)

### 2.6.1 ProcessTransactionUseCase
**New File:** `domain/usecase/transaction/ProcessTransactionUseCase.kt`

```kotlin
package com.fintrace.app.domain.usecase.transaction

import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.processor.ProcessConfig
import com.fintrace.app.data.processor.ProcessResult
import com.fintrace.app.data.processor.TransactionProcessor
import javax.inject.Inject

class ProcessTransactionUseCase @Inject constructor(
    private val transactionProcessor: TransactionProcessor
) {
    suspend operator fun invoke(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null,
        config: ProcessConfig = ProcessConfig()
    ): ProcessResult {
        return transactionProcessor.processAndSave(
            entity = entity,
            smsBody = smsBody,
            pendingEntity = pendingEntity,
            config = config
        )
    }
}
```

### 2.6.2 GetBudgetWithSpendingUseCase
**New File:** `domain/usecase/budget/GetBudgetWithSpendingUseCase.kt`

```kotlin
package com.fintrace.app.domain.usecase.budget

import com.fintrace.app.data.repository.BudgetRepository
import com.fintrace.app.data.repository.BudgetWithSpending
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetWithSpendingUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(): Flow<BudgetWithSpending?> {
        return budgetRepository.getBudgetWithSpending()
    }
}
```

### 2.6.3 ApplyRetroactiveCashbackUseCase
**New File:** `domain/usecase/cashback/ApplyRetroactiveCashbackUseCase.kt`

```kotlin
package com.fintrace.app.domain.usecase.cashback

import com.fintrace.app.domain.service.CashbackService
import com.fintrace.app.domain.service.RetroactiveResult
import java.math.BigDecimal
import javax.inject.Inject

class ApplyRetroactiveCashbackUseCase @Inject constructor(
    private val cashbackService: CashbackService
) {
    suspend operator fun invoke(
        bankName: String,
        accountLast4: String,
        cashbackPercent: BigDecimal
    ): RetroactiveResult {
        return cashbackService.applyRetroactiveCashback(
            bankName = bankName,
            accountLast4 = accountLast4,
            cashbackPercent = cashbackPercent
        )
    }
}
```

- [ ] Create `domain/usecase/transaction/` directory
- [ ] Create `ProcessTransactionUseCase.kt`
- [ ] Create `domain/usecase/budget/` directory
- [ ] Create `GetBudgetWithSpendingUseCase.kt`
- [ ] Create `domain/usecase/cashback/` directory
- [ ] Create `ApplyRetroactiveCashbackUseCase.kt`

---

## 2.7 Update Existing Components

### 2.7.1 Update OptimizedSmsReaderWorker
**File:** `worker/OptimizedSmsReaderWorker.kt`

Replace direct save logic (lines 1041-1147) with:

```kotlin
// Inject TransactionProcessor
private val transactionProcessor: TransactionProcessor

// In direct save path:
val result = transactionProcessor.processAndSave(
    entity = parsedTransaction.toEntity(),
    smsBody = sms.body,
    config = ProcessConfig(skipDuplicateCheck = false)
)

when (result) {
    is ProcessResult.Success -> {
        savedCount++
        // Log success
    }
    is ProcessResult.Blocked -> {
        blockedCount++
        // Log blocked
    }
    is ProcessResult.Duplicate -> {
        skippedCount++
        // Already exists
    }
    is ProcessResult.Error -> {
        errorCount++
        // Log error
    }
}
```

- [ ] Add `TransactionProcessor` injection to worker
- [ ] Replace lines 1041-1147 with processor call
- [ ] Handle all `ProcessResult` cases
- [ ] Remove duplicated helper methods

### 2.7.2 Update PendingTransactionManager
**File:** `data/manager/PendingTransactionManager.kt`

Replace `confirmTransaction()` (lines 173-252):

```kotlin
suspend fun confirmTransaction(
    pending: PendingTransactionEntity,
    editedTransaction: TransactionEntity,
    customCashbackPercent: BigDecimal? = null
): ProcessResult {
    val result = transactionProcessor.processAndSave(
        entity = editedTransaction,
        smsBody = pending.smsBody,
        pendingEntity = pending,
        config = ProcessConfig(
            skipDuplicateCheck = true, // Already validated
            preserveUserCategory = true, // User edited
            customCashbackPercent = customCashbackPercent
        )
    )

    if (result is ProcessResult.Success) {
        pendingTransactionRepository.confirm(pending.id)
    }

    return result
}
```

Replace `autoSaveExpiredTransactions()` (lines 305-379):

```kotlin
suspend fun autoSaveExpiredTransactions() {
    val expired = pendingTransactionRepository.getExpiredTransactions()

    for (pending in expired) {
        val entity = pending.toTransactionEntity()

        val result = transactionProcessor.processAndSave(
            entity = entity,
            smsBody = pending.smsBody,
            pendingEntity = pending,
            config = ProcessConfig(skipDuplicateCheck = true)
        )

        if (result is ProcessResult.Success) {
            pendingTransactionRepository.markAutoSaved(pending.id)
        }
    }
}
```

- [ ] Add `TransactionProcessor` injection
- [ ] Replace `confirmTransaction()` logic
- [ ] Replace `autoSaveExpiredTransactions()` logic
- [ ] Remove `applyDefaultCashback()` private method
- [ ] Remove `processBalanceUpdate()` duplicate

---

## Verification Checklist

After completing Phase 2:

- [ ] `TransactionProcessor` handles all 3 transaction paths
- [ ] Cashback applied consistently everywhere
- [ ] `CashbackService` centralizes cashback logic
- [ ] `BudgetRepository` ready for Phase 7
- [ ] `HiddenAccountsRepository` replaces SharedPreferences access
- [ ] `AppPreferencesRepository` tracks install date and scan period
- [ ] Essential use cases created
- [ ] `OptimizedSmsReaderWorker` uses `TransactionProcessor`
- [ ] `PendingTransactionManager` uses `TransactionProcessor`
- [ ] No duplicate processing logic remains
- [ ] All tests pass

---

## New Files Created

| File | Purpose |
|------|---------|
| `data/processor/ProcessResult.kt` | Processing result types |
| `data/processor/TransactionProcessor.kt` | Unified processing |
| `domain/service/CashbackService.kt` | Cashback logic |
| `data/repository/BudgetRepository.kt` | Budget interface |
| `data/repository/BudgetRepositoryImpl.kt` | Budget implementation |
| `data/repository/HiddenAccountsRepository.kt` | Hidden accounts |
| `data/repository/AppPreferencesRepository.kt` | App preferences |
| `domain/usecase/transaction/ProcessTransactionUseCase.kt` | Process use case |
| `domain/usecase/budget/GetBudgetWithSpendingUseCase.kt` | Budget use case |
| `domain/usecase/cashback/ApplyRetroactiveCashbackUseCase.kt` | Cashback use case |

---

## Next Phase
→ Proceed to `implementation-phase3-presentation-layer.md`
