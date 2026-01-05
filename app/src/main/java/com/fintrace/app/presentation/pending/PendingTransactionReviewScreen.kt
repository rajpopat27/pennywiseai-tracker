package com.fintrace.app.presentation.pending

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen shown when the app is opened from a notification to review a specific pending transaction.
 * Loads the pending transaction and shows the confirmation dialog.
 */
@Composable
fun PendingTransactionReviewScreen(
    pendingId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPendingList: () -> Unit,
    viewModel: PendingTransactionsViewModel = hiltViewModel()
) {
    val selectedPending by viewModel.selectedPending.collectAsStateWithLifecycle()
    val editedPending by viewModel.editedPending.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val estimatedCashback by viewModel.estimatedCashback.collectAsStateWithLifecycle()
    val currentAccountCashback by viewModel.currentAccountCashback.collectAsStateWithLifecycle()

    // Load the pending transaction
    LaunchedEffect(pendingId) {
        viewModel.loadPendingById(pendingId)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionConfirmed -> {
                    if (pendingCount > 0) {
                        onNavigateToPendingList()
                    } else {
                        onNavigateBack()
                    }
                }
                is PendingTransactionsViewModel.PendingTransactionEvent.TransactionRejected -> {
                    if (pendingCount > 0) {
                        onNavigateToPendingList()
                    } else {
                        onNavigateBack()
                    }
                }
                else -> {}
            }
        }
    }

    // Show loading while fetching
    if (selectedPending == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Show confirmation dialog when pending is loaded
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
            onConfirm = viewModel::confirmSelected,
            onDismiss = {
                viewModel.rejectSelected()
            }
        )
    }
}
