package com.ml.tomatoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "Home")
    object Analysis : BottomNavItem("analysis", Icons.Default.CameraAlt, "Scan")
    object History : BottomNavItem("history", Icons.Default.History, "History")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
    object Analytics : BottomNavItem("analytics", Icons.Default.Analytics, "Analytics")
}
