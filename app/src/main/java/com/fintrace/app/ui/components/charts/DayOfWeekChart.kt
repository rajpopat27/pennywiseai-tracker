package com.fintrace.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fintrace.app.ui.theme.Spacing
import com.fintrace.app.ui.theme.expense_dark
import com.fintrace.app.ui.theme.expense_light
import com.fintrace.app.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Horizontal bar chart showing spending by day of the week.
 *
 * Provides insights into which days users spend the most.
 */
@Composable
fun DayOfWeekChart(
    data: List<DaySpending>,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val isDarkTheme = isSystemInDarkTheme()
    val barColor = if (isDarkTheme) expense_dark else expense_light

    // Find max for scaling
    val maxAmount = data.maxOfOrNull { it.amount } ?: BigDecimal.ZERO

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        // Sort by day of week order
        val sortedData = data.sortedBy { it.dayOfWeek.value }

        sortedData.forEach { dayData ->
            val progress = if (maxAmount > BigDecimal.ZERO) {
                (dayData.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day label
                Text(
                    text = dayData.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(Spacing.xs))

                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.xs))

                // Amount
                Text(
                    text = CurrencyFormatter.formatCurrency(dayData.amount, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp)
                )
            }
        }
    }
}

/**
 * Creates default day spending data with all days of the week.
 */
fun createDefaultDaySpending(): List<DaySpending> {
    return DayOfWeek.entries.map { day ->
        DaySpending(
            dayOfWeek = day,
            dayName = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            amount = BigDecimal.ZERO
        )
    }
}
