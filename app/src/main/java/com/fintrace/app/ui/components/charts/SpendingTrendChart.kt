package com.fintrace.app.ui.components.charts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.fintrace.app.ui.theme.expense_dark
import com.fintrace.app.ui.theme.expense_light
import java.math.BigDecimal

/**
 * A line chart showing spending trend over time.
 *
 * Uses Vico chart library for smooth rendering and animations.
 */
@Composable
fun SpendingTrendChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val isDarkTheme = isSystemInDarkTheme()
    val lineColor = if (isDarkTheme) expense_dark else expense_light

    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Update the chart data when dailySpending changes
    remember(dailySpending) {
        if (dailySpending.isNotEmpty()) {
            modelProducer.tryRunTransaction {
                lineSeries {
                    series(dailySpending.map { it.amount.toDouble() })
                }
            }
        }
    }

    if (dailySpending.isEmpty()) return

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
    )
}

/**
 * Simplified spending chart without Vico for basic display.
 * Can be used as a fallback or for simpler use cases.
 */
@Composable
fun SimpleSpendingIndicator(
    spent: BigDecimal,
    budget: BigDecimal,
    modifier: Modifier = Modifier
) {
    val progress = if (budget > BigDecimal.ZERO) {
        (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val color = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    androidx.compose.material3.LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
