package com.fintrace.app.presentation.pending

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Savings
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
import com.fintrace.app.data.database.entity.AccountBalanceEntity
import com.fintrace.app.data.database.entity.CategoryEntity
import com.fintrace.app.data.database.entity.PendingTransactionEntity
import com.fintrace.app.data.database.entity.TransactionType
import com.fintrace.app.ui.icons.BrandIcons
import com.fintrace.app.ui.theme.Spacing
import com.fintrace.app.utils.CurrencyFormatter
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
    estimatedCashback: BigDecimal? = null,
    currentAccountCashback: BigDecimal? = null,
    onAmountChange: (BigDecimal) -> Unit,
    onMerchantChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onAccountChange: (bankName: String?, accountNumber: String?) -> Unit,
    onCashbackChange: (BigDecimal?) -> Unit,
    onCreateAccount: (bankName: String, accountLast4: String, balance: BigDecimal, isCreditCard: Boolean, creditLimit: BigDecimal?, cashbackPercent: BigDecimal?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showSmsDetails by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var amountText by remember(pending.amount) { mutableStateOf(pending.amount.toPlainString()) }
    var cashbackText by remember(currentAccountCashback) {
        mutableStateOf(currentAccountCashback?.toPlainString() ?: "")
    }

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

                    // Estimated Cashback Row (only show if there's cashback)
                    if (estimatedCashback != null && estimatedCashback > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "Est. Cashback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = "+${CurrencyFormatter.formatCurrency(estimatedCashback, pending.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

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
                    // Check if the pending transaction's account exists in the accounts list
                    val pendingAccountExists = accounts.any { account ->
                        account.bankName == pending.bankName &&
                        (account.accountLast4 == pending.accountNumber ||
                         (pending.accountNumber != null && account.accountLast4.contains(pending.accountNumber!!)) ||
                         (pending.accountNumber != null && pending.accountNumber!!.contains(account.accountLast4)))
                    }

                    // Show "Select Account" if the account doesn't exist, otherwise show the matched account
                    val currentAccountDisplay = if (pendingAccountExists && pending.bankName != null && pending.accountNumber != null) {
                        "${pending.bankName} ••${pending.accountNumber}"
                    } else if (pendingAccountExists && pending.bankName != null) {
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
                                    // Show "Create New Account" option if account doesn't exist
                                    if (!pendingAccountExists && pending.bankName != null) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            "Create New Account",
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            "${pending.bankName} ••${pending.accountNumber ?: "****"}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                accountExpanded = false
                                                showCreateAccountDialog = true
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }

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

                    // Cashback Configuration Row (only show for expense/credit transactions)
                    if (pending.transactionType == TransactionType.EXPENSE ||
                        pending.transactionType == TransactionType.EXPENSE) {
                        FormFieldRow(
                            label = "Cashback %",
                            content = {
                                OutlinedTextField(
                                    value = cashbackText,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                            val parsed = newValue.toDoubleOrNull()
                                            if (parsed == null || parsed <= 100) {
                                                cashbackText = newValue
                                                newValue.toBigDecimalOrNull()?.let { onCashbackChange(it) }
                                                    ?: if (newValue.isEmpty()) onCashbackChange(null) else Unit
                                            }
                                        }
                                    },
                                    suffix = { Text("%") },
                                    placeholder = { Text("e.g., 1.5") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
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
                    }

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
                            selected = pending.transactionType == TransactionType.EXPENSE,
                            onClick = { onTransactionTypeChange(TransactionType.EXPENSE) },
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

    // Create Account Dialog
    if (showCreateAccountDialog) {
        CreateAccountDialog(
            bankName = pending.bankName ?: "",
            accountLast4 = pending.accountNumber ?: "",
            currency = pending.currency,
            balanceAfter = pending.balanceAfter,
            onDismiss = { showCreateAccountDialog = false },
            onConfirm = { bankName, accountLast4, balance, isCreditCard, creditLimit, cashbackPercent ->
                onCreateAccount(bankName, accountLast4, balance, isCreditCard, creditLimit, cashbackPercent)
                showCreateAccountDialog = false
                // Auto-select the newly created account
                onAccountChange(bankName, accountLast4)
            }
        )
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

/**
 * Dialog for creating a new account from the pending transaction confirmation screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAccountDialog(
    bankName: String,
    accountLast4: String,
    currency: String,
    balanceAfter: BigDecimal? = null,
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, accountLast4: String, balance: BigDecimal, isCreditCard: Boolean, creditLimit: BigDecimal?, cashbackPercent: BigDecimal?) -> Unit
) {
    var bankNameText by remember { mutableStateOf(bankName) }
    var accountLast4Text by remember { mutableStateOf(accountLast4) }
    var isCreditCard by remember { mutableStateOf(false) }

    // For savings accounts: pre-fill balance with balanceAfter
    // For credit cards: pre-fill credit limit with balanceAfter (available limit)
    var balanceText by remember(isCreditCard) {
        mutableStateOf(if (!isCreditCard && balanceAfter != null) balanceAfter.toPlainString() else "")
    }
    var creditLimitText by remember(isCreditCard) {
        mutableStateOf(if (isCreditCard && balanceAfter != null) balanceAfter.toPlainString() else "")
    }
    var cashbackText by remember { mutableStateOf("") }

    val isValid = bankNameText.isNotBlank() &&
            accountLast4Text.length == 4 &&
            balanceText.isNotBlank() &&
            balanceText.toBigDecimalOrNull() != null &&
            (!isCreditCard || (creditLimitText.isNotBlank() && creditLimitText.toBigDecimalOrNull() != null))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Account Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCreditCard,
                        onClick = { isCreditCard = false },
                        label = { Text("Savings Account") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    FilterChip(
                        selected = isCreditCard,
                        onClick = { isCreditCard = true },
                        label = { Text("Credit Card") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Bank Name
                OutlinedTextField(
                    value = bankNameText,
                    onValueChange = { bankNameText = it },
                    label = { Text("Bank Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Account Last 4 Digits
                OutlinedTextField(
                    value = accountLast4Text,
                    onValueChange = { if (it.length <= 4) accountLast4Text = it },
                    label = { Text("Last 4 Digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text("Last 4 digits of account/card number")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Balance
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            balanceText = text
                        }
                    },
                    label = { Text(if (isCreditCard) "Outstanding Balance" else "Current Balance") },
                    prefix = { Text(CurrencyFormatter.getCurrencySymbol(currency)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(if (isCreditCard) "Amount currently owed" else "Available balance")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Credit Limit (only for credit cards)
                if (isCreditCard) {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { text ->
                            if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                                creditLimitText = text
                            }
                        },
                        label = { Text("Credit Limit") },
                        prefix = { Text(CurrencyFormatter.getCurrencySymbol(currency)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            Text("Total credit limit of the card")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Cashback Percentage
                OutlinedTextField(
                    value = cashbackText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            val parsed = text.toDoubleOrNull()
                            if (parsed == null || parsed <= 100) {
                                cashbackText = text
                            }
                        }
                    },
                    label = { Text("Default Cashback %") },
                    suffix = { Text("%") },
                    placeholder = { Text("e.g., 1.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text("Cashback earned on transactions (optional)")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val balance = balanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val creditLimit = if (isCreditCard) creditLimitText.toBigDecimalOrNull() else null
                            val cashback = cashbackText.toBigDecimalOrNull()
                            onConfirm(bankNameText, accountLast4Text, balance, isCreditCard, creditLimit, cashback)
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Account")
                    }
                }
            }
        }
    }
}
