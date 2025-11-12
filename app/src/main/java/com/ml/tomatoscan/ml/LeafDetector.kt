package com.ml.tomatoscan.ml

import android.graphics.Bitmap

/**
 * Interface for detecting tomato leaves in images using object detection models.
 * 
 * This interface defines the contract for leaf detection services that identify
 * and extract tomato leaf regions from input images. Implementations should use
 * YOLOv11 or similar object detection models to locate leaves with high accuracy.
 * 
 * Expected behavior:
 * - detectLeaves() should return all detected leaf regions sorted by confidence (highest first)
 * - cropLeaf() should return the highest confidence detection, or null if no leaf is found
 * - Both methods should handle edge cases gracefully (empty images, multiple leaves, etc.)
 * - Detection confidence threshold should be configurable (typically 0.5 or higher)
 * 
 * Requirements addressed:
 * - 1.1: Process images through YOLOv11 detector to identify and crop leaf regions
 * - 1.4: Return error if no leaf region is detected
 */
interface LeafDetector {
    
    /**
     * Detects all tomato leaves in the provided image.
     * 
     * This method analyzes the input image and returns a list of all detected
     * leaf regions, sorted by confidence score in descending order. Each detection
     * includes the bounding box coordinates, confidence score, and cropped image.
     * 
     * @param bitmap The input image to analyze
     * @return List of DetectionResult objects, sorted by confidence (highest first).
     *         Returns empty list if no leaves are detected.
     * @throws IllegalArgumentException if bitmap is invalid or empty
     */
    suspend fun detectLeaves(bitmap: Bitmap): List<DetectionResult>
    
    /**
     * Detects and crops the highest confidence tomato leaf from the image.
     * 
     * This is a convenience method that returns only the best detection result.
     * It's equivalent to calling detectLeaves() and taking the first result,
     * but may be optimized to avoid processing all detections.
     * 
     * The cropped region includes a 10% padding margin around the detected
     * bounding box to ensure the entire leaf is captured, while handling
     * edge cases where the detection is near image boundaries.
     * 
     * @param bitmap The input image to analyze
     * @return The cropped leaf bitmap with highest confidence, or null if no leaf detected
     * @throws IllegalArgumentException if bitmap is invalid or empty
     */
    suspend fun cropLeaf(bitmap: Bitmap): Bitmap?
    
    /**
     * Gets the minimum confidence threshold used for detections.
     * Detections below this threshold are filtered out.
     * 
     * @return The confidence threshold value (typically 0.5)
     */
    fun getConfidenceThreshold(): Float
}
