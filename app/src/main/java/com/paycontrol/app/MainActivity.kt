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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paycontrol.app.security.AppLockGate
import com.paycontrol.app.ui.navigation.AppDestination
import com.paycontrol.app.ui.screens.accounts.AccountsScreen
import com.paycontrol.app.ui.screens.accounts.AccountsViewModel
import com.paycontrol.app.ui.screens.backup.BackupScreen
import com.paycontrol.app.ui.screens.backup.BackupViewModel
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        setContent {
            PayControlTheme {
                val onboardingVm: OnboardingViewModel = hiltViewModel()
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
                            val lockVm: AppLockViewModel = hiltViewModel()
                            val pinEnabled by lockVm.pinEnabled.collectAsStateWithLifecycle()
                            val sessionUnlocked by lockVm.sessionUnlocked.collectAsStateWithLifecycle()
                            val lifecycleOwner = LocalLifecycleOwner.current

                            DisposableEffect(lifecycleOwner, pinEnabled) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_STOP &&
                                        pinEnabled &&
                                        !AppLockGate.shouldSuppressLock()
                                    ) {
                                        lockVm.lockSession()
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }

                            if (pinEnabled && !sessionUnlocked) {
                                LockScreen(viewModel = lockVm)
                            } else {
                                PayControlAppShell()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayControlAppShell() {
    val app = LocalContext.current.applicationContext as PayControlApp
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in AppDestination.bottomBarItems.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
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
                            label = {
                                Text(
                                    text = destination.navLabel,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
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
                val vm: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = vm,
                    onNavigate = { dest -> navController.navigateSingle(dest.route) }
                )
            }
            composable(AppDestination.Transactions.route) {
                val vm: TransactionsViewModel = hiltViewModel()
                TransactionsScreen(viewModel = vm)
            }
            composable(AppDestination.Suppliers.route) {
                val vm: SuppliersViewModel = hiltViewModel()
                SuppliersScreen(
                    viewModel = vm,
                    contactsRepository = app.contactsRepository
                )
            }
            composable(AppDestination.Clients.route) {
                val vm: ClientsViewModel = hiltViewModel()
                ClientsScreen(
                    viewModel = vm,
                    contactsRepository = app.contactsRepository
                )
            }
            composable(AppDestination.Accounts.route) {
                val vm: AccountsViewModel = hiltViewModel()
                AccountsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Reports.route) {
                val vm: ReportsViewModel = hiltViewModel()
                ReportsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Backup.route) {
                val vm: BackupViewModel = hiltViewModel()
                BackupScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = vm,
                    onNavigate = { dest -> navController.navigate(dest.route) }
                )
            }
            composable(AppDestination.PinLock.route) {
                val vm: PinSettingsViewModel = hiltViewModel()
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
