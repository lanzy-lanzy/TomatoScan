package com.ml.tomatoscan.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ml.tomatoscan.R

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector? = null,
    @DrawableRes val drawableRes: Int? = null,
    val title: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Rounded.Home, null, "Home")
    object Analysis : BottomNavItem("analysis", null, R.drawable.scan_icon, "Scan")
    object History : BottomNavItem("history", Icons.Rounded.History, null, "History")
    object Settings : BottomNavItem("settings", Icons.Rounded.Settings, null, "Settings")
    object Analytics : BottomNavItem("analytics", Icons.Rounded.Analytics, null, "Analytics")
}
