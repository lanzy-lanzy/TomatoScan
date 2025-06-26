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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.ml.tomatoscan.utils.DatabaseImageFetcher
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel = viewModel()
) {
    val scanHistory by viewModel.scanHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedScanResult by remember { mutableStateOf<ScanResult?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ScanResult?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadScanHistory()
    }

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
                        "Analysis History",
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
                actions = {
                    if (scanHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("analysis_tab") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Scan")
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
                isLoading -> {
                    LoadingHistoryIndicator()
                }
                scanHistory.isEmpty() -> {
                    EmptyHistoryState(
                        onStartScan = { navController.navigate("analysis_tab") }
                    )
                }
                else -> {
                    HistoryContent(
                        scanHistory = scanHistory,
                        onItemClick = { selectedScanResult = it },
                        onDeleteClick = { showDeleteDialog = it },
                        viewModel = viewModel
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
                "Loading history...",
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
                    "No Analysis History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Start analyzing tomato leaves to see your history here.",
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
                    Text("Start Your First Scan")
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
    viewModel: TomatoScanViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Recent Scans (${scanHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(
            items = scanHistory,
            key = { it.timestamp.time }
        ) { scanResult ->
            HistoryItem(
                scanResult = scanResult,
                onClick = { onItemClick(scanResult) },
                onDeleteClick = { onDeleteClick(scanResult) }
            )
        }
    }
}

@Composable
fun HistoryItem(
    scanResult: ScanResult,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val diseaseColor = getHistoryDiseaseColor(scanResult.severity)
    
    // Create custom ImageLoader with DatabaseImageFetcher
    val customImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(DatabaseImageFetcher.Factory(context))
            }
            .build()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(scanResult.imageUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = customImageLoader,
                contentDescription = "Scanned tomato leaf",
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
                    Text(
                        text = if (scanResult.diseaseDetected.isNotEmpty() && scanResult.diseaseDetected != "Unknown") 
                            scanResult.diseaseDetected else scanResult.quality,
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
                    text = dateFormat.format(scanResult.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (scanResult.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scanResult.description,
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
                        contentDescription = "Delete",
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
                "Analysis Details",
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
                                DetailRow("Disease", scanResult.diseaseDetected)
                                DetailRow("Severity", scanResult.severity)
                            } else {
                                DetailRow("Quality", scanResult.quality)
                            }
                            DetailRow("Confidence", "${String.format("%.1f", scanResult.confidence)}%")
                            DetailRow("Date", dateFormat.format(scanResult.timestamp))
                            
                            if (scanResult.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Description:",
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
                        DetailSection("Recommendations", scanResult.recommendations, Icons.Default.Lightbulb)
                    }
                }
                
                // Treatment Options
                if (scanResult.treatmentOptions.isNotEmpty()) {
                    item {
                        DetailSection("Treatment Options", scanResult.treatmentOptions, Icons.Default.LocalHospital)
                    }
                }
                
                // Prevention Measures
                if (scanResult.preventionMeasures.isNotEmpty()) {
                    item {
                        DetailSection("Prevention Measures", scanResult.preventionMeasures, Icons.Default.Shield)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
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
            Text("Delete Analysis")
        },
        text = {
            Text("Are you sure you want to delete this analysis? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(scanResult) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
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


