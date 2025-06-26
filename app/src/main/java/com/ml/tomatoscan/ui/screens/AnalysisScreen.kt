package com.ml.tomatoscan.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel = viewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scanResult by viewModel.scanResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCameraPreview by remember { mutableStateOf(false) }

    // Text-to-Speech integration
    val textToSpeech = remember(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    LaunchedEffect(scanResult) {
        android.util.Log.d("AnalysisScreen", "LaunchedEffect triggered - scanResult: $scanResult")
        scanResult?.let {
            android.util.Log.d("AnalysisScreen", "Scan result received: ${it.quality} - ${it.confidence}%")
            val textToSpeak = "Analysis complete. Quality is ${it.quality} with ${it.confidence} percent confidence."
            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    LaunchedEffect(isLoading) {
        android.util.Log.d("AnalysisScreen", "Loading state changed: $isLoading")
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Log.e("AnalysisScreen", "Camera permission denied.")
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.analyzeImage(bitmap, it)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Tomato Analysis",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = !showCameraPreview,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AnalysisContent(
                        imageUri = imageUri,
                        isLoading = isLoading,
                        scanResult = scanResult,
                        onCaptureClick = {
                            val permission = Manifest.permission.CAMERA
                            when {
                                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                                    showCameraPreview = true
                                }
                                else -> cameraPermissionLauncher.launch(permission)
                            }
                        },
                        onUploadClick = { galleryLauncher.launch("image/*") },
                        onAnalyzeAnother = {
                            imageUri = null
                            viewModel.clearScanResult()
                        }
                    )
                }

                if (showCameraPreview) {
                    CameraPreview(
                        onImageCaptured = { uri ->
                            imageUri = uri
                            showCameraPreview = false
                            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            } else {
                                val source = ImageDecoder.createSource(context.contentResolver, uri)
                                ImageDecoder.decodeBitmap(source)
                            }
                            viewModel.analyzeImage(bitmap, uri)
                        },
                        onError = { Log.e("Camera", "View error:", it) },
                        onClose = { showCameraPreview = false }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisContent(
    imageUri: Uri?,
    isLoading: Boolean,
    scanResult: ScanResult?,
    onCaptureClick: () -> Unit,
    onUploadClick: () -> Unit,
    onAnalyzeAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri == null) {
            ActionButtons(onCaptureClick, onUploadClick)
        }

        ImagePreview(imageUri)

        AnalysisStatus(isLoading, scanResult)

        if (scanResult != null) {
            Button(
                onClick = onAnalyzeAnother,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Analyze Another")
                Spacer(Modifier.width(8.dp))
                Text("Analyze Another Tomato")
            }
        }
    }
}

