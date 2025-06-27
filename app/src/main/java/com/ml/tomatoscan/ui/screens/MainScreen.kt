package com.ml.tomatoscan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.graphics.Color
import com.ml.tomatoscan.ui.theme.LimeGreen
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
        BottomNavItem.History,
        BottomNavItem.Analytics,
        BottomNavItem.Settings
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // Enhanced Navigation Bar with gradient background and rounded corners
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    )
                )
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    ambientColor = Color(0xFFFF6347).copy(alpha = 0.3f),
                    spotColor = Color(0xFFFF6347).copy(alpha = 0.3f)
                )
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First two items
                items.take(2).forEach { item ->
                    EnhancedNavigationBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
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
                        }
                    )
                }

                // Space for floating action button
                Box(modifier = Modifier.size(72.dp))

                // Last two items
                items.drop(2).forEach { item ->
                    EnhancedNavigationBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
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
                        }
                    )
                }
            }
        }

        // Floating Scan Button with animation
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isAnalysisSelected = currentRoute == BottomNavItem.Analysis.route

        val fabScale by animateFloatAsState(
            targetValue = if (isAnalysisSelected) 1.1f else 1f,
            animationSpec = tween(durationMillis = 200),
            label = "fab_scale_animation"
        )

        FloatingActionButton(
            onClick = {
                // Navigate to analysis screen and trigger camera directly
                viewModel.clearAnalysisState()
                navController.navigate(BottomNavItem.Analysis.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
                // Set a flag to trigger camera preview immediately
                viewModel.setDirectCameraMode(true)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(72.dp)
                .scale(fabScale)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFFF6347).copy(alpha = 0.4f),
                    spotColor = Color(0xFFFF6347).copy(alpha = 0.4f)
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = BottomNavItem.Analysis.icon,
                contentDescription = BottomNavItem.Analysis.title,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun EnhancedNavigationBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animation for scale and alpha effects
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale_animation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = tween(durationMillis = 200),
        label = "alpha_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .scale(scale)
            .alpha(alpha)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
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


