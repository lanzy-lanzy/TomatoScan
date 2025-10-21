package com.ml.tomatoscan.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.max
import kotlin.math.min

class ImagePreprocessor {
    
    companion object {
        private const val TARGET_SIZE = 512
        private const val JPEG_QUALITY = 90
        
        /**
         * Preprocesses image for consistent analysis results
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun preprocessForAnalysis(bitmap: Bitmap): Bitmap {
            return bitmap
                .let { convertToSoftwareBitmap(it) }
                .let { resizeToStandardSize(it) }
                .let { normalizeColors(it) }
                .let { enhanceContrast(it) }
        }
        
        /**
         * Convert HARDWARE bitmap to software bitmap for pixel access
         */
        @RequiresApi(Build.VERSION_CODES.O)
        private fun convertToSoftwareBitmap(bitmap: Bitmap): Bitmap {
            // Check if bitmap is already in a software config
            if (bitmap.config != Bitmap.Config.HARDWARE) {
                return bitmap
            }
            
            // Convert HARDWARE bitmap to ARGB_8888 for pixel access
            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            return softwareBitmap ?: bitmap
        }
        
        /**
         * Resize image to standard dimensions while maintaining aspect ratio
         */
        private fun resizeToStandardSize(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            
            // Calculate scaling factor to fit within TARGET_SIZE while maintaining aspect ratio
            val scaleFactor = min(TARGET_SIZE.toFloat() / width, TARGET_SIZE.toFloat() / height)
            
            val newWidth = (width * scaleFactor).toInt()
            val newHeight = (height * scaleFactor).toInt()
            
            val matrix = Matrix().apply {
                setScale(scaleFactor, scaleFactor)
            }
            
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }
        
        /**
         * Normalize colors for consistent lighting conditions
         */
        private fun normalizeColors(bitmap: Bitmap): Bitmap {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint()
            
            // Apply color normalization matrix
            val colorMatrix = ColorMatrix().apply {
                // Slight contrast enhancement and brightness normalization
                set(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 10f,     // Red
                    0f, 1.1f, 0f, 0f, 10f,     // Green  
                    0f, 0f, 1.1f, 0f, 10f,     // Blue
                    0f, 0f, 0f, 1f, 0f         // Alpha
                ))
            }
            
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            return result
        }
        
        /**
         * Enhance contrast for better feature detection
         */
        private fun enhanceContrast(bitmap: Bitmap): Bitmap {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint()
            
            // Apply contrast enhancement
            val colorMatrix = ColorMatrix().apply {
                val contrast = 1.2f
                val brightness = 0f
                
                set(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            return result
        }
        
        /**
         * Generate a hash for image content to detect duplicates
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun generateImageHash(bitmap: Bitmap): String {
            // Convert to software bitmap if needed
            val softwareBitmap = convertToSoftwareBitmap(bitmap)
            
            // Create a small thumbnail for hashing
            val thumbnail = Bitmap.createScaledBitmap(softwareBitmap, 8, 8, false)
            val pixels = IntArray(64)
            thumbnail.getPixels(pixels, 0, 8, 0, 0, 8, 8)
            
            // Convert to grayscale and create hash
            val grayscale = pixels.map { pixel ->
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF  
                val b = pixel and 0xFF
                (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
            
            val average = grayscale.average()
            val hash = grayscale.map { if (it > average) "1" else "0" }.joinToString("")
            
            return hash
        }
    }
}