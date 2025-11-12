package com.ml.tomatoscan.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.ml.tomatoscan.config.CacheConfig
import kotlin.math.sqrt

/**
 * Perceptual hashing utility for generating image fingerprints.
 * Uses the pHash (perceptual hash) algorithm to create a hash that is
 * resistant to minor changes in the image (compression, scaling, slight color changes).
 *
 * This allows us to identify similar images and return consistent results.
 */
object PerceptualHash {
    
    /**
     * Generates a perceptual hash for the given bitmap.
     * The hash is a string representation that can be used to identify similar images.
     *
     * Algorithm:
     * 1. Resize image to small size (8x8 or configurable)
     * 2. Convert to grayscale
     * 3. Compute Discrete Cosine Transform (DCT)
     * 4. Extract low-frequency components
     * 5. Compute average value
     * 6. Generate hash based on values above/below average
     *
     * @param bitmap The input image
     * @return A hash string representing the image fingerprint
     */
    fun generateHash(bitmap: Bitmap): String {
        val size = CacheConfig.PHASH_SIZE
        
        // Step 1: Resize to small size
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        
        // Step 2: Convert to grayscale
        val grayscale = convertToGrayscale(resized)
        
        // Step 3: Compute DCT
        val dct = computeDCT(grayscale, size)
        
        // Step 4: Extract low-frequency components (top-left corner, excluding DC component)
        val lowFreq = extractLowFrequency(dct, size)
        
        // Step 5: Compute average
        val average = lowFreq.average()
        
        // Step 6: Generate hash (1 if above average, 0 if below)
        val hash = StringBuilder()
        for (value in lowFreq) {
            hash.append(if (value > average) "1" else "0")
        }
        
        return hash.toString()
    }
    
    /**
     * Calculates the similarity between two perceptual hashes.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     *
     * Uses Hamming distance to compare the hashes.
     *
     * @param hash1 First hash string
     * @param hash2 Second hash string
     * @return Similarity score between 0.0 and 1.0
     */
    fun calculateSimilarity(hash1: String, hash2: String): Float {
        if (hash1.length != hash2.length) {
            return 0.0f
        }
        
        // Calculate Hamming distance
        var differences = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) {
                differences++
            }
        }
        
        // Convert to similarity score (1.0 = identical, 0.0 = completely different)
        return 1.0f - (differences.toFloat() / hash1.length.toFloat())
    }
    
    /**
     * Checks if two hashes are similar enough to be considered a match.
     *
     * @param hash1 First hash string
     * @param hash2 Second hash string
     * @param threshold Similarity threshold (default from CacheConfig)
     * @return True if hashes are similar enough
     */
    fun areSimilar(
        hash1: String,
        hash2: String,
        threshold: Float = CacheConfig.HASH_SIMILARITY_THRESHOLD
    ): Boolean {
        return calculateSimilarity(hash1, hash2) >= threshold
    }
    
    /**
     * Converts a bitmap to grayscale values.
     *
     * @param bitmap Input bitmap
     * @return 2D array of grayscale values (0-255)
     */
    private fun convertToGrayscale(bitmap: Bitmap): Array<DoubleArray> {
        val width = bitmap.width
        val height = bitmap.height
        val grayscale = Array(height) { DoubleArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // Standard grayscale conversion formula
                val gray = 0.299 * r + 0.587 * g + 0.114 * b
                grayscale[y][x] = gray
            }
        }
        
        return grayscale
    }
    
    /**
     * Computes the Discrete Cosine Transform (DCT) of the grayscale image.
     * This extracts frequency information from the image.
     *
     * @param grayscale 2D array of grayscale values
     * @param size Size of the image (assumed square)
     * @return 2D array of DCT coefficients
     */
    private fun computeDCT(grayscale: Array<DoubleArray>, size: Int): Array<DoubleArray> {
        val dct = Array(size) { DoubleArray(size) }
        
        for (u in 0 until size) {
            for (v in 0 until size) {
                var sum = 0.0
                
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        sum += grayscale[y][x] *
                                Math.cos((2 * x + 1) * u * Math.PI / (2.0 * size)) *
                                Math.cos((2 * y + 1) * v * Math.PI / (2.0 * size))
                    }
                }
                
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                
                dct[u][v] = 0.25 * cu * cv * sum
            }
        }
        
        return dct
    }
    
    /**
     * Extracts low-frequency components from the DCT result.
     * These components represent the overall structure of the image
     * and are less sensitive to minor changes.
     *
     * @param dct DCT coefficients
     * @param size Size of the DCT matrix
     * @return List of low-frequency values
     */
    private fun extractLowFrequency(dct: Array<DoubleArray>, size: Int): List<Double> {
        val lowFreq = mutableListOf<Double>()
        
        // Extract top-left corner (low frequencies), excluding DC component at [0][0]
        val extractSize = size / 2
        for (u in 0 until extractSize) {
            for (v in 0 until extractSize) {
                if (u != 0 || v != 0) { // Skip DC component
                    lowFreq.add(dct[u][v])
                }
            }
        }
        
        return lowFreq
    }
}
