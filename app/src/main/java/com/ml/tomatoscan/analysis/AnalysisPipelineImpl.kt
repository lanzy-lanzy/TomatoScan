package com.ml.tomatoscan.analysis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ml.tomatoscan.data.GeminiApi
import com.ml.tomatoscan.data.ResultCache
import com.ml.tomatoscan.ml.ClassificationResult
import com.ml.tomatoscan.ml.DetectionResult
import com.ml.tomatoscan.ml.DiseaseClassifier
import com.ml.tomatoscan.ml.LeafDetector
import com.ml.tomatoscan.models.AnalysisError
import com.ml.tomatoscan.models.DiagnosticReport
import com.ml.tomatoscan.utils.ImagePreprocessor
import com.ml.tomatoscan.utils.ImageQualityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of the complete tomato leaf analysis pipeline.
 * 
 * This class orchestrates the three-stage analysis process:
 * 1. YOLOv11 leaf detection and cropping
 * 2. TFLite disease classification
 * 3. Gemini AI formal diagnostic report generation
 * 
 * The pipeline includes comprehensive error handling, result caching for consistency,
 * and performance monitoring.
 * 
 * @property context Android application context
 * @property leafDetector YOLOv11 leaf detection service
 * @property diseaseClassifier TFLite disease classification service
 * @property geminiApi Gemini AI diagnostic report generation service
 * @property resultCache Cache for storing and retrieving analysis results
 */
