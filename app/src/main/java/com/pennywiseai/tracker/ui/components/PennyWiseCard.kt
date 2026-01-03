package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * Base card component with consistent styling across the app.
 *
 * Design language:
 * - Light mode: Subtle shadow (2dp elevation)
 * - Dark mode: Border stroke instead of shadow
 * - Rounded corners (16dp)
 * - Consistent padding (horizontal: 16dp, vertical: 8dp for outer; 16dp inner)
 *
 * @param modifier Modifier for the card
 * @param shape Shape of the card (default: 16dp rounded corners)
 * @param containerColor Background color of the card
 * @param onClick Optional click handler
 * @param contentPadding Padding inside the card
 * @param outerPadding Whether to apply outer padding (horizontal/vertical margins)
 * @param content Content to display inside the card
 */
@Composable
fun PennyWiseCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.md,
    outerPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val cardBorder = if (isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    } else {
        null
    }

    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (isDark) 0.dp else 2.dp
    )

    val cardModifier = if (outerPadding) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    } else {
        modifier.fillMaxWidth()
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = elevation,
            border = cardBorder
        ) {
            Column(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = elevation,
            border = cardBorder
        ) {
            Column(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

/**
 * Compact variant of PennyWiseCard without outer padding.
 * Use this when cards are placed inside other containers or need custom spacing.
 */
@Composable
fun PennyWiseCardCompact(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.md,
    content: @Composable ColumnScope.() -> Unit
) {
    PennyWiseCard(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        onClick = onClick,
        contentPadding = contentPadding,
        outerPadding = false,
        content = content
    )
}
