package com.ml.tomatoscan.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Formal diagnostic report data class for tomato leaf disease analysis.
 * Contains structured information about disease identification, symptoms,
 * confidence assessment, and management recommendations.
 *
 * @property diseaseName The identified disease name (e.g., "Early Blight", "Healthy")
 * @property observedSymptoms Description of visual symptoms observed in the leaf
 * @property confidenceLevel Assessment of confidence in the diagnosis (e.g., "High confidence", "Moderate confidence")
 * @property managementRecommendation Suggested actions for disease management or prevention
 * @property fullReport Complete formal diagnostic paragraph combining all components
 * @property isUncertain Flag indicating if the diagnosis is uncertain due to poor image quality or low confidence
 * @property timestamp Unix timestamp when the report was generated
 * @property modelVersion Version identifier of the AI model used for analysis
 */
@Parcelize
data class DiagnosticReport(
    val diseaseName: String,
    val observedSymptoms: String,
    val confidenceLevel: String,
    val managementRecommendation: String,
    val fullReport: String,
    val isUncertain: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val modelVersion: String = "1.0.0"
) : Parcelable
