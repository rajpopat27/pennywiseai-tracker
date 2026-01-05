package com.fintrace.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.net.Uri
import com.fintrace.app.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fintrace.app.presentation.home.HomeScreen
import com.fintrace.app.presentation.subscriptions.SubscriptionsScreen
import com.fintrace.app.presentation.transactions.TransactionsScreen
import com.fintrace.app.ui.components.FintraceBottomNavigation
import com.fintrace.app.ui.components.SpotlightTutorial
import com.fintrace.app.ui.screens.settings.SettingsScreen
import com.fintrace.app.ui.viewmodel.ThemeViewModel
import com.fintrace.app.ui.viewmodel.SpotlightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController? = null,
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    spotlightViewModel: SpotlightViewModel = hiltViewModel(),
    homeViewModel: com.fintrace.app.presentation.home.HomeViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val spotlightState by spotlightViewModel.spotlightState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
            val context = LocalContext.current
            FintraceTopAppBar(
                title = when (currentRoute) {
                    "home" -> "Fintrace"
                    "transactions" -> "Transactions"
                    "subscriptions" -> "Subscriptions"
                    "analytics" -> "Analytics"
                    "settings" -> "Settings"
                    "categories" -> "Categories"
                    "unrecognized_sms" -> "Unrecognized Messages"
                    "manage_accounts" -> "Manage Accounts"
                    "add_account" -> "Add Account"
                    "faq" -> "Help & FAQ"
                    "pending" -> "Pending Transactions"
                    else -> "Fintrace"
                },
                showBackButton = currentRoute in listOf("settings", "subscriptions", "transactions", "categories", "unrecognized_sms", "manage_accounts", "add_account", "faq"),
                showSettingsButton = currentRoute !in listOf("settings", "categories", "unrecognized_sms", "manage_accounts", "add_account", "faq", "pending"),
                showSyncButton = currentRoute !in listOf("settings", "categories", "unrecognized_sms", "manage_accounts", "add_account", "faq", "pending"),
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                onSyncClick = { homeViewModel.scanSmsMessages() }
            )
        },
        bottomBar = {
            // Show bottom navigation only for main screens
            if (currentRoute in listOf("home", "pending", "analytics")) {
                FintraceBottomNavigation(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("home") {
                val homeViewModel: com.fintrace.app.presentation.home.HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    navController = rootNavController ?: navController,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToTransactions = {
                        navController.navigate("transactions")
                    },
                    onNavigateToTransactionsWithSearch = {
                        navController.navigate("transactions?focusSearch=true")
                    },
                    onNavigateToSubscriptions = {
                        navController.navigate("subscriptions")
                    },
                    onNavigateToAddScreen = {
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.AddTransaction
                        )
                    },
                    onNavigateToAnalytics = {
                        navController.navigate("analytics")
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.TransactionDetail(transactionId)
                        )
                    },
                    onFabPositioned = { position ->
                        spotlightViewModel.updateFabPosition(position)
                    }
                )
            }

            composable("pending") {
                com.fintrace.app.presentation.pending.PendingTransactionsContent(
                    viewModel = hiltViewModel()
                )
            }
            
            composable(
                route = "transactions?category={category}&merchant={merchant}&period={period}&currency={currency}&focusSearch={focusSearch}",
                arguments = listOf(
                    navArgument("category") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("merchant") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("period") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("currency") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("focusSearch") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                val merchant = backStackEntry.arguments?.getString("merchant")
                val period = backStackEntry.arguments?.getString("period")
                val currency = backStackEntry.arguments?.getString("currency")
                val focusSearch = backStackEntry.arguments?.getBoolean("focusSearch") ?: false
                
                TransactionsScreen(
                    initialCategory = category,
                    initialMerchant = merchant,
                    initialPeriod = period,
                    initialCurrency = currency,
                    focusSearch = focusSearch,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.TransactionDetail(transactionId)
                        )
                    },
                    onAddTransactionClick = {
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.AddTransaction
                        )
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("subscriptions") {
                SubscriptionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAddSubscriptionClick = {
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.AddTransaction
                        )
                    }
                )
            }
            
            composable("analytics") {
                com.fintrace.app.ui.screens.analytics.AnalyticsScreen(
                    onNavigateToTransactions = { category, merchant, period, currency ->
                        val route = buildString {
                            append("transactions")
                            val params = mutableListOf<String>()
                            category?.let {
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("category=$encoded")
                            }
                            merchant?.let {
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("merchant=$encoded")
                            }
                            period?.let {
                                params.add("period=$it")
                            }
                            currency?.let {
                                params.add("currency=$it")
                            }
                            if (params.isNotEmpty()) {
                                append("?")
                                append(params.joinToString("&"))
                            }
                        }
                        navController.navigate(route)
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCategories = {
                        navController.navigate("categories")
                    },
                    onNavigateToUnrecognizedSms = {
                        navController.navigate("unrecognized_sms")
                    },
                    onNavigateToManageAccounts = {
                        navController.navigate("manage_accounts")
                    },
                    onNavigateToFaq = {
                        navController.navigate("faq")
                    },
                    onNavigateToRules = {
                        rootNavController?.navigate(
                            com.fintrace.app.navigation.Rules
                        )
                    }
                )
            }
            
            composable("categories") {
                com.fintrace.app.presentation.categories.CategoriesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("unrecognized_sms") {
                com.fintrace.app.ui.screens.unrecognized.UnrecognizedSmsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("faq") {
                com.fintrace.app.ui.screens.settings.FAQScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("manage_accounts") {
                com.fintrace.app.presentation.accounts.ManageAccountsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddAccount = {
                        navController.navigate("add_account")
                    }
                )
            }
            
            composable("add_account") {
                com.fintrace.app.presentation.accounts.AddAccountScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    
    // Spotlight Tutorial overlay - outside Scaffold to overlay everything
    if (currentRoute == "home" && spotlightState.showTutorial && spotlightState.fabPosition != null) {
        val homeViewModel: com.fintrace.app.presentation.home.HomeViewModel? = 
            navController.currentBackStackEntry?.let { hiltViewModel(it) }
        
        SpotlightTutorial(
            isVisible = true,
            targetPosition = spotlightState.fabPosition,
            message = "Tap here to scan your SMS messages for transactions",
            onDismiss = {
                spotlightViewModel.dismissTutorial()
            },
            onTargetClick = {
                homeViewModel?.scanSmsMessages()
            }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FintraceTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    showSettingsButton: Boolean = true,
    showSyncButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSyncClick: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                if (showSyncButton) {
                    IconButton(onClick = onSyncClick) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync SMS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showSettingsButton) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}