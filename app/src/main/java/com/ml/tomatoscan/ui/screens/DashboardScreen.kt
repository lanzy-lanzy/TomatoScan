package com.ml.tomatoscan.ui.screens

import android.graphics.Typeface
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.ml.tomatoscan.R
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel = viewModel()
) {
    val scanHistory by viewModel.scanHistory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadScanHistory()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("analysis_tab") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Scan",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHeader()
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                StatsSection(scanHistory)
                Spacer(modifier = Modifier.height(24.dp))
                QuickActionsSection(navController)
                Spacer(modifier = Modifier.height(24.dp))
                ScanHistoryChart(scanHistory)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DashboardHeader() {
    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning!"
        in 12..17 -> "Good Afternoon!"
        else -> "Good Evening!"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with a real profile picture
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun StatsSection(scanHistory: List<ScanResult>) {
    val totalScans = scanHistory.size
    val healthyScans = scanHistory.count { it.quality.equals("Excellent", ignoreCase = true) || it.quality.equals("Good", ignoreCase = true) }
    val diseasedScans = scanHistory.count { it.quality.equals("Fair", ignoreCase = true) || it.quality.equals("Poor", ignoreCase = true) }

    Column {
        Text(
            text = "Your Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(title = "Total Scans", value = totalScans.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Healthy", value = healthyScans.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Diseased", value = diseasedScans.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                title = "Recent Scans",
                icon = Icons.Default.History,
                onClick = { navController.navigate("recent_scans") },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Analytics",
                icon = Icons.Default.Analytics,
                onClick = { navController.navigate("analytics") },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = { navController.navigate("settings") },
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

    val chartColors = listOf(
        Color(0xFF4CAF50), // Excellent
        Color(0xFF8BC34A), // Good
        Color(0xFFFFC107), // Fair
        Color(0xFFF44336), // Poor
        Color(0xFF9E9E9E), // Invalid
        Color(0xFF795548)  // Unripe
    ).map { it.toArgb() }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
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
                            legend.isEnabled = false
                            setEntryLabelColor(onSurfaceColor)
                            setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                            setEntryLabelTextSize(12f)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
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
                        chart.invalidate()
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No scan history available to display chart.")
                }
            }
        }
    }
}
