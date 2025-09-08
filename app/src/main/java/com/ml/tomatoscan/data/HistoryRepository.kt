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
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*



class HistoryRepository(private val context: Context) {

    private val database = TomatoScanDatabase.getDatabase(context)
    private val analysisDao = database.analysisDao()
    private val imageStorageHelper = ImageStorageHelper(context)

    init {
        // Perform initial cleanup of old temp files on repository initialization
        try {
            val cacheDir = context.cacheDir
            val tempFiles = cacheDir.listFiles { file ->
                file.name.startsWith("temp_analysis_") && file.name.endsWith(".jpg")
            }
            tempFiles?.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    // Ignore individual file deletion errors
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors during initialization
        }
    }

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
                diseaseDetected = scanResult.diseaseDetected,
                severity = scanResult.severity,
                confidence = scanResult.confidence,
                description = scanResult.description,
                recommendations = scanResult.recommendations,
                treatmentOptions = scanResult.treatmentOptions,
                preventionMeasures = scanResult.preventionMeasures,
                timestamp = Date(scanResult.timestamp),
                quality = scanResult.quality
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
        return analysisDao.getRecentAnalyses(100).map { analyses ->
            analyses.map { entity ->
                val imageUri = if (entity.imageData.isNotEmpty()) {
                    createImageUriFromByteArray(entity.imageData, entity.timestamp.time)
                } else {
                    entity.imagePath ?: ""
                }

                ScanResult(
                    imageUrl = imageUri,
                    quality = entity.quality,
                    confidence = entity.confidence,
                    timestamp = entity.timestamp.time,
                    diseaseDetected = entity.diseaseDetected,
                    severity = entity.severity,
                    description = entity.description,
                    recommendations = entity.recommendations,
                    treatmentOptions = entity.treatmentOptions,
                    preventionMeasures = entity.preventionMeasures
                )
            }
        }
    }

    suspend fun deleteFromHistory(scanResult: ScanResult) = withContext(Dispatchers.IO) {
        try {
            val entityToDelete = analysisDao.findAnalysisByTimestamp(Date(scanResult.timestamp))
            entityToDelete?.let { entity ->
                // Delete image file if exists (legacy path)
                entity.imagePath?.let { path ->
                    imageStorageHelper.deleteImageFile(path)
                }

                // Delete consistent history image file
                val historyImageFile = File(imageStorageHelper.getAnalysisImageDirectory(), "history_image_${entity.timestamp.time}.jpg")
                if (historyImageFile.exists()) {
                    historyImageFile.delete()
                    Log.d("HistoryRepository", "Deleted history image file: ${historyImageFile.name}")
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

            // Delete all image files (legacy paths)
            analyses.forEach { entity ->
                entity.imagePath?.let { path ->
                    imageStorageHelper.deleteImageFile(path)
                }

                // Delete consistent history image files
                val historyImageFile = File(imageStorageHelper.getAnalysisImageDirectory(), "history_image_${entity.timestamp.time}.jpg")
                if (historyImageFile.exists()) {
                    historyImageFile.delete()
                }
            }

            // Clear database
            analysisDao.deleteAllAnalyses()

            // Clean up image directory
            imageStorageHelper.cleanupOldImages(0) // Delete all images

            // Clean up temp files
            cleanupOldTempFiles()

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

            // Clean up old temporary files from cache directory (from previous implementation)
            cleanupOldTempFiles()

        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to cleanup old analyses", e)
        }
    }

    private suspend fun cleanupOldTempFiles() {
        try {
            val cacheDir = context.cacheDir
            val tempFiles = cacheDir.listFiles { file ->
                file.name.startsWith("temp_analysis_") && file.name.endsWith(".jpg")
            }

            tempFiles?.forEach { file ->
                try {
                    if (file.delete()) {
                        Log.d("HistoryRepository", "Deleted old temp file: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.w("HistoryRepository", "Failed to delete temp file: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to cleanup temp files", e)
        }
    }

    private suspend fun createImageUriFromByteArray(byteArray: ByteArray, timestamp: Long): String {
        return try {
            if (byteArray.isEmpty()) return ""

            // Create a consistent filename based on timestamp to avoid recreating files
            val filename = "history_image_${timestamp}.jpg"
            val imageFile = File(imageStorageHelper.getAnalysisImageDirectory(), filename)

            // Only create the file if it doesn't exist
            if (!imageFile.exists()) {
                imageFile.writeBytes(byteArray)
                Log.d("HistoryRepository", "Created image file: ${imageFile.absolutePath}")
            }

            // Return the file URI
            "file://${imageFile.absolutePath}"
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Failed to create image URI from byte array", e)
            ""
        }
    }
}
