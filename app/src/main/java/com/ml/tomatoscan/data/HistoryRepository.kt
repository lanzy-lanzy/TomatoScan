package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.ml.tomatoscan.data.database.TomatoScanDatabase
import com.ml.tomatoscan.data.database.entities.AnalysisEntity
import com.ml.tomatoscan.models.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*



class HistoryRepository(private val context: Context) {

    private val database = TomatoScanDatabase.getDatabase(context)
    private val analysisDao = database.analysisDao()
    private val imageStorageHelper = ImageStorageHelper(context)

    suspend fun saveToHistory(scanResult: ScanResult, imageUri: Uri? = null, bitmap: Bitmap? = null) = withContext(Dispatchers.IO) {
        try {
            Log.d("HistoryRepository", "Saving scan result to Room database")
            
            // Convert image to byte array for storage
            val imageData = when {
                imageUri != null -> imageStorageHelper.uriToByteArray(imageUri)
                bitmap != null -> imageStorageHelper.bitmapToByteArray(bitmap)
                else -> null
            }
            
            if (imageData == null) {
                Log.w("HistoryRepository", "Could not convert image to byte array, saving without image")
            }
            
            // Save image to internal storage as backup (optional)
            val imagePath = imageUri?.let { imageStorageHelper.saveImageToInternalStorage(it) }
            
            val analysisEntity = AnalysisEntity(
                imageData = imageData ?: ByteArray(0),
                imagePath = imagePath,
                diseaseDetected = scanResult.diseaseDetected.takeIf { it.isNotEmpty() } ?: "Unknown",
                severity = scanResult.severity.takeIf { it.isNotEmpty() } ?: "Unknown",
                confidence = scanResult.confidence.coerceIn(0f, 100f),
                description = scanResult.description,
                recommendations = scanResult.recommendations.filterNot { it.isBlank() },
                treatmentOptions = scanResult.treatmentOptions.filterNot { it.isBlank() },
                preventionMeasures = scanResult.preventionMeasures.filterNot { it.isBlank() },
                timestamp = Date(scanResult.timestamp.coerceAtLeast(0)),
                quality = scanResult.quality.takeIf { it.isNotEmpty() } ?: "Unknown"
            )
            
            val id = analysisDao.insertAnalysis(analysisEntity)
            Log.d("HistoryRepository", "Successfully saved to database with ID: $id")
            
            // Clean up old analyses to prevent database bloat
            cleanupOldAnalyses()
            
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to save to history", e)
        }
    }

    fun getHistory(): Flow<List<ScanResult>> {
        return analysisDao.getRecentAnalyses(100)
            .map { analyses ->
                analyses.mapNotNull { entity ->
                    try {
                        val imageUri = if (entity.imageData.isNotEmpty()) {
                            createImageUriFromByteArray(entity.imageData)
                        } else {
                            entity.imagePath ?: ""
                        }

                        ScanResult(
                            imageUrl = imageUri,
                            quality = entity.quality.takeIf { it.isNotEmpty() } ?: "Unknown",
                            confidence = entity.confidence.coerceIn(0f, 100f),
                            timestamp = entity.timestamp.time,
                            diseaseDetected = entity.diseaseDetected.takeIf { it.isNotEmpty() } ?: "Unknown",
                            severity = entity.severity.takeIf { it.isNotEmpty() } ?: "Unknown",
                            description = entity.description,
                            recommendations = entity.recommendations.filterNot { it.isBlank() },
                            treatmentOptions = entity.treatmentOptions.filterNot { it.isBlank() },
                            preventionMeasures = entity.preventionMeasures.filterNot { it.isBlank() }
                        )
                    } catch (e: Exception) {
                        Log.e("HistoryRepository", "Error processing analysis entity: ${entity.id}", e)
                        null
                    }
                }
            }
            .catch { throwable ->
                Log.e("HistoryRepository", "Error getting history from database", throwable)
                emit(emptyList())
            }
    }