@Composable
fun ActionButtons(onCaptureClick: () -> Unit, onUploadClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButton(
            onClick = onCaptureClick,
            icon = Icons.Default.CameraAlt,
            label = "Capture Image",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            onClick = onUploadClick,
            icon = Icons.Default.Image,
            label = "Upload from Gallery",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionButton(onClick: () -> Unit, icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(40.dp), tint = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ImagePreview(imageUri: Uri?) {
    AnimatedVisibility(visible = imageUri != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun AnalysisStatus(isLoading: Boolean, scanResult: ScanResult?) {
    when {
        isLoading -> {
            AnimatedLoadingIndicator()
        }
        scanResult != null -> {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(800, easing = EaseOutBounce)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                EnhancedResultCard(scanResult)
            }
        }
    }
}

@Composable
fun AnimatedLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                    },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val dots by infiniteTransition.animateValue(
            initialValue = "",
            targetValue = "...",
            typeConverter = TwoWayConverter(
                convertToVector = { AnimationVector1D(it.length.toFloat()) },
                convertFromVector = { ".".repeat(it.value.toInt().coerceIn(0, 3)) }
            ),
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Restart
            ),
            label = "dots"
        )
        
        Text(
            "Analyzing tomato quality$dots",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Using AI to assess ripeness and quality",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EnhancedResultCard(scanResult: ScanResult) {
    val qualityColor = getQualityColor(scanResult.quality)
    val qualityIcon = getQualityIcon(scanResult.quality)
    val qualityLower = scanResult.quality.lowercase(Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            qualityColor.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (qualityLower == "invalid") "Invalid Image" else "Analysis Complete",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    qualityIcon,
                    contentDescription = null,
                    tint = qualityColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            QualityIndicator(
                quality = scanResult.quality,
                color = qualityColor
            )
            if (qualityLower != "invalid") {
                Spacer(modifier = Modifier.height(16.dp))
                ConfidenceIndicator(
                    confidence = scanResult.confidence,
                    progress = scanResult.confidence / 100f
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            QualityDetails(scanResult.quality, scanResult)
        }
    }
}

@Composable
fun QualityIndicator(quality: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Quality Assessment",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    quality.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun ConfidenceIndicator(confidence: Float, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "confidence"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Confidence Level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${String.format("%.1f", confidence)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                confidence >= 80 -> Color(0xFF4CAF50)
                confidence >= 60 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun QualityDetails(quality: String, scanResult: ScanResult? = null) {
    val qualityLower = quality.lowercase(Locale.getDefault())
    val (description, recommendations, storageAdvice) = when (qualityLower) {
        "excellent", "fresh" -> Triple(
            "This tomato is of excellent quality. It appears fresh, firm, and ready for consumption.",
            listOf(
                "Perfect for salads and sandwiches",
                "Can be eaten raw or cooked",
                "Retains flavor well"
            ),
            listOf(
                "Store at room temperature",
                "Keep out of direct sunlight",
                "Consume within 3-5 days"
            )
        )
        "good", "ripe" -> Triple(
            "A good quality tomato, suitable for most uses. It may have minor imperfections but is generally fresh.",
            listOf(
                "Great for sauces and soups",
                "Can be roasted or grilled",
                "Flavor is well-developed"
            ),
            listOf(
                "Store at room temperature",
                "Use within 2-4 days for best taste",
                "Can be refrigerated to extend life"
            )
        )
        "fair" -> Triple(
            "This tomato is of fair quality. It may be slightly overripe or have some blemishes. Best used soon.",
            listOf(
                "Ideal for cooking immediately",
                "Good for stews or purees",
                "Remove any soft spots before use"
            ),
            listOf(
                "Refrigerate to prevent further ripening",
                "Use within 1-2 days",
                "Check for mold before using"
            )
        )
        "poor", "overripe" -> Triple(
            "A poor quality tomato may have significant blemishes, signs of rot, or disease. It is not recommended for consumption.",
            listOf(
                "Inspect carefully before use",
                "Discard if mold or rot is present",
                "Not suitable for fresh consumption"
            ),
            listOf(
                "Discard immediately to avoid contamination",
                "Do not compost if diseased",
                "Clean storage area thoroughly"
            )
        )
        "unripe" -> Triple(
            "This tomato is not yet ripe. It needs more time to develop its full flavor and color.",
            listOf(
                "Can be used for fried green tomatoes",
                "Good for pickling or chutneys",
                "Will not be sweet if eaten raw"
            ),
            listOf(
                "Place in a paper bag to speed up ripening",
                "Keep at room temperature",
                "May take 3-7 days to ripen"
            )
        )
        "invalid" -> Triple(
            scanResult?.description ?: "The uploaded image does not appear to be a tomato leaf. Please upload a clear image of a tomato leaf for analysis.",
            emptyList(),
            emptyList()
        )
        else -> Triple(
            "Analysis could not determine the quality. Please try again with a clearer picture.",
            listOf("Ensure good lighting", "Hold the camera steady"),
            listOf("N/A")
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (scanResult != null && qualityLower != "unripe" && qualityLower != "invalid") {
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            DiseaseAnalysisDetails(scanResult)
        }

        if (recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            DetailSection(
                title = "Usage Recommendations",
                items = recommendations,
                icon = Icons.Default.Lightbulb,
                color = Color(0xFF4CAF50)
            )
        }

        if (storageAdvice.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            DetailSection(
                title = "Storage & Handling",
                items = storageAdvice,
                icon = Icons.Default.Storage,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, RoundedCornerShape(2.dp))
                            .offset(y = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DiseaseAnalysisDetails(scanResult: ScanResult) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Disease Detection Section
        DetailSection(
            title = "Disease Detection",
            items = listOf(
                "Disease: ${scanResult.diseaseDetected}",
                "Severity: ${scanResult.severity}",
                "Confidence: ${String.format("%.1f", scanResult.confidence)}%",
                scanResult.description
            ),
            icon = Icons.Default.BugReport,
            color = getDiseaseColor(scanResult.severity)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Immediate Recommendations Section
        if (scanResult.recommendations.isNotEmpty()) {
            DetailSection(
                title = "Immediate Actions",
                items = scanResult.recommendations,
                icon = Icons.Default.Warning,
                color = Color(0xFFFF9800)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Treatment Options Section
        if (scanResult.treatmentOptions.isNotEmpty()) {
            DetailSection(
                title = "Treatment Options",
                items = scanResult.treatmentOptions,
                icon = Icons.Default.LocalHospital,
                color = Color(0xFF2196F3)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Prevention Measures Section
        if (scanResult.preventionMeasures.isNotEmpty()) {
            DetailSection(
                title = "Prevention Measures",
                items = scanResult.preventionMeasures,
                icon = Icons.Default.Shield,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun getDiseaseColor(severity: String): Color {
    return when (severity.lowercase()) {
        "healthy" -> Color(0xFF4CAF50)
        "mild" -> Color(0xFF8BC34A)
        "moderate" -> Color(0xFFFF9800)
        "severe" -> Color(0xFFF44336)
        else -> Color(0xFF757575)
    }
}

@Composable
fun getQualityColor(quality: String): Color {
    return when (quality.lowercase(Locale.getDefault())) {
        "excellent" -> Color(0xFF4CAF50) // Green
        "good" -> Color(0xFF8BC34A) // Light Green
        "fair" -> Color(0xFFFFC107) // Amber
        "poor" -> Color(0xFFF44336) // Red
        "unripe" -> Color(0xFF9E9E9E) // Grey
        "invalid" -> Color(0xFF607D8B) // Blue Grey
        else -> Color.Gray
    }
}

fun getQualityIcon(quality: String): ImageVector {
    return when (quality.lowercase()) {
        "excellent", "fresh" -> Icons.Default.Stars
        "good", "ripe" -> Icons.Default.CheckCircle
        "fair" -> Icons.Default.Warning
        "poor", "overripe" -> Icons.Default.Error
        "unripe" -> Icons.Default.Schedule
        else -> Icons.Default.Info
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val imageCapture: ImageCapture = remember { 
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build() 
    }
    var isCameraInitialized by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun setupCamera() {
        try {
            Log.d("CameraPreview", "Starting camera setup...")
            coroutineScope.launch {
                try {
                    val cameraProvider = context.getCameraProvider()
                    
                    // Unbind all previous use cases
                    cameraProvider.unbindAll()
                    Log.d("CameraPreview", "Unbound previous use cases")
                    
                    // Create preview use case
                    val preview = Preview.Builder()
                        .build()
                    
                    // Set surface provider
                    previewView?.let { pv ->
                        preview.setSurfaceProvider(pv.surfaceProvider)
                        Log.d("CameraPreview", "Surface provider set")
                    }
                    
                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    isCameraInitialized = true
                    errorMessage = null
                    Log.d("CameraPreview", "Camera setup completed successfully")
                    
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Camera setup failed", exc)
                    errorMessage = exc.message ?: "Unknown camera error"
                    isCameraInitialized = false
                }
            }
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Camera coroutine launch failed", exc)
            errorMessage = "Failed to start camera initialization"
            isCameraInitialized = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = scaleType
                    this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                    Log.d("CameraPreview", "PreviewView created")
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Setup camera when the view is ready
                if (previewView != null && !isCameraInitialized) {
                    Log.d("CameraPreview", "PreviewView ready, setting up camera")
                    setupCamera()
                }
            }
        )

        // Dark overlay for better UI visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
            }
            
            if (showGuide) {
                IconButton(
                    onClick = { showGuide = false },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = "Hide Guide", tint = Color.White)
                }
            }
        }

        // Camera Guide Overlay
        if (showGuide) {
            CameraGuideOverlay(onDismiss = { showGuide = false })
        }

        // Viewfinder Frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .background(Color.Transparent)
        ) {
            // Corner indicators
            val cornerSize = 20.dp
            val strokeWidth = 3.dp
            
            // Top-left corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(cornerSize)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(strokeWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .width(strokeWidth)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
            
            // Top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(cornerSize)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(strokeWidth)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .width(strokeWidth)
                        .fillMaxHeight()
                        .align(Alignment.TopEnd)
                        .background(Color.White)
                )
            }
            
            // Bottom-left corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(cornerSize)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(strokeWidth)
                        .align(Alignment.BottomStart)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .width(strokeWidth)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
            
            // Bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(cornerSize)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(strokeWidth)
                        .align(Alignment.BottomEnd)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .width(strokeWidth)
                        .fillMaxHeight()
                        .align(Alignment.BottomEnd)
                        .background(Color.White)
                )
            }
        }

        // Instructions Text
        Text(
            text = "Position tomato within the frame",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 180.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Capture Button
            Button(
                onClick = {
                    if (isCameraInitialized) {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onImageCaptured = onImageCaptured,
                            onError = onError
                        )
                    }
                },
                modifier = Modifier
                    .size(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = isCameraInitialized
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Take Picture",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    errorMessage != null -> "Camera Error: $errorMessage"
                    isCameraInitialized -> "Tap to capture"
                    else -> "Initializing camera..."
                },
                color = if (errorMessage != null) Color.Red else Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CameraGuideOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "How to Scan Tomatoes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val steps = listOf(
                    "1. Position tomato within the white frame",
                    "2. Ensure good lighting for best results",
                    "3. Hold camera steady and focus on the tomato",
                    "4. Tap the capture button to take photo",
                    "5. AI will analyze quality and ripeness"
                )
                
                steps.forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.externalCacheDir,
        "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                onError(exc)
            }
        }
    )
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProviderFuture ->
        cameraProviderFuture.addListener({
            continuation.resume(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }
}