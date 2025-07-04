package com.ml.tomatoscan.ui.screens.analysis

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onClose: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var showGuide by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = scaleType
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        ViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // Top controls: Close and Help
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White
                )
            }
            IconButton(onClick = { showGuide = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Show Guide",
                    tint = Color.White
                )
            }
        }

        // Bottom controls: Instructions and Capture Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Position tomato within the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            CaptureButton(
                onClick = {
                    takePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        onImageCaptured = onImageCaptured,
                        onError = onError
                    )
                }
            )
        }

        if (showGuide) {
            CameraGuideOverlay(onDismiss = { showGuide = false })
        }
    }
}

@Composable
fun CaptureButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(3.dp, Color.White, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable ripple effect
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "Take Photo",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_transition")
    val scannerPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scanner_position"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val frameSize = size.width * 0.75f
        val frameRect = Rect(
            left = (size.width - frameSize) / 2,
            top = (size.height - frameSize) / 2,
            right = (size.width + frameSize) / 2,
            bottom = (size.height + frameSize) / 2
        )
        val cornerRadius = CornerRadius(16.dp.toPx())
        val cornerLength = 32.dp.toPx()

        // Draw the semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            blendMode = BlendMode.DstOut
        )

        // Draw the clear viewfinder rectangle
        drawRoundRect(
            topLeft = frameRect.topLeft,
            size = frameRect.size,
            cornerRadius = cornerRadius,
            color = Color.Transparent,
            blendMode = BlendMode.Clear
        )

        // Draw the viewfinder corners
        val strokeWidth = 4.dp.toPx()

        // Top-left corner
        drawPath(
            path = Path().apply {
                moveTo(frameRect.left, frameRect.top + cornerLength)
                lineTo(frameRect.left, frameRect.top)
                lineTo(frameRect.left + cornerLength, frameRect.top)
            },
            color = primaryColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Top-right corner
        drawPath(
            path = Path().apply {
                moveTo(frameRect.right - cornerLength, frameRect.top)
                lineTo(frameRect.right, frameRect.top)
                lineTo(frameRect.right, frameRect.top + cornerLength)
            },
            color = primaryColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-left corner
        drawPath(
            path = Path().apply {
                moveTo(frameRect.left, frameRect.bottom - cornerLength)
                lineTo(frameRect.left, frameRect.bottom)
                lineTo(frameRect.left + cornerLength, frameRect.bottom)
            },
            color = primaryColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Bottom-right corner
        drawPath(
            path = Path().apply {
                moveTo(frameRect.right - cornerLength, frameRect.bottom)
                lineTo(frameRect.right, frameRect.bottom)
                lineTo(frameRect.right, frameRect.bottom - cornerLength)
            },
            color = primaryColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw the scanning line
        val scannerY = frameRect.top + (frameRect.height * scannerPosition)
        val scannerBrush = Brush.verticalGradient(
            colors = listOf(primaryColor.copy(alpha = 0f), primaryColor, primaryColor.copy(alpha = 0f))
        )
        drawLine(
            brush = scannerBrush,
            start = Offset(frameRect.left, scannerY),
            end = Offset(frameRect.right, scannerY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CameraGuideOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Camera Guide",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Camera Guide",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "1. Position the tomato in the center of the frame.\n" +
                            "2. Ensure the lighting is bright and even.\n" +
                            "3. Tap the camera button to capture.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
