package com.ml.tomatoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Dashboard : BottomNavItem("dashboard_tab", Icons.Default.Home, "Home")
    object Analysis : BottomNavItem("analysis_tab", Icons.Default.CameraAlt, "Scan")
    object History : BottomNavItem("history_tab", Icons.Default.History, "History")
    object Settings : BottomNavItem("settings_tab", Icons.Default.Settings, "Settings")
}
