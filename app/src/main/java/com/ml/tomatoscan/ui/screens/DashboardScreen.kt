package com.ml.tomatoscan.ui.screens

import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.ml.tomatoscan.R
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.ui.navigation.BottomNavItem
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import com.ml.tomatoscan.viewmodels.UserViewModel

import com.ml.tomatoscan.viewmodels.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel,
    userViewModel: UserViewModel
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val userName by userViewModel.userName.collectAsState()
    val userProfilePictureUri by userViewModel.userProfilePictureUri.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.refresh() })

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        userViewModel.updateUserProfilePictureUri(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
            ) {
            DashboardHeader(
                userName = userName,
                profilePictureUri = userProfilePictureUri,
                onProfileClick = { imagePickerLauncher.launch("image/*") }
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Crossfade to smoothly transition between loading and content
            Crossfade(targetState = isHistoryLoading && scanHistory.isEmpty()) { isLoading ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().padding(top=100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DiseaseInformationSection()
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
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

//        FloatingActionButton(
//            onClick = { navController.navigate(BottomNavItem.Analysis.route) },
//            shape = RoundedCornerShape(16.dp),
//            containerColor = MaterialTheme.colorScheme.primary,
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = "New Scan",
//                tint = MaterialTheme.colorScheme.onPrimary
//            )
//        }
    }
}

@Composable
fun DashboardHeader(userName: String, profilePictureUri: String?, onProfileClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    val (greeting, icon) = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> stringResource(R.string.good_morning) to Icons.Default.WbSunny
        in 12..17 -> stringResource(R.string.good_afternoon) to Icons.Default.WbSunny
        else -> stringResource(R.string.good_evening) to Icons.Default.Nightlight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Reduced height
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // --- Lively Animated Tomato Logo ---
                            val infiniteTransition = rememberInfiniteTransition(label = "tomato-logo")
                            val swing by infiniteTransition.animateFloat(
                                initialValue = -10f,
                                targetValue = 10f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = { it * it }),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "swing"
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.tomato_leaf),
                                contentDescription = "App Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer(rotationZ = swing)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // --- Lively Animated Day/Night Icon ---
                            val dayNightTransition = rememberInfiniteTransition(label = "day-night")
                            val dayNightAnim by dayNightTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = if (icon == Icons.Default.WbSunny) 360f else if (icon == Icons.Default.Nightlight) 10f else 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = if (icon == Icons.Default.WbSunny) 3000 else 1200,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Restart
                                ), label = "day-night"
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer(
                                        rotationZ = if (icon == Icons.Default.WbSunny) dayNightAnim else 0f,
                                        translationY = if (icon == Icons.Default.Nightlight) dayNightAnim else 0f
                                    )
                            )
                        }
                        Text(
                            text = userName,
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
                    AsyncImage(
                        model = profilePictureUri ?: R.drawable.ic_launcher_foreground,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            .clickable { onProfileClick() }
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
            text = stringResource(R.string.your_statistics),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(title = stringResource(R.string.total_scans), value = totalScans.toString(), modifier = Modifier.weight(1f))
            StatCard(title = stringResource(R.string.avg_quality), value = averageQuality, modifier = Modifier.weight(1f))
            StatCard(title = stringResource(R.string.last_scan), value = lastScanDate, modifier = Modifier.weight(1f))
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

// Enhanced data class for tomato leaf diseases
data class TomatoDisease(
    val name: String,
    val description: String,
    val symptoms: String,
    val severity: DiseaseSeverity,
    val imageUrl: String,
    val detailedSymptoms: List<String>,
    val causes: List<String>,
    val prevention: List<String>,
    val treatment: List<String>,
    val progressionStages: List<String>,
    val optimalConditions: String
)

enum class DiseaseSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Composable
fun DiseaseInformationSection() {
    var selectedDisease by remember { mutableStateOf<TomatoDisease?>(null) }
    val context = LocalContext.current
    val diseases = remember { loadDiseasesFromAssets(context) }

    Column {
        Text(
            text = stringResource(R.string.common_tomato_diseases),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.height(400.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(diseases) { disease ->
                DiseaseCard(
                    disease = disease,
                    onClick = { selectedDisease = disease }
                )
            }
        }
    }

    // Disease Detail Modal
    selectedDisease?.let { disease ->
        DiseaseDetailDialog(
            disease = disease,
            onDismiss = { selectedDisease = null }
        )
    }
}

private fun loadDiseasesFromAssets(context: android.content.Context): List<TomatoDisease> {
    val jsonString: String
    try {
        jsonString = context.assets.open("diseases.json").bufferedReader().use { it.readText() }
    } catch (ioException: java.io.IOException) {
        ioException.printStackTrace()
        return emptyList()
    }
    val listType = object : com.google.gson.reflect.TypeToken<List<TomatoDisease>>() {}.type
    return com.google.gson.Gson().fromJson(jsonString, listType)
}

@Composable
fun DiseaseCard(
    disease: TomatoDisease,
    onClick: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "card_scale"
    )
    val severityColor = when (disease.severity) {
        DiseaseSeverity.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        DiseaseSeverity.MEDIUM -> MaterialTheme.colorScheme.secondary
        DiseaseSeverity.HIGH -> MaterialTheme.colorScheme.tertiary
        DiseaseSeverity.CRITICAL -> MaterialTheme.colorScheme.error
    }

    val severityText = when (disease.severity) {
        DiseaseSeverity.LOW -> stringResource(R.string.low_risk)
        DiseaseSeverity.MEDIUM -> stringResource(R.string.medium_risk)
        DiseaseSeverity.HIGH -> stringResource(R.string.high_risk)
        DiseaseSeverity.CRITICAL -> stringResource(R.string.critical)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onClick()
                }
            )
            .semantics {
                contentDescription = "Disease information card for ${disease.name}. Tap for detailed information."
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Disease image with severity color background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = severityColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (disease.name) {
                    "Early Blight" -> Icons.Default.Warning
                    "Late Blight" -> Icons.Default.Dangerous
                    "Bacterial Spot" -> Icons.Default.Circle
                    "Mosaic Virus" -> Icons.Default.Texture
                    "Septoria Leaf Spot" -> Icons.Default.FiberManualRecord
                    "Fusarium Wilt" -> Icons.Default.LocalFlorist
                    else -> Icons.Default.Help
                }
                Icon(
                    imageVector = icon,
                    contentDescription = disease.name,
                    modifier = Modifier.size(32.dp),
                    tint = severityColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Disease information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = disease.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Severity badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = severityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = severityText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = disease.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${stringResource(R.string.symptoms)}: ${disease.symptoms}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
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
                text = stringResource(R.string.scan_results_distribution),
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
                            stringResource(R.string.no_scan_history_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiseaseDetailDialog(
    disease: TomatoDisease,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = disease.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Disease image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(disease.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${disease.name} affected leaf",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                    error = painterResource(android.R.drawable.ic_menu_report_image)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Severity badge
                val severityColor = when (disease.severity) {
                    DiseaseSeverity.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    DiseaseSeverity.MEDIUM -> MaterialTheme.colorScheme.secondary
                    DiseaseSeverity.HIGH -> MaterialTheme.colorScheme.tertiary
                    DiseaseSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                }

                val severityText = when (disease.severity) {
                    DiseaseSeverity.LOW -> stringResource(R.string.low_risk)
                    DiseaseSeverity.MEDIUM -> stringResource(R.string.medium_risk)
                    DiseaseSeverity.HIGH -> stringResource(R.string.high_risk)
                    DiseaseSeverity.CRITICAL -> stringResource(R.string.critical)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = severityColor.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = severityText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = disease.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detailed Symptoms
                DiseaseDetailSection(
                    title = stringResource(R.string.detailed_symptoms),
                    items = disease.detailedSymptoms
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Causes
                DiseaseDetailSection(
                    title = stringResource(R.string.causes),
                    items = disease.causes
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Prevention
                DiseaseDetailSection(
                    title = stringResource(R.string.prevention_methods),
                    items = disease.prevention
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Treatment
                DiseaseDetailSection(
                    title = stringResource(R.string.treatment_options),
                    items = disease.treatment
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progression Stages
                DiseaseDetailSection(
                    title = stringResource(R.string.disease_progression),
                    items = disease.progressionStages
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Optimal Conditions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.optimal_growing_conditions),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = disease.optimalConditions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiseaseDetailSection(
    title: String,
    items: List<String>
) {
    val icon = when (title) {
        stringResource(R.string.detailed_symptoms) -> Icons.Default.Visibility
        stringResource(R.string.causes) -> Icons.Default.Science
        stringResource(R.string.prevention_methods) -> Icons.Default.Shield
        stringResource(R.string.treatment_options) -> Icons.Default.LocalHospital
        stringResource(R.string.disease_progression) -> Icons.Default.Timeline
        else -> Icons.Default.Help
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index < items.size - 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
