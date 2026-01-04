# Implementation Phase 2: UI Fixes & Consistency

## Overview
This phase focuses on fixing UI inconsistencies and ensuring consistent patterns across all screens.

**Estimated scope:** UI fixes, no new features
**Dependencies:** Phase 1 (cleanup) should be complete

---

## 2.1 Pending Transactions Screen - Add Actions

### Task: Add sync and settings to TopAppBar
**File:** `presentation/pending/PendingTransactionsScreen.kt`

**Current:** Missing sync and settings icons
**Expected:** Same TopAppBar actions as other screens

**Implementation:**
```kotlin
FintraceScaffold(
    topBar = {
        TopAppBar(
            title = { Text("Pending Transactions") },
            actions = {
                // Sync button
                IconButton(onClick = { viewModel.triggerSync() }) {
                    Icon(Icons.Default.Sync, "Sync")
                }
                // Settings button
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        )
    }
)
```

- [ ] Add sync icon with onClick handler
- [ ] Add settings icon with navigation
- [ ] Verify consistency with HomeScreen, TransactionsScreen
- [ ] Test both actions work correctly

---

## 2.2 Categories Overview - Line Separator

### Task: Add dividers between category items
**File:** Find categories overview composable (likely in AnalyticsScreen or HomeScreen)

**Implementation approach:**
```kotlin
categories.forEachIndexed { index, category ->
    CategoryOverviewItem(category = category)
    if (index < categories.lastIndex) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
```

OR using LazyColumn:
```kotlin
LazyColumn {
    itemsIndexed(categories) { index, category ->
        CategoryOverviewItem(category = category)
        if (index < categories.lastIndex) {
            HorizontalDivider(...)
        }
    }
}
```

- [ ] Find categories overview location
- [ ] Add `HorizontalDivider` between items
- [ ] Skip divider after last item
- [ ] Use consistent divider styling

---

## 2.3 Filter Card - Consistent Padding

### Task: Fix layout shift when filters applied
**File:** Find filter card composable (likely in TransactionsScreen)

**Problem:** Card padding changes when filter count and clear button appear.

**Solution - Reserve space:**
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    // Filter chips
    Row { ... }

    // Always reserve space for count + clear, use alpha for visibility
    Row(
        modifier = Modifier.alpha(if (hasActiveFilters) 1f else 0f)
    ) {
        FilterCountBadge(count = activeFilterCount)
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = { clearFilters() }) {
            Text("Clear")
        }
    }
}
```

**Alternative - Fixed width container:**
```kotlin
Box(
    modifier = Modifier.width(100.dp), // Fixed width
    contentAlignment = Alignment.CenterEnd
) {
    if (hasActiveFilters) {
        Row {
            FilterCountBadge(...)
            TextButton(...) { Text("Clear") }
        }
    }
}
```

- [ ] Find filter card implementation
- [ ] Implement fixed layout (either alpha approach or fixed width)
- [ ] Test: Apply filter → verify no layout shift
- [ ] Test: Remove filter → verify no layout shift

---

## 2.4 Transactions Screen - Alignment Fix

### Task: Fix Income/Expense/Net summary alignment
**File:** `presentation/transactions/TransactionsScreen.kt`

**Current:** Alignment is inconsistent
**Expected:** Clean, consistent alignment

**Implementation pattern:**
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    SummaryItem(
        label = "Income",
        amount = income,
        color = incomeColor,
        modifier = Modifier.weight(1f)
    )
    SummaryItem(
        label = "Expense",
        amount = expense,
        color = expenseColor,
        modifier = Modifier.weight(1f)
    )
    SummaryItem(
        label = "Net",
        amount = net,
        color = netColor,
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun SummaryItem(
    label: String,
    amount: BigDecimal,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}
```

- [ ] Find summary card in TransactionsScreen
- [ ] Apply consistent `weight(1f)` to each item
- [ ] Center align text within each item
- [ ] Extract to `SummaryItem` composable if not already
- [ ] Verify alignment looks correct

---

## 2.5 Home Screen - Center Align Balance

### Task: Center align total balance
**File:** `presentation/home/HomeScreen.kt`

