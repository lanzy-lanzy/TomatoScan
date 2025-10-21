package com.ml.tomatoscan.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

data class ImageQualityReport(
    val isValid: Boolean,
    val score: Float, // 0-100
    val issues: List<String>,
    val suggestions: List<String>
)

class ImageQualityValidator {
    
    companion object {
        private const val MIN_RESOLUTION = 200
        private const val MIN_BRIGHTNESS_THRESHOLD = 30
        private const val MAX_BRIGHTNESS_THRESHOLD = 225
        private const val MIN_CONTRAST_THRESHOLD = 20
        
        /**
         * Validate image quality for consistent analysis
         */
        fun validateImageQuality(bitmap: Bitmap): ImageQualityReport {
            // Convert HARDWARE bitmap to software bitmap for pixel access
            val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            
            val issues = mutableListOf<String>()
            val suggestions = mutableListOf<String>()
            var score = 100f
            
            // Check resolution
            if (softwareBitmap.width < MIN_RESOLUTION || softwareBitmap.height < MIN_RESOLUTION) {
                issues.add("Image resolution too low (${softwareBitmap.width}x${softwareBitmap.height})")
                suggestions.add("Use a higher resolution camera or move closer to the leaf")
                score -= 30f
            }
            
            // Analyze brightness and contrast
            val brightnessStats = analyzeBrightness(softwareBitmap)
            val contrastScore = analyzeContrast(softwareBitmap)
            
            // Check brightness
            when {
                brightnessStats.average < MIN_BRIGHTNESS_THRESHOLD -> {
                    issues.add("Image too dark (brightness: ${brightnessStats.average.toInt()})")
                    suggestions.add("Increase lighting or use flash")
                    score -= 25f
                }
                brightnessStats.average > MAX_BRIGHTNESS_THRESHOLD -> {
                    issues.add("Image too bright (brightness: ${brightnessStats.average.toInt()})")
                    suggestions.add("Reduce lighting or avoid direct sunlight")
                    score -= 25f
                }
            }
            
            // Check contrast
            if (contrastScore < MIN_CONTRAST_THRESHOLD) {
                issues.add("Low contrast detected")
                suggestions.add("Ensure good lighting conditions and clear background")
                score -= 20f
            }
            
            // Check for blur (basic edge detection)
            val sharpnessScore = analyzeSharpness(softwareBitmap)
            if (sharpnessScore < 0.3f) {
                issues.add("Image appears blurry")
                suggestions.add("Hold camera steady and ensure proper focus")
                score -= 15f
            }
            
            // Check color distribution (ensure it's not monochrome)
            val colorVariance = analyzeColorVariance(softwareBitmap)
            if (colorVariance < 10f) {
                issues.add("Limited color information")
                suggestions.add("Ensure proper white balance and natural lighting")
                score -= 10f
            }
            
            val isValid = score >= 60f && issues.isEmpty()
            
            return ImageQualityReport(
                isValid = isValid,
                score = maxOf(0f, score),
                issues = issues,
                suggestions = suggestions
            )
        }
        
        private data class BrightnessStats(
            val average: Float,
            val min: Int,
            val max: Int
        )
        
        private fun analyzeBrightness(bitmap: Bitmap): BrightnessStats {
            // Use a smaller sample to avoid OOM
            val sampleBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            val pixels = IntArray(10000)
            sampleBitmap.getPixels(pixels, 0, 100, 0, 0, 100, 100)
            
            var sum = 0f
            var min = 255
            var max = 0
            
            for (pixel in pixels) {
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                sum += brightness
                min = minOf(min, brightness)
                max = maxOf(max, brightness)
            }
            
            sampleBitmap.recycle()
            return BrightnessStats(
                average = sum / pixels.size,
                min = min,
                max = max
            )
        }
        
        private fun analyzeContrast(bitmap: Bitmap): Float {
            // Use a smaller sample to avoid OOM
            val sampleBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            val pixels = IntArray(10000)
            sampleBitmap.getPixels(pixels, 0, 100, 0, 0, 100, 100)
            
            val brightness = FloatArray(pixels.size)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                brightness[i] = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3f
            }
            
            var sum = 0.0
            for (b in brightness) {
                sum += b
            }
            val mean = sum / brightness.size
            
            var varianceSum = 0.0
            for (b in brightness) {
                val diff = b - mean
                varianceSum += diff * diff
            }
            val variance = varianceSum / brightness.size
            
            sampleBitmap.recycle()
            return sqrt(variance).toFloat()
        }
        
        private fun analyzeSharpness(bitmap: Bitmap): Float {
            // Simple edge detection for sharpness estimation
            val smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            val pixels = IntArray(10000)
            smallBitmap.getPixels(pixels, 0, 100, 0, 0, 100, 100)
            
            var edgeSum = 0f
            for (y in 1 until 99) {
                for (x in 1 until 99) {
                    val current = pixels[y * 100 + x]
                    val right = pixels[y * 100 + x + 1]
                    val bottom = pixels[(y + 1) * 100 + x]
                    
                    val currentGray = (Color.red(current) + Color.green(current) + Color.blue(current)) / 3
                    val rightGray = (Color.red(right) + Color.green(right) + Color.blue(right)) / 3
                    val bottomGray = (Color.red(bottom) + Color.green(bottom) + Color.blue(bottom)) / 3
                    
                    val edgeStrength = kotlin.math.abs(currentGray - rightGray) + kotlin.math.abs(currentGray - bottomGray)
                    edgeSum += edgeStrength
                }
            }
            
            return edgeSum / (98 * 98 * 255 * 2) // Normalize to 0-1
        }
        
        private fun analyzeColorVariance(bitmap: Bitmap): Float {
            val smallBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            val pixels = IntArray(2500)
            smallBitmap.getPixels(pixels, 0, 50, 0, 0, 50, 50)
            
            val reds = pixels.map { Color.red(it) }
            val greens = pixels.map { Color.green(it) }
            val blues = pixels.map { Color.blue(it) }
            
            val redVariance = calculateVariance(reds)
            val greenVariance = calculateVariance(greens)
            val blueVariance = calculateVariance(blues)
            
            return (redVariance + greenVariance + blueVariance) / 3f
        }
        
        private fun calculateVariance(values: List<Int>): Float {
            val mean = values.average()
            return values.map { (it - mean) * (it - mean) }.average().toFloat()
        }
    }
}