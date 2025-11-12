package com.ml.tomatoscan.analysis

import android.graphics.Bitmap
import com.ml.tomatoscan.ml.ClassificationResult
import com.ml.tomatoscan.ml.DetectionResult
import com.ml.tomatoscan.models.AnalysisError
import com.ml.tomatoscan.models.DiagnosticReport

/**
 * Interface for the complete tomato leaf analysis pipeline.
 * 
 * This interface defines the contract for orchestrating the three-stage analysis process:
 * 1. YOLOv11 leaf detection and cropping
 * 2. TFLite disease classification
 * 3. Gemini AI formal diagnostic report generation
 * 
 * The pipeline handles errors at each stage with appropriate fallbacks and tracks
 * processing time for performance monitoring.
 * 
 * Requirements addressed:
 * - 1.1: Process images through YOLOv11 detector to identify and crop leaf regions
 * - 1.2: Pass cropped image to TFLite classifier for preliminary disease prediction
 * - 2.1: Classify disease from validated set of conditions
 * - 3.1: Gemini receives cropped image and preliminary classification for validation
 */
interface AnalysisPipeline {
    
    /**
     * Executes the complete analysis pipeline with all three stages.
     * 
     * Pipeline flow:
     * 1. Detection Stage: YOLOv11 detects and crops leaf region
     * 2. Classification Stage: TFLite classifies disease from cropped leaf
     * 3. Validation Stage: Gemini validates classification and generates formal report
     * 
     * If any stage fails, appropriate error handling and fallbacks are applied.
     * Results are cached for consistency across multiple analyses of the same image.
     * 
     * @param inputImage User-provided image containing a tomato leaf
     * @return Complete analysis result with detection, classification, and diagnostic report
     */
    suspend fun analyze(inputImage: Bitmap): AnalysisResult
    
    /**
     * Executes the pipeline in TFLite-only mode without Gemini validation.
     * 
     * This fallback mode is used when:
     * - Gemini API is unavailable (network issues, API quota exceeded)
     * - User preference to skip AI validation
     * - Quick analysis mode is enabled
     * 
     * Pipeline flow:
     * 1. Detection Stage: YOLOv11 detects and crops leaf region
     * 2. Classification Stage: TFLite classifies disease from cropped leaf
     * 3. Fallback Report: Generate basic report from TFLite prediction
     * 
     * @param inputImage User-provided image containing a tomato leaf
     * @return Analysis result with detection and classification, but basic report
     */
    suspend fun analyzeFallback(inputImage: Bitmap): AnalysisResult
}

/**
 * Result of the complete analysis pipeline.
 * 
 * Contains results from all stages of the pipeline, or error information if any stage failed.
 * The success flag indicates whether the analysis completed successfully.
 * 
 * @property success True if analysis completed successfully, false if any stage failed
 * @property detectionResult Result from YOLOv11 leaf detection stage (null if detection failed)
 * @property classificationResult Result from TFLite classification stage (null if classification failed)
 * @property diagnosticReport Formal diagnostic report from Gemini (null if report generation failed)
 * @property error Error information if analysis failed (null if successful)
 * @property processingTimeMs Total time taken for the complete pipeline in milliseconds
 */
data class AnalysisResult(
    val success: Boolean,
    val detectionResult: DetectionResult?,
    val classificationResult: ClassificationResult?,
    val diagnosticReport: DiagnosticReport?,
    val error: AnalysisError?,
    val processingTimeMs: Long
) {
    companion object {
        /**
         * Creates a successful analysis result.
         */
        fun success(
            detectionResult: DetectionResult,
            classificationResult: ClassificationResult,
            diagnosticReport: DiagnosticReport,
            processingTimeMs: Long
        ): AnalysisResult {
            return AnalysisResult(
                success = true,
                detectionResult = detectionResult,
                classificationResult = classificationResult,
                diagnosticReport = diagnosticReport,
                error = null,
                processingTimeMs = processingTimeMs
            )
        }
        
        /**
         * Creates a failed analysis result with error information.
         */
        fun failure(
            error: AnalysisError,
            processingTimeMs: Long,
            detectionResult: DetectionResult? = null,
            classificationResult: ClassificationResult? = null
        ): AnalysisResult {
            return AnalysisResult(
                success = false,
                detectionResult = detectionResult,
                classificationResult = classificationResult,
                diagnosticReport = null,
                error = error,
                processingTimeMs = processingTimeMs
            )
        }
    }
}
