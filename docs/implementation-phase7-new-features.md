# Phase 7: New Features

## Overview
Implement new features on top of the clean architecture established in Phases 1-6.

**Goal:** Add Budget feature and Tabbed Analytics with charts.

---

## 7.1 Chart Library Integration

### 7.1.1 Add Vico Dependency
**File:** `app/build.gradle.kts`

```kotlin
dependencies {
    // Vico Charts for Compose
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.19")
    implementation("com.patrykandpatrick.vico:core:2.0.0-alpha.19")
}
```

- [ ] Add Vico dependencies
- [ ] Sync Gradle
- [ ] Verify no conflicts

### 7.1.2 Create Chart Components
**Directory:** `ui/components/charts/`

#### SpendingTrendChart
**File:** `ui/components/charts/SpendingTrendChart.kt`

```kotlin
@Composable
fun SpendingTrendChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(message = "No spending data")
        return
    }

    val modelProducer = remember(data) {
        CartesianChartModelProducer.build {
            lineSeries {
                series(data.map { it.amount.toFloat() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                LineCartesianLayer.LineSpec(
                    shader = DynamicShaders.color(MaterialTheme.colorScheme.primary)
                )
            ),
            startAxis = rememberStartAxis(
                valueFormatter = { value, _, _ ->
                    CurrencyFormatter.formatCompact(value.toBigDecimal())
                }
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _, _ ->
                    data.getOrNull(value.toInt())?.date?.format(
                        DateTimeFormatter.ofPattern("dd")
                    ) ?: ""
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(200.dp)
    )
}

data class DailySpending(
    val date: LocalDate,
    val amount: BigDecimal
)

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

#### BudgetComparisonChart
**File:** `ui/components/charts/BudgetComparisonChart.kt`

```kotlin
@Composable
fun BudgetComparisonChart(
    data: List<MonthlyBudgetData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder(message = "No budget history")
        return
    }

    // Two lines: budget (dashed) and spending (solid)
    val modelProducer = remember(data) {
        CartesianChartModelProducer.build {
            lineSeries {
                // Budget line
                series(data.map { it.budgetAmount?.toFloat() ?: 0f })
            }
            lineSeries {
                // Spending line
                series(data.map { it.spentAmount.toFloat() })
            }
        }
    }

    Column(modifier = modifier) {
        // Legend
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(
                color = MaterialTheme.colorScheme.outline,
                label = "Budget"
            )
            LegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = "Spending"
            )
        }

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    listOf(
                        LineCartesianLayer.LineSpec(
                            shader = DynamicShaders.color(MaterialTheme.colorScheme.outline)
                        ),
                        LineCartesianLayer.LineSpec(
                            shader = DynamicShaders.color(MaterialTheme.colorScheme.primary)
                        )
                    )
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _, _ ->
                        data.getOrNull(value.toInt())?.let {
                            "${it.month}/${it.year % 100}"
                        } ?: ""
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier.height(200.dp)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

