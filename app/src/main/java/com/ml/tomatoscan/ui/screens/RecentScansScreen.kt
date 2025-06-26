package com.ml.tomatoscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.ui.navigation.BottomNavItem

@Composable
fun RecentScansSection(
    navController: NavController,
    scanHistory: List<ScanResult>
) {
    if (scanHistory.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Scans",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { navController.navigate(BottomNavItem.History.route) }) {
                    Text("View All")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                scanHistory.take(3).forEach { scanResult ->
                    HistoryItem(
                        scanResult = scanResult,
                        onClick = { /* Handle item click if needed */ },
                        onDeleteClick = { /* Handle delete click if needed */ }
                    )
                }
            }
        }
    }
}
