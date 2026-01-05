package com.fintrace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fintrace.app.ui.theme.Spacing

/**
 * A card component styled for settings screens.
 * This is now an alias for FintraceCard with settings-appropriate defaults.
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler
 * @param containerColor Background color of the card
 * @param content Content to display inside the card
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    FintraceCard(
        modifier = modifier,
        containerColor = containerColor,
        onClick = onClick,
        content = content
    )
}

/**
 * A settings group container that groups multiple settings items together.
 *
 * Use this to wrap multiple SettingsItem composables that belong to the same logical group.
 *
 * @param modifier Modifier for the group
 * @param content Content (typically multiple SettingsItem composables)
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsCard(modifier = modifier) {
        content()
    }
}

/**
 * A single settings item with icon, title, subtitle, and optional trailing content.
 *
 * @param icon Icon to display
 * @param title Primary text
 * @param subtitle Secondary text
 * @param modifier Modifier for the item
 * @param iconContainerColor Background color for the icon circle
 * @param onClick Click handler
 * @param trailing Optional trailing content (switch, chevron, text, etc.)
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * A navigation settings item that displays a chevron and handles click.
 * This version wraps itself in a SettingsCard - use for standalone items.
 *
 * @param icon Icon to display
 * @param title Primary text
 * @param subtitle Secondary text
 * @param onClick Navigation click handler
 * @param modifier Modifier for the item
 */
@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        modifier = modifier,
        onClick = onClick
    ) {
        SettingsItem(
            icon = icon,
            title = title,
            subtitle = subtitle,
            trailing = {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

/**
 * A navigation settings item for use inside a SettingsGroup (no card wrapper).
 * Use this when grouping multiple items in a single card.
 *
 * @param icon Icon to display
 * @param title Primary text
 * @param subtitle Secondary text
 * @param onClick Navigation click handler
 * @param modifier Modifier for the item
 */
@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * A divider for use between items in a SettingsGroup.
 */
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier
            .padding(vertical = Spacing.xs)
            .padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * A toggle settings item with a switch.
 *
 * @param icon Icon to display
 * @param title Primary text
 * @param subtitle Secondary text
 * @param checked Current toggle state
 * @param onCheckedChange Toggle state change handler
 * @param modifier Modifier for the item
 * @param enabled Whether the toggle is enabled
 */
@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * A settings item that displays a value and navigates on click.
 *
 * @param icon Icon to display
 * @param title Primary text
 * @param subtitle Secondary text
 * @param value Current value to display
 * @param onClick Click handler
 * @param modifier Modifier for the item
 * @param valueColor Color for the value text
 */
@Composable
fun SettingsValueItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    SettingsItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor
            )
        }
    )
}
