package com.paycontrol.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paycontrol.app.di.AppViewModelFactory
import com.paycontrol.app.ui.navigation.AppDestination
import com.paycontrol.app.ui.screens.accounts.AccountsScreen
import com.paycontrol.app.ui.screens.accounts.AccountsViewModel
import com.paycontrol.app.ui.screens.clients.ClientsScreen
import com.paycontrol.app.ui.screens.clients.ClientsViewModel
import com.paycontrol.app.ui.screens.dashboard.DashboardScreen
import com.paycontrol.app.ui.screens.dashboard.DashboardViewModel
import com.paycontrol.app.ui.screens.lock.AppLockViewModel
import com.paycontrol.app.ui.screens.lock.LockScreen
import com.paycontrol.app.ui.screens.lock.PinSettingsScreen
import com.paycontrol.app.ui.screens.lock.PinSettingsViewModel
import com.paycontrol.app.ui.screens.manual.UserManualScreen
import com.paycontrol.app.ui.screens.onboarding.GuideScreen
import com.paycontrol.app.ui.screens.onboarding.OnboardingViewModel
import com.paycontrol.app.ui.screens.onboarding.WelcomeScreen
import com.paycontrol.app.ui.screens.reports.ReportsScreen
import com.paycontrol.app.ui.screens.reports.ReportsViewModel
import com.paycontrol.app.ui.screens.settings.AboutScreen
import com.paycontrol.app.ui.screens.settings.ChangelogScreen
import com.paycontrol.app.ui.screens.settings.LicensesScreen
import com.paycontrol.app.ui.screens.settings.PrivacyScreen
import com.paycontrol.app.ui.screens.settings.SettingsScreen
import com.paycontrol.app.ui.screens.settings.SettingsViewModel
import com.paycontrol.app.ui.screens.settings.TeamScreen
import com.paycontrol.app.ui.screens.suppliers.SuppliersScreen
import com.paycontrol.app.ui.screens.suppliers.SuppliersViewModel
import com.paycontrol.app.ui.screens.transactions.TransactionsScreen
import com.paycontrol.app.ui.screens.transactions.TransactionsViewModel
import com.paycontrol.app.ui.theme.PayControlTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        val app = application as PayControlApp
        val factory = AppViewModelFactory(app)

        setContent {
            PayControlTheme {
                val onboardingVm: OnboardingViewModel = viewModel(factory = factory)
                val onboardingDone by onboardingVm.onboardingDone.collectAsStateWithLifecycle()
                val guideSeen by onboardingVm.guideSeen.collectAsStateWithLifecycle()
                val displayName by onboardingVm.displayName.collectAsStateWithLifecycle()

                when (onboardingDone) {
                    null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    false -> WelcomeScreen(viewModel = onboardingVm)

                    true -> {
                        if (!guideSeen) {
                            var showManualFromGuide by remember { mutableStateOf(false) }
                            if (showManualFromGuide) {
                                UserManualScreen(onBack = { showManualFromGuide = false })
                            } else {
                                GuideScreen(
                                    displayName = displayName,
                                    onContinue = onboardingVm::markGuideSeen,
                                    onOpenManual = { showManualFromGuide = true }
                                )
                            }
                        } else {
                            val lockVm: AppLockViewModel = viewModel(factory = factory)
                            val pinEnabled by lockVm.pinEnabled.collectAsStateWithLifecycle()
                            val sessionUnlocked by lockVm.sessionUnlocked.collectAsStateWithLifecycle()

                            if (pinEnabled && !sessionUnlocked) {
                                LockScreen(viewModel = lockVm)
                            } else {
                                PayControlAppShell(
                                    app = app,
                                    factory = factory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayControlAppShell(
    app: PayControlApp,
    factory: AppViewModelFactory
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in AppDestination.bottomBarItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppDestination.bottomBarItems.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon(),
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    viewModel = vm,
                    onNavigate = { dest -> navController.navigateSingle(dest.route) }
                )
            }
            composable(AppDestination.Transactions.route) {
                val vm: TransactionsViewModel = viewModel(factory = factory)
                TransactionsScreen(viewModel = vm)
            }
            composable(AppDestination.Suppliers.route) {
                val vm: SuppliersViewModel = viewModel(factory = factory)
                SuppliersScreen(
                    viewModel = vm,
                    contactsRepository = app.contactsRepository
                )
            }
            composable(AppDestination.Clients.route) {
                val vm: ClientsViewModel = viewModel(factory = factory)
                ClientsScreen(
                    viewModel = vm,
                    contactsRepository = app.contactsRepository
                )
            }
            composable(AppDestination.Accounts.route) {
                val vm: AccountsViewModel = viewModel(factory = factory)
                AccountsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Reports.route) {
                val vm: ReportsViewModel = viewModel(factory = factory)
                ReportsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onNavigate = { dest -> navController.navigate(dest.route) }
                )
            }
            composable(AppDestination.PinLock.route) {
                val vm: PinSettingsViewModel = viewModel(factory = factory)
                PinSettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Manual.route) {
                UserManualScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Changelog.route) {
                ChangelogScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Team.route) {
                TeamScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Licenses.route) {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
            composable(AppDestination.Privacy.route) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) { launchSingleTop = true }
}

private fun AppDestination.icon(): ImageVector = when (this) {
    AppDestination.Dashboard -> Icons.Outlined.Home
    AppDestination.Transactions -> Icons.Outlined.SwapHoriz
    AppDestination.Suppliers -> Icons.Outlined.LocalShipping
    AppDestination.Clients -> Icons.Outlined.Groups
    AppDestination.Settings -> Icons.Outlined.Settings
    else -> Icons.Outlined.Home
}
