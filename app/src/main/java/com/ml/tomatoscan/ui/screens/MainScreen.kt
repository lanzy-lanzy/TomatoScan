package com.ml.tomatoscan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel

@Composable
fun MainScreen(mainNavController: NavController) {
    val bottomNavController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigation(navController = bottomNavController) }
    ) {
        Box(modifier = Modifier.padding(it)) {
            BottomNavGraph(mainNavController = mainNavController, bottomNavController = bottomNavController)
        }
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Analysis,
        BottomNavItem.History,
        BottomNavItem.Settings
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let {
                            popUpTo(it) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavGraph(mainNavController: NavController, bottomNavController: NavHostController) {
    val context = LocalContext.current
    val viewModel: TomatoScanViewModel = viewModel()
    
    // Initialize the ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initializeWithContext(context)
    }
    
    NavHost(navController = bottomNavController, startDestination = BottomNavItem.Dashboard.route) {
        composable(BottomNavItem.Dashboard.route) {
            DashboardScreen(navController = mainNavController)
        }
        composable(BottomNavItem.Analysis.route) {
            AnalysisScreen(navController = bottomNavController, viewModel = viewModel)
        }
        composable(BottomNavItem.History.route) {
            HistoryScreen(navController = bottomNavController, viewModel = viewModel)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(navController = bottomNavController)
        }
    }
}


