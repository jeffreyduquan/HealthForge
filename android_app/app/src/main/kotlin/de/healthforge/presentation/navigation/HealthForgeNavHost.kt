package de.healthforge.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.healthforge.presentation.auth.AuthViewModel
import de.healthforge.presentation.auth.LoginScreen
import de.healthforge.presentation.auth.RegisterScreen
import de.healthforge.presentation.auth.ResetPasswordScreen
import de.healthforge.presentation.main.MainShell
import de.healthforge.presentation.onboarding.OnboardingScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RESET_PASSWORD = "reset-password"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
}

/**
 * Root nav graph. Three top-level branches:
 * - Auth (Login/Register/Reset) — no BottomBar
 * - Onboarding — no BottomBar
 * - MAIN — 5-Tab Bottom-Navigation (REQ-NAV-001), hosted by [MainShell].
 */
@Composable
fun HealthForgeNavHost(onboardingCompleted: Boolean) {
    val navController = rememberNavController()
    val rootVm: AuthViewModel = hiltViewModel()
    val start = when {
        !rootVm.isLoggedIn() -> Routes.LOGIN
        !onboardingCompleted -> Routes.ONBOARDING
        else -> Routes.MAIN
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    val target = if (onboardingCompleted) Routes.MAIN else Routes.ONBOARDING
                    navController.navigate(target) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateReset = { navController.navigate(Routes.RESET_PASSWORD) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                onRestartOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
