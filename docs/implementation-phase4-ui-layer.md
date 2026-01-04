# Phase 4: UI Layer Architecture

## Overview
Consolidate duplicate UI components, create reusable composables, and ensure consistent styling across the app.

**Goal:** Fewer components, more reusability, consistent look and feel.

---

## 4.1 TransactionListItem (Consolidate 4 → 1)

### 4.1.1 Analyze Existing Implementations
**File:** `presentation/home/HomeScreen.kt`

| Component | Lines | Features |
|-----------|-------|----------|
| `ReferenceTransactionItem` | 986-1115 | Icon, category, amount, date, swipe |
| `ExpenzioTransactionItem` | 1390-1470 | Compact, icon, amount |
| `SimpleTransactionItem` | 1474-1503 | Very basic |
| `TransactionItem` | 1579-1662 | Full featured |

### 4.1.2 Create Unified TransactionListItem
**New File:** `ui/components/items/TransactionListItem.kt`

```kotlin
package com.fintrace.app.ui.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.ui.components.CategoryIcon
import com.fintrace.app.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

data class TransactionItemConfig(
    val showIcon: Boolean = true,
    val showCategory: Boolean = true,
    val showAccount: Boolean = false,
    val showCashback: Boolean = false,
    val showDate: Boolean = true,
    val showTime: Boolean = false,
    val compact: Boolean = false
)

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    config: TransactionItemConfig = TransactionItemConfig()
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.tertiary
    }

    val amountPrefix = when (transaction.transactionType) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = if (config.compact) 8.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        if (config.showIcon) {
            Box(
                modifier = Modifier
                    .size(if (config.compact) 36.dp else 44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = transaction.category ?: "Other",
                    size = if (config.compact) 20.dp else 24.dp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            // Merchant name
            Text(
                text = transaction.merchantName ?: "Unknown",
                style = if (config.compact)
                    MaterialTheme.typography.bodyMedium
                else
                    MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Category and/or Account
            if (config.showCategory || config.showAccount) {
                val subtitle = buildList {
                    if (config.showCategory && transaction.category != null) {
                        add(transaction.category)
                    }
                    if (config.showAccount && transaction.bankName != null) {
                        add("${transaction.bankName} ••${transaction.accountNumber ?: ""}")
                    }
                }.joinToString(" • ")

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Date/Time
            if (config.showDate || config.showTime) {
                val dateFormat = when {
                    config.showDate && config.showTime -> "dd MMM, HH:mm"
                    config.showTime -> "HH:mm"
                    else -> "dd MMM"
                }
                Text(
                    text = transaction.dateTime.format(DateTimeFormatter.ofPattern(dateFormat)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount and Cashback
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${CurrencyFormatter.format(transaction.amount, transaction.currency)}",
                style = if (config.compact)
                    MaterialTheme.typography.bodyMedium
                else
                    MaterialTheme.typography.titleMedium,
                color = amountColor
            )

            if (config.showCashback && transaction.cashbackAmount != null &&
                transaction.cashbackAmount > java.math.BigDecimal.ZERO) {
                Text(
                    text = "+${CurrencyFormatter.format(transaction.cashbackAmount, transaction.currency)} cashback",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Convenience composables for common configurations
@Composable
fun TransactionListItemCompact(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionListItem(
        transaction = transaction,
        onClick = onClick,
        modifier = modifier,
        config = TransactionItemConfig(
            compact = true,
            showCategory = false,
            showDate = false
        )
    )
}

@Composable
fun TransactionListItemFull(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionListItem(
        transaction = transaction,
        onClick = onClick,
        modifier = modifier,
        config = TransactionItemConfig(
            showAccount = true,
            showCashback = true
        )
    )
}
```

- [ ] Create `ui/components/items/` directory
- [ ] Create `TransactionListItem.kt`
- [ ] Create `TransactionItemConfig`
- [ ] Add convenience composables

