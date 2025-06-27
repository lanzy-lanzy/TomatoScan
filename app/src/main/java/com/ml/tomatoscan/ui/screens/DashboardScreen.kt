package com.ml.tomatoscan.ui.screens

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import com.ml.tomatoscan.R
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import com.ml.tomatoscan.viewmodels.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ml.tomatoscan.viewmodels.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel,
    userViewModel: UserViewModel
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val userName by userViewModel.userName.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = { viewModel.refresh() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
            ) {
            DashboardHeader(userName = userName)
            Spacer(modifier = Modifier.height(24.dp))
            // Crossfade to smoothly transition between loading and content
            Crossfade(targetState = isHistoryLoading && scanHistory.isEmpty()) { isLoading ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().padding(top=100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        QuickActionsSection(navController = navController)
                        Spacer(modifier = Modifier.height(24.dp))
                        RecentScansSection(navController = navController, scanHistory = scanHistory, imageLoader = viewModel.imageLoader)
                        Spacer(modifier = Modifier.height(24.dp))
                        StatsSection(scanHistory = scanHistory)
                        Spacer(modifier = Modifier.height(24.dp))
                        ScanHistoryChart(scanHistory = scanHistory)
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = { navController.navigate(BottomNavItem.Analysis.route) },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Scan",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun DashboardHeader(userName: String) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning!"
        in 12..17 -> "Good Afternoon!"
        else -> "Good Evening!"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200)) +
                            slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(durationMillis = 500, delayMillis = 200))
                ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = userName, // Replace with dynamic user name
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 400)) +
                            slideInHorizontally(initialOffsetX = { 200 }, animationSpec = tween(durationMillis = 500, delayMillis = 400))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with user avatar
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    )
                }
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 600))
            ) {
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun StatsSection(scanHistory: List<ScanResult>) {
    val totalScans = scanHistory.size
    val lastScanDate = if (scanHistory.isNotEmpty()) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(scanHistory.maxByOrNull { it.timestamp }!!.timestamp)
    } else {
        "N/A"
    }
    // Simplified quality score for demonstration
    val averageQuality = if (scanHistory.isNotEmpty()) {
        val qualityScore = scanHistory.map {
            when (it.quality) {
                "Excellent" -> 4
                "Good" -> 3
                "Fair" -> 2
                "Poor" -> 1
                else -> 0
            }
        }.sum()
        val avg = qualityScore.toFloat() / totalScans
        "%.1f".format(avg) + "/4.0"
    } else {
        "N/A"
    }

    Column {
        Text(
            text = "Your Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(title = "Total Scans", value = totalScans.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Avg. Quality", value = averageQuality, modifier = Modifier.weight(1f))
            StatCard(title = "Last Scan", value = lastScanDate, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionCard(
                icon = Icons.Default.CameraAlt,
                title = "New Scan",
                onClick = { navController.navigate(BottomNavItem.Analysis.route) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            QuickActionCard(
                icon = Icons.Default.History,
                title = "History",
                onClick = { navController.navigate(BottomNavItem.History.route) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionCard(
                icon = Icons.Default.Assessment,
                title = "Reports",
                onClick = { navController.navigate(BottomNavItem.History.route) }, // Navigate to History as a placeholder
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            QuickActionCard(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = { navController.navigate(BottomNavItem.Settings.route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ScanHistoryChart(scanHistory: List<ScanResult>) {
    val qualityCounts = scanHistory.groupingBy { it.quality }.eachCount()
    val chartData = qualityCounts.map { (quality, count) ->
        PieEntry(count.toFloat(), quality)
    }

    // Use theme colors for the chart
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.inversePrimary
    ).map { it.toArgb() }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Scan Results Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (chartData.isNotEmpty()) {
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            setUsePercentValues(true)
                            description.isEnabled = false
                            isDrawHoleEnabled = true
                            setHoleColor(Color.Transparent.toArgb())
                            setHoleRadius(58f)
                            setTransparentCircleRadius(61f)

                            legend.apply {
                                isEnabled = true
                                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                                orientation = Legend.LegendOrientation.HORIZONTAL
                                setDrawInside(false)
                                textColor = onSurfaceColor
                                textSize = 12f
                                xEntrySpace = 10f
                                yEntrySpace = 5f
                                isWordWrapEnabled = true
                            }

                            setEntryLabelColor(onSurfaceColor)
                            setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                            setEntryLabelTextSize(12f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    update = { chart ->
                        val dataSet = PieDataSet(chartData, "Scan Results")
                        dataSet.sliceSpace = 3f
                        dataSet.colors = chartColors
                        dataSet.valueLinePart1OffsetPercentage = 80f
                        dataSet.valueLinePart1Length = 0.5f
                        dataSet.valueLinePart2Length = 0.6f
                        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

                        val data = PieData(dataSet)
                        data.setValueFormatter(PercentFormatter(chart))
                        data.setValueTextSize(11f)
                        data.setValueTextColor(onSurfaceColor)
                        data.setValueTypeface(Typeface.DEFAULT)

                        chart.data = data
                        chart.animateY(1400)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with a more suitable icon
                            contentDescription = "No data",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No scan history available.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
