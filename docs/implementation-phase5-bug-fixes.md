# Phase 5: Bug Fixes

## Overview
Fix all identified bugs using the clean architecture established in Phases 1-4.

**Goal:** All known bugs fixed, stable app behavior.

---

## 5.1 Memory Leak in HomeViewModel (Critical)

### Issue
**File:** `presentation/home/HomeViewModel.kt:258-277`

```kotlin
// MEMORY LEAK: observeForever without removeObserver
workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME)
    .observeForever { workInfos ->
        // Never removed in onCleared()
    }
```

### Fix Option 1: Use Flow (Preferred)
```kotlin
init {
    // Use Flow instead of LiveData
    workManager.getWorkInfosByTagFlow(OptimizedSmsReaderWorker.WORK_NAME)
        .onEach { workInfos ->
            val isRunning = workInfos.any {
                it.state == WorkInfo.State.RUNNING
            }
            _isSyncing.value = isRunning
        }
        .launchIn(viewModelScope)
}
```

### Fix Option 2: Store and Remove Observer
```kotlin
private var workObserver: Observer<List<WorkInfo>>? = null

init {
    workObserver = Observer { workInfos ->
        val isRunning = workInfos.any {
            it.state == WorkInfo.State.RUNNING
        }
        _isSyncing.value = isRunning
    }
    workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME)
        .observeForever(workObserver!!)
}

override fun onCleared() {
    workObserver?.let {
        workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME)
            .removeObserver(it)
    }
    super.onCleared()
}
```

### Implementation
- [ ] Choose fix option (prefer Option 1)
- [ ] Implement fix in HomeViewModel
- [ ] Test WorkManager observation still works
- [ ] Verify with LeakCanary: no memory leak

---

## 5.2 Custom Cashback Applied Retroactively (Critical)

### Issue
When confirming a pending transaction with custom cashback (e.g., 5%), it incorrectly applies that percentage to ALL existing transactions for that account.

### Root Cause
The confirm flow is calling `applyRetroactiveCashback()` with the custom percentage.

### Fix
**After Phase 2 refactor:** TransactionProcessor handles this correctly by separating custom vs default cashback.

**If fixing before Phase 2:**

**File:** `data/manager/PendingTransactionManager.kt`

```kotlin
suspend fun confirmTransaction(
    pending: PendingTransactionEntity,
    editedTransaction: TransactionEntity,
    customCashbackPercent: BigDecimal? = null
): Long {
    // Create transaction with custom cashback (THIS transaction only)
    val transactionWithCashback = if (customCashbackPercent != null) {
        editedTransaction.copy(
            cashbackPercent = customCashbackPercent.toDouble(),
            cashbackAmount = editedTransaction.amount * customCashbackPercent / BigDecimal(100)
        )
    } else {
        // Apply default from account (THIS transaction only)
        applyDefaultCashbackToTransaction(editedTransaction)
    }

    // Insert the single transaction
    val transactionId = transactionRepository.insertTransaction(transactionWithCashback)

    // Update pending status
    pendingTransactionRepository.confirm(pending.id)

    // Update balance
    processBalanceUpdate(pending)

    // DO NOT call applyRetroactiveCashback here!
    // Retroactive only happens from Manage Accounts

    return transactionId
}

private suspend fun applyDefaultCashbackToTransaction(
    transaction: TransactionEntity
): TransactionEntity {
    val account = transaction.bankName?.let { bankName ->
        transaction.accountNumber?.let { accountNumber ->
            accountBalanceRepository.getLatestBalance(bankName, accountNumber)
        }
    }

    val cashbackPercent = account?.defaultCashbackPercent

    return if (cashbackPercent != null && cashbackPercent > BigDecimal.ZERO) {
        transaction.copy(
            cashbackPercent = cashbackPercent.toDouble(),
            cashbackAmount = transaction.amount * cashbackPercent / BigDecimal(100)
        )
    } else {
        transaction
    }
}
```

### Implementation
- [ ] Locate where retroactive cashback is incorrectly called
- [ ] Remove retroactive call from confirm flow
- [ ] Apply cashback only to the single transaction being confirmed
- [ ] Test: Confirm with custom 5% cashback
- [ ] Verify: Other transactions NOT affected
- [ ] Test: Confirm with default cashback (uses account setting)

---

## 5.3 UI Alignment Issues

### 5.3.1 Transactions Screen - Income/Expense/Net Alignment
**File:** `presentation/transactions/TransactionsScreen.kt`

```kotlin
// Fix alignment
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    SummaryItem(
        label = "Income",
        amount = income,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f)
    )
    SummaryItem(
        label = "Expense",
        amount = expense,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f)
    )
    SummaryItem(
        label = "Net",
        amount = net,
        color = if (net >= BigDecimal.ZERO)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun SummaryItem(
    label: String,
    amount: BigDecimal,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = CurrencyFormatter.format(amount),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
```

