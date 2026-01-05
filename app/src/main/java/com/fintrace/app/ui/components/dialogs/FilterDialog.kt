package com.fintrace.app.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic filter dialog for selecting multiple items from a list.
 *
 * Usage:
 * ```
 * FilterDialog(
 *     title = "Filter by Category",
 *     items = allCategories,
 *     selectedItems = selectedCategories,
 *     itemLabel = { it },
 *     onConfirm = { selected -> viewModel.updateCategories(selected) },
 *     onDismiss = { showDialog = false }
 * )
 * ```
 */
@Composable
fun <T> FilterDialog(
    title: String,
    items: List<T>,
    selectedItems: Set<T>,
    itemLabel: (T) -> String,
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchEnabled: Boolean = true,
    itemIcon: (@Composable (T) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var tempSelection by remember(selectedItems) { mutableStateOf(selectedItems) }

    val filteredItems = if (searchEnabled && searchQuery.isNotEmpty()) {
        items.filter { itemLabel(it).contains(searchQuery, ignoreCase = true) }
    } else {
        items
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column {
                // Search field (only show if many items)
                if (searchEnabled && items.size > 10) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Select All / Clear All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { tempSelection = filteredItems.toSet() }
                    ) {
                        Text("Select All")
                    }
                    TextButton(
                        onClick = { tempSelection = emptySet() }
                    ) {
                        Text("Clear All")
                    }
                }

                HorizontalDivider()

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = item in tempSelection

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelection = if (isSelected) {
                                        tempSelection - item
                                    } else {
                                        tempSelection + item
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    tempSelection = if (checked) {
                                        tempSelection + item
                                    } else {
                                        tempSelection - item
                                    }
                                }
                            )

                            if (itemIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                itemIcon(item)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = itemLabel(item),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelection) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Amount range filter dialog.
 */
@Composable
fun AmountRangeFilterDialog(
    currentRange: Pair<Double, Double>?,
    onConfirm: (Pair<Double, Double>?) -> Unit,
    onDismiss: () -> Unit
) {
    var minAmount by remember { mutableStateOf(currentRange?.first?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentRange?.second?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Amount") },
        text = {
            Column {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) minAmount = it },
                    label = { Text("Min Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) maxAmount = it },
                    label = { Text("Max Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minAmount.toDoubleOrNull() ?: 0.0
                    val max = maxAmount.toDoubleOrNull() ?: Double.MAX_VALUE
                    onConfirm(Pair(min, max))
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
