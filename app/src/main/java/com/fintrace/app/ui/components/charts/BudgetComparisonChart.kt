package com.fintrace.app.ui.components.charts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.fintrace.app.ui.theme.Spacing
import com.fintrace.app.ui.theme.expense_dark
import com.fintrace.app.ui.theme.expense_light
import com.fintrace.app.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * A comparison chart showing budget vs actual spending over months.
 *
 * Uses a grouped bar chart with two colors:
 * - Primary color for budget
 * - Expense color for actual spending
 */
@Composable
fun BudgetComparisonChart(
    data: List<MonthlyBudgetData>,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val isDarkTheme = isSystemInDarkTheme()
    val spentColor = if (isDarkTheme) expense_dark else expense_light
    val budgetColor = MaterialTheme.colorScheme.primary

    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Update the chart data
    remember(data) {
        if (data.isNotEmpty()) {
            modelProducer.tryRunTransaction {
                columnSeries {
                    // Budget series
                    series(data.map { it.budgetAmount?.toDouble() ?: 0.0 })
                    // Spent series
                    series(data.map { it.spentAmount.toDouble() })
                }
            }
        }
    }

    if (data.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Chart
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = budgetColor, label = "Budget")
            Spacer(modifier = Modifier.width(Spacing.md))
            LegendItem(color = spentColor, label = "Spent")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Surface(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape),
            color = color
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A simplified budget progress bar with month labels.
 * Alternative to full chart for simpler displays.
 */
@Composable
fun MonthlyBudgetProgressBars(
    data: List<MonthlyBudgetData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val spentColor = if (isDarkTheme) expense_dark else expense_light

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        data.takeLast(3).forEach { monthData ->
            val progress = if (monthData.budgetAmount != null && monthData.budgetAmount > BigDecimal.ZERO) {
                (monthData.spentAmount.toFloat() / monthData.budgetAmount.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            val isOverBudget = monthData.budgetAmount != null &&
                monthData.spentAmount > monthData.budgetAmount

            val progressColor = when {
                isOverBudget -> MaterialTheme.colorScheme.error
                progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            val monthName = Month.of(monthData.month)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(40.dp)
                )

                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .padding(horizontal = Spacing.xs),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = CurrencyFormatter.formatCurrency(monthData.spentAmount, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
