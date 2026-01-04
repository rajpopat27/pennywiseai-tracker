# Phase 1: Data Layer Architecture

## Overview
Establish a solid data foundation with proper entities, DAOs, repositories, and database optimizations.

**Goal:** Clean, efficient, consistent data layer that supports all current and upcoming features.

---

## 1.1 Entity Cleanup

### 1.1.1 Create Currency Constant
**New File:** `data/database/entity/Currency.kt`

```kotlin
object Currency {
    const val DEFAULT = "INR"

    val SUPPORTED = listOf("INR", "USD", "EUR", "GBP", "AED", "SAR", "KES", "NPR", "BYN")

    fun isSupported(currency: String): Boolean = currency in SUPPORTED
}
```

**Update all entities to use `Currency.DEFAULT`:**
- [ ] `TransactionEntity.kt` - change `currency: String = "INR"` to `currency: String = Currency.DEFAULT`
- [ ] `AccountBalanceEntity.kt`
- [ ] `CardEntity.kt`
- [ ] `SubscriptionEntity.kt`
- [ ] `PendingTransactionEntity.kt`

### 1.1.2 Add Missing Timestamps
**File:** `data/database/entity/AccountBalanceEntity.kt`

```kotlin
@ColumnInfo(name = "updated_at")
val updatedAt: LocalDateTime = LocalDateTime.now()
```

- [ ] Add `updatedAt` to `AccountBalanceEntity`
- [ ] Update repository to set `updatedAt` on updates
- [ ] Create database migration

### 1.1.3 Simplify TransactionType (Prep for Phase 6)
**Note:** Actual removal happens in Phase 6, but prepare the structure now.

**File:** `parser-core/.../TransactionType.kt` and `app/.../TransactionType.kt`

```kotlin
// Current
enum class TransactionType {
    INCOME, EXPENSE, CREDIT, INVESTMENT, TRANSFER
}

// Target (Phase 6)
enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}
```

- [ ] Document which parsers use CREDIT
- [ ] Document which parsers use INVESTMENT
- [ ] Plan migration path for each

---

## 1.2 Database Indexes

### 1.2.1 Add Transaction Indexes
**File:** `data/database/entity/TransactionEntity.kt`

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_hash"], unique = true),
        Index(value = ["merchant_name"]),
        Index(value = ["category"]),
        Index(value = ["bank_name"]),
        Index(value = ["account_number"]),
        Index(value = ["bank_name", "account_number"]),
        Index(value = ["date_time"]),
        Index(value = ["is_deleted"]),
        Index(value = ["transaction_type"]),
        Index(value = ["currency"])
    ]
)
data class TransactionEntity(...)
```

- [ ] Add index on `merchant_name`
- [ ] Add index on `category`
- [ ] Add index on `bank_name`
- [ ] Add index on `account_number`
- [ ] Add composite index `(bank_name, account_number)`
- [ ] Add index on `date_time`
- [ ] Add index on `is_deleted`
- [ ] Add index on `transaction_type`
- [ ] Add index on `currency`
- [ ] Create database migration

### 1.2.2 Add Account Balance Indexes
**File:** `data/database/entity/AccountBalanceEntity.kt`

```kotlin
@Entity(
    tableName = "account_balances",
    indices = [
        Index(value = ["bank_name", "account_last4"]),
        Index(value = ["timestamp"]),
        Index(value = ["is_credit_card"])
    ]
)
```

- [ ] Add composite index `(bank_name, account_last4)`
- [ ] Add index on `timestamp`
- [ ] Add index on `is_credit_card`

---

## 1.3 Database Views

### 1.3.1 Create Latest Balances View
**Why:** The complex join query for "latest balance per account" is repeated 3+ times.

**File:** `data/database/AppDatabase.kt`

```kotlin
@Database(
    entities = [...],
    views = [LatestAccountBalanceView::class],
    version = X
)
abstract class AppDatabase : RoomDatabase()
```

**New File:** `data/database/view/LatestAccountBalanceView.kt`

```kotlin
@DatabaseView(
    viewName = "latest_account_balances",
    value = """
        SELECT ab1.*
        FROM account_balances ab1
        INNER JOIN (
            SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
            FROM account_balances
            GROUP BY bank_name, account_last4
        ) ab2 ON ab1.bank_name = ab2.bank_name
            AND ab1.account_last4 = ab2.account_last4
            AND ab1.timestamp = ab2.max_timestamp
    """
)
data class LatestAccountBalanceView(
    val id: Long,
    val bankName: String,
    val accountLast4: String,
    val balance: BigDecimal,
    val creditLimit: BigDecimal?,
    val timestamp: LocalDateTime,
    val isCreditCard: Boolean,
    val accountType: String?,
    val sourceType: String?,
    val smsSource: String?,
    val defaultCashbackPercent: BigDecimal?,
    val currency: String,
    val createdAt: LocalDateTime
)
```

- [ ] Create `view/` directory
- [ ] Create `LatestAccountBalanceView.kt`
- [ ] Update `AppDatabase` to include view
- [ ] Update `AccountBalanceDao` to use view
- [ ] Remove duplicate join queries

---

## 1.4 DAO Standardization

### 1.4.1 Standardize Return Types
**Pattern:**
- `Flow<T>` for reactive (UI observing)
- `suspend fun(): T` for one-shot (background operations)

**File:** `data/database/dao/AccountBalanceDao.kt`

```kotlin
// KEEP BOTH - different use cases
@Query("SELECT * FROM latest_account_balances WHERE bank_name = :bankName AND account_last4 = :accountLast4")
fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?>

