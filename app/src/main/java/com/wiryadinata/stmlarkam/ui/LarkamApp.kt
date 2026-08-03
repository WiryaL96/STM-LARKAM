package com.wiryadinata.stmlarkam.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wiryadinata.stmlarkam.ui.home.HomeScreen
import com.wiryadinata.stmlarkam.ui.session.PageAddScreen
import com.wiryadinata.stmlarkam.ui.splash.SplashScreen

/** Navigation routes for the app. */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ADD = "add"
    const val ARG_ANGKATAN = "angkatanId"
}

/**
 * App root: Splash -> Home -> Page_add. The selected angkatan filter on Home is
 * forwarded to the add screen as an optional argument, and finishing a session
 * pops back to Home (which refreshes from Firestore automatically).
 */
@Composable
fun LarkamApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onAddClick = { angkatanId ->
                    navController.navigate("${Routes.ADD}?${Routes.ARG_ANGKATAN}=${angkatanId ?: ""}")
                }
            )
        }

        composable(
            route = "${Routes.ADD}?${Routes.ARG_ANGKATAN}={${Routes.ARG_ANGKATAN}}",
            arguments = listOf(
                navArgument(Routes.ARG_ANGKATAN) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val angkatanId = backStackEntry.arguments
                ?.getString(Routes.ARG_ANGKATAN)
                ?.ifBlank { null }
            PageAddScreen(
                initialAngkatanId = angkatanId,
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
