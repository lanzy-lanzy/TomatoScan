package com.ml.tomatoscan.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
        object Dashboard : BottomNavItem("dashboard", Icons.Rounded.Home, "Home")
    object Analysis : BottomNavItem("analysis", Icons.Rounded.Camera, "Scan")
    object History : BottomNavItem("history", Icons.Rounded.History, "History")
    object Settings : BottomNavItem("settings", Icons.Rounded.Settings, "Settings")
    object Analytics : BottomNavItem("analytics", Icons.Rounded.Analytics, "Analytics")
}