@Query("SELECT * FROM latest_account_balances WHERE bank_name = :bankName AND account_last4 = :accountLast4")
suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity?
```

- [ ] Review `AccountBalanceDao` - document which methods need Flow vs suspend
- [ ] Review `PendingTransactionDao` - standardize naming
- [ ] Review `SubscriptionDao` - standardize naming
- [ ] Review `TransactionDao` - ensure proper usage
- [ ] Add comments explaining when to use Flow vs suspend

### 1.4.2 Add Missing Aggregation Queries
**For upcoming analytics features**

**File:** `data/database/dao/TransactionDao.kt`

```kotlin
// Daily spending trend
@Query("""
    SELECT DATE(date_time) as date, SUM(amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND transaction_type = 'EXPENSE'
    AND currency = :currency
    AND date_time >= :startDate
    GROUP BY DATE(date_time)
    ORDER BY date ASC
""")
suspend fun getDailySpending(startDate: LocalDateTime, currency: String): List<DailySpendingResult>

// Spending by day of week
@Query("""
    SELECT strftime('%w', date_time) as dayOfWeek, SUM(amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND transaction_type = 'EXPENSE'
    AND currency = :currency
    AND date_time BETWEEN :startDate AND :endDate
    GROUP BY dayOfWeek
""")
suspend fun getSpendingByDayOfWeek(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    currency: String
): List<DayOfWeekSpendingResult>

// Top merchants
@Query("""
    SELECT merchant_name, SUM(amount) as total, COUNT(*) as transactionCount
    FROM transactions
    WHERE is_deleted = 0
    AND transaction_type = 'EXPENSE'
    AND currency = :currency
    AND date_time BETWEEN :startDate AND :endDate
    AND merchant_name IS NOT NULL
    GROUP BY merchant_name
    ORDER BY total DESC
    LIMIT :limit
""")
suspend fun getTopMerchants(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    currency: String,
    limit: Int
): List<TopMerchantResult>

// Total cashback
@Query("""
    SELECT SUM(cashback_amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND cashback_amount > 0
    AND date_time BETWEEN :startDate AND :endDate
""")
suspend fun getTotalCashback(startDate: LocalDateTime, endDate: LocalDateTime): BigDecimal?

// Cashback by account
@Query("""
    SELECT bank_name, account_number, SUM(cashback_amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND cashback_amount > 0
    AND date_time BETWEEN :startDate AND :endDate
    GROUP BY bank_name, account_number
""")
suspend fun getCashbackByAccount(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): List<AccountCashbackResult>
```

**New File:** `data/database/dao/result/TransactionAggregateResults.kt`

```kotlin
data class DailySpendingResult(
    val date: String,
    val total: BigDecimal
)

