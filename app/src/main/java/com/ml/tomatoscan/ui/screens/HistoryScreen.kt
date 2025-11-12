package com.ml.tomatoscan.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.ui.components.DiagnosticReportCard
import com.ml.tomatoscan.utils.DatabaseImageFetcher
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    var selectedScanResult by rememberSaveable { mutableStateOf<ScanResult?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf<ScanResult?>(null) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }




    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(id = com.ml.tomatoscan.R.string.history_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (scanHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(id = com.ml.tomatoscan.R.string.history_clear_all),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("analysis") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = com.ml.tomatoscan.R.string.history_new_scan))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            when {
                isHistoryLoading && scanHistory.isEmpty() -> {
                    LoadingHistoryIndicator()
                }
                scanHistory.isEmpty() -> {
                    EmptyHistoryState(
                        onStartScan = { navController.navigate("analysis") }
                    )
                }
                else -> {
                    HistoryContent(
                        scanHistory = scanHistory,
                        onItemClick = { selectedScanResult = it },
                        onDeleteClick = { showDeleteDialog = it },
                        imageLoader = viewModel.imageLoader
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedScanResult?.let { scanResult ->
        ScanResultDetailDialog(
            scanResult = scanResult,
            onDismiss = { selectedScanResult = null }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { scanResult ->
        DeleteConfirmationDialog(
            scanResult = scanResult,
            onConfirm = {
                viewModel.deleteFromHistory(it)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        ClearAllConfirmationDialog(
            onConfirm = {
                viewModel.clearHistory()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
fun LoadingHistoryIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(id = com.ml.tomatoscan.R.string.history_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyHistoryState(onStartScan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    stringResource(id = com.ml.tomatoscan.R.string.history_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    stringResource(id = com.ml.tomatoscan.R.string.history_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = com.ml.tomatoscan.R.string.history_start_first_scan))
                }
            }
        }
    }
}

@Composable
fun HistoryContent(
    scanHistory: List<ScanResult>,
    onItemClick: (ScanResult) -> Unit,
    onDeleteClick: (ScanResult) -> Unit,
    imageLoader: ImageLoader
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(id = com.ml.tomatoscan.R.string.history_recent_scans_count, scanHistory.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(
            items = scanHistory,
            key = { it.timestamp }
        ) { scanResult ->
            HistoryItem(
                scanResult = scanResult,
                onClick = { onItemClick(scanResult) },
                onDeleteClick = { onDeleteClick(scanResult) },
                imageLoader = imageLoader
            )
        }
    }
}

@Composable
fun HistoryItem(
    scanResult: ScanResult,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    imageLoader: ImageLoader
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val diseaseColor = getHistoryDiseaseColor(scanResult.severity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar by severity
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(diseaseColor.copy(alpha = 0.85f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(scanResult.imageUrl)
                    .memoryCacheKey("history_${scanResult.timestamp}")
                    .diskCacheKey("history_${scanResult.timestamp}")
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = stringResource(id = com.ml.tomatoscan.R.string.history_image_cd),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                onError = {
                    Log.d("HistoryItem", "Failed to load image: ${scanResult.imageUrl}")
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prioritize diagnostic report disease name if available
                    val displayName = scanResult.diagnosticReport?.diseaseName?.takeIf { it.isNotBlank() }
                        ?: if (scanResult.diseaseDetected.isNotEmpty() && scanResult.diseaseDetected != "Unknown") 
                            scanResult.diseaseDetected else scanResult.quality
                    
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = diseaseColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (scanResult.severity.isNotEmpty() && scanResult.severity != "Unknown") {
                        Spacer(modifier = Modifier.width(8.dp))
                        SeverityChip(severity = scanResult.severity)
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Confidence: ${String.format("%.1f", scanResult.confidence)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = dateFormat.format(Date(scanResult.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show diagnostic report summary if available, otherwise show description
                val summaryText = scanResult.diagnosticReport?.observedSymptoms?.takeIf { it.isNotBlank() }
                    ?: scanResult.description
                
                if (summaryText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = com.ml.tomatoscan.R.string.history_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityChip(severity: String) {
    val color = getHistoryDiseaseColor(severity)
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = severity,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ScanResultDetailDialog(
    scanResult: ScanResult,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                    stringResource(id = com.ml.tomatoscan.R.string.history_details_title),
                    fontWeight = FontWeight.Bold
                )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(scanResult.imageUrl)
                            .memoryCacheKey("history_${scanResult.timestamp}")
                            .diskCacheKey("history_${scanResult.timestamp}")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Scanned tomato leaf",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Show diagnostic report if available
                scanResult.diagnosticReport?.let { report ->
                    item {
                        DiagnosticReportCard(
                            diagnosticReport = report,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                item {
                    // Basic Info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (scanResult.diseaseDetected.isNotEmpty() && scanResult.diseaseDetected != "Unknown") {
                                DetailRow(stringResource(id = com.ml.tomatoscan.R.string.label_disease), scanResult.diseaseDetected)
                                DetailRow(stringResource(id = com.ml.tomatoscan.R.string.label_severity), scanResult.severity)
                            } else {
                                DetailRow(stringResource(id = com.ml.tomatoscan.R.string.label_quality), scanResult.quality)
                            }
                            DetailRow(stringResource(id = com.ml.tomatoscan.R.string.label_confidence), "${String.format("%.1f", scanResult.confidence)}%")
                            DetailRow(stringResource(id = com.ml.tomatoscan.R.string.label_date), dateFormat.format(scanResult.timestamp))
                            
                            if (scanResult.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(id = com.ml.tomatoscan.R.string.label_description),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    scanResult.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Recommendations
                if (scanResult.recommendations.isNotEmpty()) {
                    item {
                        DetailSection(stringResource(id = com.ml.tomatoscan.R.string.label_recommendations), scanResult.recommendations, Icons.Default.Lightbulb)
                    }
                }
                
                // Treatment Options
                if (scanResult.treatmentOptions.isNotEmpty()) {
                    item {
                        DetailSection(stringResource(id = com.ml.tomatoscan.R.string.label_treatment_options), scanResult.treatmentOptions, Icons.Default.LocalHospital)
                    }
                }
                
                // Prevention Measures
                if (scanResult.preventionMeasures.isNotEmpty()) {
                    item {
                        DetailSection(stringResource(id = com.ml.tomatoscan.R.string.label_prevention_measures), scanResult.preventionMeasures, Icons.Default.Shield)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.ml.tomatoscan.R.string.common_close_btn))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetailSection(title: String, items: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        "• ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        item,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    scanResult: ScanResult,
    onConfirm: (ScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = com.ml.tomatoscan.R.string.dialog_delete_title))
        },
        text = {
            Text(stringResource(id = com.ml.tomatoscan.R.string.dialog_delete_message))
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(scanResult) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(id = com.ml.tomatoscan.R.string.history_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClearAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clear All History")
        },
        text = {
            Text("Are you sure you want to clear all analysis history? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getHistoryDiseaseColor(severity: String): Color {
    return when (severity.lowercase()) {
        "healthy" -> Color(0xFF4CAF50)
        "mild" -> Color(0xFF8BC34A)
        "moderate" -> Color(0xFFFF9800)
        "severe" -> Color(0xFFF44336)
        "excellent", "fresh" -> Color(0xFF4CAF50)
        "good", "ripe" -> Color(0xFF8BC34A)
        "fair" -> Color(0xFFFF9800)
        "poor" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }
}


