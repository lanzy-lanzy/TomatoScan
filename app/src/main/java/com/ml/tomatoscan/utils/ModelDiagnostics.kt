package com.ml.tomatoscan.utils

import android.content.Context
import android.util.Log
import com.ml.tomatoscan.config.ModelConfig
import java.io.File

/**
 * Diagnostic utilities for troubleshooting model issues.
 * 
 * Provides tools to:
 * - Verify model file exists and is valid
 * - Check model size and format
 * - Validate configuration
 * - Generate diagnostic reports
 */
object ModelDiagnostics {
    private const val TAG = "ModelDiagnostics"
    
    /**
     * Runs a comprehensive diagnostic check on the model setup
     */
    fun runDiagnostics(context: Context): DiagnosticReport {
        Log.d(TAG, "Running model diagnostics...")
        
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val info = mutableListOf<String>()
        
        // Check 1: Model file exists
        val modelExists = checkModelFileExists(context)
        if (!modelExists) {
            issues.add("Model file not found: ${ModelConfig.YOLO_MODEL_PATH}")
        } else {
            info.add("✓ Model file found: ${ModelConfig.YOLO_MODEL_PATH}")
        }
        
        // Check 2: Model file size
        val modelSize = getModelFileSize(context)
        if (modelSize > 0) {
            val sizeMB = modelSize / (1024.0 * 1024.0)
            info.add("✓ Model size: ${String.format("%.2f", sizeMB)} MB")
            
            // Warn if size seems unusual
            if (sizeMB < 1.0) {
                warnings.add("Model size is very small (${String.format("%.2f", sizeMB)} MB). Expected 5-20 MB.")
            } else if (sizeMB > 50.0) {
                warnings.add("Model size is very large (${String.format("%.2f", sizeMB)} MB). May impact app size.")
            }
        }
        
        // Check 3: Configuration validation
        if (ModelConfig.YOLO_INPUT_SIZE != 640) {
            warnings.add("Input size is ${ModelConfig.YOLO_INPUT_SIZE}, but model was trained on 640x640")
        } else {
            info.add("✓ Input size matches training: ${ModelConfig.YOLO_INPUT_SIZE}x${ModelConfig.YOLO_INPUT_SIZE}")
        }
        
        // Check 4: Disease classes count
        if (ModelConfig.DISEASE_CLASSES.size != 6) {
            issues.add("Expected 6 disease classes, found ${ModelConfig.DISEASE_CLASSES.size}")
        } else {
            info.add("✓ Disease classes count: ${ModelConfig.DISEASE_CLASSES.size}")
        }
        
        // Check 5: Confidence thresholds
        if (ModelConfig.DETECTION_CONFIDENCE_THRESHOLD < 0.5f) {
            warnings.add("Detection threshold is low (${ModelConfig.DETECTION_CONFIDENCE_THRESHOLD}). May have false positives.")
        } else {
            info.add("✓ Detection threshold: ${ModelConfig.DETECTION_CONFIDENCE_THRESHOLD}")
        }
        
        if (ModelConfig.CONFIDENCE_THRESHOLD < 0.3f) {
            warnings.add("Classification threshold is very low (${ModelConfig.CONFIDENCE_THRESHOLD}). May have unreliable results.")
        } else {
            info.add("✓ Classification threshold: ${ModelConfig.CONFIDENCE_THRESHOLD}")
        }
        
        // Check 6: Model version
        info.add("✓ Model version: ${ModelConfig.MODEL_VERSION}")
        info.add("✓ Training date: ${ModelConfig.MODEL_TRAINING_DATE}")
        info.add("✓ Training epochs: ${ModelConfig.MODEL_TRAINING_EPOCHS}")
        
        // Check 7: Performance expectations
        info.add("✓ Expected accuracy: ${String.format("%.1f%%", ModelConfig.MODEL_MAP50_95 * 100)}")
        info.add("✓ Augmentation: ${ModelConfig.TRAINING_AUGMENTATION_STRATEGY}")
        
        val status = when {
            issues.isNotEmpty() -> DiagnosticStatus.ERROR
            warnings.isNotEmpty() -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.OK
        }
        
        return DiagnosticReport(
            status = status,
            issues = issues,
            warnings = warnings,
            info = info
        )
    }
    
    /**
     * Checks if the model file exists in assets
     */
    private fun checkModelFileExists(context: Context): Boolean {
        return try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            assetFiles.contains(ModelConfig.YOLO_MODEL_PATH)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model file", e)
            false
        }
    }
    
    /**
     * Gets the model file size in bytes
     */
    private fun getModelFileSize(context: Context): Long {
        return try {
            val inputStream = context.assets.open(ModelConfig.YOLO_MODEL_PATH)
            val size = inputStream.available().toLong()
            inputStream.close()
            size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting model file size", e)
            0L
        }
    }
    
    /**
     * Generates a formatted diagnostic report string
     */
    fun generateReportString(report: DiagnosticReport): String {
        val sb = StringBuilder()
        sb.appendLine("=== Model Diagnostics Report ===")
        sb.appendLine()
        sb.appendLine("Status: ${report.status}")
        sb.appendLine()
        
        if (report.issues.isNotEmpty()) {
            sb.appendLine("❌ ISSUES:")
            report.issues.forEach { sb.appendLine("  • $it") }
            sb.appendLine()
        }
        
        if (report.warnings.isNotEmpty()) {
            sb.appendLine("⚠️  WARNINGS:")
            report.warnings.forEach { sb.appendLine("  • $it") }
            sb.appendLine()
        }
        
        if (report.info.isNotEmpty()) {
            sb.appendLine("ℹ️  INFORMATION:")
            report.info.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        sb.appendLine("================================")
        return sb.toString()
    }
    
    /**
     * Logs the diagnostic report
     */
    fun logDiagnostics(context: Context) {
        val report = runDiagnostics(context)
        val reportString = generateReportString(report)
        
        when (report.status) {
            DiagnosticStatus.ERROR -> Log.e(TAG, reportString)
            DiagnosticStatus.WARNING -> Log.w(TAG, reportString)
            DiagnosticStatus.OK -> Log.i(TAG, reportString)
        }
    }
    
    /**
     * Gets a quick status check
     */
    fun quickCheck(context: Context): String {
        val report = runDiagnostics(context)
        return when (report.status) {
            DiagnosticStatus.ERROR -> "❌ Model setup has errors"
            DiagnosticStatus.WARNING -> "⚠️  Model setup has warnings"
            DiagnosticStatus.OK -> "✅ Model setup OK"
        }
    }
}

/**
 * Diagnostic status enum
 */
enum class DiagnosticStatus {
    OK,
    WARNING,
    ERROR
}

/**
 * Diagnostic report data class
 */
data class DiagnosticReport(
    val status: DiagnosticStatus,
    val issues: List<String>,
    val warnings: List<String>,
    val info: List<String>
)
