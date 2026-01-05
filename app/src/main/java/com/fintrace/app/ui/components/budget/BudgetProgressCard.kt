package com.fintrace.app.ui.components.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.repository.BudgetWithSpending
import com.fintrace.app.ui.components.FintraceCard
import com.fintrace.app.utils.CurrencyFormatter

/**
 * Card showing current budget progress.
 */
@Composable
fun BudgetProgressCard(
    budgetWithSpending: BudgetWithSpending,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        budgetWithSpending.percentUsed >= 100f -> MaterialTheme.colorScheme.error
        budgetWithSpending.percentUsed >= 80f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val animatedProgress by animateFloatAsState(
        targetValue = budgetWithSpending.progressPercent / 100f,
        label = "budgetProgress"
    )

    FintraceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${budgetWithSpending.percentUsed.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amounts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            budgetWithSpending.spentThisMonth,
                            budgetWithSpending.budget.currency
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (budgetWithSpending.isOverBudget) "Over by" else "Remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            budgetWithSpending.remaining.abs(),
                            budgetWithSpending.budget.currency
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (budgetWithSpending.isOverBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            budgetWithSpending.budget.amount,
                            budgetWithSpending.budget.currency
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Card shown when no budget is set.
 */
@Composable
fun NoBudgetCard(
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    FintraceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No Budget Set",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Set a monthly budget to track your spending",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onSetBudget) {
                Text("Set Budget")
            }
        }
    }
}