### 4.1.3 Update All Usages
- [ ] HomeScreen: Replace `ReferenceTransactionItem` with `TransactionListItem`
- [ ] HomeScreen: Replace `ExpenzioTransactionItem` with `TransactionListItemCompact`
- [ ] HomeScreen: Replace `SimpleTransactionItem`
- [ ] HomeScreen: Replace `TransactionItem`
- [ ] TransactionsScreen: Use `TransactionListItem`
- [ ] PendingTransactionsScreen: Use `TransactionListItem`
- [ ] Delete old implementations from HomeScreen

---

## 4.2 EmptyStateCard (Consolidate 3 → 1)

### 4.2.1 Create Unified EmptyStateCard
**New File:** `ui/components/states/EmptyStateCard.kt`

```kotlin
package com.fintrace.app.ui.components.states

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrace.app.ui.components.FintraceCard

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    FintraceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (action != null) {
                Spacer(modifier = Modifier.height(20.dp))
                action()
            }
        }
    }
}

// Convenience composables for common empty states
@Composable
fun EmptyTransactionsState(
    onAddTransaction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyStateCard(
        icon = Icons.Outlined.Receipt,
        title = "No Transactions",
        message = "Your transactions will appear here once you start tracking.",
        modifier = modifier,
        action = onAddTransaction?.let {
            {
                Button(onClick = it) {
                    Text("Add Transaction")
                }
            }
        }
    )
}

@Composable
fun EmptySubscriptionsState(
    modifier: Modifier = Modifier
) {
    EmptyStateCard(
        icon = Icons.Outlined.Repeat,
        title = "No Subscriptions",
        message = "Recurring payments will be detected automatically from your transactions.",
        modifier = modifier
    )
}

@Composable
fun EmptySearchResultsState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    EmptyStateCard(
        icon = Icons.Outlined.SearchOff,
        title = "No Results",
        message = "No transactions found matching \"$searchQuery\"",
        modifier = modifier
    )
}
```

- [ ] Create `ui/components/states/` directory
- [ ] Create `EmptyStateCard.kt`
- [ ] Add convenience composables

### 4.2.2 Update All Usages
- [ ] HomeScreen: Replace `ExpenzioEmptyTransactions`
- [ ] TransactionsScreen: Replace `EmptyTransactionsState`
- [ ] SubscriptionsScreen: Replace `EmptySubscriptionsState`
- [ ] Delete old implementations

---

## 4.3 FilterDialog (Consolidate 4 → 1)

### 4.3.1 Create Generic FilterDialog
**New File:** `ui/components/dialogs/FilterDialog.kt`

```kotlin
package com.fintrace.app.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> FilterDialog(
    title: String,
    items: List<T>,
    selectedItems: Set<T>,
    itemLabel: (T) -> String,
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchEnabled: Boolean = true,
    itemIcon: (@Composable (T) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelection by remember(selectedItems) { mutableStateOf(selectedItems) }

    val filteredItems = if (searchEnabled && searchQuery.isNotEmpty()) {
        items.filter { itemLabel(it).contains(searchQuery, ignoreCase = true) }
    } else {
        items
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column {
                // Search field
                if (searchEnabled && items.size > 10) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Select All / Clear All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { tempSelection = filteredItems.toSet() }
                    ) {
                        Text("Select All")
                    }
                    TextButton(
                        onClick = { tempSelection = emptySet() }
                    ) {
                        Text("Clear All")
                    }
                }

                HorizontalDivider()

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = item in tempSelection

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelection = if (isSelected) {
                                        tempSelection - item
                                    } else {
                                        tempSelection + item
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    tempSelection = if (checked) {
                                        tempSelection + item
                                    } else {
                                        tempSelection - item
                                    }
                                }
                            )

                            if (itemIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                itemIcon(item)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = itemLabel(item),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelection) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Amount Range Filter Dialog (special case)
@Composable
fun AmountRangeFilterDialog(
    currentRange: Pair<Double, Double>?,
    onConfirm: (Pair<Double, Double>?) -> Unit,
    onDismiss: () -> Unit
) {
    var minAmount by remember { mutableStateOf(currentRange?.first?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentRange?.second?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Amount") },
        text = {
            Column {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) minAmount = it },
                    label = { Text("Min Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) maxAmount = it },
                    label = { Text("Max Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minAmount.toDoubleOrNull() ?: 0.0
                    val max = maxAmount.toDoubleOrNull() ?: Double.MAX_VALUE
                    onConfirm(Pair(min, max))
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
```

