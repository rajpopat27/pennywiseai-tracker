package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pennywiseai.tracker.presentation.navigation.BottomNavItem
import com.pennywiseai.tracker.presentation.pending.PendingTransactionsViewModel

@Composable
fun PennyWiseBottomNavigation(
    navController: NavController,
    pendingViewModel: PendingTransactionsViewModel = hiltViewModel()
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Pending,
        BottomNavItem.Analytics,
        BottomNavItem.Chat
    )

    val pendingCount by pendingViewModel.pendingCount.collectAsStateWithLifecycle()

    Column {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        if (item == BottomNavItem.Pending && pendingCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(
                                            text = if (pendingCount > 99) "99+" else pendingCount.toString()
                                        )
                                    }
                                }
                            ) {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.title)
                        }
                    },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}