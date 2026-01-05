package com.fintrace.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.fintrace.app.navigation.Home
import com.fintrace.app.navigation.Permission
import com.fintrace.app.navigation.FintraceNavHost
import com.fintrace.app.ui.theme.FintraceTheme
import com.fintrace.app.ui.viewmodel.ThemeViewModel

@Composable
fun FintraceApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    editTransactionId: Long? = null,
    onEditComplete: () -> Unit = {},
    reviewPendingId: Long? = null,
    onPendingReviewComplete: () -> Unit = {}
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    val navController = rememberNavController()

    // Determine initial destination based on permissions
    val startDestination = remember {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) Home else Permission
    }

    // Navigate to transaction detail when editTransactionId changes
    LaunchedEffect(editTransactionId) {
        editTransactionId?.let { transactionId ->
            navController.navigate(com.fintrace.app.navigation.TransactionDetail(transactionId))
        }
    }

    // Navigate to pending transaction review when reviewPendingId changes
    LaunchedEffect(reviewPendingId) {
        reviewPendingId?.let { pendingId ->
            navController.navigate(com.fintrace.app.navigation.PendingTransactionReview(pendingId))
            onPendingReviewComplete()
        }
    }

    FintraceTheme(
        darkTheme = darkTheme,
        dynamicColor = themeUiState.isDynamicColorEnabled
    ) {
        FintraceNavHost(
            navController = navController,
            themeViewModel = themeViewModel,
            startDestination = startDestination,
            onEditComplete = onEditComplete
        )
    }
}
