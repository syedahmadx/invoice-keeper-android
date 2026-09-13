package com.example.invoicekeeper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.invoicekeeper.ui.navigation.Routes
import com.example.invoicekeeper.ui.navigation.TopLevelDestination
import com.example.invoicekeeper.ui.screens.home.HomeScreen
import com.example.invoicekeeper.ui.screens.invoices.InvoiceDetailScreen
import com.example.invoicekeeper.ui.screens.invoices.InvoicesScreen
import com.example.invoicekeeper.ui.screens.scan.ScanScreen
import com.example.invoicekeeper.ui.screens.settings.SettingsScreen

@Composable
fun InvoiceKeeperApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = TopLevelDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination?.hierarchy
                                ?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTopLevel(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onScanClick = { navController.navigateToTopLevel(Routes.SCAN) },
                    onInvoiceClick = { navController.navigate(Routes.invoiceDetail(it)) },
                    onSettingsClick = { navController.navigateToTopLevel(Routes.SETTINGS) },
                    onSeeAllClick = { navController.navigateToTopLevel(Routes.INVOICES) },
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onSettingsClick = { navController.navigateToTopLevel(Routes.SETTINGS) },
                    onOpenSavedInvoice = { navController.navigate(Routes.invoiceDetail(it)) },
                )
            }
            composable(Routes.INVOICES) {
                InvoicesScreen(
                    onInvoiceClick = { navController.navigate(Routes.invoiceDetail(it)) },
                    onScanClick = { navController.navigateToTopLevel(Routes.SCAN) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.INVOICE_DETAIL,
                arguments = listOf(navArgument(Routes.INVOICE_ID_ARG) { type = NavType.LongType }),
            ) {
                InvoiceDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