- [ ] Create `ui/components/dialogs/` directory
- [ ] Create `FilterDialog.kt`
- [ ] Create `AmountRangeFilterDialog`

### 4.3.2 Update TransactionsScreen
```kotlin
// Replace MerchantFilterDialog with:
FilterDialog(
    title = "Filter by Merchant",
    items = allMerchants,
    selectedItems = selectedMerchants,
    itemLabel = { it },
    onConfirm = { viewModel.filterStateManager.updateMerchants(it) },
    onDismiss = { showMerchantDialog = false }
)

// Replace CategoryFilterDialog with:
FilterDialog(
    title = "Filter by Category",
    items = allCategories,
    selectedItems = selectedCategories,
    itemLabel = { it },
    itemIcon = { CategoryIcon(category = it) },
    onConfirm = { viewModel.filterStateManager.updateCategories(it) },
    onDismiss = { showCategoryDialog = false }
)
```

- [ ] Replace `MerchantFilterDialog` in TransactionsScreen
- [ ] Replace `CategoryFilterDialog` in TransactionsScreen
- [ ] Replace `AccountFilterDialog` in TransactionsScreen
- [ ] Replace `AmountFilterDialog` in TransactionsScreen
- [ ] Delete old dialog implementations

---

## 4.4 Use FintraceCard Everywhere

### 4.4.1 Find and Replace Raw Card Usage
**Search:** `Card(` in HomeScreen.kt and other screens

```kotlin
// BEFORE
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) { ... }

// AFTER
FintraceCard(
    modifier = Modifier.fillMaxWidth()
) { ... }
```

- [ ] Search for raw `Card(` usage in HomeScreen.kt
- [ ] Replace with `FintraceCard`
- [ ] Search in other screens (TransactionsScreen, etc.)
- [ ] Ensure consistent elevation and colors

---

## 4.5 Create Spacing Constants

### 4.5.1 Define Spacing System
**New/Update File:** `ui/theme/Dimensions.kt`

```kotlin
package com.fintrace.app.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Common patterns
    val cardPadding = lg
    val listItemPadding = md
    val screenPadding = lg
    val sectionSpacing = xl
}

object Dimensions {
    val iconSizeSmall = 20.dp
    val iconSizeMedium = 24.dp
    val iconSizeLarge = 32.dp
    val iconSizeXLarge = 48.dp

    val avatarSizeSmall = 36.dp
    val avatarSizeMedium = 44.dp
    val avatarSizeLarge = 56.dp

    val buttonHeight = 48.dp
    val inputHeight = 56.dp

    val cardElevation = 2.dp
    val dialogElevation = 8.dp

    val bottomNavHeight = 80.dp
}
```

- [ ] Create/update `Dimensions.kt`
- [ ] Define `Spacing` object
- [ ] Define `Dimensions` object

### 4.5.2 Replace Hardcoded Values
**Priority areas:**
- [ ] HomeScreen.kt: Replace `16.dp`, `12.dp`, `8.dp` with `Spacing.*`
- [ ] HomeScreen.kt: Replace `PaddingValues(bottom = 80.dp)` with `Dimensions.bottomNavHeight`
- [ ] Apply to other screens as time permits

---

## 4.6 Split HomeScreen

### 4.6.1 Extract to Separate Files
**Current:** `HomeScreen.kt` is 2104 lines

**Target structure:**
```
presentation/home/
├── HomeScreen.kt (main composition, ~300 lines)
├── components/
│   ├── AccountBalanceCard.kt
│   ├── TransactionSummaryCard.kt
│   ├── QuickActionsRow.kt
│   └── RecentTransactionsSection.kt
└── dialogs/
    └── (any home-specific dialogs)
```

### 4.6.2 Extract AccountBalanceCard
**New File:** `presentation/home/components/AccountBalanceCard.kt`

