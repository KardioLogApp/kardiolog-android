package com.example.addnevnik.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.addnevnik.AppDnevnikApplication
import com.example.addnevnik.data.repository.NotesRepository
import com.example.addnevnik.data.repository.PressureRepository
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.viewmodel.AppViewModelFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.addnevnik.R
import com.example.addnevnik.ui.screens.DisclaimerScreen
import com.example.addnevnik.screens.HomeScreen
import com.example.addnevnik.ui.screens.ChartScreen
import com.example.addnevnik.ui.screens.report.ReportScreen
import com.example.addnevnik.ui.screens.scan.ScanScreen
import com.example.addnevnik.screens.NotesScreen
import com.example.addnevnik.screens.SettingsScreen
import com.example.addnevnik.viewmodel.HomeViewModel
import com.example.addnevnik.viewmodel.NotesViewModel
import com.example.addnevnik.viewmodel.SettingsViewModel

data class BottomDestination(
    val route: String,
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

object AppRoute {
    const val Home = "home"
    const val Notes = "notes"
    const val Settings = "settings"
    const val Chart = "chart"
    const val Scan = "scan"
    const val Report = "report"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val app = context.applicationContext as AppDnevnikApplication
    val dao = app.database.appDao()

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var disclaimerAccepted by remember {
        mutableStateOf(prefs.getBoolean("disclaimer_accepted", false))
    }

    val pressureRepository = remember { PressureRepository.getInstance(dao) }
    val notesRepository = remember { NotesRepository.getInstance(dao) }
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val factory = remember {
        AppViewModelFactory(
            app,
            pressureRepository,
            notesRepository,
            settingsRepository
        )
    }

    val destinations = listOf(
        BottomDestination(AppRoute.Home, R.string.screen_home, Icons.Outlined.Home),
        BottomDestination(AppRoute.Chart, R.string.screen_chart, Icons.AutoMirrored.Outlined.ShowChart),
        BottomDestination(AppRoute.Notes, R.string.screen_notes, Icons.Outlined.NoteAlt),
        BottomDestination(AppRoute.Settings, R.string.screen_settings, Icons.Outlined.Settings)
    )

    Box {
        Scaffold(
            bottomBar = {
                BottomBar(
                    navController = navController,
                    destinations = destinations
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Home,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppRoute.Home) {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    val scanResult = navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.get<String>("scan_result")

                    HomeScreen(
                        viewModel = vm,
                        onShowChart = { navController.navigate(AppRoute.Chart) },
                        onScanRequest = { navController.navigate(AppRoute.Scan) },
                        onShowReport = { navController.navigate(AppRoute.Report) },
                        scanResult = scanResult
                    )
                }
                composable(AppRoute.Chart) {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    val allPressure by vm.allPressure.collectAsStateWithLifecycle()
                    ChartScreen(
                        data = allPressure,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.Notes) {
                    val vm: NotesViewModel = viewModel(factory = factory)
                    NotesScreen(viewModel = vm)
                }
                composable(AppRoute.Settings) {
                    val vm: SettingsViewModel = viewModel(factory = factory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    SettingsScreen(
                        state = state,
                        onNotificationsToggle = vm::updateNotifications,
                        onThemeToggle = vm::updateTheme,
                        onMorningReminderToggle = vm::updateMorningReminder,
                        onEveningReminderToggle = vm::updateEveningReminder,
                        onEditClick = vm::enterEditMode
                    )
                }
                composable(AppRoute.Scan) {
                    ScanScreen(
                        onResult = { result ->
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("scan_result", "${result.systolic},${result.diastolic},${result.pulse}")
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(AppRoute.Report) {
                    ReportScreen(
                        onBack = { navController.popBackStack() },
                        repository = pressureRepository
                    )
                }
            }
        }

        if (!disclaimerAccepted) {
            DisclaimerScreen(
                onAccept = {
                    prefs.edit().putBoolean("disclaimer_accepted", true).apply()
                    disclaimerAccepted = true
                }
            )
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    destinations: List<BottomDestination>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
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
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.titleRes)
                    )
                },
                label = { Text(text = stringResource(destination.titleRes)) }
            )
        }
    }
}
