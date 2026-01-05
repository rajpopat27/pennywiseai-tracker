# Implementation Phase 4: Budget Feature

## Overview
This phase implements the complete budget feature including data model, repository, UI, and analytics integration.

**Estimated scope:** New feature implementation
**Dependencies:** Phase 3 (architecture) should be complete

---

## 4.1 Create Budget Data Model

### Task: Create budget entities
**New File:** `app/src/main/java/com/fintrace/app/data/database/entity/BudgetEntity.kt`

```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "period_type")
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class BudgetPeriod {
    MONTHLY
    // Future: WEEKLY, YEARLY
}
```

**New File:** `app/src/main/java/com/fintrace/app/data/database/entity/BudgetHistoryEntity.kt`

```kotlin
@Entity(
    tableName = "budget_history",
    indices = [
        Index(value = ["month", "year"], unique = true)
    ]
)
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "month")
    val month: Int, // 1-12

    @ColumnInfo(name = "year")
    val year: Int, // 2024, 2025, etc.

    @ColumnInfo(name = "budget_amount")
    val budgetAmount: BigDecimal,

    @ColumnInfo(name = "spent_amount")
    val spentAmount: BigDecimal,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val remainingAmount: BigDecimal
        get() = budgetAmount - spentAmount

    val percentUsed: Float
        get() = if (budgetAmount > BigDecimal.ZERO) {
            (spentAmount.toFloat() / budgetAmount.toFloat() * 100).coerceIn(0f, 150f)
        } else 0f

    val isOverBudget: Boolean
        get() = spentAmount > budgetAmount
}
```

**Implementation steps:**
- [ ] Create `BudgetEntity.kt`
- [ ] Create `BudgetHistoryEntity.kt`
- [ ] Create `BudgetPeriod` enum
- [ ] Add type converters for `BigDecimal` if not exists
- [ ] Add type converters for `BudgetPeriod`

---

## 4.2 Create Budget DAO

### Task: Create database access object
**New File:** `app/src/main/java/com/fintrace/app/data/database/dao/BudgetDao.kt`

```kotlin
@Dao
interface BudgetDao {

    // Current Budget
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

    // Budget History
    @Query("""
        SELECT * FROM budget_history
        ORDER BY year DESC, month DESC
        LIMIT :limit
    """)
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>

    @Query("SELECT * FROM budget_history WHERE month = :month AND year = :year")
    suspend fun getBudgetHistoryForMonth(month: Int, year: Int): BudgetHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetHistory(history: BudgetHistoryEntity): Long

    @Query("""
        SELECT * FROM budget_history
        WHERE (year > :startYear OR (year = :startYear AND month >= :startMonth))
        ORDER BY year ASC, month ASC
    """)
    fun getBudgetHistorySince(startMonth: Int, startYear: Int): Flow<List<BudgetHistoryEntity>>
}
```

**Implementation steps:**
- [ ] Create `BudgetDao.kt`
- [ ] Add to `AppDatabase` DAOs list
- [ ] Create database migration (increment version)

---

## 4.3 Create Budget Repository

### Task: Create repository for budget operations
**New File:** `app/src/main/java/com/fintrace/app/data/repository/BudgetRepository.kt`

```kotlin
interface BudgetRepository {
    // Current budget
    fun getActiveBudget(): Flow<BudgetEntity?>
    suspend fun setBudget(amount: BigDecimal, currency: String = "INR")
    suspend fun deleteBudget()

    // Budget with spending calculation
    fun getBudgetWithSpending(): Flow<BudgetWithSpending?>

    // History
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>
    suspend fun saveBudgetSnapshot(month: Int, year: Int)

    // Trend data
    fun getSpendingTrend(months: Int = 6): Flow<List<MonthlySpending>>
}

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val spentThisMonth: BigDecimal,
    val remaining: BigDecimal,
    val percentUsed: Float
)

data class MonthlySpending(
    val month: Int,
    val year: Int,
    val amount: BigDecimal,
    val budgetAmount: BigDecimal? // null if no budget was set that month
)
```

**New File:** `app/src/main/java/com/fintrace/app/data/repository/BudgetRepositoryImpl.kt`

```kotlin
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getActiveBudget(): Flow<BudgetEntity?> =
        budgetDao.getActiveBudget()

    override suspend fun setBudget(amount: BigDecimal, currency: String) {
        // Deactivate existing budgets
        budgetDao.deactivateAllBudgets()

        // Create new active budget
        budgetDao.insertBudget(
            BudgetEntity(
                amount = amount,
                currency = currency,
                isActive = true
            )
        )
    }

    override fun getBudgetWithSpending(): Flow<BudgetWithSpending?> {
        return budgetDao.getActiveBudget().map { budget ->
            budget?.let {
                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay()
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .toLocalDate().atTime(23, 59, 59)

                val spent = transactionDao.getTotalAmountByTypeAndPeriod(
                    type = TransactionType.EXPENSE,
                    startDate = startOfMonth,
                    endDate = endOfMonth
                )?.toBigDecimal() ?: BigDecimal.ZERO

                BudgetWithSpending(
                    budget = it,
                    spentThisMonth = spent,
                    remaining = it.amount - spent,
                    percentUsed = if (it.amount > BigDecimal.ZERO) {
                        (spent.toFloat() / it.amount.toFloat() * 100)
                    } else 0f
                )
            }
        }
    }

    // ... other implementations
}
```

**Add DI binding:**
```kotlin
// In AppModule.kt
@Binds
@Singleton
abstract fun bindBudgetRepository(
    impl: BudgetRepositoryImpl
): BudgetRepository
```

