# Implementation Phase 5: Analytics Tabbed Screen

## Overview
This phase implements the tabbed analytics screen with Overview, Budget, and Insights tabs, including chart integration.

**Estimated scope:** Major UI refactor, chart library integration
**Dependencies:** Phase 4 (budget feature) should be complete

---

## 5.1 Add Chart Library

### Task: Add Vico chart library
**File:** `app/build.gradle.kts`

```kotlin
dependencies {
    // Vico Charts
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.19")
    implementation("com.patrykandpatrick.vico:core:2.0.0-alpha.19")
}
```

**Why Vico:**
- Native Compose support
- Material 3 compatible
- Smooth animations
- Active development
- Lightweight

**Implementation steps:**
- [ ] Add Vico dependencies
- [ ] Sync Gradle
- [ ] Verify no dependency conflicts

---

## 5.2 Create Chart Components

### Task: Create reusable chart composables
**New Directory:** `app/src/main/java/com/fintrace/app/ui/components/charts/`

#### SpendingTrendChart

**New File:** `ui/components/charts/SpendingTrendChart.kt`

```kotlin
@Composable
fun SpendingTrendChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    val chartEntryModelProducer = remember(data) {
        entryModelOf(data.mapIndexed { index, spending ->
            entryOf(index.toFloat(), spending.amount.toFloat())
        })
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = MaterialTheme.colorScheme.primary,
                    lineThickness = 3.dp
                )
            )
        ),
        chartModelProducer = chartEntryModelProducer,
        modifier = modifier.height(200.dp),
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ ->
                CurrencyFormatter.formatCompact(value.toBigDecimal())
            }
        ),
        bottomAxis = if (showLabels) {
            rememberBottomAxis(
                valueFormatter = { value, _ ->
                    data.getOrNull(value.toInt())?.date?.format(DateTimeFormatter.ofPattern("dd")) ?: ""
                }
            )
        } else null
    )
}

data class DailySpending(
    val date: LocalDate,
    val amount: BigDecimal
)
```

#### BudgetTrendChart

**New File:** `ui/components/charts/BudgetTrendChart.kt`

```kotlin
@Composable
fun BudgetTrendChart(
    data: List<MonthlyBudgetData>,
    modifier: Modifier = Modifier
) {
    // Two-line chart: Budget line (dashed) and Spending line (solid)
    val chartEntryModel = remember(data) {
        composedChartEntryModelOf(
            // Budget line
            entryModelOf(data.mapIndexed { index, item ->
                entryOf(index.toFloat(), item.budgetAmount?.toFloat() ?: 0f)
            }),
            // Spending line
            entryModelOf(data.mapIndexed { index, item ->
                entryOf(index.toFloat(), item.spentAmount.toFloat())
            })
        )
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = MaterialTheme.colorScheme.outline,
                    lineThickness = 2.dp,
                    lineBackgroundShader = null // No fill for budget line
                ),
                lineSpec(
                    lineColor = MaterialTheme.colorScheme.primary,
                    lineThickness = 3.dp
                )
            )
        ),
        chartModelProducer = chartEntryModel,
        modifier = modifier.height(200.dp),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                data.getOrNull(value.toInt())?.let {
                    "${it.month}/${it.year % 100}"
                } ?: ""
            }
        )
    )
}

data class MonthlyBudgetData(
    val month: Int,
    val year: Int,
    val budgetAmount: BigDecimal?,
    val spentAmount: BigDecimal
)
```

#### DayOfWeekChart

**New File:** `ui/components/charts/DayOfWeekChart.kt`

```kotlin
@Composable
fun DayOfWeekChart(
    data: List<DaySpending>,
    modifier: Modifier = Modifier
) {
    val maxAmount = data.maxOfOrNull { it.amount } ?: BigDecimal.ONE

    Column(modifier = modifier) {
        data.forEach { daySpending ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySpending.dayName.take(3),
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelMedium
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                    )
                    // Filled portion
                    val fraction = (daySpending.amount.toFloat() / maxAmount.toFloat())
                        .coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Text(
                    text = CurrencyFormatter.formatCompact(daySpending.amount),
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

data class DaySpending(
    val dayOfWeek: DayOfWeek,
    val dayName: String,
    val amount: BigDecimal
)
```

**Implementation steps:**
- [ ] Create `charts/` directory
- [ ] Create `SpendingTrendChart.kt`
- [ ] Create `BudgetTrendChart.kt`
- [ ] Create `DayOfWeekChart.kt`
- [ ] Test charts with sample data

---

## 5.3 Update AnalyticsViewModel

### Task: Add state for all three tabs
**File:** `ui/screens/analytics/AnalyticsViewModel.kt`

