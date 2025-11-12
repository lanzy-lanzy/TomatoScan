package com.ml.tomatoscan.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ml.tomatoscan.analysis.AnalysisPipeline
import com.ml.tomatoscan.analysis.AnalysisPipelineImpl
import com.ml.tomatoscan.data.FirebaseData
import coil.ImageLoader
import com.ml.tomatoscan.data.GeminiApi
import com.ml.tomatoscan.data.HistoryRepository
import com.ml.tomatoscan.data.ResultCacheImpl
import com.ml.tomatoscan.ml.TFLiteDiseaseClassifier
import com.ml.tomatoscan.ml.YoloLeafDetector
import com.ml.tomatoscan.models.AnalysisError
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.utils.DatabaseImageFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date

class TomatoScanViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiApi = GeminiApi(application)
    private val firebaseData = FirebaseData()
    private val historyRepository: HistoryRepository = HistoryRepository(application)
    
    // Initialize pipeline components lazily to avoid blocking app startup
    private val leafDetector by lazy { 
        try {
            YoloLeafDetector(application)
        } catch (e: Exception) {
            android.util.Log.e("TomatoScanViewModel", "Failed to initialize YoloLeafDetector", e)
            throw e
        }
    }
    
    private val diseaseClassifier by lazy { 
        try {
            TFLiteDiseaseClassifier(application)
        } catch (e: Exception) {
            android.util.Log.e("TomatoScanViewModel", "Failed to initialize TFLiteDiseaseClassifier", e)
            throw e
        }
    }
    
    private val database by lazy { 
        com.ml.tomatoscan.data.database.TomatoScanDatabase.getDatabase(application)
    }
    
    private val resultCache by lazy { 
        ResultCacheImpl(database.resultCacheDao())
    }
    
    private val analysisPipeline: AnalysisPipeline by lazy {
        AnalysisPipelineImpl(
            context = application,
            leafDetector = leafDetector,
            diseaseClassifier = diseaseClassifier,
            geminiApi = geminiApi,
            resultCache = resultCache
        )
    }

    val imageLoader: ImageLoader = ImageLoader.Builder(application)
        .components {
            add(DatabaseImageFetcher.Factory(application))
        }
        .build()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading

    val scanHistory: StateFlow<List<ScanResult>> = historyRepository.getHistory()
        .onStart { _isHistoryLoading.value = true }
        .onEach { _isHistoryLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _analysisImageUri = MutableStateFlow<Uri?>(null)
    val analysisImageUri: StateFlow<Uri?> = _analysisImageUri

    private val _directCameraMode = MutableStateFlow(false)
    val directCameraMode: StateFlow<Boolean> = _directCameraMode



    fun setAnalysisImageUri(uri: Uri?) {
        _analysisImageUri.value = uri
    }

    fun setDirectCameraMode(enabled: Boolean) {
        _directCameraMode.value = enabled
    }

    fun analyzeImage(bitmap: Bitmap, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Clear previous errors
            
            try {
                android.util.Log.d("TomatoScanViewModel", "Starting tomato leaf disease analysis with pipeline...")
                
                // Execute the analysis pipeline
                val pipelineResult = analysisPipeline.analyze(bitmap)
                
                if (pipelineResult.success && pipelineResult.diagnosticReport != null) {
                    // Map AnalysisResult to ScanResult
                    val diagnosticReport = pipelineResult.diagnosticReport
                    val classificationResult = pipelineResult.classificationResult
                    
                    // Convert to legacy quality format for UI compatibility
                    val quality = when {
                        diagnosticReport.isUncertain -> "Uncertain"
                        diagnosticReport.diseaseName.equals("Healthy", ignoreCase = true) -> "Excellent"
                        classificationResult?.confidence ?: 0f >= 0.9f -> "Excellent"
                        classificationResult?.confidence ?: 0f >= 0.75f -> "Good"
                        classificationResult?.confidence ?: 0f >= 0.6f -> "Fair"
                        else -> "Poor"
                    }
                    
                    val result = ScanResult(
                        imageUrl = imageUri.toString(),
                        quality = quality,
                        confidence = classificationResult?.confidence ?: 0f,
                        timestamp = Date().time,
                        diseaseDetected = diagnosticReport.diseaseName,
                        severity = determineSeverity(diagnosticReport.diseaseName, classificationResult?.confidence ?: 0f),
                        description = diagnosticReport.fullReport,
                        recommendations = parseRecommendations(diagnosticReport.managementRecommendation),
                        treatmentOptions = listOf("Follow management recommendations from diagnostic report"),
                        preventionMeasures = listOf("Regular monitoring", "Proper plant spacing", "Good air circulation"),
                        diagnosticReport = diagnosticReport
                    )
                    
                    android.util.Log.d("TomatoScanViewModel", "Analysis complete - Disease: ${result.diseaseDetected}, Confidence: ${result.confidence}")
                    android.util.Log.d("TomatoScanViewModel", "Processing time: ${pipelineResult.processingTimeMs}ms")
                    
                    _scanResult.value = result
                    
                    // Save to local Room database with image and diagnostic report
                    try {
                        historyRepository.saveToHistory(result, imageUri, bitmap)
                        android.util.Log.d("TomatoScanViewModel", "Saved to Room database successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("TomatoScanViewModel", "Failed to save to Room database", e)
                    }
                    
                } else {
                    // Handle pipeline failure
                    val error = pipelineResult.error ?: AnalysisError.UnknownError(Exception("Unknown pipeline error"))
                    android.util.Log.e("TomatoScanViewModel", "Pipeline failed: ${error.message}")
                    
                    // Map error to user-friendly message and emit error state
                    _errorMessage.value = mapErrorToMessage(error)
                    
                    // Create error result for UI display
                    val errorResult = ScanResult(
                        imageUrl = imageUri.toString(),
                        quality = "Error",
                        confidence = 0f,
                        timestamp = Date().time,
                        diseaseDetected = "Analysis Error",
                        severity = "Unknown",
                        description = error.message,
                        recommendations = getErrorRecommendations(error),
                        treatmentOptions = listOf("Consult with a local agricultural expert"),
                        preventionMeasures = listOf("Regular monitoring", "Proper plant spacing")
                    )
                    _scanResult.value = errorResult
                }
                
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Analysis failed with unexpected error: ${e.message}", e)
                e.printStackTrace()
                
                val error = AnalysisError.UnknownError(e)
                _errorMessage.value = mapErrorToMessage(error)
                
                // Create error result with disease analysis structure
                val errorResult = ScanResult(
                    imageUrl = imageUri.toString(),
                    quality = "Error",
                    confidence = 0f,
                    timestamp = Date().time,
                    diseaseDetected = "Analysis Error",
                    severity = "Unknown",
                    description = "Unable to analyze the image: ${e.message}",
                    recommendations = listOf("Please try again with a clearer image", "Ensure good lighting conditions"),
                    treatmentOptions = listOf("Consult with a local agricultural expert"),
                    preventionMeasures = listOf("Regular monitoring", "Proper plant spacing")
                )
                _scanResult.value = errorResult
            } finally {
                _isLoading.value = false
                android.util.Log.d("TomatoScanViewModel", "Loading state cleared")
            }
        }
    }
    
    /**
     * Determines severity based on disease name and confidence.
     * Maps disease classifications to severity levels for UI display.
     */
    private fun determineSeverity(diseaseName: String, confidence: Float): String {
        return when {
            diseaseName.equals("Healthy", ignoreCase = true) -> "Healthy"
            diseaseName.equals("Uncertain", ignoreCase = true) -> "Unknown"
            confidence >= 0.9f -> "Severe"
            confidence >= 0.75f -> "Moderate"
            confidence >= 0.6f -> "Mild"
            else -> "Unknown"
        }
    }
    
    /**
     * Parses management recommendations into a list format.
     * Splits recommendations by common delimiters for UI display.
     */
    private fun parseRecommendations(recommendation: String): List<String> {
        // Split by periods or semicolons and filter out empty strings
        return recommendation.split(Regex("[.;]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 10 } // Filter out very short fragments
            .take(5) // Limit to 5 recommendations
    }
    
    /**
     * Maps AnalysisError to user-friendly error messages.
     * Requirement 5.1, 5.2, 5.3, 5.4: Provide clear feedback for different error types
     */
    private fun mapErrorToMessage(error: AnalysisError): String {
        return when (error) {
            is AnalysisError.NoLeafDetected -> error.message
            is AnalysisError.PoorImageQuality -> error.message
            is AnalysisError.LowConfidence -> error.message
            is AnalysisError.GeminiUnavailable -> error.message
            is AnalysisError.InvalidImage -> error.message
            is AnalysisError.UnknownError -> error.message
        }
    }
    
    /**
     * Gets context-specific recommendations based on error type.
     * Provides actionable guidance to help users resolve issues.
     */
    private fun getErrorRecommendations(error: AnalysisError): List<String> {
        return when (error) {
            is AnalysisError.NoLeafDetected -> listOf(
                "Ensure the tomato leaf is clearly visible in the frame",
                "Avoid capturing multiple leaves or cluttered backgrounds",
                "Center the leaf in the image"
            )
            is AnalysisError.PoorImageQuality -> listOf(
                "Use better lighting conditions",
                "Hold the camera steady to avoid blur",
                "Move closer to the leaf for better detail",
                "Clean the camera lens"
            )
            is AnalysisError.LowConfidence -> listOf(
                "Capture a clearer image with better focus",
                "Ensure good lighting without shadows",
                "Try photographing a different part of the leaf"
            )
            is AnalysisError.GeminiUnavailable -> listOf(
                "Check your internet connection",
                "Try again in a few moments",
                "Basic classification is still available"
            )
            is AnalysisError.InvalidImage -> listOf(
                "Select a valid image file",
                "Ensure the image is not corrupted",
                "Try capturing a new photo"
            )
            is AnalysisError.UnknownError -> listOf(
                "Please try again",
                "Restart the app if the problem persists",
                "Contact support if the issue continues"
            )
        }
    }

    private fun createMockAnalysisResult(): com.ml.tomatoscan.data.TomatoAnalysisResult {
        val diseases = listOf("Healthy", "Early Blight", "Late Blight", "Septoria Leaf Spot", "Bacterial Spot")
        val severities = listOf("Healthy", "Mild", "Moderate", "Severe")
        val confidences = listOf(95.5f, 87.2f, 78.8f, 92.1f, 85.3f)
        
        val randomIndex = (0..4).random()
        val disease = diseases[randomIndex]
        val severity = if (disease == "Healthy") "Healthy" else severities[(1..3).random()]
        
        return com.ml.tomatoscan.data.TomatoAnalysisResult(
            diseaseDetected = disease,
            confidence = confidences[randomIndex],
            severity = severity,
            description = when (disease) {
                "Healthy" -> "The tomato leaf appears healthy with no visible signs of disease. Good color and structure observed."
                "Early Blight" -> "Dark spots with concentric rings visible on leaves, characteristic of Alternaria solani infection."
                "Late Blight" -> "Water-soaked lesions with white fuzzy growth detected, indicating Phytophthora infestans."
                "Septoria Leaf Spot" -> "Small circular spots with gray centers and dark borders observed on leaf surface."
                "Bacterial Spot" -> "Small, dark, greasy spots with yellow halos detected, indicating bacterial infection."
                else -> "Analysis completed successfully."
            },
            recommendations = when (disease) {
                "Healthy" -> listOf("Continue current care routine", "Monitor regularly for changes", "Maintain good air circulation")
                "Early Blight" -> listOf("Remove affected leaves immediately", "Improve air circulation", "Apply fungicide treatment")
                "Late Blight" -> listOf("Isolate plant immediately", "Remove all infected material", "Apply copper-based fungicide")
                "Septoria Leaf Spot" -> listOf("Remove affected leaves", "Avoid overhead watering", "Apply preventive fungicide")
                "Bacterial Spot" -> listOf("Remove infected plant parts", "Avoid water splash", "Apply copper bactericide")
                else -> listOf("Monitor plant closely", "Consult agricultural expert", "Follow best practices")
            },
            treatmentOptions = when (disease) {
                "Healthy" -> listOf("No treatment needed", "Preventive care only", "Regular monitoring")
                else -> listOf("Organic fungicide spray", "Copper-based treatment", "Systemic fungicide application")
            },
            preventionMeasures = listOf("Proper plant spacing", "Good air circulation", "Avoid overhead watering", "Regular inspection")
        )
    }

    private fun createMockResult(imageUri: Uri): ScanResult {
        // Create realistic mock results for testing
        val qualities = listOf("Excellent", "Good", "Fair", "Poor", "Unripe")
        val confidences = listOf(95.5f, 87.2f, 78.8f, 65.3f, 92.1f)
        
        val randomIndex = (0..4).random()
        
        return ScanResult(
            imageUrl = imageUri.toString(),
            quality = qualities[randomIndex],
            confidence = confidences[randomIndex],
            timestamp = Date().time
        )
    }

    fun clearAnalysisState() {
        _scanResult.value = null
        _analysisImageUri.value = null
        _directCameraMode.value = false
        _errorMessage.value = null
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Data is already live from Room, so we just show the indicator for a bit for good UX.
            delay(1500)
            _isRefreshing.value = false
        }
    }



    fun deleteFromHistory(scanResult: ScanResult) {
        viewModelScope.launch {
            try {
                historyRepository.deleteFromHistory(scanResult)
                android.util.Log.d("TomatoScanViewModel", "Deleted item from history")
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to delete from history", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyRepository.clearHistory()
                android.util.Log.d("TomatoScanViewModel", "Cleared all history")
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to clear history", e)
            }
        }
    }
}
