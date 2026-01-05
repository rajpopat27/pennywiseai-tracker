package com.fintrace.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A floating pill segmented button component with a sliding selection indicator.
 *
 * This component displays a row of selectable options with a filled pill that
 * slides to indicate the selected option. The design follows a modern floating
 * pill aesthetic with a container background and elevated selected indicator.
 *
 * @param options List of string options to display
 * @param selectedIndex The currently selected option index
 * @param onOptionSelected Callback when an option is selected
 * @param modifier Modifier for the component
 * @param containerColor Background color of the entire container
 * @param selectedColor Background color of the selected pill
 * @param selectedTextColor Text color for the selected option
 * @param unselectedTextColor Text color for unselected options
 * @param cornerRadius Corner radius for the container and pill
 * @param pillElevation Shadow elevation for the selected pill
 * @param height Height of the segmented button
 */
@Composable
fun FloatingPillSegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedColor: Color = MaterialTheme.colorScheme.surface,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerRadius: Dp = 24.dp,
    pillElevation: Dp = 2.dp,
    height: Dp = 48.dp
) {
    require(options.isNotEmpty()) { "Options list cannot be empty" }
    require(selectedIndex in options.indices) { "Selected index must be within options range" }

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val pillPadding = 4.dp
    val pillPaddingPx = with(density) { pillPadding.toPx() }

    val optionWidthPx = if (containerWidthPx > 0) {
        (containerWidthPx - pillPaddingPx * 2) / options.size
    } else {
        0f
    }

    val animatedOffsetFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "pillOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .onSizeChanged { size ->
                containerWidthPx = size.width
            }
    ) {
        // Sliding pill indicator
        if (optionWidthPx > 0f) {
            val pillHeight = height - pillPadding * 2
            val pillWidth = with(density) { (optionWidthPx - pillPaddingPx).toDp() }
            val offsetX = (pillPaddingPx + optionWidthPx * animatedOffsetFraction).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, with(density) { pillPadding.toPx().roundToInt() }) }
                    .width(pillWidth)
                    .height(pillHeight)
                    .shadow(pillElevation, RoundedCornerShape(cornerRadius - 2.dp))
                    .clip(RoundedCornerShape(cornerRadius - 2.dp))
                    .background(selectedColor)
            )
        }

        // Options row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pillPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == selectedIndex) selectedTextColor else unselectedTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Overload that accepts a generic type with a display string mapper.
 * Useful for enum values or data classes.
 *
 * @param options List of options of type T
 * @param selectedOption The currently selected option
 * @param onOptionSelected Callback when an option is selected
 * @param optionLabel Lambda to convert option to display string
 * @param modifier Modifier for the component
 * @param containerColor Background color of the entire container
 * @param selectedColor Background color of the selected pill
 * @param selectedTextColor Text color for the selected option
 * @param unselectedTextColor Text color for unselected options
 * @param cornerRadius Corner radius for the container and pill
 */
@Composable
fun <T> FloatingPillSegmentedButton(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedColor: Color = MaterialTheme.colorScheme.surface,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerRadius: Dp = 24.dp
) {
    val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)

    FloatingPillSegmentedButton(
        options = options.map(optionLabel),
        selectedIndex = selectedIndex,
        onOptionSelected = { index -> onOptionSelected(options[index]) },
        modifier = modifier,
        containerColor = containerColor,
        selectedColor = selectedColor,
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        cornerRadius = cornerRadius
    )
}
