# PennyWise Features Roadmap

## Overview
This document tracks all planned features, changes, and fixes for the upcoming release.

---

## 1. Data Model Changes

### 1.1 Simplify Transaction Types
**Priority:** High

- [ ] Remove `CREDIT` from `TransactionType` enum (keep as account type only)
- [ ] Remove `INVESTMENT` from `TransactionType` enum
- [ ] Keep only: `INCOME`, `EXPENSE`, `TRANSFER`
- [ ] Update all bank parsers to use only 3 types
- [ ] Update UI filters to show only 3 options
- [ ] No migration needed (alpha stage)

---

## 2. Feature Removals (Simplification)

### 2.1 Remove Developer Options
**Priority:** High

- [ ] Remove developer options section from Settings
- [ ] Remove related ViewModel state/functions
- [ ] Clean up unused code

### 2.2 Remove Support & Community Features
**Priority:** High

- [ ] Remove support section from Settings
- [ ] Remove community section (Discord links, etc.)
- [ ] Remove GitHub issue templates reference
- [ ] Clean up unused strings/resources

### 2.3 Remove App Lock Feature
**Priority:** High

- [ ] Remove app lock toggle from Settings
- [ ] Remove lock screen UI
- [ ] Remove biometric authentication code
- [ ] Remove lock-related SharedPreferences
- [ ] Remove lock check from app startup
- [ ] Remove biometric library dependency if unused

---

## 3. Settings Changes

### 3.1 SMS Scan Period Options
**Priority:** High

**New options:**
- [ ] 1 day
- [ ] 1 week
- [ ] 15 days
- [ ] Install date (only transactions after app installation)

**Implementation:**
- [ ] Update scan period enum/options
- [ ] Store app install date on first launch (SharedPreferences)
- [ ] Update `OptimizedSmsReaderWorker` date calculation
- [ ] Update Settings UI picker

---

## 4. UI Fixes

### 4.1 Pending Transactions Screen - Missing Actions
**Priority:** Medium

- [ ] Add sync icon to TopAppBar
- [ ] Add settings icon to TopAppBar
- [ ] Ensure consistency with other screens

### 4.2 Categories Overview - Line Separator
**Priority:** Low

- [ ] Add `HorizontalDivider` between category items
- [ ] Skip divider after last item

### 4.3 Filter Card - Consistent Padding
**Priority:** Medium

- [ ] Fix padding to be constant regardless of filter state
- [ ] Reserve space for filter count badge
- [ ] Reserve space for clear button
- [ ] No layout jump when filters applied/removed

### 4.4 Transactions Screen - Income/Expense/Net Alignment
**Priority:** Medium

- [ ] Fix alignment of summary card items
- [ ] Use consistent `Arrangement` and `Alignment`
- [ ] Consider extracting to reusable `SummaryCard`

### 4.5 Home Screen - Center Align Total Balance
**Priority:** Low

- [ ] Change balance text to `TextAlign.Center`
- [ ] Ensure parent uses `Alignment.CenterHorizontally`

---

## 5. Transaction Details Enhancement

### 5.1 Show Cashback in Transaction Details
**Priority:** Medium

- [ ] Add cashback section to transaction details view
- [ ] Display `cashback_percent` (e.g., "2%")
- [ ] Display `cashback_amount` (e.g., "₹15.00")
- [ ] Only show if cashback exists (non-null, > 0)

---

## 6. Bug Fixes

### 6.1 Custom Cashback Incorrectly Applied Retroactively
**Priority:** Critical

**Problem:** When confirming pending transaction with custom cashback (e.g., 5%), it applies to ALL existing transactions for that account.

**Expected:**
- Custom cashback on pending → applies to THAT transaction only
- Default cashback from Manage Accounts → applies retroactively

**Fix:**
- [ ] Don't call `applyRetroactiveCashback()` with custom percentage
- [ ] Separate "apply to this transaction" vs "apply retroactively" logic
- [ ] Fix in `PendingTransactionManager.confirmTransaction()`

---

## 7. Account Overview Enhancements

### 7.1 Show Remaining Limit for Credit Accounts
**Priority:** Medium

- [ ] For credit accounts, display:
  - Credit Limit: ₹50,000
  - Used: ₹15,000
  - **Available: ₹35,000** (limit - balance)
- [ ] Different display for credit vs savings accounts
- [ ] Calculate: `available = creditLimit - balance`

---

## 8. Analytics - Tabbed Screen

### 8.1 Implement Tab Layout
**Priority:** High

**Tabs:**
1. **Overview** - Spending trend, summary cards, category breakdown
2. **Budget** - Budget progress, history, trends
3. **Insights** - Top merchants, patterns, cashback, comparisons

- [ ] Implement `TabRow` with 3 tabs
- [ ] Create `OverviewTab` composable
- [ ] Create `BudgetTab` composable
- [ ] Create `InsightsTab` composable
- [ ] Maintain scroll position per tab

### 8.2 Overview Tab
**Priority:** High

- [ ] Spending trend line chart (daily/weekly/monthly)
- [ ] Summary cards (income/expense/net)
- [ ] Category breakdown donut chart (existing)
- [ ] Add chart library (Vico recommended)

### 8.3 Budget Tab
**Priority:** High