```kotlin
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Tab state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Overview tab state
    private val _spendingTrend = MutableStateFlow<List<DailySpending>>(emptyList())
    val spendingTrend: StateFlow<List<DailySpending>> = _spendingTrend.asStateFlow()

    // Existing: categoryBreakdown, income/expense totals

    // Budget tab state
    val budgetWithSpending: StateFlow<BudgetWithSpending?> =
        budgetRepository.getBudgetWithSpending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val budgetHistory: StateFlow<List<BudgetHistoryEntity>> =
        budgetRepository.getBudgetHistory(12)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _budgetTrendData = MutableStateFlow<List<MonthlyBudgetData>>(emptyList())
    val budgetTrendData: StateFlow<List<MonthlyBudgetData>> = _budgetTrendData.asStateFlow()

    // Insights tab state
    private val _topMerchants = MutableStateFlow<List<MerchantSpending>>(emptyList())
    val topMerchants: StateFlow<List<MerchantSpending>> = _topMerchants.asStateFlow()

    private val _dayOfWeekSpending = MutableStateFlow<List<DaySpending>>(emptyList())
    val dayOfWeekSpending: StateFlow<List<DaySpending>> = _dayOfWeekSpending.asStateFlow()

    private val _cashbackSummary = MutableStateFlow<CashbackSummary?>(null)
    val cashbackSummary: StateFlow<CashbackSummary?> = _cashbackSummary.asStateFlow()

    private val _monthComparison = MutableStateFlow<MonthComparison?>(null)
    val monthComparison: StateFlow<MonthComparison?> = _monthComparison.asStateFlow()

    private val _quickStats = MutableStateFlow<QuickStats?>(null)
    val quickStats: StateFlow<QuickStats?> = _quickStats.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
        when (index) {
            0 -> loadOverviewData()
            1 -> loadBudgetData()
            2 -> loadInsightsData()
        }
    }

    private fun loadOverviewData() {
        viewModelScope.launch {
            // Load spending trend for last 30 days
            val trend = transactionRepository.getDailySpendingTrend(30)
            _spendingTrend.value = trend
        }
    }

    private fun loadBudgetData() {
        viewModelScope.launch {
            val trendData = budgetRepository.getSpendingTrend(6)
                .first()
                .map { MonthlyBudgetData(it.month, it.year, it.budgetAmount, it.amount) }
            _budgetTrendData.value = trendData
        }
    }

    private fun loadInsightsData() {
        viewModelScope.launch {
            // Load all insights data in parallel
            launch { loadTopMerchants() }
            launch { loadDayOfWeekSpending() }
            launch { loadCashbackSummary() }
            launch { loadMonthComparison() }
            launch { loadQuickStats() }
        }
    }

    // ... implementation methods
}

// Data classes
data class MerchantSpending(
    val merchantName: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class CashbackSummary(
    val thisMonth: BigDecimal,
    val lastMonth: BigDecimal,
    val lifetime: BigDecimal,
    val byAccount: List<AccountCashback>
)

data class AccountCashback(
    val bankName: String,
    val accountLast4: String,
    val amount: BigDecimal
)

data class MonthComparison(
    val thisMonthTotal: BigDecimal,
    val lastMonthTotal: BigDecimal,
    val percentChange: Float,
    val categoryChanges: List<CategoryChange>
)

data class CategoryChange(
    val categoryName: String,
    val percentChange: Float,
    val amountChange: BigDecimal
)

data class QuickStats(
    val avgTransaction: BigDecimal,
    val totalTransactions: Int,
    val highestSpend: BigDecimal,
    val mostActiveDay: String,
    val topCategory: String
)
```

**Implementation steps:**
- [ ] Add tab state
- [ ] Add Overview tab state and loading
- [ ] Add Budget tab state and loading
- [ ] Add Insights tab state and loading
- [ ] Create all data classes
- [ ] Implement loading functions

---

## 5.4 Create Tab Content Composables

### Task: Create separate composables for each tab

#### OverviewTab

**New File:** `presentation/analytics/tabs/OverviewTab.kt`

```kotlin
@Composable
fun OverviewTab(
    spendingTrend: List<DailySpending>,
    incomeSummary: BigDecimal,
    expenseSummary: BigDecimal,
    categoryBreakdown: List<CategoryWithAmount>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spending Trend Chart
        item {
            FintraceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spending Trend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (spendingTrend.isNotEmpty()) {
                        SpendingTrendChart(
                            data = spendingTrend,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("No data available")
                    }
                }
            }
        }

        // Income/Expense Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = incomeSummary,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Expense",
                    amount = expenseSummary,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Category Breakdown
        item {
            CategoryBreakdownCard(categories = categoryBreakdown)
        }
    }
}
```

#### BudgetTab

**New File:** `presentation/analytics/tabs/BudgetTab.kt`

