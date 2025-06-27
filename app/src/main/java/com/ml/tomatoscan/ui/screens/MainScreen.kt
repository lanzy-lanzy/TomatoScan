package com.ml.tomatoscan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import com.ml.tomatoscan.ui.screens.AnalyticsScreen
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import com.ml.tomatoscan.viewmodels.UserViewModel
import com.ml.tomatoscan.viewmodels.UserViewModelFactory

@Composable
fun MainScreen() {
    val bottomNavController = rememberNavController()
    val viewModel: TomatoScanViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(LocalContext.current.applicationContext as Application))
    Scaffold(
        bottomBar = { BottomBar(navController = bottomNavController, viewModel = viewModel) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            BottomNavGraph(bottomNavController = bottomNavController, viewModel = viewModel, userViewModel = userViewModel)
        }
    }
}

@Composable
fun BottomBar(navController: NavController, viewModel: TomatoScanViewModel) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Analysis,
        BottomNavItem.History,
        BottomNavItem.Analytics,
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
                    if (currentRoute != item.route) {
                        if (currentRoute == BottomNavItem.Analysis.route) {
                            viewModel.clearAnalysisState()
                        }
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun BottomNavGraph(bottomNavController: NavHostController, viewModel: TomatoScanViewModel, userViewModel: UserViewModel) {
    NavHost(navController = bottomNavController, startDestination = BottomNavItem.Dashboard.route) {
        composable(BottomNavItem.Dashboard.route) {
            DashboardScreen(navController = bottomNavController, viewModel = viewModel, userViewModel = userViewModel)
        }
        composable(BottomNavItem.Analysis.route) {
            AnalysisScreen(viewModel = viewModel)
        }
        composable(BottomNavItem.History.route) {
            HistoryScreen(navController = bottomNavController, viewModel = viewModel)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(navController = bottomNavController, userViewModel = userViewModel)
        }
        composable(BottomNavItem.Analytics.route) {
            AnalyticsScreen(viewModel = viewModel)
        }
    }
}


