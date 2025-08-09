package com.ml.tomatoscan.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ml.tomatoscan.models.ScanResult
import java.util.Locale

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
            "Store at room temperature for 1-2 days. Refrigerate if fully ripe to extend freshness."
        )
        "good", "ripe" -> Triple(
            "This tomato is of good quality. It's ripe and suitable for most culinary uses.",
            listOf(
                "Ideal for sauces, soups, and stews",
                "Can be roasted or grilled",
                "Good for canning or preserving"
            ),
            "Use within a few days. Can be refrigerated to slow down further ripening."
        )
        "fair", "overripe" -> Triple(
            "This tomato is fair quality and slightly overripe. It's best used in cooked dishes.",
            listOf(
                "Best for cooked sauces or soups where texture is less critical",
                "Avoid using in fresh salads",
                "Can be used for tomato paste"
            ),
            "Use immediately. Do not store for an extended period."
        )
        else -> Triple(
            "The quality of this tomato is undetermined. Please ensure the image is clear and well-lit.",
            emptyList(),
            "Assess the tomato's condition visually and by touch before deciding on storage."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = getQualityIcon(qualityLower),
            contentDescription = "Quality Icon",
            tint = getQualityColor(qualityLower),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = quality,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = getQualityColor(qualityLower)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (recommendations.isNotEmpty()) {
            DetailSection(
                title = "Recommendations",
                items = recommendations,
                icon = Icons.Default.ThumbUp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (storageAdvice.isNotBlank()) {
            DetailSection(
                title = "Storage Advice",
                items = listOf(storageAdvice),
                icon = Icons.Default.Kitchen,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        scanResult?.let {
            if (it.diseaseDetected != "Healthy" && it.diseaseDetected != "Not Tomato") {
                Spacer(modifier = Modifier.height(24.dp))
                DiseaseAnalysisDetails(scanResult = it)
            }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiseaseAnalysisDetails(scanResult: ScanResult) {
    val severity = scanResult.severity
    val diseaseColor = getDiseaseColor(severity)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Disease Detected: ${scanResult.diseaseDetected}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = diseaseColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Severity: $severity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = diseaseColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Description:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(scanResult.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Recommended Actions:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        scanResult.recommendations.forEach { action ->
            Text("• $action", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun getDiseaseColor(severity: String): Color {
    return when (severity.lowercase(Locale.getDefault())) {
        "low" -> Color(0xFFFFA726) // Orange
        "medium" -> Color(0xFFEF5350) // Red
        "high" -> Color(0xFFB71C1C)  // Dark Red
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun getQualityColor(quality: String): Color {
    return when (quality.lowercase(Locale.getDefault())) {
        "excellent", "fresh" -> Color(0xFF4CAF50) // Green
        "good", "ripe" -> Color(0xFF8BC34A) // Light Green
        "fair", "overripe" -> Color(0xFFFFC107) // Amber
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun getQualityIcon(quality: String): ImageVector {
    return when (quality.lowercase(Locale.getDefault())) {
        "excellent", "fresh" -> Icons.Default.CheckCircle
        "good", "ripe" -> Icons.Default.ThumbUp
        "fair", "overripe" -> Icons.Default.Warning
        else -> Icons.AutoMirrored.Filled.HelpOutline
    }
}