```kotlin
@Composable
fun AccountBalanceCard(
    accounts: List<AccountBalanceEntity>,
    totalBalance: Map<String, BigDecimal>,
    onAccountClick: (AccountBalanceEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    FintraceCard(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            // Total balance
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            totalBalance.forEach { (currency, amount) ->
                Text(
                    text = CurrencyFormatter.format(amount, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Individual accounts
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    onClick = { onAccountClick(account) }
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountBalanceEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = account.bankName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "••${account.accountLast4}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CurrencyFormatter.format(account.balance, account.currency),
                style = MaterialTheme.typography.titleMedium
            )

            // Show available balance for credit cards
            if (account.isCreditCard && account.creditLimit != null) {
                val available = account.creditLimit - account.balance
                Text(
                    text = "${CurrencyFormatter.format(available)} available",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (available > BigDecimal.ZERO)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

- [ ] Create `presentation/home/components/` directory
- [ ] Extract `AccountBalanceCard.kt`
- [ ] Extract `TransactionSummaryCard.kt`
- [ ] Extract `RecentTransactionsSection.kt`
- [ ] Update `HomeScreen.kt` to use extracted components
- [ ] Remove dead code from HomeScreen

### 4.6.3 Remove Dead Code
- [ ] Identify unused composables in HomeScreen
- [ ] Delete `ReferenceTransactionItem` (replaced)
- [ ] Delete `ExpenzioTransactionItem` (replaced)
- [ ] Delete `SimpleTransactionItem` (replaced)
- [ ] Delete `TransactionItem` (replaced)
- [ ] Delete `ExpenzioEmptyTransactions` (replaced)
- [ ] Delete any other unused code

---

## 4.7 Filter Card Consistent Padding

### 4.7.1 Fix Layout Stability
**File:** TransactionsScreen filter section

```kotlin
@Composable
fun FilterChipsRow(
    filterState: FilterState,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter chips (scrollable)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Period chip, Currency chip, etc.
        }

        // Fixed width container for count + clear (prevents layout shift)
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = filterState.hasActiveFilters,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge { Text("${filterState.activeFilterCount}") }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    TextButton(onClick = onClearAll) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}
```

- [ ] Find filter chips implementation
- [ ] Add fixed-width container for count/clear
- [ ] Use `AnimatedVisibility` with fade
- [ ] Test: no layout shift when filters change

---

## Verification Checklist

After completing Phase 4:

- [ ] Single `TransactionListItem` component used everywhere
- [ ] Single `EmptyStateCard` component used everywhere
- [ ] Generic `FilterDialog` used for all filters
- [ ] All cards use `FintraceCard`
- [ ] `Spacing` and `Dimensions` constants defined
- [ ] HomeScreen reduced to ~800-1000 lines
- [ ] Components extracted to separate files
- [ ] No dead code in HomeScreen
- [ ] Filter card has stable layout
- [ ] Light and dark themes look correct
- [ ] All screens have consistent styling

---

## New Files Created

| File | Purpose |
|------|---------|
| `ui/components/items/TransactionListItem.kt` | Unified transaction item |
| `ui/components/states/EmptyStateCard.kt` | Reusable empty state |
| `ui/components/dialogs/FilterDialog.kt` | Generic filter dialog |
| `ui/theme/Dimensions.kt` | Spacing and dimension constants |
| `presentation/home/components/AccountBalanceCard.kt` | Account card |
| `presentation/home/components/TransactionSummaryCard.kt` | Summary card |
| `presentation/home/components/RecentTransactionsSection.kt` | Recent transactions |

## Files Deleted

| File/Code | Reason |
|-----------|--------|
| `ReferenceTransactionItem` in HomeScreen | Consolidated |
| `ExpenzioTransactionItem` in HomeScreen | Consolidated |
| `SimpleTransactionItem` in HomeScreen | Consolidated |
| `TransactionItem` in HomeScreen | Consolidated |
| `ExpenzioEmptyTransactions` in HomeScreen | Consolidated |
| `EmptyTransactionsState` in TransactionsScreen | Consolidated |
| `EmptySubscriptionsState` in SubscriptionsScreen | Consolidated |
| `MerchantFilterDialog` in TransactionsScreen | Consolidated |
| `CategoryFilterDialog` in TransactionsScreen | Consolidated |
| `AccountFilterDialog` in TransactionsScreen | Consolidated |

---

## Next Phase
→ Proceed to `implementation-phase5-bug-fixes.md`
