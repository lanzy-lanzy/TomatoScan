package com.ml.tomatoscan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ml.tomatoscan.models.DiagnosticReport

/**
 * Displays a formal diagnostic report card with professional styling.
 * Shows disease name, observed symptoms, confidence assessment, and management recommendations.
 *
 * @param diagnosticReport The diagnostic report to display
 * @param modifier Optional modifier for the card
 */
@Composable
fun DiagnosticReportCard(
    diagnosticReport: DiagnosticReport,
    modifier: Modifier = Modifier
) {
    val diseaseColor = getDiseaseColor(diagnosticReport.diseaseName, diagnosticReport.isUncertain)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (diagnosticReport.isUncertain) Icons.Default.Warning else Icons.Default.Science,
                    contentDescription = "Diagnostic Report",
                    tint = diseaseColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Diagnostic Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Disease Name (Bold)
            DiagnosticSection(
                title = "Disease Identification",
                icon = if (diagnosticReport.diseaseName.contains("Healthy", ignoreCase = true)) 
                    Icons.Default.CheckCircle else Icons.Default.Warning
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = diseaseColor)) {
                            append(diagnosticReport.diseaseName)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Observed Symptoms
            if (diagnosticReport.observedSymptoms.isNotBlank()) {
                DiagnosticSection(
                    title = "Observed Symptoms",
                    icon = Icons.Default.Science
                ) {
                    Text(
                        text = diagnosticReport.observedSymptoms,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Confidence Assessment
            if (diagnosticReport.confidenceLevel.isNotBlank()) {
                DiagnosticSection(
                    title = "Confidence Assessment",
                    icon = Icons.Default.Science
                ) {
                    Text(
                        text = diagnosticReport.confidenceLevel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Management Recommendation
            if (diagnosticReport.managementRecommendation.isNotBlank()) {
                DiagnosticSection(
                    title = "Management Recommendation",
                    icon = Icons.Default.LocalHospital
                ) {
                    Text(
                        text = diagnosticReport.managementRecommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Full Report Section (if different from components)
            if (diagnosticReport.fullReport.isNotBlank() && 
                diagnosticReport.fullReport != diagnosticReport.diseaseName) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Complete Analysis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = diagnosticReport.fullReport,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f),
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * A section within the diagnostic report with a title and icon
 */
@Composable
private fun DiagnosticSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

/**
 * Determines the color based on disease name and uncertainty
 */
@Composable
private fun getDiseaseColor(diseaseName: String, isUncertain: Boolean): Color {
    return when {
        isUncertain -> Color(0xFFFF9800) // Orange for uncertain
        diseaseName.contains("Healthy", ignoreCase = true) -> Color(0xFF4CAF50) // Green for healthy
        diseaseName.contains("Early Blight", ignoreCase = true) -> Color(0xFFFF5722) // Deep Orange
        diseaseName.contains("Late Blight", ignoreCase = true) -> Color(0xFFD32F2F) // Red
        diseaseName.contains("Leaf Mold", ignoreCase = true) -> Color(0xFF9C27B0) // Purple
        diseaseName.contains("Septoria", ignoreCase = true) -> Color(0xFFE91E63) // Pink
        diseaseName.contains("Bacterial", ignoreCase = true) -> Color(0xFF795548) // Brown
        else -> Color(0xFFD32F2F) // Default red for diseases
    }
}
