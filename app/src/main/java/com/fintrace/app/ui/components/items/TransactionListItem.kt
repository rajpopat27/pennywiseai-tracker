package com.fintrace.app.ui.components.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.ui.components.CategoryChip
import com.fintrace.app.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Configuration for TransactionListItem appearance.
 */
data class TransactionItemConfig(
    val showIcon: Boolean = true,
    val showCategory: Boolean = true,
    val showAccount: Boolean = false,
    val showCashback: Boolean = false,
    val showDate: Boolean = true,
    val showTime: Boolean = false,
    val compact: Boolean = false
)

/**
 * Unified transaction list item component.
 *
 * Use this component for all transaction lists in the app.
 */
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
                CategoryChip(
                    category = transaction.category ?: "Other",
                    showLabel = false
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
                        add(transaction.category!!)
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
                text = "$amountPrefix${CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency)}",
                style = if (config.compact)
                    MaterialTheme.typography.bodyMedium
                else
                    MaterialTheme.typography.titleMedium,
                color = amountColor
            )

            if (config.showCashback && transaction.cashbackAmount != null &&
                transaction.cashbackAmount > BigDecimal.ZERO) {
                Text(
                    text = "+${CurrencyFormatter.formatCurrency(transaction.cashbackAmount, transaction.currency)} cashback",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Compact version of TransactionListItem for home screen and other compact displays.
 */
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

/**
 * Full version of TransactionListItem with all details.
 */
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
