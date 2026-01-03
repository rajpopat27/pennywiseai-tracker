package com.pennywiseai.tracker.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.PendingTransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet-style confirmation dialog for pending transactions.
 * Styled similar to Expenzio PDF Service modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionConfirmationDialog(
    pending: PendingTransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountBalanceEntity>,
    isLoading: Boolean,
    onAmountChange: (BigDecimal) -> Unit,
    onMerchantChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onAccountChange: (bankName: String?, accountNumber: String?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showSmsDetails by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var amountText by remember(pending.amount) { mutableStateOf(pending.amount.toPlainString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with close button and title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Bank icon and name
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val bankIcon: Int? = pending.bankName?.let { name -> BrandIcons.getIconResource(name) }
                        if (bankIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = bankIcon),
                                    contentDescription = pending.bankName,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pending.bankName?.firstOrNull()?.uppercase() ?: "T",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = pending.bankName ?: "New Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = pending.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Transaction Details Section
                    SectionHeader(title = "Transaction Details")

                    // Amount Row
                    FormFieldRow(
                        label = "Amount",
                        content = {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { newValue ->
                                    amountText = newValue
                                    newValue.toBigDecimalOrNull()?.let { onAmountChange(it) }
                                },
                                prefix = {
                                    Text(
                                        CurrencyFormatter.getCurrencySymbol(pending.currency),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(140.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    )

                    // Merchant Row
                    FormFieldRow(
                        label = "Merchant",
                        content = {
                            OutlinedTextField(
                                value = pending.merchantName,
                                onValueChange = onMerchantChange,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.6f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    )

                    // Category Row
                    FormFieldRow(
                        label = "Category",
                        content = {
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = it },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { categoryExpanded = true },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pending.category,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                onCategoryChange(category.name)
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // Account Row
                    val currentAccountDisplay = if (pending.bankName != null && pending.accountNumber != null) {
                        "${pending.bankName} ••${pending.accountNumber}"
                    } else if (pending.bankName != null) {
                        pending.bankName
                    } else {
                        "Select Account"
                    }

                    FormFieldRow(
                        label = "Account",
                        content = {
                            ExposedDropdownMenuBox(
                                expanded = accountExpanded,
                                onExpandedChange = { accountExpanded = it },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { accountExpanded = true },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentAccountDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                ExposedDropdownMenu(
                                    expanded = accountExpanded,
                                    onDismissRequest = { accountExpanded = false }
                                ) {
                                    accounts.forEach { account ->
                                        val displayText = "${account.bankName} ••${account.accountLast4}"
                                        DropdownMenuItem(
                                            text = { Text(displayText) },
                                            onClick = {
                                                onAccountChange(account.bankName, account.accountLast4)
                                                accountExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transaction Type Section
                    SectionHeader(title = "Transaction Type")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = pending.transactionType == TransactionType.INCOME,
                            onClick = { onTransactionTypeChange(TransactionType.INCOME) },
                            label = { Text("Income", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        FilterChip(
                            selected = pending.transactionType == TransactionType.EXPENSE,
                            onClick = { onTransactionTypeChange(TransactionType.EXPENSE) },
                            label = { Text("Expense", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        FilterChip(
                            selected = pending.transactionType == TransactionType.CREDIT,
                            onClick = { onTransactionTypeChange(TransactionType.CREDIT) },
                            label = { Text("Credit", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description (optional)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Description (optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = pending.description ?: "",
                            onValueChange = { onDescriptionChange(it.ifBlank { null }) },
                            placeholder = { Text("Add a note...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // SMS details (expandable)
                    if (pending.smsBody != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clickable { showSmsDetails = !showSmsDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Original SMS",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (showSmsDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (showSmsDetails) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = pending.smsBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Confirm button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading && pending.merchantName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Confirm Transaction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FormFieldRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Box(
            modifier = Modifier.weight(0.65f),
            contentAlignment = Alignment.CenterEnd
        ) {
            content()
        }
    }
}
