package com.ml.tomatoscan.ui.screens.analysis

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Analyzed Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay affected areas if present
                    val sr = viewModel.scanResult.collectAsState().value
                    val strokeColor = MaterialTheme.colorScheme.error
                    val fillColor = strokeColor.copy(alpha = 0.25f)
                    if (!sr?.affectedAreas.isNullOrEmpty()) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 2.dp.toPx()
                            val w = size.width
                            val h = size.height
                            sr!!.affectedAreas.forEach { box ->
                                // Gemini boxes are normalized 0..1000
                                val left = (box.x1 / 1000f) * w
                                val top = (box.y1 / 1000f) * h
                                val right = (box.x2 / 1000f) * w
                                val bottom = (box.y2 / 1000f) * h
                                val safeLeft = left.coerceIn(0f, w)
                                val safeTop = top.coerceIn(0f, h)
                                val safeRight = right.coerceIn(0f, w)
                                val safeBottom = bottom.coerceIn(0f, h)
                                val rectW = (safeRight - safeLeft).coerceAtLeast(0f)
                                val rectH = (safeBottom - safeTop).coerceAtLeast(0f)
                                drawRect(
                                    color = fillColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(safeLeft, safeTop),
                                    size = androidx.compose.ui.geometry.Size(rectW, rectH)
                                )
                                drawRect(
                                    color = strokeColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(safeLeft, safeTop),
                                    size = androidx.compose.ui.geometry.Size(rectW, rectH),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                                )
                            }
                        }
                    }
                }
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
        val isNotTomato = scanResult?.diseaseDetected == "Not Tomato"
        val severityColor = when {
            isNotTomato -> Color(0xFF757575) // Gray for "not tomato"
            (scanResult?.severity ?: "").lowercase() == "healthy" -> Color(0xFF2E7D32)
            (scanResult?.severity ?: "").lowercase() == "mild" -> Color(0xFFFBC02D)
            (scanResult?.severity ?: "").lowercase() == "moderate" -> Color(0xFFF57C00)
            (scanResult?.severity ?: "").lowercase() == "severe" -> Color(0xFFD32F2F)
            else -> MaterialTheme.colorScheme.primary
        }

        val displayText = when {
            isNotTomato -> "Not a Tomato Leaf"
            else -> scanResult?.diseaseDetected?.takeIf { it.isNotBlank() } ?: "Healthy"
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = severityColor,
            textAlign = TextAlign.Center
        )

        // Only show severity for tomato images
        if (!isNotTomato) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- Accurate Confidence Calculation ---
        val rawConfidence = scanResult?.confidence ?: 0f
        val displayConfidence = when {
            rawConfidence > 1.0f -> rawConfidence.coerceAtMost(100f).toInt() // Already percent, cap at 100
            else -> (rawConfidence * 100).toInt().coerceAtMost(100)
        }

        // --- Confidence Color Coding ---
        val (confidenceColor, confidenceLabel, confidenceIcon) = when {
            isNotTomato -> Triple(Color(0xFF388E3C), "Not a Tomato Leaf", "\u274C") // Red X for not tomato
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
                    text = if (isNotTomato) "Confidence: $displayConfidence%" else "Confidence: $displayConfidence%",
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
        HorizontalDivider(
            color = Color(0xFFD32F2F).copy(alpha = 0.35f),
            thickness = 2.dp,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 32.dp)
        )
        // Skip recommendations if not a tomato
        if (scanResult?.diseaseDetected != "Not Tomato") {
            // Grouped recommendations by urgency if available
            val hasGrouped = !scanResult?.recommendationsImmediate.isNullOrEmpty() ||
                    !scanResult?.recommendationsShortTerm.isNullOrEmpty() ||
                    !scanResult?.recommendationsLongTerm.isNullOrEmpty()
            if (hasGrouped) {
                if (!scanResult?.recommendationsImmediate.isNullOrEmpty()) {
                    ResultSectionCard(
                        icon = "\u23F1\uFE0F", // timer
                        title = "Immediate Actions",
                        items = scanResult!!.recommendationsImmediate,
                        cardColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (!scanResult?.recommendationsShortTerm.isNullOrEmpty()) {
                    ResultSectionCard(
                        icon = "\uD83D\uDCCA", // chart
                        title = "Short-term",
                        items = scanResult!!.recommendationsShortTerm,
                        cardColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (!scanResult?.recommendationsLongTerm.isNullOrEmpty()) {
                    ResultSectionCard(
                        icon = "\uD83C\uDF31", // seedling
                        title = "Long-term Prevention",
                        items = scanResult!!.recommendationsLongTerm,
                        cardColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                if (!scanResult?.recommendations.isNullOrEmpty()) {
                    ResultSectionCard(
                        icon = "\uD83D\uDCA1", // Lightbulb
                        title = "Recommendations",
                        items = scanResult!!.recommendations,
                        cardColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Always show treatment options and prevention measures when available
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
