package com.ml.tomatoscan.models

import java.util.Date

data class ScanResult(
    val imageUrl: String = "",
    val quality: String = "",
    val confidence: Float = 0.0f,
    val timestamp: Date = Date(),
    // New fields for disease analysis
    val diseaseDetected: String = "",
    val severity: String = "",
    val description: String = "",
    val recommendations: List<String> = emptyList(),
    val treatmentOptions: List<String> = emptyList(),
    val preventionMeasures: List<String> = emptyList()
)
