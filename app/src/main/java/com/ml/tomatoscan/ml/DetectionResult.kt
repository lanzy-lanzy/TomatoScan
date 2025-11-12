package com.ml.tomatoscan.ml

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Data class representing the result of a leaf detection operation.
 * Contains the bounding box coordinates, confidence score, and the cropped leaf image.
 *
 * @property boundingBox The rectangular region where the leaf was detected (normalized coordinates 0-1)
 * @property confidence Detection confidence score between 0.0 and 1.0
 * @property croppedBitmap The cropped image of the detected leaf region
 */
data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    val croppedBitmap: Bitmap,
    val classProbabilities: Map<Int, Float>? = null  // Optional: class probabilities from YOLO
) {
    init {
        require(confidence in 0f..1f) {
            "Confidence must be between 0.0 and 1.0, got $confidence"
        }
    }
    
    /**
     * Returns true if this detection meets the minimum confidence threshold
     * @param threshold Minimum confidence threshold (default 0.5)
     */
    fun meetsThreshold(threshold: Float = 0.5f): Boolean {
        return confidence >= threshold
    }
}