class AnalysisPipelineImpl(
    private val context: Context,
    private val leafDetector: LeafDetector,
    private val diseaseClassifier: DiseaseClassifier,
    private val geminiApi: GeminiApi,
    private val resultCache: ResultCache
) : AnalysisPipeline {
    
    companion object {
        private const val TAG = "AnalysisPipeline"
        private const val CONFIDENCE_THRESHOLD = 0.5f
    }
    
    /**
     * Executes the complete analysis pipeline with all three stages.
     * 
     * Requirements addressed:
     * - 1.1: Process images through YOLOv11 detector to identify and crop leaf regions
     * - 1.2: Pass cropped image to TFLite classifier for preliminary disease prediction
     * - 2.1: Classify disease from validated set of conditions
     * - 3.1: Gemini receives cropped image and preliminary classification for validation
     * - 1.5: Complete pipeline within 5 seconds per image
     * - 2.5: Execute TFLite inference within 2 seconds
     */
    override suspend fun analyze(inputImage: Bitmap): AnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Starting analysis pipeline...")
            
            // Check cache first for consistency
            val cachedReport = resultCache.getCachedResult(inputImage)
            if (cachedReport != null) {
                Log.d(TAG, "Returning cached result for consistency")
                val processingTime = System.currentTimeMillis() - startTime
                // Note: We don't have detection/classification results from cache,
                // but we have the final report which is what matters for consistency
                // Return as a special success case with null intermediate results
                return@withContext AnalysisResult(
                    success = true,
                    detectionResult = null,
                    classificationResult = null,
                    diagnosticReport = cachedReport,
                    error = null,
                    processingTimeMs = processingTime
                )
            }
            
            // Stage 0: Validate image quality
            Log.d(TAG, "Stage 0: Validating image quality...")
            val qualityReport = ImageQualityValidator.validateImageQuality(inputImage)
            if (!qualityReport.isValid) {
                val error = AnalysisError.PoorImageQuality(qualityReport.issues)
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "quality_validation",
                    "imageWidth" to inputImage.width,
                    "imageHeight" to inputImage.height,
                    "qualityScore" to qualityReport.score
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime
                )
            }
            
            // Stage 0.5: Gemini pre-validation (optional, if enabled by user and available)
            val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val geminiPreValidationEnabled = sharedPreferences.getBoolean("gemini_pre_validation", true)
            
            if (geminiPreValidationEnabled && geminiApi.isAvailable()) {
                Log.d(TAG, "Stage 0.5: Gemini pre-validation - checking if image contains tomato leaf...")
                val preValidationStartTime = System.currentTimeMillis()
                
                val (isTomatoLeaf, reason) = geminiApi.validateIsTomatoLeaf(inputImage)
                val preValidationTime = System.currentTimeMillis() - preValidationStartTime
                
                Log.d(TAG, "Gemini pre-validation completed in ${preValidationTime}ms: $reason")
                
                if (!isTomatoLeaf) {
                    val error = AnalysisError.NoLeafDetected()
                    AnalysisErrorHandler.logError(error, mapOf(
                        "stage" to "gemini_pre_validation",
                        "reason" to reason,
                        "preValidationTime" to preValidationTime,
                        "imageWidth" to inputImage.width,
                        "imageHeight" to inputImage.height
                    ))
                    val processingTime = System.currentTimeMillis() - startTime
                    return@withContext AnalysisResult.failure(
                        error = error,
                        processingTimeMs = processingTime
                    )
                }
            } else {
                Log.d(TAG, "Gemini pre-validation skipped (disabled or unavailable)")
            }
            
            // Stage 1: YOLOv11 leaf detection and cropping
            Log.d(TAG, "Stage 1: Detecting and cropping leaf...")
            val detectionStartTime = System.currentTimeMillis()
            
            val croppedLeaf = leafDetector.cropLeaf(inputImage)
            if (croppedLeaf == null) {
                val error = AnalysisError.NoLeafDetected()
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "leaf_detection",
                    "imageWidth" to inputImage.width,
                    "imageHeight" to inputImage.height,
                    "detectionTime" to (System.currentTimeMillis() - detectionStartTime)
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime
                )
            }
            
            // Get the detection result for metadata
            val detections = leafDetector.detectLeaves(inputImage)
            val detectionResult = detections.firstOrNull()
            
            val detectionTime = System.currentTimeMillis() - detectionStartTime
            Log.d(TAG, "Leaf detected and cropped in ${detectionTime}ms, confidence: ${detectionResult?.confidence}")
            
            // Validate that the detected object is actually a tomato leaf
            // The YOLO model is trained ONLY on tomato leaves, so low confidence means it's not a tomato leaf
            if (detectionResult == null || detectionResult.confidence < 0.6f) {
                val error = AnalysisError.NoLeafDetected()
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "leaf_validation",
                    "reason" to "Not a tomato leaf - detection confidence too low",
                    "confidence" to (detectionResult?.confidence ?: 0f),
                    "threshold" to 0.6f,
                    "imageWidth" to inputImage.width,
                    "imageHeight" to inputImage.height
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime
                )
            }
            
            // Stage 2: Extract disease classification from YOLO output
            Log.d(TAG, "Stage 2: Extracting disease classification from detection...")
            val classificationStartTime = System.currentTimeMillis()
            
            // ALWAYS use class probabilities from YOLO detection result
            // The tomato_leaf_640.tflite model provides both detection AND classification
            val classificationResult = if (detectionResult?.classProbabilities != null && detectionResult.classProbabilities.isNotEmpty()) {
                Log.d(TAG, "Using YOLO class probabilities for classification")
                extractClassificationFromDetection(detectionResult)
            } else {
                Log.e(TAG, "YOLO detection missing class probabilities - this should not happen!")
                // This should never happen with tomato_leaf_640.tflite
                throw IllegalStateException("YOLO model did not provide class probabilities")
            }
            
            val classificationTime = System.currentTimeMillis() - classificationStartTime
            Log.d(TAG, "Disease classified in ${classificationTime}ms: ${classificationResult.diseaseClass.displayName}, confidence: ${classificationResult.confidence}")
            
            // Stage 2.5: Enhanced Gemini validation for classification (if enabled and available)
            var finalClassificationResult = classificationResult
            if (geminiPreValidationEnabled && geminiApi.isAvailable()) {
                Log.d(TAG, "Stage 2.5: Gemini validation - verifying YOLO classification...")
                val validationStartTime = System.currentTimeMillis()
                
                val (isCorrect, correctedClass, reason) = geminiApi.validateClassification(
                    croppedLeaf, 
                    classificationResult
                )
                val validationTime = System.currentTimeMillis() - validationStartTime
                
                Log.d(TAG, "Gemini classification validation completed in ${validationTime}ms: $reason")
                
                if (!isCorrect && correctedClass != null) {
                    Log.d(TAG, "Gemini corrected classification from ${classificationResult.diseaseClass.displayName} to ${correctedClass.displayName}")
                    finalClassificationResult = com.ml.tomatoscan.ml.ClassificationResult(
                        diseaseClass = correctedClass,
                        confidence = 0.85f, // High confidence from Gemini
                        allProbabilities = mapOf(correctedClass to 0.85f)
                    )
                }
            } else {
                Log.d(TAG, "Gemini classification validation skipped (disabled or unavailable)")
            }
            
            // Check if confidence is too low
            if (finalClassificationResult.confidence < CONFIDENCE_THRESHOLD) {
                val error = AnalysisError.LowConfidence(finalClassificationResult.confidence)
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "classification",
                    "diseaseClass" to finalClassificationResult.diseaseClass.displayName,
                    "confidence" to finalClassificationResult.confidence,
                    "threshold" to CONFIDENCE_THRESHOLD,
                    "classificationTime" to classificationTime
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime,
                    detectionResult = detectionResult,
                    classificationResult = finalClassificationResult
                )
            }
            
            // Stage 3: Gemini AI formal diagnostic report generation
            Log.d(TAG, "Stage 3: Generating formal diagnostic report...")
            val reportStartTime = System.currentTimeMillis()
            
            val diagnosticReport = if (geminiApi.isAvailable()) {
                try {
                    geminiApi.generateDiagnosticReport(croppedLeaf, finalClassificationResult)
                } catch (e: Exception) {
                    val error = AnalysisError.GeminiUnavailable(e.message ?: "Unknown error")
                    AnalysisErrorHandler.logError(error, mapOf(
                        "stage" to "report_generation",
                        "exception" to e::class.simpleName.toString(),
                        "diseaseClass" to finalClassificationResult.diseaseClass.displayName
                    ))
                    Log.e(TAG, "Gemini API failed, using fallback", e)
                    // Fallback to basic report if Gemini fails
                    createFallbackReport(finalClassificationResult)
                }
            } else {
                val error = AnalysisError.GeminiUnavailable("Service unavailable")
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "report_generation",
                    "reason" to "Gemini API not available"
                ))
                Log.w(TAG, "Gemini API unavailable, using fallback report")
                createFallbackReport(finalClassificationResult)
            }
            
            val reportTime = System.currentTimeMillis() - reportStartTime
            Log.d(TAG, "Diagnostic report generated in ${reportTime}ms")
            
            // Cache the result for future consistency
            resultCache.cacheResult(inputImage, diagnosticReport)
            Log.d(TAG, "Result cached for consistency")
            
            val totalProcessingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Analysis pipeline completed in ${totalProcessingTime}ms")
            
            // Ensure we have a valid detection result before returning success
            if (detectionResult == null) {
                val error = AnalysisError.NoLeafDetected()
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "final_validation",
                    "reason" to "Detection result is null despite successful crop"
                ))
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = totalProcessingTime,
                    classificationResult = finalClassificationResult
                )
            }
            
            AnalysisResult.success(
                detectionResult = detectionResult,
                classificationResult = finalClassificationResult,
                diagnosticReport = diagnosticReport,
                processingTimeMs = totalProcessingTime
            )
            
        } catch (e: Exception) {
            val error = AnalysisError.UnknownError(e)
            AnalysisErrorHandler.logError(error, mapOf(
                "stage" to "pipeline",
                "exception" to e::class.simpleName.toString(),
                "imageWidth" to inputImage.width,
                "imageHeight" to inputImage.height
            ))
            Log.e(TAG, "Unexpected error in analysis pipeline", e)
            val processingTime = System.currentTimeMillis() - startTime
            AnalysisResult.failure(
                error = error,
                processingTimeMs = processingTime
            )
        }
    }
    
    /**
     * Executes the pipeline in TFLite-only mode without Gemini validation.
     * 
     * Requirements addressed:
     * - 5.3: Fall back to TFLite prediction with disclaimer when Gemini fails
     * - 6.5: Operate in TFLite-only mode when Gemini API is unavailable
     */
    override suspend fun analyzeFallback(inputImage: Bitmap): AnalysisResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "Starting fallback analysis pipeline (TFLite-only)...")
            
            // Stage 1: Validate image quality
            val qualityReport = ImageQualityValidator.validateImageQuality(inputImage)
            if (!qualityReport.isValid) {
                val error = AnalysisError.PoorImageQuality(qualityReport.issues)
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "quality_validation",
                    "mode" to "fallback",
                    "imageWidth" to inputImage.width,
                    "imageHeight" to inputImage.height,
                    "qualityScore" to qualityReport.score
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime
                )
            }
            
            // Stage 2: YOLOv11 leaf detection and cropping
            Log.d(TAG, "Stage 1: Detecting and cropping leaf...")
            val croppedLeaf = leafDetector.cropLeaf(inputImage)
            if (croppedLeaf == null) {
                val error = AnalysisError.NoLeafDetected()
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "leaf_detection",
                    "mode" to "fallback",
                    "imageWidth" to inputImage.width,
                    "imageHeight" to inputImage.height
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime
                )
            }
            
            val detections = leafDetector.detectLeaves(inputImage)
            val detectionResult = detections.firstOrNull()
            
            // Stage 3: Extract disease classification from YOLO output
            Log.d(TAG, "Stage 2: Extracting disease classification from detection...")
            val classificationResult = if (detectionResult?.classProbabilities != null && detectionResult.classProbabilities.isNotEmpty()) {
                Log.d(TAG, "Using YOLO class probabilities for classification")
                extractClassificationFromDetection(detectionResult)
            } else {
                Log.e(TAG, "YOLO detection missing class probabilities - this should not happen!")
                throw IllegalStateException("YOLO model did not provide class probabilities")
            }
            
            Log.d(TAG, "Disease classified: ${classificationResult.diseaseClass.displayName}, confidence: ${classificationResult.confidence}")
            
            // Check if confidence is too low
            if (classificationResult.confidence < CONFIDENCE_THRESHOLD) {
                val error = AnalysisError.LowConfidence(classificationResult.confidence)
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "classification",
                    "mode" to "fallback",
                    "diseaseClass" to classificationResult.diseaseClass.displayName,
                    "confidence" to classificationResult.confidence,
                    "threshold" to CONFIDENCE_THRESHOLD
                ))
                val processingTime = System.currentTimeMillis() - startTime
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = processingTime,
                    detectionResult = detectionResult,
                    classificationResult = classificationResult
                )
            }
            
            // Stage 4: Generate fallback report (no Gemini validation)
            Log.d(TAG, "Stage 3: Generating fallback report...")
            val diagnosticReport = createFallbackReport(classificationResult)
            
            val totalProcessingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Fallback analysis pipeline completed in ${totalProcessingTime}ms")
            
            // Ensure we have a valid detection result before returning success
            if (detectionResult == null) {
                val error = AnalysisError.NoLeafDetected()
                AnalysisErrorHandler.logError(error, mapOf(
                    "stage" to "final_validation",
                    "mode" to "fallback",
                    "reason" to "Detection result is null despite successful crop"
                ))
                return@withContext AnalysisResult.failure(
                    error = error,
                    processingTimeMs = totalProcessingTime,
                    classificationResult = classificationResult
                )
            }
            
            AnalysisResult.success(
                detectionResult = detectionResult,
                classificationResult = classificationResult,
                diagnosticReport = diagnosticReport,
                processingTimeMs = totalProcessingTime
            )
            
        } catch (e: Exception) {
            val error = AnalysisError.UnknownError(e)
            AnalysisErrorHandler.logError(error, mapOf(
                "stage" to "pipeline",
                "mode" to "fallback",
                "exception" to e::class.simpleName.toString(),
                "imageWidth" to inputImage.width,
                "imageHeight" to inputImage.height
            ))
            Log.e(TAG, "Unexpected error in fallback analysis pipeline", e)
            val processingTime = System.currentTimeMillis() - startTime
            AnalysisResult.failure(
                error = error,
                processingTimeMs = processingTime
            )
        }
    }
    
    /**
     * Extracts disease classification from YOLO detection result.
     * The YOLO model outputs both detection and classification in one pass.
     */
    private fun extractClassificationFromDetection(detectionResult: DetectionResult): ClassificationResult {
        val classProbabilities = detectionResult.classProbabilities ?: emptyMap()
        
        // Find the class with highest probability
        var maxProb = 0f
        var maxClassIdx = 0
        for ((classIdx, prob) in classProbabilities) {
            if (prob > maxProb) {
                maxProb = prob
                maxClassIdx = classIdx
            }
        }
        
        // Map class index to DiseaseClass
        // YOLOv11 model (best_int8.tflite) trained on 6 disease classes
        // Order matches the training data from PlantVillage dataset:
        // 0: Tomato_Bacterial_spot
        // 1: Tomato_Early_blight
        // 2: Tomato_Late_blight
        // 3: Tomato_Septoria_leaf_spot
        // 4: Tomato_Tomato_mosaic_virus
        // 5: Tomato_healthy
        val diseaseClasses = listOf(
            com.ml.tomatoscan.models.DiseaseClass.BACTERIAL_SPOT,       // Index 0
            com.ml.tomatoscan.models.DiseaseClass.EARLY_BLIGHT,         // Index 1
            com.ml.tomatoscan.models.DiseaseClass.LATE_BLIGHT,          // Index 2
            com.ml.tomatoscan.models.DiseaseClass.SEPTORIA_LEAF_SPOT,   // Index 3
            com.ml.tomatoscan.models.DiseaseClass.TOMATO_MOSAIC_VIRUS,  // Index 4
            com.ml.tomatoscan.models.DiseaseClass.HEALTHY               // Index 5
        )
        
        // Log all probabilities for debugging with class names
        Log.d(TAG, "=== YOLO Class Probabilities ===")
        for ((idx, prob) in classProbabilities.entries.sortedBy { it.key }) {
            val className = if (idx < diseaseClasses.size) {
                diseaseClasses[idx].displayName
            } else {
                "Unknown"
            }
            Log.d(TAG, "  Index $idx ($className): ${String.format("%.4f", prob)} (${(prob * 100).toInt()}%)")
        }
        
        val diseaseClass = if (maxClassIdx < diseaseClasses.size) {
            diseaseClasses[maxClassIdx]
        } else {
            com.ml.tomatoscan.models.DiseaseClass.UNCERTAIN
        }
        
        Log.d(TAG, "  Winner: Index $maxClassIdx (${diseaseClass.displayName}) = ${String.format("%.4f", maxProb)} (${(maxProb * 100).toInt()}%)")
        
        // Convert to map with DiseaseClass keys
        val allProbabilities = mutableMapOf<com.ml.tomatoscan.models.DiseaseClass, Float>()
        for ((idx, prob) in classProbabilities) {
            if (idx < diseaseClasses.size) {
                allProbabilities[diseaseClasses[idx]] = prob
            }
        }
        
        return ClassificationResult(
            diseaseClass = diseaseClass,
            confidence = maxProb,
            allProbabilities = allProbabilities
        )
    }
    
    /**
     * Creates a fallback diagnostic report based on TFLite prediction.
     * Used when Gemini API is unavailable or fails.
     * 
     * Maintains consistent report structure with a disclaimer about lack of formal validation.
     */
    private fun createFallbackReport(classificationResult: com.ml.tomatoscan.ml.ClassificationResult): DiagnosticReport {
        val diseaseName = classificationResult.diseaseClass.displayName
        val confidence = String.format("%.1f%%", classificationResult.confidence * 100)
        
        val fullReport = "Based on the image analysis, the tomato leaf is identified as **$diseaseName**. " +
                "Classification confidence: $confidence. " +
                "Note: This is a preliminary classification without formal validation. " +
                "Consult with an agricultural expert for detailed diagnosis and treatment recommendations."
        
        return DiagnosticReport(
            diseaseName = diseaseName,
            observedSymptoms = "Automated classification based on visual patterns.",
            confidenceLevel = "Classification confidence: $confidence (preliminary)",
            managementRecommendation = "Consult with agricultural expert for specific treatment recommendations.",
            fullReport = fullReport,
            isUncertain = classificationResult.confidence < CONFIDENCE_THRESHOLD,
            timestamp = System.currentTimeMillis(),
            modelVersion = "1.0.0"
        )
    }
}
