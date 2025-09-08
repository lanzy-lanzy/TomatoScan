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
    @androidx.annotation.StringRes val titleRes: Int
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Rounded.Home, null, R.string.nav_home)
    object Analysis : BottomNavItem("analysis", null, R.drawable.scan_icon, R.string.nav_scan)
    object History : BottomNavItem("history", Icons.Rounded.History, null, R.string.nav_history)
    object Settings : BottomNavItem("settings", Icons.Rounded.Settings, null, R.string.nav_settings)
    object Analytics : BottomNavItem("analytics", Icons.Rounded.Analytics, null, R.string.nav_analytics)
}
