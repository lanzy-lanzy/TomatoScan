package com.ml.tomatoscan.data.database.converters

import androidx.room.TypeConverter
import com.ml.tomatoscan.models.DiagnosticReport
import org.json.JSONObject

/**
 * Type converter for DiagnosticReport to store in Room database.
 * Converts DiagnosticReport objects to JSON strings and vice versa.
 */
class DiagnosticReportConverter {
    
    @TypeConverter
    fun fromDiagnosticReport(report: DiagnosticReport?): String? {
        if (report == null) {
            return null
        }
        
        return try {
            val json = JSONObject()
            json.put("diseaseName", report.diseaseName)
            json.put("observedSymptoms", report.observedSymptoms)
            json.put("confidenceLevel", report.confidenceLevel)
            json.put("managementRecommendation", report.managementRecommendation)
            json.put("fullReport", report.fullReport)
            json.put("isUncertain", report.isUncertain)
            json.put("timestamp", report.timestamp)
            json.put("modelVersion", report.modelVersion)
            json.toString()
        } catch (e: Exception) {
            android.util.Log.e("DiagnosticReportConverter", "Error converting diagnostic report to JSON", e)
            null
        }
    }
    
    @TypeConverter
    fun toDiagnosticReport(json: String?): DiagnosticReport? {
        if (json == null || json.isBlank()) {
            return null
        }
        
        return try {
            val jsonObject = JSONObject(json)
            DiagnosticReport(
                diseaseName = jsonObject.optString("diseaseName", "Unknown"),
                observedSymptoms = jsonObject.optString("observedSymptoms", ""),
                confidenceLevel = jsonObject.optString("confidenceLevel", ""),
                managementRecommendation = jsonObject.optString("managementRecommendation", ""),
                fullReport = jsonObject.optString("fullReport", ""),
                isUncertain = jsonObject.optBoolean("isUncertain", false),
                timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis()),
                modelVersion = jsonObject.optString("modelVersion", "1.0.0")
            )
        } catch (e: Exception) {
            android.util.Log.e("DiagnosticReportConverter", "Error parsing diagnostic report JSON", e)
            null
        }
    }
}