**Find balance display and update:**
```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally // Add this
) {
    Text(
        text = "Total Balance",
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center // Add this
    )
    Text(
        text = formattedBalance,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center // Add this
    )
}
```

- [ ] Find balance display in HomeScreen
- [ ] Add `Alignment.CenterHorizontally` to parent Column
- [ ] Add `textAlign = TextAlign.Center` to Text composables
- [ ] Verify balance is centered on all screen sizes

---

## 2.6 Transaction Details - Show Cashback

### Task: Add cashback info to transaction details
**File:** Find transaction details screen/dialog

**Implementation:**
```kotlin
// In transaction details view
if (transaction.cashbackAmount != null && transaction.cashbackAmount > BigDecimal.ZERO) {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Cashback",
            style = MaterialTheme.typography.bodyMedium
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${transaction.cashbackPercent}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CurrencyFormatter.format(transaction.cashbackAmount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

- [ ] Find transaction details implementation
- [ ] Add cashback section (only if cashback exists)
- [ ] Display both percentage and amount
- [ ] Use proper formatting
- [ ] Test with transaction that has cashback
- [ ] Test with transaction without cashback (section hidden)

---

## 2.7 Credit Account - Show Remaining Limit

### Task: Display available balance for credit accounts
**File:** Account overview card (HomeScreen or ManageAccountsScreen)

**Implementation:**
```kotlin
@Composable
fun AccountBalanceCard(account: AccountBalanceEntity) {
    Column {
        // Bank name and account
        Text("${account.bankName} ••${account.accountLast4}")

        if (account.isCreditCard && account.creditLimit != null) {
            // Credit card display
            val available = account.creditLimit - account.balance

            Text("Used: ${CurrencyFormatter.format(account.balance)}")
            Text("Limit: ${CurrencyFormatter.format(account.creditLimit)}")
            Text(
                text = "Available: ${CurrencyFormatter.format(available)}",
                color = if (available > BigDecimal.ZERO)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )

            // Optional: Progress bar showing usage
            LinearProgressIndicator(
                progress = (account.balance / account.creditLimit).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Regular account display
            Text(
                text = CurrencyFormatter.format(account.balance),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
```

- [ ] Find account card implementation
- [ ] Add credit account specific display
- [ ] Show: Used, Limit, Available
- [ ] Color code available amount (green if positive, red if negative)
- [ ] Optional: Add usage progress bar
- [ ] Test with credit account
- [ ] Test with regular savings account (no limit shown)

---

## 2.8 Consolidate Transaction Item (Prep for Phase 3)

### Task: Analyze transaction item implementations
**File:** `presentation/home/HomeScreen.kt`

**4 implementations to analyze:**
1. `ReferenceTransactionItem` (lines 986-1115)
2. `ExpenzioTransactionItem` (lines 1390-1470)
3. `SimpleTransactionItem` (lines 1474-1503)
4. `TransactionItem` (lines 1579-1662)

**Analysis checklist:**
- [ ] Document what each implementation does
- [ ] Note differences between them
- [ ] Identify which features are used where
- [ ] Plan unified `TransactionListItem` with parameters

**Proposed unified component:**
```kotlin
@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showCategory: Boolean = true,
    showAccount: Boolean = false,
    showCashback: Boolean = false,
    compact: Boolean = false
)
```

- [ ] Create analysis notes for Phase 3 implementation

---

## Verification Checklist

After completing Phase 2:

- [ ] Pending screen has sync and settings in TopAppBar
- [ ] Categories have line separator between items
- [ ] Filter card has no layout shift when filters applied/removed
- [ ] Income/Expense/Net properly aligned on Transactions screen
- [ ] Total balance centered on Home screen
- [ ] Transaction details show cashback (when applicable)
- [ ] Credit accounts show remaining limit
- [ ] No visual regressions on any screen
- [ ] Light and dark themes both look correct

---

## Reusable Components Created/Updated

| Component | Status |
|-----------|--------|
| `SummaryItem` | Extract if not exists |
| `AccountBalanceCard` | Update for credit display |
| Transaction details cashback section | New |

---

## Next Phase
→ Proceed to `implementation-phase3-architecture.md`
