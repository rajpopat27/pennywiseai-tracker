package com.fintrace.app.presentation.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.ui.icons.BrandIcons
import com.fintrace.app.ui.theme.Dimensions
import com.fintrace.app.ui.theme.Spacing
import com.fintrace.app.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PendingTransactionsViewModel = hiltViewModel()
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val selectedPending by viewModel.selectedPending.collectAsStateWithLifecycle()
    val editedPending by viewModel.editedPending.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val estimatedCashback by viewModel.estimatedCashback.collectAsStateWithLifecycle()
    val currentAccountCashback by viewModel.currentAccountCashback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionConfirmed -> {
                    snackbarHostState.showSnackbar("Transaction saved!")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionRejected -> {
                    snackbarHostState.showSnackbar("Transaction dismissed")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.AllConfirmed -> {
                    snackbarHostState.showSnackbar("${event.count} transactions saved")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionAlreadyAdded -> {
                    snackbarHostState.showSnackbar("Transaction already added")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Show confirmation dialog when a transaction is selected
    editedPending?.let { pending ->
        TransactionConfirmationDialog(
            pending = pending,
            categories = categories,
            accounts = accounts,
            isLoading = isLoading,
            estimatedCashback = estimatedCashback,
            currentAccountCashback = currentAccountCashback,
            onAmountChange = viewModel::updateAmount,
            onMerchantChange = viewModel::updateMerchant,
            onCategoryChange = viewModel::updateCategory,
            onTransactionTypeChange = viewModel::updateTransactionType,
            onDescriptionChange = viewModel::updateDescription,
            onAccountChange = viewModel::updateAccount,
            onCashbackChange = viewModel::updateAccountCashback,
            onCreateAccount = viewModel::createAccount,
            onSaveAsAlias = viewModel::saveAsAlias,
            onConfirm = viewModel::confirmSelected,
            onDismiss = viewModel::clearSelection
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Refresh/sync button
                    IconButton(onClick = { viewModel.refreshPendingTransactions() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (pendingTransactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content)
                ) {
                    Button(
                        onClick = viewModel::confirmAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Confirm All (${pendingTransactions.size})")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPendingTransactions() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pendingTransactions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Pending Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "All transactions have been reviewed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = pendingTransactions,
                        key = { it.id }
                    ) { pending ->
                        SwipeablePendingTransactionItem(
                            pending = pending,
                            onClick = { viewModel.selectPending(pending) },
                            onDismiss = { viewModel.rejectPending(pending.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content-only version for embedding in MainScreen with bottom navigation.
 * Does not include its own Scaffold/TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsContent(
    viewModel: PendingTransactionsViewModel
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val editedPending by viewModel.editedPending.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val estimatedCashback by viewModel.estimatedCashback.collectAsStateWithLifecycle()
    val currentAccountCashback by viewModel.currentAccountCashback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionConfirmed -> {
                    snackbarHostState.showSnackbar("Transaction saved!")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionRejected -> {
                    snackbarHostState.showSnackbar("Transaction dismissed")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.AllConfirmed -> {
                    snackbarHostState.showSnackbar("${event.count} transactions saved")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionAlreadyAdded -> {
                    snackbarHostState.showSnackbar("Transaction already added")
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Show confirmation dialog when a transaction is selected
    editedPending?.let { pending ->
        TransactionConfirmationDialog(
            pending = pending,
            categories = categories,
            accounts = accounts,
            isLoading = isLoading,
            estimatedCashback = estimatedCashback,
            currentAccountCashback = currentAccountCashback,
            onAmountChange = viewModel::updateAmount,
            onMerchantChange = viewModel::updateMerchant,
            onCategoryChange = viewModel::updateCategory,
            onTransactionTypeChange = viewModel::updateTransactionType,
            onDescriptionChange = viewModel::updateDescription,
            onAccountChange = viewModel::updateAccount,
            onCashbackChange = viewModel::updateAccountCashback,
            onCreateAccount = viewModel::createAccount,
            onSaveAsAlias = viewModel::saveAsAlias,
            onConfirm = viewModel::confirmSelected,
            onDismiss = viewModel::clearSelection
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPendingTransactions() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (pendingTransactions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Pending Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "All transactions have been reviewed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(
                            items = pendingTransactions,
                            key = { it.id }
                        ) { pending ->
                            SwipeablePendingTransactionItem(
                                pending = pending,
                                onClick = { viewModel.selectPending(pending) },
                                onDismiss = { viewModel.rejectPending(pending.id) }
                            )
                        }
                    }

                    // Confirm all button at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content)
                    ) {
                        Button(
                            onClick = viewModel::confirmAll,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text("Confirm All (${pendingTransactions.size})")
                            }
                        }
                    }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePendingTransactionItem(
    pending: PendingTransactionEntity,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = Spacing.lg),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        PendingTransactionItem(
            pending = pending,
            onClick = onClick
        )
    }
}

@Composable
private fun PendingTransactionItem(
    pending: PendingTransactionEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank icon
            val bankIcon: Int? = pending.bankName?.let { name -> BrandIcons.getIconResource(name) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bankIcon != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = bankIcon),
                        contentDescription = pending.bankName,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(
                        text = pending.bankName?.firstOrNull()?.uppercase() ?: "T",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = pending.merchantName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Show alias indicator if an alias was applied
                    if (pending.originalMerchantName != null) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Aliased from ${pending.originalMerchantName}",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${pending.category} • ${pending.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val isIncome = pending.transactionType == TransactionType.INCOME
                Text(
                    text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.getCurrencySymbol(pending.currency)}${pending.amount.toPlainString()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                // Show account info (bank + account number)
                val accountDisplay = if (pending.bankName != null && pending.accountNumber != null) {
                    "${pending.bankName} ••${pending.accountNumber}"
                } else {
                    pending.bankName ?: ""
                }
                Text(
                    text = accountDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