data class DayOfWeekSpendingResult(
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val total: BigDecimal
)

data class TopMerchantResult(
    val merchantName: String,
    val total: BigDecimal,
    val transactionCount: Int
)

data class AccountCashbackResult(
    val bankName: String?,
    val accountNumber: String?,
    val total: BigDecimal
)
```

- [ ] Create `dao/result/` directory
- [ ] Create `TransactionAggregateResults.kt`
- [ ] Add aggregation queries to `TransactionDao`
- [ ] Test queries with sample data

---

## 1.5 Repository Cleanup

### 1.5.1 Extract Monthly Breakdown Helper
**File:** `data/repository/TransactionRepository.kt`

**Before:** 4 nearly identical methods (~100 lines)
**After:** 1 helper + 4 thin methods (~40 lines)

```kotlin
private suspend fun getMonthBreakdown(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    currency: String? = null
): MonthBreakdown {
    val transactions = if (currency != null) {
        transactionDao.getTransactionsBetweenDatesByCurrency(startDate, endDate, currency)
    } else {
        transactionDao.getTransactionsBetweenDatesList(startDate, endDate)
    }

    var income = BigDecimal.ZERO
    var expense = BigDecimal.ZERO

    transactions.forEach { tx ->
        when (tx.transactionType) {
            TransactionType.INCOME -> income += tx.amount
            TransactionType.EXPENSE -> expense += tx.amount
            else -> {}
        }
    }

    return MonthBreakdown(income, expense, income - expense)
}

fun getCurrentMonthBreakdown() = getMonthBreakdown(startOfMonth, endOfMonth)
fun getLastMonthBreakdown() = getMonthBreakdown(startOfLastMonth, endOfLastMonth)
fun getCurrentMonthBreakdownByCurrency(currency: String) = getMonthBreakdown(startOfMonth, endOfMonth, currency)
fun getLastMonthBreakdownByCurrency(currency: String) = getMonthBreakdown(startOfLastMonth, endOfLastMonth, currency)
```

- [ ] Create private `getMonthBreakdown()` helper
- [ ] Refactor 4 public methods to use helper
- [ ] Test all 4 methods still work correctly

### 1.5.2 Consolidate Currency Utilities
**Delete:** `utils/CurrencyUtils.kt`
**Keep:** `utils/CurrencyFormatter.kt`

```kotlin
// Add to CurrencyFormatter.kt if not present
object CurrencyFormatter {
    // ... existing methods

    fun sortCurrencies(
        currencies: List<String>,
        priorityCurrency: String = Currency.DEFAULT
    ): List<String> {
        return currencies.sortedWith { a, b ->
            when {
                a == priorityCurrency -> -1
                b == priorityCurrency -> 1
                else -> a.compareTo(b)
            }
        }
    }
}
```

- [ ] Review `CurrencyUtils.kt` for unique functionality
- [ ] Migrate any unique functions to `CurrencyFormatter.kt`
- [ ] Search for all `CurrencyUtils` imports
- [ ] Update imports to `CurrencyFormatter`
- [ ] Delete `CurrencyUtils.kt`
- [ ] Build and verify

---

## 1.6 Budget Entities (Prep for Phase 7)

### 1.6.1 Create Budget Entity
**New File:** `data/database/entity/BudgetEntity.kt`

```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = Currency.DEFAULT,

    @ColumnInfo(name = "period_type")
    val periodType: String = "MONTHLY",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.6.2 Create Budget History Entity
**New File:** `data/database/entity/BudgetHistoryEntity.kt`

```kotlin
@Entity(
    tableName = "budget_history",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "month")
    val month: Int,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "budget_amount")
    val budgetAmount: BigDecimal,

    @ColumnInfo(name = "spent_amount")
    val spentAmount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = Currency.DEFAULT,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val remainingAmount: BigDecimal
        get() = budgetAmount - spentAmount

    val percentUsed: Float
        get() = if (budgetAmount > BigDecimal.ZERO) {
            (spentAmount.toFloat() / budgetAmount.toFloat() * 100)
        } else 0f

    val isOverBudget: Boolean
        get() = spentAmount > budgetAmount
}
```

