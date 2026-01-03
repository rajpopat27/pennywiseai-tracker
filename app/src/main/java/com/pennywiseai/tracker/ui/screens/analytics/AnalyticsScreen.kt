package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.DateRangeUtils
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()

    // Custom filter states
    val merchantFilter by viewModel.merchantFilter.collectAsStateWithLifecycle()
    val amountFilter by viewModel.amountFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val activeCustomFilterCount by viewModel.activeCustomFilterCount.collectAsStateWithLifecycle()

    // Available filter options
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val availableMerchants by viewModel.availableMerchants.collectAsStateWithLifecycle()

    var showCustomFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Cache expensive operations
    val timePeriods = remember { TimePeriod.values().toList() }
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Spacing.md,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Period Selector - Always visible
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(timePeriods) { period ->
                    FilterChip(
                        // Only show CUSTOM as selected if both period is CUSTOM AND dates are set
                        selected = if (period == TimePeriod.CUSTOM) {
                            selectedPeriod == period && customDateRange != null
                        } else {
                            selectedPeriod == period
                        },
                        onClick = {
                            if (period == TimePeriod.CUSTOM) {
                                showDateRangePicker = true
                                // Don't change selectedPeriod until user confirms dates
                            } else {
                                viewModel.selectPeriod(period)
                            }
                        },
                        label = {
                            Text(
                                if (period == TimePeriod.CUSTOM && customRangeLabel != null) {
                                    customRangeLabel
                                } else {
                                    period.label
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Currency Selector (if multiple currencies available)
        if (availableCurrencies.size > 1) {
            item {
                CurrencyFilterRow(
                    selectedCurrency = selectedCurrency,
                    availableCurrencies = availableCurrencies,
                    onCurrencySelected = { viewModel.selectCurrency(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Filters Section (combined transaction type + custom filters)
        item {
            FiltersSection(
                isExpanded = showCustomFilters,
                onToggle = { showCustomFilters = !showCustomFilters },
                // Transaction type filter
                transactionTypeFilter = transactionTypeFilter,
                onTransactionTypeFilterChange = { viewModel.setTransactionTypeFilter(it) },
                // Custom filters
                merchantFilter = merchantFilter,
                onMerchantFilterChange = { viewModel.setMerchantFilter(it) },
                amountFilter = amountFilter,
                onAmountFilterChange = { operator, value ->
                    viewModel.setAmountFilter(operator, value)
                },
                categoryFilter = categoryFilter,
                onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                availableCategories = availableCategories,
                accountFilter = accountFilter,
                onAccountFilterChange = { viewModel.setAccountFilter(it) },
                availableAccounts = availableAccounts,
                onClearAllFilters = {
                    viewModel.clearAllCustomFilters()
                    viewModel.setTransactionTypeFilter(TransactionTypeFilter.ALL)
                }
            )
        }

        // Analytics Summary Card
        if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
            item {
                AnalyticsSummaryCard(
                    totalAmount = uiState.totalSpending,
                    transactionCount = uiState.transactionCount,
                    averageAmount = uiState.averageAmount,
                    topCategory = uiState.topCategory,
                    topCategoryPercentage = uiState.topCategoryPercentage,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading
                )
            }
        }
        
        // Category Breakdown Section
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    currency = selectedCurrency,
                    onCategoryClick = { category ->
                        onNavigateToTransactions(category.name, null, selectedPeriod.name, selectedCurrency)
                    }
                )
            }
        }
        
        // Top Merchants Section
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Top Merchants"
                )
            }
            
            // All Merchants with expandable list
            item {
                ExpandableList(
                    items = uiState.topMerchants,
                    visibleItemCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) { merchant ->
                    MerchantListItem(
                        merchant = merchant,
                        currency = selectedCurrency,
                        onClick = {
                            onNavigateToTransactions(null, merchant.name, selectedPeriod.name, selectedCurrency)
                        }
                    )
                }
            }
        }
        
        
        // Empty state
        if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
            item {
                EmptyAnalyticsState()
            }
        }
    }
    
//    // Chat FAB
//    SmallFloatingActionButton(
//        onClick = onNavigateToChat,
//        modifier = Modifier
//            .align(Alignment.BottomEnd)
//            .padding(Dimensions.Padding.content),
//        containerColor = MaterialTheme.colorScheme.secondaryContainer,
//        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//    ) {
//        Icon(
//            imageVector = Icons.AutoMirrored.Filled.Chat,
//            contentDescription = "Open AI Assistant"
//        )
//    }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second
        )
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryData,
    currency: String
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!
    
    ListItemCard(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = 24.dp,
                    tint = categoryInfo.color
                )
            }
        },
        title = category.name,
        subtitle = "${category.transactionCount} transactions",
        amount = CurrencyFormatter.formatCurrency(category.amount, currency),
        trailingContent = {
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }
    
    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Start tracking expenses to see analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    // Transaction type filter
    transactionTypeFilter: TransactionTypeFilter,
    onTransactionTypeFilterChange: (TransactionTypeFilter) -> Unit,
    // Custom filters
    merchantFilter: String?,
    onMerchantFilterChange: (String?) -> Unit,
    amountFilter: AmountFilter?,
    onAmountFilterChange: (AmountOperator?, BigDecimal?) -> Unit,
    categoryFilter: String?,
    onCategoryFilterChange: (String?) -> Unit,
    availableCategories: List<String>,
    accountFilter: String?,
    onAccountFilterChange: (String?) -> Unit,
    availableAccounts: List<String>,
    onClearAllFilters: () -> Unit
) {
    // Dialog states
    var showMerchantDialog by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    // Calculate active filter count
    val activeFilterCount = listOfNotNull(
        merchantFilter,
        amountFilter,
        categoryFilter,
        accountFilter,
        if (transactionTypeFilter != TransactionTypeFilter.ALL) transactionTypeFilter else null
    ).size

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with toggle
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (activeFilterCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(activeFilterCount.toString())
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (activeFilterCount > 0) {
                        TextButton(
                            onClick = { onClearAllFilters() },
                            contentPadding = PaddingValues(horizontal = Spacing.xs)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Expandable content
        if (isExpanded) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                // Income Chip
                val isIncomeSelected = transactionTypeFilter == TransactionTypeFilter.INCOME
                FilterChip(
                    selected = isIncomeSelected,
                    onClick = {
                        onTransactionTypeFilterChange(
                            if (isIncomeSelected) TransactionTypeFilter.ALL
                            else TransactionTypeFilter.INCOME
                        )
                    },
                    label = {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Expense Chip
                val isExpenseSelected = transactionTypeFilter == TransactionTypeFilter.EXPENSE
                FilterChip(
                    selected = isExpenseSelected,
                    onClick = {
                        onTransactionTypeFilterChange(
                            if (isExpenseSelected) TransactionTypeFilter.ALL
                            else TransactionTypeFilter.EXPENSE
                        )
                    },
                    label = {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Merchant Chip
                FilterChip(
                    selected = merchantFilter != null,
                    onClick = { showMerchantDialog = true },
                    label = {
                        Text(
                            text = merchantFilter ?: "Merchant",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Amount Chip
                val amountLabel = amountFilter?.let {
                    "${it.operator.symbol}${it.value.toPlainString()}"
                } ?: "Amount"
                FilterChip(
                    selected = amountFilter != null,
                    onClick = { showAmountDialog = true },
                    label = {
                        Text(
                            text = amountLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Category Chip
                FilterChip(
                    selected = categoryFilter != null,
                    onClick = { showCategoryDialog = true },
                    label = {
                        Text(
                            text = categoryFilter ?: "Category",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Account Chip
                FilterChip(
                    selected = accountFilter != null,
                    onClick = { showAccountDialog = true },
                    label = {
                        Text(
                            text = accountFilter ?: "Account",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }

    // Dialogs
    if (showMerchantDialog) {
        MerchantFilterDialog(
            currentValue = merchantFilter,
            onDismiss = { showMerchantDialog = false },
            onConfirm = { value ->
                onMerchantFilterChange(value)
                showMerchantDialog = false
            },
            onClear = {
                onMerchantFilterChange(null)
                showMerchantDialog = false
            }
        )
    }

    if (showAmountDialog) {
        AmountFilterDialog(
            currentFilter = amountFilter,
            onDismiss = { showAmountDialog = false },
            onConfirm = { operator, value ->
                onAmountFilterChange(operator, value)
                showAmountDialog = false
            },
            onClear = {
                onAmountFilterChange(null, null)
                showAmountDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        CategoryFilterDialog(
            currentValue = categoryFilter,
            availableCategories = availableCategories,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { value ->
                onCategoryFilterChange(value)
                showCategoryDialog = false
            },
            onClear = {
                onCategoryFilterChange(null)
                showCategoryDialog = false
            }
        )
    }

    if (showAccountDialog) {
        AccountFilterDialog(
            currentValue = accountFilter,
            availableAccounts = availableAccounts,
            onDismiss = { showAccountDialog = false },
            onConfirm = { value ->
                onAccountFilterChange(value)
                showAccountDialog = false
            },
            onClear = {
                onAccountFilterChange(null)
                showAccountDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantFilterDialog(
    currentValue: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onClear: () -> Unit
) {
    var text by remember { mutableStateOf(currentValue ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Filter by Merchant") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Merchant name") },
                placeholder = { Text("e.g., Amazon, Swiggy") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.takeIf { it.isNotBlank() }) },
                enabled = text.isNotBlank()
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (currentValue != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountFilterDialog(
    currentFilter: AmountFilter?,
    onDismiss: () -> Unit,
    onConfirm: (AmountOperator, BigDecimal) -> Unit,
    onClear: () -> Unit
) {
    var amountText by remember { mutableStateOf(currentFilter?.value?.toPlainString() ?: "") }
    var selectedOperator by remember { mutableStateOf(currentFilter?.operator ?: AmountOperator.GREATER_THAN) }
    var showOperatorDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Filter by Amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                // Operator selection
                Text(
                    text = "Condition",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(AmountOperator.values().toList()) { operator ->
                        FilterChip(
                            selected = selectedOperator == operator,
                            onClick = { selectedOperator = operator },
                            label = {
                                Text(
                                    "${operator.symbol} ${operator.label}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("Enter amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amountText.toBigDecimalOrNull()?.let { amount ->
                        onConfirm(selectedOperator, amount)
                    }
                },
                enabled = amountText.toBigDecimalOrNull() != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (currentFilter != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun CategoryFilterDialog(
    currentValue: String?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Filter by Category") },
        text = {
            if (availableCategories.isEmpty()) {
                Text(
                    text = "No categories available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(availableCategories) { category ->
                        Surface(
                            onClick = { onConfirm(category) },
                            shape = MaterialTheme.shapes.small,
                            color = if (category == currentValue) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (category == currentValue) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (category == currentValue) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (currentValue != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun AccountFilterDialog(
    currentValue: String?,
    availableAccounts: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Filter by Account") },
        text = {
            if (availableAccounts.isEmpty()) {
                Text(
                    text = "No accounts available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(availableAccounts) { account ->
                        Surface(
                            onClick = { onConfirm(account) },
                            shape = MaterialTheme.shapes.small,
                            color = if (account == currentValue) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (account == currentValue) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = account,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (account == currentValue) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (currentValue != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Spacing.sm,
                    horizontal = Spacing.xs
                )
            )
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}