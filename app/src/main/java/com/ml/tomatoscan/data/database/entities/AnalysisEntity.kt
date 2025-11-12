package com.ml.tomatoscan.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ml.tomatoscan.data.database.converters.DateConverter
import com.ml.tomatoscan.data.database.converters.DiagnosticReportConverter
import com.ml.tomatoscan.data.database.converters.StringListConverter
import com.ml.tomatoscan.models.DiagnosticReport
import java.util.Date

@Entity(tableName = "analysis_results")
@TypeConverters(DateConverter::class, StringListConverter::class, DiagnosticReportConverter::class)
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageData: ByteArray, // Store image as byte array
    val imagePath: String? = null, // Optional: store file path as backup
    val diseaseDetected: String,
    val severity: String,
    val confidence: Float,
    val description: String,
    val recommendations: List<String>,
    val treatmentOptions: List<String>,
    val preventionMeasures: List<String>,
    val timestamp: Date,
    val quality: String, // For legacy compatibility
    val diagnosticReport: DiagnosticReport? = null // Formal diagnostic report from pipeline
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalysisEntity

        if (id != other.id) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (imagePath != other.imagePath) return false
        if (diseaseDetected != other.diseaseDetected) return false
        if (severity != other.severity) return false
        if (confidence != other.confidence) return false
        if (description != other.description) return false
        if (recommendations != other.recommendations) return false
        if (treatmentOptions != other.treatmentOptions) return false
        if (preventionMeasures != other.preventionMeasures) return false
        if (timestamp != other.timestamp) return false
        if (quality != other.quality) return false
        if (diagnosticReport != other.diagnosticReport) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        result = 31 * result + diseaseDetected.hashCode()
        result = 31 * result + severity.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + recommendations.hashCode()
        result = 31 * result + treatmentOptions.hashCode()
        result = 31 * result + preventionMeasures.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + quality.hashCode()
        result = 31 * result + (diagnosticReport?.hashCode() ?: 0)
        return result
    }
}