- [ ] Current month budget progress bar
- [ ] Budget amount display (spent / total)
- [ ] Remaining budget with percentage
- [ ] Color states: Green (0-70%), Yellow (70-90%), Red (90%+)
- [ ] Budget vs Spending trend chart (line chart)
- [ ] Budget history list (past 6-12 months)

### 8.4 Insights Tab
**Priority:** Medium

**Top Merchants:**
- [ ] Top 5 merchants by spend
- [ ] Percentage bars
- [ ] "View All" option

**Spending by Day of Week:**
- [ ] Horizontal bar chart
- [ ] Highlight peak days
- [ ] Insight text (e.g., "You spend 42% on weekends")

**Cashback Earned:**
- [ ] This month total
- [ ] Last month comparison (% change)
- [ ] Lifetime total
- [ ] By account breakdown

**Month Comparison:**
- [ ] This month vs last month total
- [ ] Category changes (↑↓ percentages)
- [ ] Highlight biggest changes

**Account Usage:**
- [ ] Most used accounts by transaction count
- [ ] Percentage bars
- [ ] Transaction count per account

**Quick Stats:**
- [ ] Average transaction amount
- [ ] Total transaction count
- [ ] Highest single spend
- [ ] Most active day
- [ ] Top category

---

## 9. Budget Feature

### 9.1 Budget Data Model
**Priority:** High

**Option: Database Entity**
```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: BigDecimal,
    val currency: String = "INR",
    val periodType: String = "MONTHLY",
    val isActive: Boolean = true,
    val createdAt: LocalDateTime
)

@Entity(tableName = "budget_history")
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val year: Int,
    val budgetAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val currency: String = "INR",
    val createdAt: LocalDateTime
)
```

- [ ] Create `BudgetEntity`
- [ ] Create `BudgetHistoryEntity`
- [ ] Create `BudgetDao`
- [ ] Create database migration
- [ ] Add to `AppDatabase`

### 9.2 Budget Repository
**Priority:** High

```kotlin
interface BudgetRepository {
    fun getCurrentBudget(): Flow<BudgetEntity?>
    suspend fun setBudget(amount: BigDecimal, currency: String)
    suspend fun deleteBudget()
    fun getBudgetHistory(limit: Int = 12): Flow<List<BudgetHistoryEntity>>
    suspend fun saveBudgetSnapshot(month: Int, year: Int, budgetAmount: BigDecimal, spentAmount: BigDecimal)
}
```

- [ ] Create `BudgetRepository` interface
- [ ] Implement `BudgetRepositoryImpl`
- [ ] Add Hilt binding

### 9.3 Budget Settings UI
**Priority:** High

- [ ] Add budget section to Settings OR dedicated Budget screen
- [ ] Input field for budget amount
- [ ] Toggle to enable/disable budget
- [ ] Save budget to database

### 9.4 Budget Snapshot Worker (Optional)
**Priority:** Low

- [ ] Monthly WorkManager job to snapshot budget vs spent
- [ ] Runs on 1st of each month
- [ ] Saves to `budget_history` table

---

## 10. Chart Library Integration

### 10.1 Add Vico Chart Library
**Priority:** High

- [ ] Add Vico dependency to build.gradle
- [ ] Create reusable chart composables:
  - [ ] `SpendingTrendChart` (line chart)
  - [ ] `BudgetProgressChart` (progress bar/gauge)
  - [ ] `BudgetTrendChart` (line chart with budget line)
  - [ ] `DayOfWeekChart` (horizontal bar chart)

---

## Priority Summary

### Critical
1. Custom cashback retroactive bug fix

### High Priority
1. Simplify transaction types (INCOME, EXPENSE, TRANSFER only)
2. Remove developer options, support, community, app lock
3. SMS scan period options (1d, 1w, 15d, install date)
4. Tabbed analytics screen
5. Budget feature (data model, repository, UI)
6. Spending trend chart
7. Chart library integration

### Medium Priority
1. Pending transactions screen - add sync/settings
2. Filter card consistent padding
3. Transactions screen alignment fix
4. Transaction details - show cashback
5. Credit account - show remaining limit
6. Insights tab content

### Low Priority
1. Categories overview line separator
2. Home screen center align balance
3. Budget snapshot worker

---

## Dependencies

| Feature | Depends On |
|---------|------------|
| Tabbed Analytics | Chart library |
| Spending Trend | Chart library |
| Budget Tab | Budget data model, Budget repository |
| Insights Tab | Various aggregation queries |

---

## Estimated New Files

| File | Purpose |
|------|---------|
| `data/database/entity/BudgetEntity.kt` | Budget data model |
| `data/database/entity/BudgetHistoryEntity.kt` | Budget history |
| `data/database/dao/BudgetDao.kt` | Budget database operations |
| `data/repository/BudgetRepository.kt` | Budget business logic |
| `ui/components/charts/SpendingTrendChart.kt` | Line chart component |
| `ui/components/charts/BudgetProgressBar.kt` | Progress bar component |
| `presentation/analytics/tabs/OverviewTab.kt` | Analytics overview tab |
| `presentation/analytics/tabs/BudgetTab.kt` | Analytics budget tab |
| `presentation/analytics/tabs/InsightsTab.kt` | Analytics insights tab |