**Implementation steps:**
- [ ] Create `BudgetRepository.kt` interface
- [ ] Create `BudgetWithSpending` data class
- [ ] Create `MonthlySpending` data class
- [ ] Create `BudgetRepositoryImpl.kt`
- [ ] Add Hilt binding
- [ ] Test repository functions

---

## 4.4 Create Budget Settings UI

### Task: Add budget configuration to Settings
**Option A:** Add to existing SettingsScreen
**Option B:** Create dedicated BudgetSettingsScreen

**Recommended: Option A for simplicity**

```kotlin
// In SettingsScreen
// Add Budget section
SettingsSection(title = "Budget") {
    val budgetState by viewModel.budgetState.collectAsState()

    when (budgetState) {
        is BudgetState.NoBudget -> {
            SettingsItem(
                title = "Set Monthly Budget",
                subtitle = "Track your spending against a monthly limit",
                onClick = { showBudgetDialog = true }
            )
        }
        is BudgetState.HasBudget -> {
            val budget = (budgetState as BudgetState.HasBudget).budget
            SettingsItem(
                title = "Monthly Budget",
                subtitle = CurrencyFormatter.format(budget.amount),
                onClick = { showBudgetDialog = true }
            )
            SettingsItem(
                title = "Remove Budget",
                onClick = { viewModel.removeBudget() }
            )
        }
    }
}

// Budget dialog
if (showBudgetDialog) {
    BudgetInputDialog(
        currentAmount = currentBudget?.amount,
        onConfirm = { amount ->
            viewModel.setBudget(amount)
            showBudgetDialog = false
        },
        onDismiss = { showBudgetDialog = false }
    )
}
```

**New composable:** `BudgetInputDialog`
```kotlin
@Composable
fun BudgetInputDialog(
    currentAmount: BigDecimal?,
    onConfirm: (BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(currentAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toBigDecimalOrNull()?.let(onConfirm) },
                enabled = amount.toBigDecimalOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

**Implementation steps:**
- [ ] Add budget state to SettingsViewModel
- [ ] Create `BudgetInputDialog` composable
- [ ] Add budget section to SettingsScreen
- [ ] Implement `setBudget()` and `removeBudget()` in ViewModel
- [ ] Test budget creation and removal

---

## 4.5 Update AppDatabase

### Task: Add new entities and migration
**File:** `app/src/main/java/com/fintrace/app/data/database/AppDatabase.kt`

```kotlin
@Database(
    entities = [
        // ... existing entities
        BudgetEntity::class,
        BudgetHistoryEntity::class
    ],
    version = X, // Increment version
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // ... existing DAOs
    abstract fun budgetDao(): BudgetDao
}

// Migration
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
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

        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_budget_history_month_year
            ON budget_history (month, year)
        """)
    }
}
```

**Implementation steps:**
- [ ] Add entities to `@Database` annotation
- [ ] Add `budgetDao()` abstract function
- [ ] Increment database version
- [ ] Create migration
- [ ] Add migration to database builder
- [ ] Test migration on existing database

---

## 4.6 Create Budget Progress Component

### Task: Create reusable budget progress UI
**New File:** `app/src/main/java/com/fintrace/app/ui/components/budget/BudgetProgressCard.kt`

```kotlin
@Composable
fun BudgetProgressCard(
    budgetWithSpending: BudgetWithSpending,
    modifier: Modifier = Modifier
) {
    val percentUsed = budgetWithSpending.percentUsed
    val progressColor = when {
        percentUsed >= 100 -> MaterialTheme.colorScheme.error
        percentUsed >= 90 -> MaterialTheme.colorScheme.error
        percentUsed >= 70 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    FintraceCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Monthly Budget",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(budgetWithSpending.spentThisMonth),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(budgetWithSpending.budget.amount),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remaining
            Text(
                text = if (budgetWithSpending.remaining >= BigDecimal.ZERO) {
                    "${CurrencyFormatter.format(budgetWithSpending.remaining)} remaining"
                } else {
                    "${CurrencyFormatter.format(budgetWithSpending.remaining.abs())} over budget"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (budgetWithSpending.remaining >= BigDecimal.ZERO) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
```

**Implementation steps:**
- [ ] Create `BudgetProgressCard.kt`
- [ ] Implement color logic based on percentage
- [ ] Test with various budget/spending scenarios
- [ ] Test over-budget display

---

## Verification Checklist

After completing Phase 4:

- [ ] Can set monthly budget in Settings
- [ ] Budget stored in database
- [ ] Can remove budget
- [ ] Budget progress card shows correctly
- [ ] Color changes based on percentage used
- [ ] Over-budget state displays correctly
- [ ] Budget history saved correctly
- [ ] All database migrations work
- [ ] No crashes on fresh install
- [ ] No crashes on upgrade from previous version

---

## New Files Created

| File | Purpose |
|------|---------|
| `data/database/entity/BudgetEntity.kt` | Budget data model |
| `data/database/entity/BudgetHistoryEntity.kt` | Budget history model |
| `data/database/dao/BudgetDao.kt` | Database operations |
| `data/repository/BudgetRepository.kt` | Repository interface |
| `data/repository/BudgetRepositoryImpl.kt` | Repository implementation |
| `ui/components/budget/BudgetProgressCard.kt` | Progress UI component |
| `ui/components/dialogs/BudgetInputDialog.kt` | Budget input dialog |

---

## Next Phase
→ Proceed to `implementation-phase5-analytics-tabs.md`
