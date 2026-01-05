package com.fintrace.app.ui.components.states

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrace.app.ui.components.FintraceCard

/**
 * Unified empty state card component.
 *
 * Use this for all empty state displays throughout the app.
 */
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

/**
 * Empty state for transactions list.
 */
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

/**
 * Empty state for subscriptions list.
 */
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

/**
 * Empty state for search results.
 */
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
