package com.ml.tomatoscan.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.ImageDecoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ml.tomatoscan.data.HistoryRepository
import com.ml.tomatoscan.models.ScanResult
import com.ml.tomatoscan.utils.DatabaseImageFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

class TomatoScanViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository: HistoryRepository = HistoryRepository(application)
    // TFLite classifier removed; relying solely on Gemini analysis
    private val geminiService = com.ml.tomatoscan.data.GeminiService(com.ml.tomatoscan.data.GeminiConfig::provideApiKey)

    val imageLoader: ImageLoader = ImageLoader.Builder(application)
        .components {
            add(DatabaseImageFetcher.Factory(application, historyRepository))
        }
        .build()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading

    val scanHistory: StateFlow<List<ScanResult>> = historyRepository.getHistory()
        .onStart { _isHistoryLoading.value = true }
        .onEach { _isHistoryLoading.value = false }
        .catch { throwable ->
            android.util.Log.e("TomatoScanViewModel", "Error loading history", throwable)
            _isHistoryLoading.value = false
            emit(emptyList())
        }
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

    private val _analysisBitmap = MutableStateFlow<Bitmap?>(null)
    val analysisBitmap: StateFlow<Bitmap?> = _analysisBitmap

    fun setAnalysisImageUri(uri: Uri?) {
        _analysisImageUri.value = uri
        _scanResult.value = null
        _isLoading.value = false
        if (uri == null) {
            _analysisBitmap.value = null
            return
        }
        // Preload bitmap for preview so the UI doesn't show an indefinite spinner
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bm = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                _analysisBitmap.value = bm
            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Failed to load preview bitmap", e)
                _analysisBitmap.value = null
            }
        }
    }

    fun setDirectCameraMode(enabled: Boolean) {
        _directCameraMode.value = enabled
    }

    fun analyzeImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, imageUri)
                } else {
                    val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source)
                }
                _analysisBitmap.value = bitmap

                android.util.Log.d("TomatoScanViewModel", "Starting tomato leaf disease analysis...")

                // Gemini-only analysis with timeout on IO dispatcher
                val gemini = withTimeoutOrNull(45_000) {
                    withContext(Dispatchers.IO) {
                        runCatching { geminiService.analyzeTomatoLeaf(bitmap) }.getOrNull()
                    }
                }

                // Build overlay boxes from Gemini
                val affectedBoxes = gemini?.affectedAreas?.mapNotNull { area ->
                    val b = area.box
                    if (b.size == 4) com.ml.tomatoscan.models.LeafOverlayBox(
                        label = area.label,
                        x1 = b[0], y1 = b[1], y2 = b[3], x2 = b[2]
                    ) else null
                } ?: emptyList()

                // Build description from Gemini (only for tomato images)
                val descParts = mutableListOf<String>()
                if (gemini?.isTomato != false) {
                    // Only build detailed description for tomato images
                    if (!gemini?.description.isNullOrBlank()) descParts.add(gemini!!.description)
                    if (!gemini?.symptoms.isNullOrEmpty()) {
                        descParts.add("Symptoms: " + gemini!!.symptoms.joinToString(", "))
                    }
                    gemini?.prognosis?.let { if (it.isNotBlank()) descParts.add("Prognosis: $it") }
                    if (gemini == null && descParts.isEmpty()) {
                        descParts.add("Gemini analysis unavailable. Please check your network connection or API key and try again.")
                    }
                }

                val result = if (gemini?.isTomato == false) {
                    // Not a tomato image - set confidence to 100% that it's not a tomato
                    ScanResult(
                        imageUrl = imageUri.toString(),
                        quality = "Not Applicable",
                        confidence = 100f, // High confidence it's not a tomato
                        timestamp = Date().time,
                        diseaseDetected = "Not Tomato",
                        severity = "Not Applicable",
                        description = gemini.description.ifBlank { "This image does not appear to contain a tomato leaf. Please upload an image of a tomato leaf for analysis." },
                        recommendations = emptyList(),
                        treatmentOptions = emptyList(),
                        preventionMeasures = emptyList(),
                        imageBitmap = null,
                        affectedAreas = emptyList(),
                        prognosis = "",
                        recommendationsImmediate = emptyList(),
                        recommendationsShortTerm = emptyList(),
                        recommendationsLongTerm = emptyList()
                    )
                } else {
                    // Valid tomato image
                    ScanResult(
                        imageUrl = imageUri.toString(),
                        quality = (gemini?.severity ?: "Unknown"),
                        confidence = (gemini?.confidence ?: 0f).coerceIn(0f, 100f),
                        timestamp = Date().time,
                        diseaseDetected = (gemini?.disease ?: "Unknown"),
                        severity = (gemini?.severity ?: "Unknown"),
                        description = descParts.joinToString("\n\n").ifBlank { "Could not identify the disease." },
                        recommendations = gemini?.recommendationsImmediate.orEmpty() +
                            gemini?.recommendationsShortTerm.orEmpty() +
                            gemini?.recommendationsLongTerm.orEmpty(),
                        treatmentOptions = gemini?.treatmentOptions ?: emptyList(),
                        preventionMeasures = gemini?.preventionMeasures ?: emptyList(),
                        imageBitmap = null,
                        affectedAreas = affectedBoxes,
                        prognosis = gemini?.prognosis ?: "",
                        recommendationsImmediate = gemini?.recommendationsImmediate ?: emptyList(),
                        recommendationsShortTerm = gemini?.recommendationsShortTerm ?: emptyList(),
                        recommendationsLongTerm = gemini?.recommendationsLongTerm ?: emptyList()
                    )
                }

                if (gemini?.isTomato == false) {
                    android.util.Log.d("TomatoScanViewModel", "Analysis complete - Not a tomato image")
                } else {
                    android.util.Log.d("TomatoScanViewModel", "Analysis complete - Disease: ${result.diseaseDetected}, Severity: ${result.severity}")
                }
                _scanResult.value = result

                historyRepository.saveToHistory(result, imageUri, bitmap)

            } catch (e: Exception) {
                android.util.Log.e("TomatoScanViewModel", "Analysis failed with error: ${e.message}", e)
                e.printStackTrace()

                val errorResult = ScanResult(
                    imageUrl = imageUri.toString(),
                    quality = "Error",
                    confidence = 0f,
                    timestamp = Date().time,
                    diseaseDetected = "Analysis Error",
                    severity = "Unknown",
                    description = "Unable to analyze the image: ${e.message}",
                )
                _scanResult.value = errorResult
            } finally {
                _isLoading.value = false
                android.util.Log.d("TomatoScanViewModel", "Loading state cleared")
            }
        }
    }

    fun clearAnalysisState() {
        _scanResult.value = null
        _analysisImageUri.value = null
        _analysisBitmap.value = null
        _directCameraMode.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1500)
            _isRefreshing.value = false
        }
    }

    fun deleteFromHistory(scanResult: ScanResult) {
        viewModelScope.launch {
            historyRepository.deleteFromHistory(scanResult)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