    suspend fun deleteFromHistory(scanResult: ScanResult) = withContext(Dispatchers.IO) {
        try {
            val entityToDelete = analysisDao.findAnalysisByTimestamp(Date(scanResult.timestamp))
            entityToDelete?.let { entity ->
                // Delete image file if exists
                entity.imagePath?.let { path ->
                    imageStorageHelper.deleteImageFile(path)
                }
                
                // Delete from database
                analysisDao.deleteAnalysis(entity)
                Log.d("HistoryRepository", "Deleted analysis with ID: ${entity.id}")
            }
            
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to delete from history", e)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            // Get all analyses to clean up image files
            val analyses = analysisDao.getRecentAnalyses(1000).first()
            
            // Delete all image files
            analyses.forEach { entity ->
                entity.imagePath?.let { path ->
                    imageStorageHelper.deleteImageFile(path)
                }
            }
            
            // Clear database
            analysisDao.deleteAllAnalyses()
            
            // Clean up image directory
            imageStorageHelper.cleanupOldImages(0) // Delete all images
            
            Log.d("HistoryRepository", "Cleared all history and images")
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to clear history", e)
        }
    }

    suspend fun getAnalysisStatistics() = withContext(Dispatchers.IO) {
        try {
            val diseaseStats = analysisDao.getDiseaseStatistics()
            val severityStats = analysisDao.getSeverityStatistics()
            val averageConfidence = analysisDao.getAverageConfidence() ?: 0f
            val totalCount = analysisDao.getAnalysisCount()
            
            mapOf(
                "diseases" to diseaseStats,
                "severities" to severityStats,
                "averageConfidence" to averageConfidence,
                "totalCount" to totalCount
            )
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to get statistics", e)
            emptyMap<String, Any>()
        }
    }

    fun getImageBitmap(scanResult: ScanResult): Bitmap? {
        return try {
            // Try to load from file path first
            if (scanResult.imageUrl.startsWith("/") && scanResult.imageUrl.contains("analysis_images")) {
                imageStorageHelper.loadImageFromInternalStorage(scanResult.imageUrl)
            } else {
                // If it's a byte array URI, decode it
                // This is a simplified approach - you might want to implement proper URI handling
                null
            }
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to load image bitmap", e)
            null
        }
    }

    suspend fun getScanResultByTimestamp(timestamp: Long): ScanResult? = withContext(Dispatchers.IO) {
        try {
            val entity = analysisDao.findAnalysisByTimestamp(Date(timestamp))
            entity?.let { 
                val bitmap = imageStorageHelper.byteArrayToBitmap(it.imageData)
                ScanResult(
                    imageUrl = it.imagePath ?: "",
                    quality = it.quality,
                    confidence = it.confidence,
                    timestamp = it.timestamp.time,
                    diseaseDetected = it.diseaseDetected,
                    severity = it.severity,
                    description = it.description,
                    recommendations = it.recommendations,
                    treatmentOptions = it.treatmentOptions,
                    preventionMeasures = it.preventionMeasures,
                    imageBitmap = bitmap
                )
            }
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to get scan result by timestamp", e)
            null
        }
    }

    private suspend fun cleanupOldAnalyses() {
        try {
            val totalCount = analysisDao.getAnalysisCount()
            if (totalCount > 100) {
                // Keep only the latest 100 analyses
                val cutoffDate = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days
                analysisDao.deleteOldAnalyses(cutoffDate)
            }
            
            // Clean up old image files
            imageStorageHelper.cleanupOldImages(30)
            
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to cleanup old analyses", e)
        }
    }

    private suspend fun createImageUriFromByteArray(byteArray: ByteArray): String {
        return try {
            if (byteArray.isEmpty()) return ""
            
            // Validate the byte array is a valid image
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
            
            if (options.outWidth == -1 || options.outHeight == -1) {
                Log.w("HistoryRepository", "Invalid image data in byte array")
                return ""
            }
            
            // Create a temporary file to store the image
            val tempFile = File(context.cacheDir, "temp_analysis_${System.currentTimeMillis()}.jpg")
            tempFile.writeBytes(byteArray)
            
            // Return the file URI
            "file://${tempFile.absolutePath}"
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to create image URI from byte array", e)
            ""
        }
    }
}
