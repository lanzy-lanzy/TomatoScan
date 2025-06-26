package com.ml.tomatoscan.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.ml.tomatoscan.data.FirebaseData
import com.ml.tomatoscan.data.GeminiApi
import com.ml.tomatoscan.data.HistoryRepository
import com.ml.tomatoscan.models.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class TomatoScanViewModel : ViewModel() {

    private val geminiApi = GeminiApi()
    private val firebaseData = FirebaseData()
    private var historyRepository: HistoryRepository? = null
    
    fun initializeWithContext(context: Context) {
        if (historyRepository == null) {
            historyRepository = HistoryRepository(context)
        }
    }

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    private val _scanHistory = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanHistory: StateFlow<List<ScanResult>> = _scanHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun analyzeImage(bitmap: Bitmap, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("TomatoScanViewModel", "Starting tomato leaf disease analysis...")
                
                // Call the real Gemini API for analysis
                val analysisResult = geminiApi.analyzeTomatoLeaf(bitmap)
                
                // Store image locally instead of cloud upload
                val imageUrl = imageUri.toString()
                
                // Convert to legacy quality format for UI compatibility
                val quality = when {
                    analysisResult.diseaseDetected.equals("Invalid Image", ignoreCase = true) -> "Invalid"
                    analysisResult.diseaseDetected.equals("Healthy", ignoreCase = true) -> "Excellent"
                    analysisResult.severity.equals("Mild", ignoreCase = true) -> "Good"
                    analysisResult.severity.equals("Moderate", ignoreCase = true) -> "Fair"
                    else -> "Poor"
                }
                
                val result = ScanResult(
                    imageUrl = imageUrl,
                    quality = quality,
                    confidence = analysisResult.confidence,
                    timestamp = Date(),
                    diseaseDetected = analysisResult.diseaseDetected,
                    severity = analysisResult.severity,
                    description = analysisResult.description,
                    recommendations = analysisResult.recommendations,
                    treatmentOptions = analysisResult.treatmentOptions,
                    preventionMeasures = analysisResult.preventionMeasures
                )
                
                android.util.Log.d("TomatoScanViewModel", "Analysis complete - Disease: ${result.diseaseDetected}, Severity: ${result.severity}")
                _scanResult.value = result
                
                // Save to local Room database with image
                try {
                    historyRepository?.saveToHistory(result, imageUri, bitmap)
                    android.util.Log.d("TomatoScanViewModel", "Saved to Room database successfully")
                } catch (e: Exception) {
                    android.util.Log.e("TomatoScanViewModel", "Failed to save to Room database", e)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Analysis failed with error: ${e.message}", e)
                e.printStackTrace()
                
                // Create error result with disease analysis structure
                val errorResult = ScanResult(
                    imageUrl = imageUri.toString(),
                    quality = "Error",
                    confidence = 0f,
                    timestamp = Date(),
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
            timestamp = Date()
        )
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    fun loadScanHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val history = historyRepository?.getHistory() ?: emptyList()
                _scanHistory.value = history
                android.util.Log.d("TomatoScanViewModel", "Loaded ${history.size} items from history")
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to load history", e)
                _scanHistory.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFromHistory(scanResult: ScanResult) {
        viewModelScope.launch {
            try {
                historyRepository?.deleteFromHistory(scanResult)
                loadScanHistory() // Refresh the list
                android.util.Log.d("TomatoScanViewModel", "Deleted item from history")
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to delete from history", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyRepository?.clearHistory()
                _scanHistory.value = emptyList()
                android.util.Log.d("TomatoScanViewModel", "Cleared all history")
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to clear history", e)
            }
        }
    }
}
