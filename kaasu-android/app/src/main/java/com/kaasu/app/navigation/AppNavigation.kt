package com.kaasu.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kaasu.app.ui.theme.KaasuColors
import com.kaasu.app.AppViewModel
import com.kaasu.app.feature.accounts.AccountsScreen
import com.kaasu.app.feature.accounts.AddEditAccountScreen
import com.kaasu.app.feature.budgets.BudgetsScreen
import com.kaasu.app.feature.needstag.NeedsTagScreen
import com.kaasu.app.feature.subscriptions.SubscriptionDetailScreen
import com.kaasu.app.feature.subscriptions.SubscriptionsScreen
import com.kaasu.app.feature.categories.AddEditCategoryScreen
import com.kaasu.app.feature.categories.CategoriesScreen
import com.kaasu.app.feature.dashboard.DashboardScreen
import com.kaasu.app.feature.duplicates.DuplicatesScreen
import com.kaasu.app.feature.onboarding.OnboardingScreen
import com.kaasu.app.feature.privacy.PrivacyDataScreen
import com.kaasu.app.feature.reports.ReportsScreen
import com.kaasu.app.feature.settings.AboutScreen
import com.kaasu.app.feature.settings.AppLockSetupScreen
import com.kaasu.app.feature.profile.ProfileScreen
import com.kaasu.app.feature.settings.BankSourcesScreen
import com.kaasu.app.feature.settings.HelpScreen
import com.kaasu.app.feature.settings.LegalScreen
import com.kaasu.app.feature.settings.MerchantRulesScreen
import com.kaasu.app.feature.settings.SettingsScreen
import com.kaasu.app.feature.settings.SmsSourcesScreen
import com.kaasu.app.feature.statement.ImportStatementScreen
import com.kaasu.app.feature.transactions.AddEditTransactionScreen
import com.kaasu.app.feature.transactions.TransactionDetailScreen
import com.kaasu.app.feature.transactions.TransactionsScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Transactions, "List", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    BottomNavItem(Screen.Reports, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Screen.Settings, "More", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val bottomNavRoutes = bottomNavItems.map { it.screen.route }.toSet()

@Composable
fun AppNavHost(appViewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    val isOnboardingComplete by appViewModel.isOnboardingComplete.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                KaasuBottomBar(
                    currentRoute = currentDestination?.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                LaunchedEffect(isOnboardingComplete) {
                    when (isOnboardingComplete) {
                        true -> navController.navigate(Screen.Dashboard.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                        false -> navController.navigate(Screen.Onboarding.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                        null -> Unit
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KaasuColors.forest)
                }
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onContinue = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    },
                    onSeeAllClick = { navController.navigate(Screen.Transactions.route) },
                    onSearchClick = { navController.navigate(Screen.Transactions.route) }
                )
            }
            composable(Screen.Transactions.route) { backStackEntry ->
                TransactionsScreen(
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    },
                    onAddClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) },
                    // Reached from the dashboard as well as from its own tab. Only offer back when
                    // there is genuinely somewhere to go, so the arrow is never a dead control.
                    onBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else {
                        null
                    }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBankSources = { navController.navigate(Screen.BankSources.route) },
                    onNavigateToSmsSources = { navController.navigate(Screen.SmsSources.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                    onNavigateToNeedsTag = { navController.navigate(Screen.NeedsTag.route) },
                    onNavigateToDuplicates = { navController.navigate(Screen.Duplicates.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.PrivacyData.route) },
                    onNavigateToAppLock = { navController.navigate(Screen.AppLock.route) },
                    onNavigateToMerchantRules = { navController.navigate(Screen.MerchantRules.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToLegal = { doc -> navController.navigate(Screen.Legal.createRoute(doc)) },
                    onNavigateToImportStatement = { uri, mimeType ->
                        navController.navigate(Screen.ImportStatement.createRoute(uri, mimeType))
                    }
                )
            }
            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    onBack = { navController.popBackStack() },
                    onEditBudgets = { navController.navigate(Screen.Categories.route) }
                )
            }
            composable(Screen.BankSources.route) {
                BankSourcesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SmsSources.route) {
                SmsSourcesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    onBack = { navController.popBackStack() },
                    onSubscriptionClick = { merchant -> navController.navigate(Screen.SubscriptionDetail.createRoute(merchant)) }
                )
            }
            composable(
                route = Screen.SubscriptionDetail.route,
                arguments = listOf(navArgument("merchant") { type = NavType.StringType })
            ) {
                SubscriptionDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.NeedsTag.route) {
                NeedsTagScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Duplicates.route) {
                DuplicatesScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.AddEditTransaction.createRoute(id)) }
                )
            }

            composable(
                route = Screen.AddEditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditTransactionScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onAddClick = { navController.navigate(Screen.AddEditCategory.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.AddEditCategory.createRoute(id)) }
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onBack = { navController.popBackStack() },
                    onAddClick = { navController.navigate(Screen.AddEditAccount.createRoute()) },
                    onEditClick = { id -> navController.navigate(Screen.AddEditAccount.createRoute(id)) }
                )
            }

            composable(
                route = Screen.AddEditAccount.route,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditAccountScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PrivacyData.route) {
                PrivacyDataScreen(
                    onBack = { navController.popBackStack() },
                    onExportClick = { navController.navigate(Screen.Reports.route) }
                )
            }

            composable(
                route = Screen.AddEditCategory.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditCategoryScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AppLock.route) {
                AppLockSetupScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Help.route) {
                HelpScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.MerchantRules.route) {
                MerchantRulesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Legal.route,
                arguments = listOf(navArgument("doc") { type = NavType.StringType })
            ) { backStackEntry ->
                LegalScreen(
                    doc = backStackEntry.arguments?.getString("doc") ?: "privacy",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ImportStatement.route,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("mimeType") { type = NavType.StringType }
                )
            ) {
                ImportStatementScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun KaasuBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, KaasuColors.border, RoundedCornerShape(22.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                // Center slot = FAB Add button
                if (index == 2) {
                    AddButton(onClick = onAddClick)
                }

                val selected = currentRoute == item.screen.route
                NavTab(
                    label = item.label,
                    icon = if (selected) item.selectedIcon else item.unselectedIcon,
                    selected = selected,
                    onClick = { onNavigate(item.screen.route) }
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(KaasuColors.forest) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (selected) KaasuColors.onForest else KaasuColors.muted
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) KaasuColors.onForest else KaasuColors.muted
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .background(KaasuColors.surfaceAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add transaction",
            modifier = Modifier.size(18.dp),
            tint = KaasuColors.ink
        )
    }
}