- [ ] Find summary card in TransactionsScreen
- [ ] Apply `weight(1f)` to each item
- [ ] Center align text within each item
- [ ] Test alignment looks correct

### 5.3.2 Home Screen - Center Align Balance
**File:** `presentation/home/HomeScreen.kt`

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = "Total Balance",
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center
    )
    Text(
        text = formattedBalance,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )
}
```

- [ ] Find balance display in HomeScreen
- [ ] Add `Alignment.CenterHorizontally`
- [ ] Add `textAlign = TextAlign.Center`
- [ ] Verify balance is centered

---

## 5.4 Pending Transactions Screen - Missing Actions

### Issue
Pending transactions screen doesn't show sync and settings options in TopAppBar.

### Fix
**File:** `presentation/pending/PendingTransactionsScreen.kt`

```kotlin
FintraceScaffold(
    topBar = {
        TopAppBar(
            title = { Text("Pending Transactions") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                // Add sync button
                IconButton(onClick = { viewModel.triggerSync() }) {
                    Icon(Icons.Default.Sync, "Sync")
                }
                // Add settings button
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        )
    }
) { padding ->
    // ... content
}
```

- [ ] Find PendingTransactionsScreen TopAppBar
- [ ] Add sync icon with onClick
- [ ] Add settings icon with navigation
- [ ] Test both actions work

---

## 5.5 Categories Overview - Line Separator

### Issue
Categories overview items need visual separation.

### Fix
**File:** Find categories list composable

```kotlin
categories.forEachIndexed { index, category ->
    CategoryOverviewItem(category = category)
    if (index < categories.lastIndex) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
```

- [ ] Find categories overview location
- [ ] Add `HorizontalDivider` between items
- [ ] Skip divider after last item
- [ ] Test visual appearance

---

## 5.6 Transaction Details - Show Cashback

### Issue
Transaction details view doesn't show cashback information.

### Fix
**File:** Find transaction details screen/dialog

```kotlin
// Add cashback section in transaction details
if (transaction.cashbackAmount != null &&
    transaction.cashbackAmount > BigDecimal.ZERO) {

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cashback",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${transaction.cashbackPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+${CurrencyFormatter.format(transaction.cashbackAmount, transaction.currency)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

- [ ] Find transaction details implementation
- [ ] Add cashback section
- [ ] Only show if cashback exists (non-null, > 0)
- [ ] Test with transaction that has cashback
- [ ] Test with transaction without cashback (hidden)

---

## 5.7 Credit Account - Show Remaining Limit

### Issue
Credit accounts don't show available balance.

### Fix
**File:** Account overview card (HomeScreen or ManageAccountsScreen)

```kotlin
@Composable
fun AccountBalanceDisplay(account: AccountBalanceEntity) {
    Column {
        // Bank name and account
        Text("${account.bankName} ••${account.accountLast4}")

        if (account.isCreditCard && account.creditLimit != null) {
            // Credit card specific display
            val available = account.creditLimit - account.balance
            val usagePercent = (account.balance.toFloat() / account.creditLimit.toFloat() * 100)
                .coerceIn(0f, 100f)

            // Usage progress bar
            LinearProgressIndicator(
                progress = { usagePercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    usagePercent >= 90 -> MaterialTheme.colorScheme.error
                    usagePercent >= 70 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Used",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = CurrencyFormatter.format(account.balance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = CurrencyFormatter.format(available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (available > BigDecimal.ZERO)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Regular account - just show balance
            Text(
                text = CurrencyFormatter.format(account.balance),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
```

- [ ] Find account card implementation
- [ ] Add credit-specific display
- [ ] Show: Used, Limit (optional), Available
- [ ] Add usage progress bar
- [ ] Color code based on usage
- [ ] Test with credit account
- [ ] Test with regular savings account

---

## Verification Checklist

After completing Phase 5:

- [ ] No memory leak in HomeViewModel (verify with LeakCanary)
- [ ] Custom cashback only affects single transaction
- [ ] Income/Expense/Net properly aligned
- [ ] Total balance centered on Home screen
- [ ] Pending screen has sync/settings in TopAppBar
- [ ] Categories have line separators
- [ ] Transaction details show cashback
- [ ] Credit accounts show remaining limit
- [ ] No crashes on any screen
- [ ] All existing tests pass

---

## Testing Checklist

| Bug | Test Case | Expected |
|-----|-----------|----------|
| Memory leak | Open/close HomeScreen multiple times | No leaked ViewModels |
| Custom cashback | Confirm pending with 5% custom | Only that tx has 5% |
| Retroactive cashback | Update account default in Manage | Old txs get updated |
| Alignment | View Transactions screen | Income/Expense/Net aligned |
| Center balance | View Home screen | Balance centered |
| Pending actions | Open Pending screen | Sync/Settings visible |
| Category separator | View category overview | Lines between items |
| Cashback display | Open transaction with cashback | Shows amount and % |
| Credit limit | View credit card account | Shows available |

---

## Next Phase
→ Proceed to `implementation-phase6-removals.md`