```kotlin
@Composable
fun BudgetTab(
    budgetWithSpending: BudgetWithSpending?,
    budgetHistory: List<BudgetHistoryEntity>,
    budgetTrendData: List<MonthlyBudgetData>,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Budget Progress
        item {
            if (budgetWithSpending != null) {
                BudgetProgressCard(budgetWithSpending = budgetWithSpending)
            } else {
                NoBudgetCard(onSetBudget = onSetBudget)
            }
        }

        // Budget vs Spending Trend
        if (budgetTrendData.isNotEmpty()) {
            item {
                FintraceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Budget vs Spending",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            LegendItem(color = MaterialTheme.colorScheme.outline, label = "Budget")
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendItem(color = MaterialTheme.colorScheme.primary, label = "Spending")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetTrendChart(
                            data = budgetTrendData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Budget History
        if (budgetHistory.isNotEmpty()) {
            item {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(budgetHistory) { historyItem ->
                BudgetHistoryItem(item = historyItem)
            }
        }
    }
}

@Composable
fun NoBudgetCard(onSetBudget: () -> Unit) {
    FintraceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Budget Set",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set a monthly budget to track your spending",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSetBudget) {
                Text("Set Budget")
            }
        }
    }
}

@Composable
fun BudgetHistoryItem(item: BudgetHistoryEntity) {
    val monthName = Month.of(item.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$monthName ${item.year}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${CurrencyFormatter.formatCompact(item.spentAmount)}/${CurrencyFormatter.formatCompact(item.budgetAmount)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (item.isOverBudget) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${item.percentUsed.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

#### InsightsTab

**New File:** `presentation/analytics/tabs/InsightsTab.kt`

```kotlin
@Composable
fun InsightsTab(
    topMerchants: List<MerchantSpending>,
    dayOfWeekSpending: List<DaySpending>,
    cashbackSummary: CashbackSummary?,
    monthComparison: MonthComparison?,
    quickStats: QuickStats?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Merchants
        item {
            TopMerchantsCard(merchants = topMerchants)
        }

        // Spending by Day
        item {
            FintraceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "When You Spend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DayOfWeekChart(
                        data = dayOfWeekSpending,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Cashback Summary
        if (cashbackSummary != null) {
            item {
                CashbackSummaryCard(summary = cashbackSummary)
            }
        }

        // Month Comparison
        if (monthComparison != null) {
            item {
                MonthComparisonCard(comparison = monthComparison)
            }
        }

        // Quick Stats
        if (quickStats != null) {
            item {
                QuickStatsCard(stats = quickStats)
            }
        }
    }
}

@Composable
fun TopMerchantsCard(merchants: List<MerchantSpending>) {
    FintraceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Merchants",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            merchants.forEachIndexed { index, merchant ->
                MerchantItem(
                    rank = index + 1,
                    merchant = merchant
                )
                if (index < merchants.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun CashbackSummaryCard(summary: CashbackSummary) {
    FintraceCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cashback Earned",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "This Month", value = CurrencyFormatter.format(summary.thisMonth))
                StatItem(label = "Last Month", value = CurrencyFormatter.format(summary.lastMonth))
                StatItem(label = "Lifetime", value = CurrencyFormatter.format(summary.lifetime))
            }

            if (summary.byAccount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "By Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                summary.byAccount.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${account.bankName} ••${account.accountLast4}")
                        Text(CurrencyFormatter.format(account.amount))
                    }
                }
            }
        }
    }
}

