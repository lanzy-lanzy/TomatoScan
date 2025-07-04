package com.ml.tomatoscan.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: TomatoScanViewModel = viewModel()
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
        val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()



    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Scan Analytics", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isHistoryLoading && scanHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (scanHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No analytics data available.")
                }
            } else {
                AnalyticsSummary(scanHistory = scanHistory)
                Spacer(modifier = Modifier.height(24.dp))
                AnalyticsChart(scanHistory = scanHistory)
            }
        }
    }
}

@Composable
fun AnalyticsSummary(scanHistory: List<ScanResult>) {
    val totalScans = scanHistory.size
    val severityCounts = scanHistory.groupingBy { it.severity }.eachCount()

    val healthyCount = severityCounts["Healthy"] ?: 0
    val mildCount = severityCounts["Mild"] ?: 0
    val moderateCount = severityCounts["Moderate"] ?: 0
    val severeCount = severityCounts["Severe"] ?: 0

    Column {
        Text(
            "Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard("Total Scans", totalScans.toString(), Modifier.weight(1f))
            SummaryCard("Healthy", healthyCount.toString(), Modifier.weight(1f), Color(0xFF4CAF50))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard("Mild", mildCount.toString(), Modifier.weight(1f), Color(0xFF8BC34A))
            SummaryCard("Moderate", moderateCount.toString(), Modifier.weight(1f), Color(0xFFFF9800))
            SummaryCard("Severe", severeCount.toString(), Modifier.weight(1f), Color(0xFFF44336))
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
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
fun AnalyticsChart(scanHistory: List<ScanResult>) {
    val severityCounts = scanHistory.groupingBy { it.severity }.eachCount()
    val labels = listOf("Healthy", "Mild", "Moderate", "Severe")
    val entries = labels.mapIndexed { index, label ->
        BarEntry(index.toFloat(), (severityCounts[label] ?: 0).toFloat())
    }
    
    val chartColors = listOf(
        Color(0xFF4CAF50).toArgb(),
        Color(0xFF8BC34A).toArgb(),
        Color(0xFFFF9800).toArgb(),
        Color(0xFFF44336).toArgb()
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Severity Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (entries.any { it.y > 0 }) {
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawValueAboveBar(true)
                            setFitBars(true)

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                valueFormatter = IndexAxisValueFormatter(labels)
                                textColor = onSurfaceColor
                                granularity = 1f
                                setLabelCount(labels.size, false)
                            }

                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 0f
                                textColor = onSurfaceColor
                            }
                            axisRight.isEnabled = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    update = { chart ->
                        val dataSet = BarDataSet(entries, "Severity")
                        dataSet.colors = chartColors
                        dataSet.valueTextColor = onSurfaceColor
                        dataSet.valueTextSize = 12f
                        
                        val data = BarData(dataSet)
                        data.barWidth = 0.5f
                        
                        chart.data = data
                        chart.invalidate()
                        chart.animateY(1000)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Not enough data to display chart.")
                }
            }
        }
    }
}
