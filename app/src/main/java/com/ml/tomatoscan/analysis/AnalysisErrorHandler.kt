package com.ml.tomatoscan.analysis

import android.util.Log
import com.ml.tomatoscan.models.AnalysisError

/**
 * Utility class for handling and logging analysis errors.
 * 
 * Provides comprehensive error handling with:
 * - User-friendly error messages for UI display
 * - Detailed logging with context for debugging
 * - Error categorization and severity levels
 * 
 * Requirements addressed:
 * - 5.1: Provide clear feedback for poor image quality
 * - 5.2: Provide user-friendly error message when no leaf detected
 * - 5.3: Fall back to TFLite prediction with disclaimer when Gemini fails
 * - 5.4: Log all errors with timestamps and input metadata
 * - 5.5: Inform user when network connectivity is unavailable
 */
object AnalysisErrorHandler {
    
    private const val TAG = "AnalysisErrorHandler"
    
    /**
     * Error severity levels for categorization and prioritization.
     */
    enum class ErrorSeverity {
        /** User can retry with better input (e.g., clearer photo) */
        RECOVERABLE,
        
        /** System issue but analysis can proceed with limitations (e.g., Gemini unavailable) */
        WARNING,
        
        /** Critical error that prevents analysis (e.g., invalid image) */
        CRITICAL
    }
    
