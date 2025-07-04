package com.ml.tomatoscan.ui.screens.analysis

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel

@Composable
fun AnalysisContent(
    modifier: Modifier = Modifier,
    viewModel: TomatoScanViewModel,
    imageUri: Uri,
    onAnalyzeAnother: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Analyzed Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                ScanResultDetails(viewModel.scanResult.collectAsState().value)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAnalyzeAnother,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Analyze Another Tomato")
        }
    }
}

@Composable
fun ScanResultDetails(scanResult: ScanResult?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Enhanced Disease Header ---
        Text(
            text = scanResult?.diseaseDetected?.takeIf { it.isNotBlank() } ?: "Healthy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFD32F2F), // Tomato Red
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        // --- Accurate Confidence Calculation ---
        val rawConfidence = scanResult?.confidence ?: 0f
        val displayConfidence = when {
            rawConfidence > 1.0f -> rawConfidence.coerceAtMost(100f).toInt() // Already percent, cap at 100
            else -> (rawConfidence * 100).toInt().coerceAtMost(100)
        }

        // --- Confidence Color Coding ---
        val (confidenceColor, confidenceLabel, confidenceIcon) = when {
            displayConfidence >= 80 -> Triple(Color(0xFF388E3C), "High Reliability", "\uD83C\uDF45") // Tomato
            displayConfidence >= 50 -> Triple(Color(0xFFFFA000), "Medium Reliability", "\uD83D\uDD36") // Orange
            else -> Triple(Color(0xFFD32F2F), "Low Reliability", "\u26A0\uFE0F") // Red, Warning
        }

        // --- Confidence Chip ---
        Card(
            colors = CardDefaults.cardColors(containerColor = confidenceColor.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(confidenceIcon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confidence: $displayConfidence%",
                    style = MaterialTheme.typography.titleLarge,
                    color = confidenceColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = confidenceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = confidenceColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- Confidence Warning for Low Reliability ---
        if (displayConfidence < 50) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)), // Light tomato red
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\u26A0\uFE0F", fontSize = MaterialTheme.typography.titleLarge.fontSize, color = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Low confidence: Please retake or upload a clearer image for reliable results.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // --- Description ---
        Text(
            text = scanResult?.description ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Spacer(modifier = Modifier.height(22.dp))
        // --- Tomato Gradient Divider ---
        androidx.compose.material3.Divider(
            color = Color(0xFFD32F2F).copy(alpha = 0.35f),
            thickness = 2.dp,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 32.dp)
        )
        if (!scanResult?.recommendations.isNullOrEmpty()) {
            ResultSectionCard(
                icon = "\uD83D\uDCA1", // Lightbulb
                title = "Recommendations",
                items = scanResult!!.recommendations,
                cardColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!scanResult?.treatmentOptions.isNullOrEmpty()) {
            ResultSectionCard(
                icon = "\uD83D\uDC89", // Syringe
                title = "Treatment Options",
                items = scanResult!!.treatmentOptions,
                cardColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!scanResult?.preventionMeasures.isNullOrEmpty()) {
            ResultSectionCard(
                icon = "\uD83D\uDEE1\uFE0F", // Shield
                title = "Prevention Measures",
                items = scanResult!!.preventionMeasures,
                cardColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun ResultSectionCard(
    icon: String,
    title: String,
    items: List<String>,
    cardColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("\u2022", fontSize = MaterialTheme.typography.bodyLarge.fontSize, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
