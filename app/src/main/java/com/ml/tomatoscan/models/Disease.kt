package com.ml.tomatoscan.models

data class Disease(
    val name: String,
    val description: String,
    val symptoms: String,
    val severity: String,
    val imageUrl: String,
    val detailedSymptoms: List<String>,
    val causes: List<String>,
    val prevention: List<String>,
    val treatment: List<String>,
    val progressionStages: List<String>,
    val optimalConditions: String
)
