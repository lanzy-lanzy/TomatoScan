package com.ml.tomatoscan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ml.tomatoscan.ui.screens.analysis.ActionButtons
import com.ml.tomatoscan.ui.screens.analysis.AnalysisContent
import com.ml.tomatoscan.ui.screens.analysis.CameraPreview
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.util.Locale

private val UriSaver = Saver<Uri?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it.isNotEmpty()) Uri.parse(it) else null }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: TomatoScanViewModel
) {
    val context = LocalContext.current
    val scanResult by viewModel.scanResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val imageUri by viewModel.analysisImageUri.collectAsState()
    val directCameraMode by viewModel.directCameraMode.collectAsState()
    var showCameraPreview by rememberSaveable { mutableStateOf(false) }
    var imageFromCamera by rememberSaveable { mutableStateOf(false) }

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
        scanResult?.let {
            val textToSpeak = "Analysis complete. Quality is ${it.quality} with ${it.confidence} percent confidence."
            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Log.e("AnalysisScreen", "Camera permission denied.")
        }
    }

    // Auto-trigger camera when in direct camera mode
    LaunchedEffect(directCameraMode) {
        if (directCameraMode) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                    showCameraPreview = true
                    viewModel.setDirectCameraMode(false) // Reset the flag
                }
                else -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    viewModel.setDirectCameraMode(false) // Reset the flag
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setAnalysisImageUri(it)
            imageFromCamera = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tomato Analysis") },
                navigationIcon = {
                    if (scanResult != null || showCameraPreview || imageUri != null) {
                        IconButton(onClick = {
                            when {
                                showCameraPreview -> showCameraPreview = false
                                imageUri != null -> viewModel.setAnalysisImageUri(null)
                                else -> viewModel.clearAnalysisState()
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showCameraPreview) {
                CameraPreview(
                    onImageCaptured = { uri ->
                        showCameraPreview = false
                        viewModel.setAnalysisImageUri(uri)
                        imageFromCamera = true
                    },
                    onError = { Log.e("AnalysisScreen", "Image capture error: $it") },
                    onClose = { showCameraPreview = false }
                )
            } else {
                val currentUri = imageUri
                when {
                    isLoading -> {
                        currentUri?.let { AnalysisInProgressScreen(uri = it) }
                    }
                    scanResult != null -> {
                        currentUri?.let {
                            AnalysisContent(
                                viewModel = viewModel,
                                imageUri = it,
                                onAnalyzeAnother = {
                                    viewModel.clearAnalysisState()
                                }
                            )
                        }

                    }
                    currentUri != null -> {
                        ImagePreview(
                            uri = currentUri,
                            fromCamera = imageFromCamera,
                            onAnalyze = {
                                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, currentUri)
                                } else {
                                    val source = ImageDecoder.createSource(context.contentResolver, currentUri)
                                    ImageDecoder.decodeBitmap(source)
                                }
                                viewModel.analyzeImage(bitmap, currentUri)
                            },
                            onRetake = {
                                viewModel.setAnalysisImageUri(null)
                                if (imageFromCamera) {
                                    showCameraPreview = true
                                } else {
                                    galleryLauncher.launch("image/*")
                                }
                            }
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Analyze Tomato Quality",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Capture an image or upload from your gallery to get an instant analysis of your tomato's quality and ripeness.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            ActionButtons(
                                onCaptureClick = {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) -> {
                                            showCameraPreview = true
                                        }
                                        else -> {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                                onUploadClick = { galleryLauncher.launch("image/*") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreview(
    uri: Uri,
    fromCamera: Boolean,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Image Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onRetake,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = "Retake")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (fromCamera) "Retake" else "Choose Another")
            }
            Button(
                onClick = onAnalyze,
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Science, contentDescription = "Analyze")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze")
            }
        }
    }
}

@Composable
private fun AnalysisInProgressScreen(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = -0.1f, // Start off-screen
        targetValue = 1.1f, // End off-screen
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_position"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Analyzing Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val lineY = canvasHeight * scanPosition

            // Dark overlay
            drawRect(color = Color.Black.copy(alpha = 0.6f))

            // Glowing scanner line
            val scannerBrush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0f),
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor,
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor.copy(alpha = 0f)
                ),
                startY = lineY - 20.dp.toPx(),
                endY = lineY + 20.dp.toPx()
            )
            drawLine(
                brush = scannerBrush,
                start = Offset(0f, lineY),
                end = Offset(canvasWidth, lineY),
                strokeWidth = 40.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.White,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Analyzing...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}