package com.ml.tomatoscan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import com.ml.tomatoscan.ui.components.BottomBarCutoutShape
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import androidx.compose.ui.graphics.Color
import com.ml.tomatoscan.ui.theme.ShadowColor
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

    Box {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = BottomBarCutoutShape(cutoutRadius = 40.dp, topCornerRadius = 20.dp),
                    ambientColor = ShadowColor.copy(alpha = 0.3f),
                    spotColor = ShadowColor.copy(alpha = 0.3f)
                )
                .clip(BottomBarCutoutShape(cutoutRadius = 40.dp, topCornerRadius = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                ),
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEachIndexed { index, item ->
                if (index == 2) {
                    // Add a spacer for the FAB
                    Box(modifier = Modifier.weight(1f)) 
                }

                AppNavigationBarItem(
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
                viewModel.clearAnalysisState()
                navController.navigate(BottomNavItem.Analysis.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
                viewModel.setDirectCameraMode(true)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(72.dp)
                .scale(fabScale)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = ShadowColor.copy(alpha = 0.3f),
                    spotColor = ShadowColor.copy(alpha = 0.3f)
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = BottomNavItem.Analysis.icon,
                contentDescription = BottomNavItem.Analysis.title,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun RowScope.AppNavigationBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 28.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_size_animation"
    )
    val offset by animateDpAsState(
        targetValue = if (isSelected) (-4).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset_animation"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.offset(y = offset),
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(iconSize)
            )
        },
        label = {
            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            unselectedIconColor = Color.White.copy(alpha = 0.7f),
            selectedTextColor = Color.White,
            unselectedTextColor = Color.White.copy(alpha = 0.7f),
            indicatorColor = Color.White.copy(alpha = 0.25f)
        )
    )
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


