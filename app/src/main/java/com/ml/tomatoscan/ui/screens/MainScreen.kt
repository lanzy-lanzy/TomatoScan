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
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.rotate
import com.ml.tomatoscan.ui.components.BottomBarCutoutShape
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import androidx.compose.ui.graphics.Brush
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
                .height(104.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = BottomBarCutoutShape(cutoutRadius = 40.dp, topCornerRadius = 20.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(BottomBarCutoutShape(cutoutRadius = 40.dp, topCornerRadius = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                ),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary
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
            targetValue = if (isAnalysisSelected) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "fab_scale_animation"
        )

        val fabRotation by animateFloatAsState(
            targetValue = if (isAnalysisSelected) 360f else 0f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            label = "fab_rotation_animation"
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
                .offset(y = (-30).dp)
                .size(76.dp)
                .scale(fabScale)
                .rotate(fabRotation)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            // Main scan icon
            if (BottomNavItem.Analysis.icon != null) {
                Icon(
                    imageVector = BottomNavItem.Analysis.icon,
                    contentDescription = BottomNavItem.Analysis.title,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            } else if (BottomNavItem.Analysis.drawableRes != null) {
                Icon(
                    painter = painterResource(id = BottomNavItem.Analysis.drawableRes),
                    contentDescription = BottomNavItem.Analysis.title,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
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
        targetValue = if (isSelected) 25.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_size_animation"
    )
    // Eliminate upward offset to prevent clipping issues
    val offset by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset_animation"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_alpha_animation"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .offset(y = offset)
            .padding(vertical = 6.dp),
        icon = {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(iconAlpha)
                )
            } else if (item.drawableRes != null) {
                Icon(
                    painter = painterResource(id = item.drawableRes),
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(iconAlpha)
                )
            }
        },
        label = {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.alpha(if (isSelected) 1f else 0.85f)
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
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


