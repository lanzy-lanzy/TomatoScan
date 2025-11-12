package com.ml.tomatoscan.models

/**
 * Sealed class representing different types of errors that can occur during tomato leaf analysis.
 * Each error type includes a user-friendly message for display in the UI.
 */
sealed class AnalysisError(open val message: String) {
    
    /**
     * Error when no tomato leaf is detected in the image
     * @property message User-friendly error message
     */
    data class NoLeafDetected(
        override val message: String = "No tomato leaf detected in the image. Please ensure the leaf is clearly visible and try again."
    ) : AnalysisError(message)
    
    /**
     * Error when image quality is insufficient for accurate analysis
     * @property issues List of specific quality issues detected (e.g., "blurry", "too dark")
     * @property message User-friendly error message
     */
    data class PoorImageQuality(
        val issues: List<String>,
        override val message: String = "Image quality is insufficient for analysis. Issues detected: ${issues.joinToString(", ")}. Please capture a clearer photo with better lighting and focus."
    ) : AnalysisError(message)
    
    /**
     * Error when classification confidence is below the acceptable threshold
     * @property confidence The confidence score that was too low
     * @property message User-friendly error message
     */
    data class LowConfidence(
        val confidence: Float,
        override val message: String = "Analysis confidence is too low (${String.format("%.1f%%", confidence * 100)}). Please provide a clearer image of the leaf for more accurate results."
    ) : AnalysisError(message)
    
    /**
     * Error when Gemini AI service is unavailable
     * @property reason The reason for unavailability (e.g., "network error", "API quota exceeded")
     * @property message User-friendly error message
     */
    data class GeminiUnavailable(
        val reason: String,
        override val message: String = "AI validation service is currently unavailable ($reason). Analysis will proceed with basic classification only."
    ) : AnalysisError(message)
    
    /**
     * Error when the provided image is invalid or corrupted
     * @property message User-friendly error message
     */
    data class InvalidImage(
        override val message: String = "The provided image is invalid or corrupted. Please select a valid image file."
    ) : AnalysisError(message)
    
    /**
     * Error for unexpected or unknown errors
     * @property exception The underlying exception that caused the error
     * @property message User-friendly error message
     */
    data class UnknownError(
        val exception: Exception,
        override val message: String = "An unexpected error occurred during analysis: ${exception.message ?: "Unknown error"}. Please try again."
    ) : AnalysisError(message)
}