data class MonthlyBudgetData(
    val month: Int,
    val year: Int,
    val budgetAmount: BigDecimal?,
    val spentAmount: BigDecimal
)
```

#### DayOfWeekChart
**File:** `ui/components/charts/DayOfWeekChart.kt`

```kotlin
@Composable
fun DayOfWeekChart(
    data: List<DaySpending>,
    modifier: Modifier = Modifier
) {
    val maxAmount = data.maxOfOrNull { it.amount } ?: BigDecimal.ONE
    val peakDay = data.maxByOrNull { it.amount }?.dayName

    Column(modifier = modifier) {
        data.forEach { daySpending ->
            val isPeak = daySpending.dayName == peakDay

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySpending.dayName.take(3),
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
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
                    // Filled bar
                    val fraction = (daySpending.amount.toFloat() / maxAmount.toFloat())
                        .coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(
                                if (isPeak)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Text(
                    text = CurrencyFormatter.formatCompact(daySpending.amount),
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                )

                if (isPeak) {
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

- [ ] Create `SpendingTrendChart.kt`
- [ ] Create `BudgetComparisonChart.kt`
- [ ] Create `DayOfWeekChart.kt`
- [ ] Test charts with sample data

---

## 7.2 Budget Settings UI

### 7.2.1 Add Budget to Settings
**File:** `presentation/settings/SettingsScreen.kt`

```kotlin
// Add Budget section
item {
    SettingsSection(title = "Budget") {
        val budgetState by viewModel.activeBudget.collectAsState()

        when {
            budgetState == null -> {
                SettingsItem(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Set Monthly Budget",
                    subtitle = "Track your spending against a monthly limit",
                    onClick = { showBudgetDialog = true }
                )
            }
            else -> {
                SettingsItem(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Monthly Budget",
                    subtitle = CurrencyFormatter.format(budgetState!!.amount),
                    onClick = { showBudgetDialog = true }
                )
            }
        }
    }
}

// Budget dialog
if (showBudgetDialog) {
    BudgetInputDialog(
        currentAmount = activeBudget?.amount,
        onConfirm = { amount ->
            viewModel.setBudget(amount)
            showBudgetDialog = false
        },
        onDelete = if (activeBudget != null) {
            {
                viewModel.deleteBudget()
                showBudgetDialog = false
            }
        } else null,
        onDismiss = { showBudgetDialog = false }
    )
}
```

### 7.2.2 Create BudgetInputDialog
**File:** `ui/components/dialogs/BudgetInputDialog.kt`

```kotlin
@Composable
fun BudgetInputDialog(
    currentAmount: BigDecimal?,
    onConfirm: (BigDecimal) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var amount by remember {
        mutableStateOf(currentAmount?.toPlainString() ?: "")
    }
    val isValid = amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (currentAmount != null) "Edit Budget" else "Set Monthly Budget"
            )
        },
        text = {
            Column {
                Text(
                    text = "Set your monthly spending limit. You'll see progress in Analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = it
                        }
                    },
                    label = { Text("Budget Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amount.toBigDecimalOrNull()?.let(onConfirm) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
```

### 7.2.3 Update SettingsViewModel
**File:** `presentation/settings/SettingsViewModel.kt`

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    // ... other dependencies
) : ViewModel() {

    val activeBudget: StateFlow<BudgetEntity?> =
        budgetRepository.getActiveBudget()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setBudget(amount: BigDecimal) {
        viewModelScope.launch {
            budgetRepository.setBudget(amount, Currency.DEFAULT)
        }
    }

    fun deleteBudget() {
        viewModelScope.launch {
            budgetRepository.deleteBudget()
        }
    }
}
```

- [ ] Add budget section to SettingsScreen
- [ ] Create `BudgetInputDialog.kt`
- [ ] Update SettingsViewModel with budget functions
- [ ] Test budget creation and deletion

---

## 7.3 Tabbed Analytics Screen

### 7.3.1 Update AnalyticsViewModel
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

    // Filter state
    val filterStateManager = FilterStateManager(savedStateHandle, viewModelScope)

    // === Overview Tab ===
    private val _spendingTrend = MutableStateFlow<List<DailySpending>>(emptyList())
    val spendingTrend: StateFlow<List<DailySpending>> = _spendingTrend.asStateFlow()

    val categoryBreakdown: StateFlow<List<CategoryAmount>> =
        filterStateManager.filterState.flatMapLatest { filter ->
            val (start, end) = filterStateManager.getDateRange()
            transactionRepository.getCategoryBreakdown(
                start.atStartOfDay(),
                end.atTime(23, 59, 59),
                filter.currency
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === Budget Tab ===
    val budgetWithSpending: StateFlow<BudgetWithSpending?> =
        budgetRepository.getBudgetWithSpending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val budgetHistory: StateFlow<List<BudgetHistoryEntity>> =
        budgetRepository.getBudgetHistory(12)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _budgetTrendData = MutableStateFlow<List<MonthlyBudgetData>>(emptyList())
    val budgetTrendData: StateFlow<List<MonthlyBudgetData>> = _budgetTrendData.asStateFlow()

    // === Insights Tab ===
    private val _topMerchants = MutableStateFlow<List<MerchantSpending>>(emptyList())
    val topMerchants: StateFlow<List<MerchantSpending>> = _topMerchants.asStateFlow()

    private val _dayOfWeekSpending = MutableStateFlow<List<DaySpending>>(emptyList())
    val dayOfWeekSpending: StateFlow<List<DaySpending>> = _dayOfWeekSpending.asStateFlow()

    private val _cashbackSummary = MutableStateFlow<CashbackSummary?>(null)
    val cashbackSummary: StateFlow<CashbackSummary?> = _cashbackSummary.asStateFlow()

    private val _quickStats = MutableStateFlow<QuickStats?>(null)
    val quickStats: StateFlow<QuickStats?> = _quickStats.asStateFlow()

    init {
        loadOverviewData()
    }

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
            val (start, end) = filterStateManager.getDateRange()
            val trend = transactionRepository.getDailySpendingTrend(
                start.atStartOfDay(),
                end.atTime(23, 59, 59),
                filterStateManager.currency.value
            )
            _spendingTrend.value = trend
        }
    }

    private fun loadBudgetData() {
        viewModelScope.launch {
            // Load last 6 months of budget vs spending data
            val history = budgetRepository.getBudgetHistory(6).first()
            _budgetTrendData.value = history.map { h ->
                MonthlyBudgetData(
                    month = h.month,
                    year = h.year,
                    budgetAmount = h.budgetAmount,
                    spentAmount = h.spentAmount
                )
            }
        }
    }

    private fun loadInsightsData() {
        viewModelScope.launch {
            val (start, end) = filterStateManager.getDateRange()
            val currency = filterStateManager.currency.value

            // Load in parallel
            launch {
                _topMerchants.value = transactionRepository.getTopMerchants(
                    start.atStartOfDay(), end.atTime(23, 59, 59), currency, 5
                )
            }
            launch {
                _dayOfWeekSpending.value = transactionRepository.getSpendingByDayOfWeek(
                    start.atStartOfDay(), end.atTime(23, 59, 59), currency
                )
            }
            launch {
                _cashbackSummary.value = transactionRepository.getCashbackSummary(
                    start.atStartOfDay(), end.atTime(23, 59, 59)
                )
            }
            launch {
                _quickStats.value = transactionRepository.getQuickStats(
                    start.atStartOfDay(), end.atTime(23, 59, 59), currency
                )
            }
        }
    }
}
```

- [ ] Update AnalyticsViewModel with tab state
- [ ] Add Overview tab data loading
- [ ] Add Budget tab data loading
- [ ] Add Insights tab data loading

### 7.3.2 Create Tab Content Composables

#### OverviewTab
**File:** `presentation/analytics/tabs/OverviewTab.kt`

```kotlin
@Composable
fun OverviewTab(
    spendingTrend: List<DailySpending>,
    categoryBreakdown: List<CategoryAmount>,
    income: BigDecimal,
    expense: BigDecimal,
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
                    SpendingTrendChart(
                        data = spendingTrend,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Income / Expense Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = income,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Expense",
                    amount = expense,
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
**File:** `presentation/analytics/tabs/BudgetTab.kt`

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
                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetComparisonChart(
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

            items(budgetHistory) { item ->
                BudgetHistoryRow(item = item)
            }
        }
    }
}
```

#### InsightsTab
**File:** `presentation/analytics/tabs/InsightsTab.kt`

```kotlin
@Composable
fun InsightsTab(
    topMerchants: List<MerchantSpending>,
    dayOfWeekSpending: List<DaySpending>,
    cashbackSummary: CashbackSummary?,
    quickStats: QuickStats?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Merchants
        item { TopMerchantsCard(merchants = topMerchants) }

        // Spending by Day
        item {
            FintraceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "When You Spend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DayOfWeekChart(data = dayOfWeekSpending)
                }
            }
        }

        // Cashback Summary
        if (cashbackSummary != null) {
            item { CashbackSummaryCard(summary = cashbackSummary) }
        }

        // Quick Stats
        if (quickStats != null) {
            item { QuickStatsCard(stats = quickStats) }
        }
    }
}
```

- [ ] Create `tabs/` directory
- [ ] Create `OverviewTab.kt`
- [ ] Create `BudgetTab.kt`
- [ ] Create `InsightsTab.kt`

### 7.3.3 Update AnalyticsScreen
**File:** `ui/screens/analytics/AnalyticsScreen.kt`

```kotlin
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabs = listOf("Overview", "Budget", "Insights")

    // Collect all state
    val spendingTrend by viewModel.spendingTrend.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val budgetWithSpending by viewModel.budgetWithSpending.collectAsState()
    val budgetHistory by viewModel.budgetHistory.collectAsState()
    val budgetTrendData by viewModel.budgetTrendData.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val dayOfWeekSpending by viewModel.dayOfWeekSpending.collectAsState()
    val cashbackSummary by viewModel.cashbackSummary.collectAsState()
    val quickStats by viewModel.quickStats.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }

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
            TabRow(selectedTabIndex = selectedTab) {
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
                    categoryBreakdown = categoryBreakdown,
                    income = /* calculate */,
                    expense = /* calculate */
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
                    quickStats = quickStats
                )
            }
        }
    }

    if (showBudgetDialog) {
        BudgetInputDialog(
            currentAmount = budgetWithSpending?.budget?.amount,
            onConfirm = { /* save */ },
            onDelete = if (budgetWithSpending != null) { { /* delete */ } } else null,
            onDismiss = { showBudgetDialog = false }
        )
    }
}
```

- [ ] Update AnalyticsScreen with TabRow
- [ ] Connect tab switching to ViewModel
- [ ] Implement tab content switching
- [ ] Test all three tabs

---

## Verification Checklist

After completing Phase 7:

- [ ] Vico chart library integrated
- [ ] SpendingTrendChart displays correctly
- [ ] BudgetComparisonChart displays correctly
- [ ] DayOfWeekChart displays correctly
- [ ] Budget can be set from Settings
- [ ] Budget can be edited/deleted
- [ ] Analytics has 3 tabs: Overview, Budget, Insights
- [ ] Tab switching works smoothly
- [ ] Overview tab shows spending trend and categories
- [ ] Budget tab shows progress and history
- [ ] Insights tab shows merchants, patterns, cashback
- [ ] All data loads correctly
- [ ] Charts work in light and dark themes
- [ ] No performance issues

---

## New Files Created

| File | Purpose |
|------|---------|
| `ui/components/charts/SpendingTrendChart.kt` | Line chart |
| `ui/components/charts/BudgetComparisonChart.kt` | Two-line chart |
| `ui/components/charts/DayOfWeekChart.kt` | Horizontal bar chart |
| `ui/components/dialogs/BudgetInputDialog.kt` | Budget input |
| `ui/components/budget/BudgetProgressCard.kt` | Progress display |
| `presentation/analytics/tabs/OverviewTab.kt` | Tab 1 |
| `presentation/analytics/tabs/BudgetTab.kt` | Tab 2 |
| `presentation/analytics/tabs/InsightsTab.kt` | Tab 3 |

---

## Final Verification

After completing ALL phases:

- [ ] Full regression testing
- [ ] Performance profiling
- [ ] Memory leak check
- [ ] Light/dark theme check
- [ ] All screens accessible
- [ ] All features working
- [ ] Update version to 2.2.0
- [ ] Update CLAUDE.md with new features
- [ ] Commit all changes