    /**
     * Logs an analysis error with full context for debugging.
     * 
     * @param error The analysis error to log
     * @param context Additional context information (e.g., image dimensions, processing stage)
     */
    fun logError(error: AnalysisError, context: Map<String, Any> = emptyMap()) {
        val severity = getErrorSeverity(error)
        val timestamp = System.currentTimeMillis()
        
        val logMessage = buildString {
            appendLine("=== Analysis Error ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Error Type: ${error::class.simpleName}")
            appendLine("Severity: $severity")
            appendLine("Message: ${error.message}")
            
            if (context.isNotEmpty()) {
                appendLine("Context:")
                context.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
            
            // Add specific error details
            when (error) {
                is AnalysisError.NoLeafDetected -> {
                    appendLine("Details: No tomato leaf region detected in the image")
                    appendLine("Suggestion: Ensure leaf is clearly visible and well-framed")
                }
                
                is AnalysisError.PoorImageQuality -> {
                    appendLine("Details: Image quality issues detected")
                    appendLine("Issues: ${error.issues.joinToString(", ")}")
                    appendLine("Suggestion: Improve lighting, focus, and image resolution")
                }
                
                is AnalysisError.LowConfidence -> {
                    appendLine("Details: Classification confidence below threshold")
                    appendLine("Confidence: ${String.format("%.2f%%", error.confidence * 100)}")
                    appendLine("Threshold: 50%")
                    appendLine("Suggestion: Provide clearer image with better visible symptoms")
                }
                
                is AnalysisError.GeminiUnavailable -> {
                    appendLine("Details: Gemini AI service unavailable")
                    appendLine("Reason: ${error.reason}")
                    appendLine("Fallback: Using TFLite-only classification")
                }
                
                is AnalysisError.InvalidImage -> {
                    appendLine("Details: Image file is invalid or corrupted")
                    appendLine("Suggestion: Select a valid image file (JPEG, PNG)")
                }
                
                is AnalysisError.UnknownError -> {
                    appendLine("Details: Unexpected error occurred")
                    appendLine("Exception: ${error.exception::class.simpleName}")
                    appendLine("Exception Message: ${error.exception.message}")
                    appendLine("Stack Trace:")
                    error.exception.stackTrace.take(5).forEach { element ->
                        appendLine("  at $element")
                    }
                }
            }
            
            appendLine("======================")
        }
        
        // Log with appropriate level based on severity
        when (severity) {
            ErrorSeverity.RECOVERABLE -> Log.w(TAG, logMessage)
            ErrorSeverity.WARNING -> Log.w(TAG, logMessage)
            ErrorSeverity.CRITICAL -> Log.e(TAG, logMessage)
        }
    }
    
    /**
     * Gets the severity level for an analysis error.
     * 
     * @param error The analysis error
     * @return The severity level
     */
    fun getErrorSeverity(error: AnalysisError): ErrorSeverity {
        return when (error) {
            is AnalysisError.NoLeafDetected -> ErrorSeverity.RECOVERABLE
            is AnalysisError.PoorImageQuality -> ErrorSeverity.RECOVERABLE
            is AnalysisError.LowConfidence -> ErrorSeverity.RECOVERABLE
            is AnalysisError.GeminiUnavailable -> ErrorSeverity.WARNING
            is AnalysisError.InvalidImage -> ErrorSeverity.CRITICAL
            is AnalysisError.UnknownError -> ErrorSeverity.CRITICAL
        }
    }
    
    /**
     * Gets a user-friendly error message for display in the UI.
     * 
     * @param error The analysis error
     * @return User-friendly error message
     */
    fun getUserFriendlyMessage(error: AnalysisError): String {
        return error.message
    }
    
    /**
     * Gets actionable suggestions for the user to resolve the error.
     * 
     * @param error The analysis error
     * @return List of actionable suggestions
     */
    fun getActionableSuggestions(error: AnalysisError): List<String> {
        return when (error) {
            is AnalysisError.NoLeafDetected -> listOf(
                "Ensure the tomato leaf is clearly visible in the frame",
                "Move closer to the leaf for a better view",
                "Make sure the leaf occupies most of the image",
                "Avoid capturing multiple leaves or other objects"
            )
            
            is AnalysisError.PoorImageQuality -> listOf(
                "Use better lighting conditions (natural daylight works best)",
                "Hold the camera steady to avoid blur",
                "Clean the camera lens if necessary",
                "Ensure the image resolution is at least 224x224 pixels",
                "Avoid extreme shadows or overexposure"
            )
            
            is AnalysisError.LowConfidence -> listOf(
                "Capture a clearer image with better focus",
                "Ensure disease symptoms are clearly visible",
                "Use better lighting to show leaf details",
                "Try capturing a different leaf with more visible symptoms"
            )
            
            is AnalysisError.GeminiUnavailable -> listOf(
                "Check your internet connection",
                "Try again in a few moments",
                "Analysis will proceed with basic classification",
                "Formal validation will be available when service is restored"
            )
            
            is AnalysisError.InvalidImage -> listOf(
                "Select a valid image file (JPEG or PNG format)",
                "Ensure the image file is not corrupted",
                "Try capturing a new photo instead of using an existing one"
            )
            
            is AnalysisError.UnknownError -> listOf(
                "Try again with a different image",
                "Restart the application if the problem persists",
                "Contact support if the issue continues"
            )
        }
    }
    
    /**
     * Determines if the error is recoverable by user action.
     * 
     * @param error The analysis error
     * @return True if user can take action to resolve the error
     */
    fun isRecoverable(error: AnalysisError): Boolean {
        return getErrorSeverity(error) == ErrorSeverity.RECOVERABLE
    }
    
    /**
     * Creates a detailed error report for debugging purposes.
     * 
     * @param error The analysis error
     * @param context Additional context information
     * @return Formatted error report string
     */
    fun createErrorReport(error: AnalysisError, context: Map<String, Any> = emptyMap()): String {
        return buildString {
            appendLine("Error Report")
            appendLine("============")
            appendLine("Type: ${error::class.simpleName}")
            appendLine("Severity: ${getErrorSeverity(error)}")
            appendLine("Message: ${error.message}")
            appendLine("Recoverable: ${isRecoverable(error)}")
            appendLine()
            appendLine("Suggestions:")
            getActionableSuggestions(error).forEachIndexed { index, suggestion ->
                appendLine("${index + 1}. $suggestion")
            }
            
            if (context.isNotEmpty()) {
                appendLine()
                appendLine("Context:")
                context.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
        }
    }
}
