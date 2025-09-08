package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageStorageHelper(private val context: Context) {
    
    private val imageDirectory: File by lazy {
        File(context.filesDir, "analysis_images").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Image directory for external access
     */
    fun getAnalysisImageDirectory(): File = imageDirectory
    
    /**
     * Convert image URI to byte array for database storage
     */
    suspend fun uriToByteArray(uri: Uri, maxWidth: Int = 800, maxHeight: Int = 600): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                Log.e("ImageStorageHelper", "Failed to decode bitmap from URI: $uri")
                return null
            }
            
            // Resize bitmap to reduce storage size
            val resizedBitmap = resizeBitmap(originalBitmap, maxWidth, maxHeight)
            
            // Compress to byte array
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            
            // Clean up
            outputStream.close()
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()
            
            Log.d("ImageStorageHelper", "Converted image to byte array: ${byteArray.size} bytes")
            byteArray
            
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to convert URI to byte array", e)
            null
        }
    }
    
    /**
     * Convert bitmap to byte array for database storage
     */
    fun bitmapToByteArray(bitmap: Bitmap, maxWidth: Int = 800, maxHeight: Int = 600): ByteArray? {
        return try {
            // Resize bitmap to reduce storage size
            val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            
            // Compress to byte array
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            
            // Clean up
            outputStream.close()
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            Log.d("ImageStorageHelper", "Converted bitmap to byte array: ${byteArray.size} bytes")
            byteArray
            
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to convert bitmap to byte array", e)
            null
        }
    }
    
    /**
     * Convert byte array back to bitmap for display
     */
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to convert byte array to bitmap", e)
            null
        }
    }
    
    /**
     * Save image to internal storage and return file path
     */
    suspend fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) {
                Log.e("ImageStorageHelper", "Failed to decode bitmap from URI: $uri")
                return null
            }
            
            val filename = "analysis_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
            val file = File(imageDirectory, filename)
            
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
            bitmap.recycle()
            
            Log.d("ImageStorageHelper", "Saved image to: ${file.absolutePath}")
            file.absolutePath
            
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to save image to internal storage", e)
            null
        }
    }
    
    /**
     * Load bitmap from internal storage file path
     */
    fun loadImageFromInternalStorage(filePath: String): Bitmap? {
        return try {
            if (File(filePath).exists()) {
                BitmapFactory.decodeFile(filePath)
            } else {
                Log.w("ImageStorageHelper", "Image file not found: $filePath")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to load image from internal storage", e)
            null
        }
    }
    
    /**
     * Delete image file from internal storage
     */
    fun deleteImageFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true // File doesn't exist, consider it deleted
            }
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to delete image file", e)
            false
        }
    }
    
    /**
     * Clean up old image files to free storage space
     */
    fun cleanupOldImages(daysToKeep: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            
            imageDirectory.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                    Log.d("ImageStorageHelper", "Deleted old image: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to cleanup old images", e)
        }
    }
    
    /**
     * Get total storage used by images
     */
    fun getStorageUsed(): Long {
        return try {
            imageDirectory.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            Log.e("ImageStorageHelper", "Failed to calculate storage used", e)
            0L
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (aspectRatio > 1) {
            // Landscape
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            // Portrait
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
