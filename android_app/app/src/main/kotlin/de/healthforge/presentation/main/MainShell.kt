package de.healthforge.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import de.healthforge.presentation.essen.EssenScreen
import de.healthforge.presentation.home.HomeScreen
import de.healthforge.presentation.home.IntakeHistoryScreen
import de.healthforge.presentation.log.LogScreen
import de.healthforge.presentation.plan.PlanScreen
import de.healthforge.presentation.profile.ProfileScreen
import de.healthforge.presentation.supplements.SupplementEditScreen

/** Bottom-Navigation tab destinations. REQ-NAV-001. */
private data class TabSpec(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

object MainRoutes {
    const val HOME = "main/home"
    const val PLAN = "main/plan"
    const val ESSEN = "main/essen"
    const val LOG = "main/log"
    const val PROFIL = "main/profil"
    const val INTAKE_HISTORY = "main/intake-history"
    const val SUPPLEMENT_EDIT = "main/supplement-edit"
    const val SUPPLEMENT_EDIT_ARG = "id"
    fun supplementEdit(id: Long): String = "$SUPPLEMENT_EDIT?$SUPPLEMENT_EDIT_ARG=$id"
}

private val TABS = listOf(
    TabSpec(MainRoutes.HOME, "Home", Icons.Filled.Home),
    TabSpec(MainRoutes.PLAN, "Plan", Icons.Filled.CalendarMonth),
    TabSpec(MainRoutes.ESSEN, "Essen", Icons.Filled.Restaurant),
    TabSpec(MainRoutes.LOG, "Log", Icons.Filled.BookmarkBorder),
    TabSpec(MainRoutes.PROFIL, "Profil", Icons.Filled.Person),
)

/**
 * Shell hosting the 5-Tab Bottom-Navigation (REQ-NAV-001..004) plus sub-routes
 * that participate in the same nav graph (Intake-History, REQ-NAV-004).
 * Auth/Onboarding live OUTSIDE this shell.
 */
@Composable
fun MainShell(onRestartOnboarding: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MainRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(MainRoutes.HOME) {
                HomeScreen(
                    onOpenHistory = { navController.navigate(MainRoutes.INTAKE_HISTORY) },
                )
            }
            composable(MainRoutes.PLAN) { PlanScreen() }
            composable(MainRoutes.ESSEN) {
                EssenScreen(
                    onOpenSupplementEdit = { id ->
                        navController.navigate(MainRoutes.supplementEdit(id))
                    },
                )
            }
            composable(MainRoutes.LOG) { LogScreen() }
            composable(MainRoutes.PROFIL) {
                ProfileScreen(onRestartOnboarding = onRestartOnboarding)
            }
            composable(MainRoutes.INTAKE_HISTORY) {
                IntakeHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${MainRoutes.SUPPLEMENT_EDIT}?${MainRoutes.SUPPLEMENT_EDIT_ARG}={${MainRoutes.SUPPLEMENT_EDIT_ARG}}",
                arguments = listOf(
                    navArgument(MainRoutes.SUPPLEMENT_EDIT_ARG) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong(MainRoutes.SUPPLEMENT_EDIT_ARG) ?: 0L
                SupplementEditScreen(
                    supplementId = id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
