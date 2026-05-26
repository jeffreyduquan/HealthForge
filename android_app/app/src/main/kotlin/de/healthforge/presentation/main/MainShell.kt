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
import de.healthforge.presentation.essen.rezepte.RecipeDetailScreen
import de.healthforge.presentation.essen.rezepte.RecipeEditScreen
import de.healthforge.presentation.groups.GroupDetailScreen
import de.healthforge.presentation.groups.GroupsScreen
import de.healthforge.presentation.home.HomeScreen
import de.healthforge.presentation.home.IntakeHistoryScreen
import de.healthforge.presentation.log.CustomSymptomManagerScreen
import de.healthforge.presentation.log.LogChartsScreen
import de.healthforge.presentation.shopping.ShoppingListScreen
import de.healthforge.presentation.log.LogEntryFormScreen
import de.healthforge.presentation.log.LogScreen
import de.healthforge.presentation.plan.PlanScreen
import de.healthforge.presentation.insights.InsightsScreen
import de.healthforge.presentation.profile.ExportScreen
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
    const val RECIPE_DETAIL = "main/recipe-detail"
    const val RECIPE_DETAIL_ARG = "id"
    fun recipeDetail(id: String): String = "$RECIPE_DETAIL/$id"
    const val RECIPE_EDIT = "main/recipe-edit"
    const val RECIPE_EDIT_ARG = "id"
    /** id=null → create-mode, sonst edit-mode */
    fun recipeEdit(id: String?): String = if (id.isNullOrBlank()) RECIPE_EDIT else "$RECIPE_EDIT?$RECIPE_EDIT_ARG=$id"
    const val GROUPS = "main/groups"
    const val GROUP_DETAIL = "main/group-detail"
    const val GROUP_DETAIL_ARG = "id"
    fun groupDetail(id: String): String = "$GROUP_DETAIL/$id"
    const val LOG_CHARTS = "main/log-charts"
    const val LOG_FORM = "main/log-form"
    const val LOG_FORM_ARG = "id"
    /** id=0 → create-mode, sonst edit-mode */
    fun logForm(id: Long): String = "$LOG_FORM?$LOG_FORM_ARG=$id"
    const val SYMPTOM_MANAGER = "main/symptom-manager"
    const val SHOPPING_LIST = "main/shopping-list"
    const val EXPORT = "main/export"
    const val INSIGHTS = "main/insights"
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
            composable(MainRoutes.PLAN) {
                PlanScreen(
                    onOpenShoppingList = { navController.navigate(MainRoutes.SHOPPING_LIST) },
                )
            }
            composable(MainRoutes.ESSEN) {
                EssenScreen(
                    onOpenSupplementEdit = { id ->
                        navController.navigate(MainRoutes.supplementEdit(id))
                    },
                    onOpenRecipeDetail = { id ->
                        navController.navigate(MainRoutes.recipeDetail(id))
                    },
                    onCreateRecipe = {
                        navController.navigate(MainRoutes.recipeEdit(null))
                    },
                )
            }
            composable(MainRoutes.LOG) {
                LogScreen(
                    onOpenCharts = { navController.navigate(MainRoutes.LOG_CHARTS) },
                    onOpenEntry = { id -> navController.navigate(MainRoutes.logForm(id)) },
                )
            }
            composable(MainRoutes.PROFIL) {
                ProfileScreen(
                    onRestartOnboarding = onRestartOnboarding,
                    onOpenGroups = { navController.navigate(MainRoutes.GROUPS) },
                    onOpenSymptomManager = { navController.navigate(MainRoutes.SYMPTOM_MANAGER) },
                    onOpenExport = { navController.navigate(MainRoutes.EXPORT) },
                    onOpenInsights = { navController.navigate(MainRoutes.INSIGHTS) },
                )
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
            composable(
                route = "${MainRoutes.RECIPE_DETAIL}/{${MainRoutes.RECIPE_DETAIL_ARG}}",
                arguments = listOf(
                    navArgument(MainRoutes.RECIPE_DETAIL_ARG) { type = NavType.StringType },
                ),
            ) {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(MainRoutes.recipeEdit(id)) },
                )
            }
            composable(
                route = "${MainRoutes.RECIPE_EDIT}?${MainRoutes.RECIPE_EDIT_ARG}={${MainRoutes.RECIPE_EDIT_ARG}}",
                arguments = listOf(
                    navArgument(MainRoutes.RECIPE_EDIT_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                RecipeEditScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(MainRoutes.recipeDetail(id))
                    },
                )
            }
            composable(MainRoutes.GROUPS) {
                GroupsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { id -> navController.navigate(MainRoutes.groupDetail(id)) },
                )
            }
            composable(
                route = "${MainRoutes.GROUP_DETAIL}/{${MainRoutes.GROUP_DETAIL_ARG}}",
                arguments = listOf(
                    navArgument(MainRoutes.GROUP_DETAIL_ARG) { type = NavType.StringType },
                ),
            ) {
                GroupDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(MainRoutes.LOG_CHARTS) {
                LogChartsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${MainRoutes.LOG_FORM}?${MainRoutes.LOG_FORM_ARG}={${MainRoutes.LOG_FORM_ARG}}",
                arguments = listOf(
                    navArgument(MainRoutes.LOG_FORM_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                LogEntryFormScreen(onBack = { navController.popBackStack() })
            }
            composable(MainRoutes.SYMPTOM_MANAGER) {
                CustomSymptomManagerScreen(onBack = { navController.popBackStack() })
            }
            composable(MainRoutes.SHOPPING_LIST) {
                ShoppingListScreen(onBack = { navController.popBackStack() })
            }
            composable(MainRoutes.EXPORT) {
                ExportScreen(onBack = { navController.popBackStack() })
            }
            composable(MainRoutes.INSIGHTS) {
                InsightsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