// ... MonthComparisonCard, QuickStatsCard implementations
```

**Implementation steps:**
- [ ] Create `tabs/` directory under `presentation/analytics/`
- [ ] Create `OverviewTab.kt`
- [ ] Create `BudgetTab.kt` with NoBudgetCard, BudgetHistoryItem
- [ ] Create `InsightsTab.kt` with all insight cards
- [ ] Create helper composables (SummaryCard, StatItem, MerchantItem, etc.)

---

## 5.5 Refactor AnalyticsScreen

### Task: Implement tabbed layout
**File:** `ui/screens/analytics/AnalyticsScreen.kt`

```kotlin
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    // Overview tab state
    val spendingTrend by viewModel.spendingTrend.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    // ... other overview state

    // Budget tab state
    val budgetWithSpending by viewModel.budgetWithSpending.collectAsState()
    val budgetHistory by viewModel.budgetHistory.collectAsState()
    val budgetTrendData by viewModel.budgetTrendData.collectAsState()

    // Insights tab state
    val topMerchants by viewModel.topMerchants.collectAsState()
    val dayOfWeekSpending by viewModel.dayOfWeekSpending.collectAsState()
    val cashbackSummary by viewModel.cashbackSummary.collectAsState()
    val monthComparison by viewModel.monthComparison.collectAsState()
    val quickStats by viewModel.quickStats.collectAsState()

    val tabs = listOf("Overview", "Budget", "Insights")

    FintraceScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = { /* sync */ }) {
                        Icon(Icons.Default.Sync, "Sync")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(
                    spendingTrend = spendingTrend,
                    incomeSummary = incomeSummary,
                    expenseSummary = expenseSummary,
                    categoryBreakdown = categoryBreakdown
                )
                1 -> BudgetTab(
                    budgetWithSpending = budgetWithSpending,
                    budgetHistory = budgetHistory,
                    budgetTrendData = budgetTrendData,
                    onSetBudget = { showBudgetDialog = true }
                )
                2 -> InsightsTab(
                    topMerchants = topMerchants,
                    dayOfWeekSpending = dayOfWeekSpending,
                    cashbackSummary = cashbackSummary,
                    monthComparison = monthComparison,
                    quickStats = quickStats
                )
            }
        }
    }
}
```

**Implementation steps:**
- [ ] Add TabRow to AnalyticsScreen
- [ ] Connect to ViewModel tab state
- [ ] Implement tab switching
- [ ] Add tab content switching with when statement
- [ ] Test tab navigation
- [ ] Verify data loads when switching tabs

---

## 5.6 Add Repository Query Methods

### Task: Add missing repository methods for analytics data
**File:** `data/repository/TransactionRepository.kt`

```kotlin
// Add these methods
suspend fun getDailySpendingTrend(days: Int): List<DailySpending>

suspend fun getSpendingByDayOfWeek(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): List<DaySpending>

suspend fun getTopMerchants(
    limit: Int,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): List<MerchantSpending>

suspend fun getCashbackSummary(): CashbackSummary

suspend fun getMonthComparison(): MonthComparison

suspend fun getQuickStats(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): QuickStats
```

**File:** `data/database/dao/TransactionDao.kt`

```kotlin
// Add these queries
@Query("""
    SELECT DATE(date_time) as date, SUM(amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND transaction_type = 'EXPENSE'
    AND date_time >= :startDate
    GROUP BY DATE(date_time)
    ORDER BY date_time ASC
""")
suspend fun getDailySpending(startDate: LocalDateTime): List<DailySpendingRaw>

@Query("""
    SELECT merchant_name, SUM(amount) as total, COUNT(*) as count
    FROM transactions
    WHERE is_deleted = 0
    AND transaction_type = 'EXPENSE'
    AND date_time BETWEEN :startDate AND :endDate
    GROUP BY merchant_name
    ORDER BY total DESC
    LIMIT :limit
""")
suspend fun getTopMerchants(
    limit: Int,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): List<MerchantSpendingRaw>

@Query("""
    SELECT SUM(cashback_amount) as total
    FROM transactions
    WHERE is_deleted = 0
    AND cashback_amount > 0
    AND date_time BETWEEN :startDate AND :endDate
""")
suspend fun getTotalCashback(
    startDate: LocalDateTime,
    endDate: LocalDateTime
): BigDecimal?
```

**Implementation steps:**
- [ ] Add raw query result data classes to DAO
- [ ] Add DAO query methods
- [ ] Add repository methods
- [ ] Implement data transformations
- [ ] Test queries with real data

---

## Verification Checklist

After completing Phase 5:

- [ ] Vico chart library integrated
- [ ] TabRow displays 3 tabs correctly
- [ ] Tab switching works smoothly
- [ ] Overview tab shows:
  - [ ] Spending trend line chart
  - [ ] Income/Expense summary cards
  - [ ] Category breakdown
- [ ] Budget tab shows:
  - [ ] Current budget progress (or "Set Budget" prompt)
  - [ ] Budget vs Spending trend chart
  - [ ] Budget history list
- [ ] Insights tab shows:
  - [ ] Top merchants with bars
  - [ ] Day of week spending chart
  - [ ] Cashback summary
  - [ ] Month comparison
  - [ ] Quick stats
- [ ] All data loads correctly
- [ ] No performance issues with charts
- [ ] Light and dark themes work

---

## New Files Created

| File | Purpose |
|------|---------|
| `ui/components/charts/SpendingTrendChart.kt` | Line chart for spending trend |
| `ui/components/charts/BudgetTrendChart.kt` | Two-line budget vs spending |
| `ui/components/charts/DayOfWeekChart.kt` | Horizontal bar chart |
| `presentation/analytics/tabs/OverviewTab.kt` | Overview tab content |
| `presentation/analytics/tabs/BudgetTab.kt` | Budget tab content |
| `presentation/analytics/tabs/InsightsTab.kt` | Insights tab content |

---

## Next Steps

After all phases complete:
1. Full regression testing
2. Performance profiling
3. UI polish and animations
4. Documentation updates