### 1.6.3 Create Budget DAO
**New File:** `data/database/dao/BudgetDao.kt`

```kotlin
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    fun getActiveBudget(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveBudgetOnce(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("UPDATE budgets SET is_active = 0")
    suspend fun deactivateAllBudgets()

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Query("SELECT * FROM budget_history ORDER BY year DESC, month DESC LIMIT :limit")
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetHistory(history: BudgetHistoryEntity): Long
}
```

- [ ] Create `BudgetEntity.kt`
- [ ] Create `BudgetHistoryEntity.kt`
- [ ] Create `BudgetDao.kt`
- [ ] Add to `AppDatabase`
- [ ] Create database migration

---

## 1.7 Database Migration

### Create Consolidated Migration
**File:** `data/database/migration/Migrations.kt`

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add indexes to transactions
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_merchant_name ON transactions(merchant_name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bank_name ON transactions(bank_name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_number ON transactions(account_number)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bank_account ON transactions(bank_name, account_number)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date_time ON transactions(date_time)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_is_deleted ON transactions(is_deleted)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(transaction_type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_currency ON transactions(currency)")

        // Add indexes to account_balances
        database.execSQL("CREATE INDEX IF NOT EXISTS index_account_balances_bank_account ON account_balances(bank_name, account_last4)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_account_balances_timestamp ON account_balances(timestamp)")

        // Add updated_at to account_balances
        database.execSQL("ALTER TABLE account_balances ADD COLUMN updated_at TEXT")

        // Create budgets table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount TEXT NOT NULL,
                currency TEXT NOT NULL DEFAULT 'INR',
                period_type TEXT NOT NULL DEFAULT 'MONTHLY',
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
        """)

        // Create budget_history table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS budget_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                budget_amount TEXT NOT NULL,
                spent_amount TEXT NOT NULL,
                currency TEXT NOT NULL DEFAULT 'INR',
                created_at TEXT NOT NULL
            )
        """)
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budget_history_month_year ON budget_history(month, year)")

        // Create latest_account_balances view
        database.execSQL("""
            CREATE VIEW IF NOT EXISTS latest_account_balances AS
            SELECT ab1.*
            FROM account_balances ab1
            INNER JOIN (
                SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
                FROM account_balances
                GROUP BY bank_name, account_last4
            ) ab2 ON ab1.bank_name = ab2.bank_name
                AND ab1.account_last4 = ab2.account_last4
                AND ab1.timestamp = ab2.max_timestamp
        """)
    }
}
```

- [ ] Create migration file
- [ ] Add migration to database builder
- [ ] Test migration on existing database
- [ ] Test fresh install

---

## Verification Checklist

After completing Phase 1:

- [ ] All entities use `Currency.DEFAULT`
- [ ] `AccountBalanceEntity` has `updatedAt`
- [ ] All planned indexes created
- [ ] `LatestAccountBalanceView` works
- [ ] DAO naming is consistent
- [ ] Aggregation queries added for analytics
- [ ] Monthly breakdown helper extracted
- [ ] `CurrencyUtils.kt` deleted
- [ ] `CurrencyFormatter` has `sortCurrencies()`
- [ ] Budget entities and DAO created
- [ ] Database migration works
- [ ] All existing tests pass
- [ ] App runs without crashes

---

## New Files Created

| File | Purpose |
|------|---------|
| `data/database/entity/Currency.kt` | Currency constant |
| `data/database/view/LatestAccountBalanceView.kt` | Database view |
| `data/database/dao/result/TransactionAggregateResults.kt` | Query result types |
| `data/database/entity/BudgetEntity.kt` | Budget model |
| `data/database/entity/BudgetHistoryEntity.kt` | Budget history |
| `data/database/dao/BudgetDao.kt` | Budget DAO |

## Files Deleted

| File | Reason |
|------|--------|
| `utils/CurrencyUtils.kt` | Consolidated into CurrencyFormatter |

---

## Next Phase
→ Proceed to `implementation-phase2-domain-layer.md`
