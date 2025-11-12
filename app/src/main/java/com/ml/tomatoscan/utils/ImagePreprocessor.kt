package com.ml.tomatoscan.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import com.ml.tomatoscan.config.ModelConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Quality report for image validation
 */
data class QualityReport(
    val isValid: Boolean,
    val score: Float,
    val issues: List<String>
)

class ImagePreprocessor {
    
    companion object {
        private const val TARGET_SIZE = 512
        private const val JPEG_QUALITY = 90
        
        // Quality validation thresholds
        private const val MIN_RESOLUTION = 224
        private const val MIN_BRIGHTNESS = 30f
        private const val MAX_BRIGHTNESS = 225f
        private const val MIN_LAPLACIAN_VARIANCE = 100.0 // Threshold for blur detection
        
        /**
         * Preprocesses image for YOLOv11 detection (640x640)
         * @param bitmap Input image
         * @return Preprocessed 640x640 bitmap
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun preprocessForDetection(bitmap: Bitmap): Bitmap {
            return bitmap
                .let { convertToSoftwareBitmap(it) }
                .let { resizeToSquare(it, ModelConfig.YOLO_INPUT_SIZE) }
                .let { normalizeBrightnessAndContrast(it) }
        }
        
        /**
         * Preprocesses image for TFLite classification (224x224)
         * @param bitmap Cropped leaf image
         * @return Preprocessed 224x224 bitmap
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun preprocessForClassification(bitmap: Bitmap): Bitmap {
            return bitmap
                .let { convertToSoftwareBitmap(it) }
                .let { resizeToSquare(it, ModelConfig.TFLITE_INPUT_SIZE) }
                .let { normalizeBrightnessAndContrast(it) }
        }
        
        /**
         * Preprocesses image for consistent analysis results (legacy method)
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
         * Resize image to square dimensions (for YOLO/TFLite input)
         */
        private fun resizeToSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
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
         * Normalize brightness and contrast for consistent model input
         */
        private fun normalizeBrightnessAndContrast(bitmap: Bitmap): Bitmap {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint()
            
            // Apply normalization: slight contrast boost and brightness adjustment
            val colorMatrix = ColorMatrix().apply {
                val contrast = 1.15f
                val brightness = 5f
                
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
         * Validates image quality for analysis
         * @param bitmap Input image
         * @return QualityReport with validation results
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun validateQuality(bitmap: Bitmap): QualityReport {
            val issues = mutableListOf<String>()
            var qualityScore = 100f
            
            // Check minimum resolution
            if (bitmap.width < MIN_RESOLUTION || bitmap.height < MIN_RESOLUTION) {
                issues.add("Image resolution too low (minimum ${MIN_RESOLUTION}x${MIN_RESOLUTION})")
                qualityScore -= 40f
            }
            
            // Convert to software bitmap for pixel access
            val softwareBitmap = convertToSoftwareBitmap(bitmap)
            
            // Check brightness levels
            val brightness = calculateAverageBrightness(softwareBitmap)
            if (brightness < MIN_BRIGHTNESS) {
                issues.add("Image too dark (brightness: ${brightness.toInt()})")
                qualityScore -= 30f
            } else if (brightness > MAX_BRIGHTNESS) {
                issues.add("Image too bright (brightness: ${brightness.toInt()})")
                qualityScore -= 20f
            }
            
            // Detect blur using Laplacian variance
            val blurScore = calculateBlurScore(softwareBitmap)
            if (blurScore < MIN_LAPLACIAN_VARIANCE) {
                issues.add("Image appears blurry (sharpness score: ${blurScore.toInt()})")
                qualityScore -= 30f
            }
            
            // Ensure score doesn't go below 0
            qualityScore = max(0f, qualityScore)
            
            // Image is valid if score is above 50
            val isValid = qualityScore >= 50f
            
            return QualityReport(
                isValid = isValid,
                score = qualityScore,
                issues = issues
            )
        }
        
        /**
         * Calculate average brightness of the image
         */
        private fun calculateAverageBrightness(bitmap: Bitmap): Float {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            var totalBrightness = 0.0
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Calculate perceived brightness using standard formula
                val brightness = 0.299 * r + 0.587 * g + 0.114 * b
                totalBrightness += brightness
            }
            
            return (totalBrightness / pixels.size).toFloat()
        }
        
        /**
         * Calculate blur score using simple edge detection
         * Higher values indicate sharper images
         */
        private fun calculateBlurScore(bitmap: Bitmap): Double {
            return calculateSimpleSharpness(bitmap)
        }
        
        /**
         * Fallback sharpness calculation without OpenCV
         * Uses simple edge detection on a sample of pixels
         */
        private fun calculateSimpleSharpness(bitmap: Bitmap): Double {
            val width = bitmap.width
            val height = bitmap.height
            
            // Sample a smaller region for performance
            val sampleSize = min(width, height) / 4
            val startX = width / 2 - sampleSize / 2
            val startY = height / 2 - sampleSize / 2
            
            val pixels = IntArray(sampleSize * sampleSize)
            bitmap.getPixels(pixels, 0, sampleSize, startX, startY, sampleSize, sampleSize)
            
            // Calculate edge strength using simple gradient
            var edgeSum = 0.0
            for (y in 0 until sampleSize - 1) {
                for (x in 0 until sampleSize - 1) {
                    val idx = y * sampleSize + x
                    val current = getGrayscale(pixels[idx])
                    val right = getGrayscale(pixels[idx + 1])
                    val down = getGrayscale(pixels[idx + sampleSize])
                    
                    val dx = kotlin.math.abs(current - right)
                    val dy = kotlin.math.abs(current - down)
                    edgeSum += sqrt((dx * dx + dy * dy).toDouble())
                }
            }
            
            return edgeSum / (sampleSize * sampleSize)
        }
        
        /**
         * Convert pixel to grayscale value
         */
        private fun getGrayscale(pixel: Int): Int {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
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