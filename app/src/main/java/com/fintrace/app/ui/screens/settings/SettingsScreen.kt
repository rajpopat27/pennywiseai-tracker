package com.fintrace.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrace.app.core.Constants
import com.fintrace.app.ui.components.FloatingPillSegmentedButton
import com.fintrace.app.ui.components.FintraceCard
import com.fintrace.app.ui.components.FintraceScaffold
import com.fintrace.app.ui.components.SectionHeader
import com.fintrace.app.ui.components.SettingsCard
import com.fintrace.app.ui.components.SettingsDivider
import com.fintrace.app.ui.components.SettingsNavigationRow
import com.fintrace.app.ui.components.SettingsItem
import com.fintrace.app.ui.components.SettingsToggleItem
import com.fintrace.app.ui.theme.Dimensions
import com.fintrace.app.ui.theme.Spacing
import com.fintrace.app.ui.viewmodel.ThemeViewModel
import com.fintrace.app.ui.components.dialogs.BudgetInputDialog
import com.fintrace.app.utils.CurrencyFormatter
import com.fintrace.app.data.repository.ScanPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToMerchantAliases: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val isTransactionConfirmationEnabled by settingsViewModel.isTransactionConfirmationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val isBypassConfirmationForScans by settingsViewModel.isBypassConfirmationForScans.collectAsStateWithLifecycle(initialValue = true)
    val scanPeriod by settingsViewModel.scanPeriod.collectAsStateWithLifecycle()
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    val budgetWithSpending by settingsViewModel.budgetWithSpending.collectAsStateWithLifecycle()
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importBackup(it)
            }
        }
    )
    
    // File saver for export
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.saveBackupToFile(it)
            }
        }
    )
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Appearance Section
            SectionHeader(title = "Appearance")

            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = when (themeUiState.isDarkTheme) {
                        null -> "System default"
                        true -> "Dark mode"
                        false -> "Light mode"
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                val themeOptions = listOf("System", "Light", "Dark")
                val selectedThemeIndex = when (themeUiState.isDarkTheme) {
                    null -> 0
                    false -> 1
                    true -> 2
                }

                FloatingPillSegmentedButton(
                    options = themeOptions,
                    selectedIndex = selectedThemeIndex,
                    onOptionSelected = { index ->
                        when (index) {
                            0 -> themeViewModel.updateDarkTheme(null)
                            1 -> themeViewModel.updateDarkTheme(false)
                            2 -> themeViewModel.updateDarkTheme(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Budget Section
            SectionHeader(title = "Budget")

            SettingsCard(
                onClick = { showBudgetDialog = true }
            ) {
                SettingsItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Monthly Budget",
                    subtitle = if (budgetWithSpending != null) {
                        "${budgetWithSpending!!.percentUsed.toInt()}% used this month"
                    } else {
                        "Set a spending limit for the month"
                    },
                    trailing = {
                        if (budgetWithSpending != null) {
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    budgetWithSpending!!.budget.amount,
                                    budgetWithSpending!!.budget.currency
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // Data Management Section
            SectionHeader(title = "Data Management")

            // Group: Manage Accounts, Categories, Smart Rules
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Default.AccountBalance,
                    title = "Manage Accounts",
                    subtitle = "Add manual accounts and update balances",
                    onClick = onNavigateToManageAccounts
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Category,
                    title = "Categories",
                    subtitle = "Manage expense and income categories",
                    onClick = onNavigateToCategories
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Smart Rules",
                    subtitle = "Automatic transaction categorization",
                    onClick = onNavigateToRules
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.SwapHoriz,
                    title = "Merchant Aliases",
                    subtitle = "Rename merchants for better readability",
                    onClick = onNavigateToMerchantAliases
                )
            }

            // Group: Export/Import Data
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Default.Upload,
                    title = "Export Data",
                    subtitle = "Backup all data to a file",
                    onClick = { settingsViewModel.exportBackup() }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Default.Download,
                    title = "Import Data",
                    subtitle = "Restore data from backup",
                    onClick = { importLauncher.launch("*/*") }
                )
            }

            // SMS Scan Period
            SettingsCard(
                onClick = { showSmsScanDialog = true }
            ) {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = "SMS Scan Period",
                    subtitle = if (smsScanAllTime) "Scan all SMS messages" else "Scan last $smsScanMonths months",
                    trailing = {
                        Text(
                            text = if (smsScanAllTime) "All Time" else "$smsScanMonths months",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            // Transaction Confirmation Toggle
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Verified,
                    title = "Transaction Confirmation",
                    subtitle = if (isTransactionConfirmationEnabled)
                        "Review transactions before saving"
                    else
                        "Transactions saved automatically",
                    checked = isTransactionConfirmationEnabled,
                    onCheckedChange = { settingsViewModel.toggleTransactionConfirmation(it) }
                )

                // Bypass Confirmation for Scans - only show when confirmation is enabled
                AnimatedVisibility(visible = isTransactionConfirmationEnabled) {
                    Column {
                        SettingsDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Skip Confirmation for Scans",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isBypassConfirmationForScans)
                                        "SMS scans save directly"
                                    else
                                        "SMS scans go to pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isBypassConfirmationForScans,
                                onCheckedChange = { settingsViewModel.toggleBypassConfirmationForScans(it) }
                            )
                        }
                    }
                }
            }

            // Unrecognized Messages Section (only show if count > 0)
            val unreportedCount by settingsViewModel.unreportedSmsCount.collectAsStateWithLifecycle()

            if (unreportedCount > 0) {
                SectionHeader(title = "Help Improve Fintrace")

                SettingsCard(
                    onClick = {
                        Log.d("SettingsScreen", "Navigating to UnrecognizedSms screen")
                        onNavigateToUnrecognizedSms()
                    }
                ) {
                    SettingsItem(
                        icon = Icons.Default.Warning,
                        title = "Unrecognized Bank Messages",
                        subtitle = "$unreportedCount message${if (unreportedCount > 1) "s" else ""} from potential banks",
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(unreportedCount.toString())
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "View Messages",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            // Help Section
            SectionHeader(title = "Help")

            SettingsCard {
                // Help & FAQ
                SettingsNavigationRow(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help & FAQ",
                    subtitle = "Frequently asked questions",
                    onClick = onNavigateToFaq
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // SMS Scan Period Dialog
    if (showSmsScanDialog) {
        AlertDialog(
            onDismissRequest = { showSmsScanDialog = false },
            title = { Text("SMS Scan Period") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Choose how far back to scan SMS for transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    // New scan period options: 1 day, 1 week, 15 days, Since Install
                    ScanPeriod.entries.forEach { period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.updateScanPeriod(period)
                                    showSmsScanDialog = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scanPeriod == period,
                                onClick = {
                                    settingsViewModel.updateScanPeriod(period)
                                    showSmsScanDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when(period) {
                                    ScanPeriod.ONE_DAY -> "Last 24 hours"
                                    ScanPeriod.ONE_WEEK -> "Last 7 days"
                                    ScanPeriod.FIFTEEN_DAYS -> "Last 15 days"
                                    ScanPeriod.SINCE_INSTALL -> "Since app install"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmsScanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Show import/export message
    importExportMessage?.let { message ->
        // Check if we have an exported file ready
        if (exportedBackupFile != null && message.contains("successfully! Choose")) {
            showExportOptionsDialog = true
        } else {
            LaunchedEffect(message) {
                // Auto-clear message after 5 seconds
                kotlinx.coroutines.delay(5000)
                settingsViewModel.clearImportExportMessage()
            }
            
            AlertDialog(
                onDismissRequest = { settingsViewModel.clearImportExportMessage() },
                title = { Text("Backup Status") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.clearImportExportMessage() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
    
    // Export options dialog
    if (showExportOptionsDialog && exportedBackupFile != null) {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "Fintrace_Backup_$timestamp.pennywisebackup"
        
        AlertDialog(
            onDismissRequest = { 
                showExportOptionsDialog = false
                settingsViewModel.clearImportExportMessage()
            },
            title = { Text("Save Backup") },
            text = { 
                Column {
                    Text("Backup created successfully!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose how you want to save it:", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = { 
                            exportSaveLauncher.launch(fileName)
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Files")
                    }
                    
                    TextButton(
                        onClick = { 
                            settingsViewModel.shareBackup()
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showExportOptionsDialog = false
                        settingsViewModel.clearImportExportMessage()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Budget Dialog
    if (showBudgetDialog) {
        BudgetInputDialog(
            currentAmount = budgetWithSpending?.budget?.amount,
            onConfirm = { amount ->
                settingsViewModel.setBudget(amount)
                showBudgetDialog = false
            },
            onDelete = if (budgetWithSpending != null) {
                {
                    settingsViewModel.deleteBudget()
                    showBudgetDialog = false
                }
            } else null,
            onDismiss = { showBudgetDialog = false }
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FintraceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}