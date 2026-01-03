package com.pennywiseai.tracker.ui.screens.settings

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
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.SectionHeader
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

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
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: com.pennywiseai.tracker.ui.viewmodel.AppLockViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by settingsViewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by settingsViewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by settingsViewModel.totalMB.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled by settingsViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isTransactionConfirmationEnabled by settingsViewModel.isTransactionConfirmationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val isBypassConfirmationForScans by settingsViewModel.isBypassConfirmationForScans.collectAsStateWithLifecycle(initialValue = true)
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
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

            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
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
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (themeUiState.isDarkTheme) {
                                    null -> "System default"
                                    true -> "Dark mode"
                                    false -> "Light mode"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = themeUiState.isDarkTheme == null,
                        onClick = { themeViewModel.updateDarkTheme(null) },
                        label = { Text("System") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == false,
                        onClick = { themeViewModel.updateDarkTheme(false) },
                        label = { Text("Light") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == true,
                        onClick = { themeViewModel.updateDarkTheme(true) },
                        label = { Text("Dark") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Security Section
            SectionHeader(title = "Security")

            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
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
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Lock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (appLockUiState.canUseBiometric) {
                                    "Protect with biometric authentication"
                                } else {
                                    appLockUiState.biometricCapability.getErrorMessage()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (appLockUiState.canUseBiometric) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                    Switch(
                        checked = appLockUiState.isLockEnabled,
                        onCheckedChange = { enabled ->
                            appLockViewModel.setAppLockEnabled(enabled)
                        },
                        enabled = appLockUiState.canUseBiometric
                    )
                }

                // Lock Timeout Setting (only show if app lock is enabled)
                AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeoutDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lock Timeout",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (appLockUiState.timeoutMinutes) {
                                        0 -> "Lock immediately"
                                        1 -> "Lock after 1 minute"
                                        else -> "Lock after ${appLockUiState.timeoutMinutes} minutes"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Data Management Section
            SectionHeader(title = "Data Management")

            // Manage Accounts
            SettingsNavigationItem(
                icon = Icons.Default.AccountBalance,
                title = "Manage Accounts",
                subtitle = "Add manual accounts and update balances",
                onClick = onNavigateToManageAccounts
            )

            // Categories
            SettingsNavigationItem(
                icon = Icons.Default.Category,
                title = "Categories",
                subtitle = "Manage expense and income categories",
                onClick = onNavigateToCategories
            )

            // Smart Rules
            SettingsNavigationItem(
                icon = Icons.Default.AutoAwesome,
                title = "Smart Rules",
                subtitle = "Automatic transaction categorization",
                onClick = onNavigateToRules
            )

            // Export Data
            SettingsNavigationItem(
                icon = Icons.Default.Upload,
                title = "Export Data",
                subtitle = "Backup all data to a file",
                onClick = { settingsViewModel.exportBackup() }
            )

            // Import Data
            SettingsNavigationItem(
                icon = Icons.Default.Download,
                title = "Import Data",
                subtitle = "Restore data from backup",
                onClick = { importLauncher.launch("*/*") }
            )

            // SMS Scan Period
            PennyWiseCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSmsScanDialog = true }
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
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SMS Scan Period",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (smsScanAllTime) "Scan all SMS messages" else "Scan last $smsScanMonths months",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (smsScanAllTime) "All Time" else "$smsScanMonths months",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Transaction Confirmation Toggle
            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
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
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Transaction Confirmation",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isTransactionConfirmationEnabled)
                                        "Review transactions before saving"
                                    else
                                        "Transactions saved automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isTransactionConfirmationEnabled,
                            onCheckedChange = { settingsViewModel.toggleTransactionConfirmation(it) }
                        )
                    }

                    // Bypass Confirmation for Scans - only show when confirmation is enabled
                    AnimatedVisibility(visible = isTransactionConfirmationEnabled) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
            }

            // AI Features Section
            SectionHeader(title = "AI Features")

            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
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
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Chat Assistant",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (downloadState) {
                                    DownloadState.NOT_DOWNLOADED -> "Download model (${Constants.ModelDownload.MODEL_SIZE_MB} MB)"
                                    DownloadState.DOWNLOADING -> "Downloading..."
                                    DownloadState.PAUSED -> "Download interrupted"
                                    DownloadState.COMPLETED -> "Ready for chat"
                                    DownloadState.FAILED -> "Download failed"
                                    DownloadState.ERROR_INSUFFICIENT_SPACE -> "Not enough space"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (downloadState) {
                        DownloadState.NOT_DOWNLOADED -> {
                            Button(
                                onClick = { settingsViewModel.startModelDownload() }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download")
                            }
                        }
                        DownloadState.DOWNLOADING -> {
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadState.PAUSED, DownloadState.FAILED -> {
                            Button(
                                onClick = { settingsViewModel.startModelDownload() },
                                colors = if (downloadState == DownloadState.FAILED) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                } else ButtonDefaults.buttonColors()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                        DownloadState.COMPLETED -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                TextButton(onClick = { settingsViewModel.deleteModel() }) {
                                    Text("Delete")
                                }
                            }
                        }
                        DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = downloadState == DownloadState.DOWNLOADING,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "$downloadedMB MB / $totalMB MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { settingsViewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                }

                if (downloadState == DownloadState.NOT_DOWNLOADED || downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Chat with AI about your expenses. All data stays private on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unrecognized Messages Section (only show if count > 0)
            val unreportedCount by settingsViewModel.unreportedSmsCount.collectAsStateWithLifecycle()

            if (unreportedCount > 0) {
                SectionHeader(title = "Help Improve PennyWise")

                PennyWiseCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        Log.d("SettingsScreen", "Navigating to UnrecognizedSms screen")
                        onNavigateToUnrecognizedSms()
                    }
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
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Unrecognized Bank Messages",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$unreportedCount message${if (unreportedCount > 1) "s" else ""} from potential banks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                }
            }

            // Developer Section
            SectionHeader(title = "Developer")

            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
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
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Developer Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Show technical information in chat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDeveloperModeEnabled,
                        onCheckedChange = { settingsViewModel.toggleDeveloperMode(it) }
                    )
                }
            }

            // Support Section
            SectionHeader(title = "Support & Community")

            PennyWiseCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Help & FAQ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFaq() }
                            .padding(vertical = Spacing.sm),
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
                                    Icons.AutoMirrored.Filled.Help,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Help & FAQ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Frequently asked questions",
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

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    // GitHub Issues
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sarim2000/pennywiseai-tracker/issues/new/choose"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = Spacing.sm),
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
                                    Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Report an Issue",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Submit bug reports on GitHub",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                        text = "Choose how many months of SMS history to scan for transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // All Time option first, then period options including 24 months for 2 years coverage
                    val options = listOf(-1) + listOf(1, 2, 3, 6, 12, 24)
                    options.forEach { months ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (months == -1) {
                                        settingsViewModel.updateSmsScanAllTime(true)
                                        showSmsScanDialog = false
                                    } else {
                                        settingsViewModel.updateSmsScanMonths(months)
                                        settingsViewModel.updateSmsScanAllTime(false)
                                        showSmsScanDialog = false
                                    }
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = if (months == -1) smsScanAllTime else smsScanMonths == months && !smsScanAllTime
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (months == -1) {
                                        settingsViewModel.updateSmsScanAllTime(true)
                                        showSmsScanDialog = false
                                    } else {
                                        settingsViewModel.updateSmsScanMonths(months)
                                        settingsViewModel.updateSmsScanAllTime(false)
                                        showSmsScanDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when(months) {
                                    -1 -> "All Time"
                                    1 -> "1 month"
                                    24 -> "2 years"
                                    else -> "$months months"
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
        val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"
        
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

    // Lock Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Lock Timeout") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Choose when to lock the app after it goes to background",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val timeoutOptions = listOf(
                        0 to "Immediately",
                        1 to "1 minute",
                        5 to "5 minutes",
                        15 to "15 minutes"
                    )

                    timeoutOptions.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Done")
                }
            }
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
    PennyWiseCard(
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