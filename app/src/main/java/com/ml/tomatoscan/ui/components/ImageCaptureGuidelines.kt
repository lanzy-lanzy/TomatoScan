package com.ml.tomatoscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ImageCaptureGuidelines(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "For Best Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            GuidelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Good Lighting",
                description = "Use natural daylight or bright indoor lighting",
                color = Color(0xFF4CAF50)
            )
            
            GuidelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Clear Focus",
                description = "Ensure the leaf is sharp and in focus",
                color = Color(0xFF4CAF50)
            )
            
            GuidelineItem(
                icon = Icons.Default.CheckCircle,
                title = "Fill the Frame",
                description = "Make the leaf take up most of the image",
                color = Color(0xFF4CAF50)
            )
            
            GuidelineItem(
                icon = Icons.Default.Warning,
                title = "Avoid Shadows",
                description = "Minimize shadows and reflections",
                color = Color(0xFFFF9800)
            )
            
            GuidelineItem(
                icon = Icons.Default.Warning,
                title = "Single Leaf",
                description = "Focus on one leaf at a time for accuracy",
                color = Color(0xFFFF9800)
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Following these guidelines ensures consistent and accurate analysis results.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuidelineItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}