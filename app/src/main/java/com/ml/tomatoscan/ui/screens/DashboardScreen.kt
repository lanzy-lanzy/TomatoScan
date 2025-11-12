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
                        StatsSection(scanHistory = scanHistory)
                        Spacer(modifier = Modifier.height(24.dp))
                        DiseaseInformationSection()
                        Spacer(modifier = Modifier.height(24.dp))
                        RecentScansSection(navController = navController, scanHistory = scanHistory, imageLoader = viewModel.imageLoader)
                        Spacer(modifier = Modifier.height(24.dp))
                        DiseaseDetectionInsights(scanHistory = scanHistory)
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
        in 0..11 -> "Good Morning!" to Icons.Default.WbSunny
        in 12..17 -> "Good Afternoon!" to Icons.Default.WbSunny
        else -> "Good Evening!" to Icons.Default.Nightlight
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

// Enhanced data class for tomato leaf diseases
data class TomatoDisease(
    val name: String,
    val description: String,
    val symptoms: String,
    val severity: DiseaseSeverity,
    val icon: ImageVector,
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

    val diseases = remember {
        listOf(
            TomatoDisease(
                name = "Early Blight",
                description = "Fungal disease causing dark spots with concentric rings on leaves",
                symptoms = "Brown spots with target-like rings, yellowing leaves",
                severity = DiseaseSeverity.MEDIUM,
                icon = Icons.Default.Warning,
                imageUrl = "https://content.ces.ncsu.edu/media/images/IMG_1302.jpeg",
                detailedSymptoms = listOf(
                    "Dark brown spots with concentric rings (target-like appearance)",
                    "Yellowing of leaves starting from bottom of plant",
                    "Spots may have yellow halos around them",
                    "Leaves eventually turn brown and drop off",
                    "Stems and fruit can also be affected"
                ),
                causes = listOf(
                    "Alternaria solani fungus",
                    "High humidity and warm temperatures",
                    "Poor air circulation",
                    "Overhead watering",
                    "Plant stress from drought or nutrient deficiency"
                ),
                prevention = listOf(
                    "Ensure good air circulation around plants",
                    "Water at soil level, avoid wetting leaves",
                    "Apply mulch to prevent soil splash",
                    "Rotate crops annually",
                    "Remove infected plant debris"
                ),
                treatment = listOf(
                    "Apply copper-based fungicides",
                    "Use organic neem oil treatments",
                    "Remove affected leaves immediately",
                    "Improve air circulation",
                    "Reduce watering frequency"
                ),
                progressionStages = listOf(
                    "Small dark spots appear on lower leaves",
                    "Spots develop concentric rings",
                    "Yellowing spreads around spots",
                    "Leaves turn brown and drop",
                    "Disease moves up the plant"
                ),
                optimalConditions = "Maintain humidity below 85%, ensure good air circulation, and water at soil level to prevent this disease."
            ),
            TomatoDisease(
                name = "Late Blight",
                description = "Serious fungal disease that can destroy entire crops rapidly",
                symptoms = "Water-soaked spots, white fuzzy growth, rapid leaf death",
                severity = DiseaseSeverity.CRITICAL,
                icon = Icons.Default.Dangerous,
                imageUrl = "https://content.ces.ncsu.edu/media/images/IMG_0600.jpeg",
                detailedSymptoms = listOf(
                    "Water-soaked lesions on leaves and stems",
                    "White fuzzy growth on undersides of leaves",
                    "Rapid browning and death of affected tissue",
                    "Dark brown to black lesions on fruit",
                    "Entire plants can die within days"
                ),
                causes = listOf(
                    "Phytophthora infestans pathogen",
                    "Cool, wet weather conditions",
                    "High humidity (above 90%)",
                    "Temperature range of 60-70°F",
                    "Poor air circulation"
                ),
                prevention = listOf(
                    "Choose resistant varieties",
                    "Ensure excellent drainage",
                    "Provide good air circulation",
                    "Avoid overhead watering",
                    "Apply preventive fungicides in wet weather"
                ),
                treatment = listOf(
                    "Remove infected plants immediately",
                    "Apply copper-based fungicides",
                    "Improve air circulation",
                    "Reduce humidity around plants",
                    "Consider destroying severely affected plants"
                ),
                progressionStages = listOf(
                    "Water-soaked spots appear on leaves",
                    "White fuzzy growth develops",
                    "Lesions turn brown and expand rapidly",
                    "Stems and fruit become infected",
                    "Plant death can occur within 1-2 weeks"
                ),
                optimalConditions = "Keep humidity below 90%, ensure excellent drainage, and provide good air circulation to prevent this devastating disease."
            ),
            TomatoDisease(
                name = "Bacterial Spot",
                description = "Bacterial infection causing small dark spots on leaves and fruit",
                symptoms = "Small dark spots with yellow halos, leaf drop",
                severity = DiseaseSeverity.MEDIUM,
                icon = Icons.Default.Circle,
                imageUrl = "https://content.ces.ncsu.edu/media/images/K_Johnson_7082.JPG",
                detailedSymptoms = listOf(
                    "Small, dark brown to black spots on leaves",
                    "Yellow halos around spots",
                    "Spots may have raised or sunken centers",
                    "Leaf yellowing and premature drop",
                    "Fruit develops small, raised, dark spots"
                ),
                causes = listOf(
                    "Xanthomonas bacteria species",
                    "Warm, humid weather conditions",
                    "Overhead watering and rain splash",
                    "Wounds from insects or pruning",
                    "Contaminated seeds or transplants"
                ),
                prevention = listOf(
                    "Use certified disease-free seeds",
                    "Avoid overhead watering",
                    "Provide good air circulation",
                    "Disinfect tools between plants",
                    "Remove infected plant debris"
                ),
                treatment = listOf(
                    "Apply copper-based bactericides",
                    "Remove infected leaves and fruit",
                    "Improve air circulation",
                    "Avoid working with wet plants",
                    "Use drip irrigation instead of sprinklers"
                ),
                progressionStages = listOf(
                    "Small water-soaked spots appear",
                    "Spots turn dark brown to black",
                    "Yellow halos develop around spots",
                    "Leaves yellow and drop prematurely",
                    "Fruit develops characteristic scab-like spots"
                ),
                optimalConditions = "Maintain good air circulation, avoid overhead watering, and keep humidity levels moderate to prevent bacterial infections."
            ),
            TomatoDisease(
                name = "Mosaic Virus",
                description = "Viral disease causing mottled yellow and green patterns",
                symptoms = "Mottled coloring, stunted growth, distorted leaves",
                severity = DiseaseSeverity.HIGH,
                icon = Icons.Default.Texture,
                imageUrl = "https://content.ces.ncsu.edu/media/images/1_xsV4R5h.jpeg",
                detailedSymptoms = listOf(
                    "Mottled yellow and green patterns on leaves",
                    "Stunted plant growth",
                    "Distorted, curled, or puckered leaves",
                    "Reduced fruit size and yield",
                    "Mosaic patterns may appear on fruit"
                ),
                causes = listOf(
                    "Tobacco Mosaic Virus (TMV) or related viruses",
                    "Transmission through infected tools",
                    "Handling by smokers or tobacco users",
                    "Infected transplants or seeds",
                    "Mechanical transmission during cultivation"
                ),
                prevention = listOf(
                    "Use virus-free certified seeds and transplants",
                    "Disinfect tools with 10% bleach solution",
                    "Wash hands thoroughly before handling plants",
                    "Avoid smoking near tomato plants",
                    "Remove infected plants immediately"
                ),
                treatment = listOf(
                    "No cure available - remove infected plants",
                    "Disinfect all tools and equipment",
                    "Control aphids and other vectors",
                    "Plant resistant varieties",
                    "Maintain good garden hygiene"
                ),
                progressionStages = listOf(
                    "Light and dark green mottling appears",
                    "Leaf distortion becomes more pronounced",
                    "Plant growth slows significantly",
                    "Fruit development is affected",
                    "Overall plant vigor declines"
                ),
                optimalConditions = "Practice strict hygiene, use certified disease-free plants, and avoid tobacco products near tomatoes to prevent viral infections."
            ),
            TomatoDisease(
                name = "Septoria Leaf Spot",
                description = "Fungal disease with small circular spots and dark centers",
                symptoms = "Small circular spots with dark centers, yellowing leaves",
                severity = DiseaseSeverity.MEDIUM,
                icon = Icons.Default.FiberManualRecord,
                imageUrl = "https://content.ces.ncsu.edu/media/images/IMG_0675_NLNaTrA.jpeg",
                detailedSymptoms = listOf(
                    "Small, circular spots with dark centers",
                    "Gray to brown spots with dark borders",
                    "Tiny black specks (fruiting bodies) in spot centers",
                    "Yellowing of leaves around spots",
                    "Lower leaves affected first, progressing upward"
                ),
                causes = listOf(
                    "Septoria lycopersici fungus",
                    "Warm, humid weather conditions",
                    "Overhead watering and rain splash",
                    "Poor air circulation",
                    "Infected plant debris in soil"
                ),
                prevention = listOf(
                    "Ensure good air circulation",
                    "Water at soil level",
                    "Apply mulch to prevent soil splash",
                    "Remove lower leaves touching ground",
                    "Clean up plant debris in fall"
                ),
                treatment = listOf(
                    "Apply fungicides containing chlorothalonil",
                    "Remove affected leaves immediately",
                    "Improve air circulation around plants",
                    "Reduce watering frequency",
                    "Apply organic copper sprays"
                ),
                progressionStages = listOf(
                    "Small spots appear on lower leaves",
                    "Spots develop dark centers with gray borders",
                    "Black fruiting bodies form in centers",
                    "Leaves yellow and drop from bottom up",
                    "Disease progresses up the plant"
                ),
                optimalConditions = "Maintain good air circulation, avoid overhead watering, and remove lower leaves to prevent this common fungal disease."
            ),

        )
    }

    Column {
        Text(
            text = "Common Tomato Diseases",
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
        DiseaseSeverity.LOW -> "Low Risk"
        DiseaseSeverity.MEDIUM -> "Medium Risk"
        DiseaseSeverity.HIGH -> "High Risk"
        DiseaseSeverity.CRITICAL -> "Critical"
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(disease.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${disease.name} affected leaf",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                    error = painterResource(android.R.drawable.ic_menu_report_image)
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
                    text = "Symptoms: ${disease.symptoms}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DiseaseDetectionInsights(scanHistory: List<ScanResult>) {
    // Calculate disease detection statistics
    val diseaseCounts = scanHistory.groupingBy { it.diseaseDetected }.eachCount()
    val totalScans = scanHistory.size
    val healthyCount = diseaseCounts["Healthy"] ?: 0
    val diseaseCount = totalScans - healthyCount
    val healthyPercentage = if (totalScans > 0) (healthyCount.toFloat() / totalScans * 100).toInt() else 0
    
    // Get most common disease
    val mostCommonDisease = diseaseCounts.filter { it.key != "Healthy" }.maxByOrNull { it.value }
    
    // Get recent trend (last 5 scans)
    val recentScans = scanHistory.takeLast(5)
    val recentHealthyCount = recentScans.count { it.diseaseDetected == "Healthy" }
    val trendText = when {
        recentScans.isEmpty() -> "No recent data"
        recentHealthyCount >= 4 -> "Excellent health trend"
        recentHealthyCount >= 2 -> "Moderate health trend"
        else -> "Needs attention"
    }

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
                text = "Disease Detection Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (totalScans > 0) {
                // Health Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overall Health Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$healthyCount of $totalScans scans healthy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = when {
                                        healthyPercentage >= 70 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        healthyPercentage >= 40 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$healthyPercentage%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    healthyPercentage >= 70 -> MaterialTheme.colorScheme.primary
                                    healthyPercentage >= 40 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Most Common Disease
                if (mostCommonDisease != null && mostCommonDisease.value > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Most Detected Disease",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${mostCommonDisease.key} (${mostCommonDisease.value} cases)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Recent Trend
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Recent Trend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = trendText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Disease Breakdown
                if (diseaseCounts.size > 1) {
                    Text(
                        text = "Detection Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    diseaseCounts.entries.sortedByDescending { it.value }.forEach { entry ->
                        val disease = entry.key
                        val count = entry.value
                        val percentage = (count.toFloat() / totalScans * 100).toInt()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (disease == "Healthy") 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = disease,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$count ($percentage%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "No data",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No scan data available yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start scanning to see insights!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                    DiseaseSeverity.LOW -> "Low Risk"
                    DiseaseSeverity.MEDIUM -> "Medium Risk"
                    DiseaseSeverity.HIGH -> "High Risk"
                    DiseaseSeverity.CRITICAL -> "Critical"
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
                    title = "Detailed Symptoms",
                    items = disease.detailedSymptoms,
                    icon = Icons.Default.Visibility
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Causes
                DiseaseDetailSection(
                    title = "Causes",
                    items = disease.causes,
                    icon = Icons.Default.Science
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Prevention
                DiseaseDetailSection(
                    title = "Prevention Methods",
                    items = disease.prevention,
                    icon = Icons.Default.Shield
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Treatment
                DiseaseDetailSection(
                    title = "Treatment Options",
                    items = disease.treatment,
                    icon = Icons.Default.LocalHospital
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progression Stages
                DiseaseDetailSection(
                    title = "Disease Progression",
                    items = disease.progressionStages,
                    icon = Icons.Default.Timeline
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
                                text = "Optimal Growing Conditions",
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
    items: List<String>,
    icon: ImageVector
) {
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
