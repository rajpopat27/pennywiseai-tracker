package com.pennywiseai.tracker.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.data.repository.UnrecognizedSmsRepository
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.backup.BackupExporter
import com.pennywiseai.tracker.data.backup.BackupImporter
import com.pennywiseai.tracker.data.backup.ExportResult
import com.pennywiseai.tracker.data.backup.ImportResult
import com.pennywiseai.tracker.data.backup.ImportStrategy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.net.URLEncoder
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    // Import/Export state
    private val _importExportMessage = MutableStateFlow<String?>(null)
    val importExportMessage: StateFlow<String?> = _importExportMessage.asStateFlow()

    private val _exportedBackupFile = MutableStateFlow<File?>(null)
    val exportedBackupFile: StateFlow<File?> = _exportedBackupFile.asStateFlow()

    // Developer mode state
    val isDeveloperModeEnabled = userPreferencesRepository.isDeveloperModeEnabled

    // SMS scan period state
    val smsScanMonths = userPreferencesRepository.smsScanMonths
    val smsScanAllTime = userPreferencesRepository.smsScanAllTime

    // Transaction confirmation state
    val isTransactionConfirmationEnabled = userPreferencesRepository.isTransactionConfirmationEnabled

    // Bypass confirmation for scans state
    val isBypassConfirmationForScans = userPreferencesRepository.isBypassConfirmationForScansEnabled

    // Unrecognized SMS state
    val unreportedSmsCount = unrecognizedSmsRepository.getUnreportedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeveloperModeEnabled(enabled)
        }
    }

    fun toggleTransactionConfirmation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTransactionConfirmationEnabled(enabled)
        }
    }

    fun toggleBypassConfirmationForScans(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBypassConfirmationForScans(enabled)
        }
    }

    fun updateSmsScanMonths(months: Int) {
        viewModelScope.launch {
            val currentMonths = userPreferencesRepository.getSmsScanMonths()

            // If increasing scan period, reset scan timestamp to force full scan
            if (months > currentMonths) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "Scan period increased from $currentMonths to $months months - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanMonths(months)
        }
    }

    fun updateSmsScanAllTime(allTime: Boolean) {
        viewModelScope.launch {
            // If enabling all time scanning, reset scan timestamp to force full scan
            if (allTime) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "All time scanning enabled - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanAllTime(allTime)
        }
    }

    fun openUnrecognizedSmsReport(context: Context) {
        viewModelScope.launch {
            try {
                val firstUnreported = unrecognizedSmsRepository.getFirstUnreported()

                if (firstUnreported != null) {
                    // URL encode the parameters
                    val encodedMessage = URLEncoder.encode(firstUnreported.smsBody, "UTF-8")
                    val encodedSender = URLEncoder.encode(firstUnreported.sender, "UTF-8")

                    // Encrypt device data for verification
                    val encryptedDeviceData = com.pennywiseai.tracker.utils.DeviceEncryption.encryptDeviceData(context)
                    Log.d("SettingsViewModel", "Encrypted device data: ${encryptedDeviceData?.take(50)}... (length: ${encryptedDeviceData?.length})")

                    val encodedDeviceData = if (encryptedDeviceData != null) {
                        URLEncoder.encode(encryptedDeviceData, "UTF-8")
                    } else {
                        ""
                    }
                    Log.d("SettingsViewModel", "Encoded device data: ${encodedDeviceData.take(50)}... (length: ${encodedDeviceData.length})")

                    // Create the report URL using hash fragment for privacy
                    val url = "${Constants.Links.WEB_PARSER_URL}/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
                    Log.d("SettingsViewModel", "Full URL length: ${url.length}")

                    // Open in browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)

                    // Mark as reported
                    unrecognizedSmsRepository.markAsReported(listOf(firstUnreported.id))

                    Log.d("SettingsViewModel", "Opened report for unrecognized SMS from: ${firstUnreported.sender}")
                } else {
                    Log.d("SettingsViewModel", "No unreported SMS messages found")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error opening unrecognized SMS report", e)
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val result = backupExporter.exportBackup()
                when (result) {
                    is ExportResult.Success -> {
                        // Store the file for later saving
                        _exportedBackupFile.value = result.file
                        _importExportMessage.value = "Backup created successfully! Choose where to save it."
                    }
                    is ExportResult.Error -> {
                        _importExportMessage.value = "Export failed: ${result.message}"
                        Log.e("SettingsViewModel", "Export failed: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _importExportMessage.value = "Export error: ${e.message}"
                Log.e("SettingsViewModel", "Export error", e)
            }
        }
    }

    fun saveBackupToFile(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                _exportedBackupFile.value?.let { file ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _importExportMessage.value = "Backup saved successfully!"
                    _exportedBackupFile.value = null
                }
            } catch (e: Exception) {
                _importExportMessage.value = "Failed to save backup: ${e.message}"
                Log.e("SettingsViewModel", "Error saving backup", e)
            }
        }
    }

    fun shareBackup() {
        _exportedBackupFile.value?.let { file ->
            shareBackupFile(file)
        }
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PennyWise Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error sharing backup file", e)
        }
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                _importExportMessage.value = "Importing backup..."
                val result = backupImporter.importBackup(uri, ImportStrategy.MERGE)
                when (result) {
                    is ImportResult.Success -> {
                        _importExportMessage.value = "Import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories. Skipped ${result.skippedDuplicates} duplicates."
                    }
                    is ImportResult.Error -> {
                        _importExportMessage.value = "Import failed: ${result.message}"
                        Log.e("SettingsViewModel", "Import failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _importExportMessage.value = "Import error: ${e.message}"
                Log.e("SettingsViewModel", "Import error", e)
            }
        }
    }

    fun clearImportExportMessage() {
        _importExportMessage.value = null
    }
}
