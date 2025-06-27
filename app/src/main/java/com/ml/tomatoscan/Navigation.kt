package com.ml.tomatoscan

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ml.tomatoscan.ui.screens.AnalysisScreen
import com.ml.tomatoscan.ui.screens.AnalyticsScreen
import com.ml.tomatoscan.ui.screens.MainScreen
import com.ml.tomatoscan.ui.screens.HistoryScreen
import com.ml.tomatoscan.ui.screens.SettingsScreen
import com.ml.tomatoscan.ui.screens.SplashScreen
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Navigation() {
    val navController = rememberNavController()
    val viewModel: TomatoScanViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable(
            "dashboard",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            MainScreen()
        }
        composable("analysis") {
            AnalysisScreen(viewModel = viewModel)
        }
        composable("history") {
            HistoryScreen(navController = navController, viewModel = viewModel)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}
