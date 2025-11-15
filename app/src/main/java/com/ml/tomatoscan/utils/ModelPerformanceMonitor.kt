package com.ml.tomatoscan.utils

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Monitors and tracks model performance metrics for analysis and optimization.
 * 
 * Tracks:
 * - Inference times (detection, classification, total)
 * - Confidence score distributions
 * - Error rates and types
 * - Memory usage patterns
 * 
 * Based on training approach with dual-layer augmentation:
 * - Pre-training: 7x dataset expansion
 * - Runtime: Rotation ±15°, flip, scale, HSV jitter
 * 
 * This helps validate that the model performs as expected in production.
 */
object ModelPerformanceMonitor {
    private const val TAG = "ModelPerformance"
    private const val MAX_SAMPLES = 100 // Keep last 100 samples
    
    private val detectionTimes = ConcurrentLinkedQueue<Long>()
    private val classificationTimes = ConcurrentLinkedQueue<Long>()
    private val totalTimes = ConcurrentLinkedQueue<Long>()
    private val confidenceScores = ConcurrentLinkedQueue<Float>()
    
    private var totalInferences = 0L
    private var successfulInferences = 0L
    private var failedInferences = 0L
    
    /**
     * Records a detection inference time
     */
    fun recordDetectionTime(timeMs: Long) {
        detectionTimes.offer(timeMs)
        if (detectionTimes.size > MAX_SAMPLES) {
            detectionTimes.poll()
        }
    }
    
    /**
     * Records a classification inference time
     */
    fun recordClassificationTime(timeMs: Long) {
        classificationTimes.offer(timeMs)
        if (classificationTimes.size > MAX_SAMPLES) {
            classificationTimes.poll()
        }
    }
    
    /**
     * Records total pipeline time
     */
    fun recordTotalTime(timeMs: Long) {
        totalTimes.offer(timeMs)
        if (totalTimes.size > MAX_SAMPLES) {
            totalTimes.poll()
        }
    }
    
    /**
     * Records a confidence score
     */
    fun recordConfidence(confidence: Float) {
        confidenceScores.offer(confidence)
        if (confidenceScores.size > MAX_SAMPLES) {
            confidenceScores.poll()
        }
    }
    
    /**
     * Records a successful inference
     */
    fun recordSuccess() {
        totalInferences++
        successfulInferences++
    }
    
    /**
     * Records a failed inference
     */
    fun recordFailure() {
        totalInferences++
        failedInferences++
    }
    
    /**
     * Gets performance statistics
     */
    fun getStatistics(): PerformanceStats {
        val detectionList = detectionTimes.toList()
        val classificationList = classificationTimes.toList()
        val totalList = totalTimes.toList()
        val confidenceList = confidenceScores.toList()
        
        return PerformanceStats(
            avgDetectionTimeMs = detectionList.average().takeIf { !it.isNaN() } ?: 0.0,
            avgClassificationTimeMs = classificationList.average().takeIf { !it.isNaN() } ?: 0.0,
            avgTotalTimeMs = totalList.average().takeIf { !it.isNaN() } ?: 0.0,
            avgConfidence = confidenceList.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f,
            minConfidence = confidenceList.minOrNull() ?: 0f,
            maxConfidence = confidenceList.maxOrNull() ?: 0f,
            totalInferences = totalInferences,
            successRate = if (totalInferences > 0) {
                (successfulInferences.toFloat() / totalInferences.toFloat())
            } else 0f,
            sampleSize = totalList.size
        )
    }
    
    /**
     * Logs current performance statistics
     */
    fun logStatistics() {
        val stats = getStatistics()
        Log.i(TAG, "=== Model Performance Statistics ===")
        Log.i(TAG, "Avg Detection Time: ${stats.avgDetectionTimeMs.toInt()}ms")
        Log.i(TAG, "Avg Classification Time: ${stats.avgClassificationTimeMs.toInt()}ms")
        Log.i(TAG, "Avg Total Time: ${stats.avgTotalTimeMs.toInt()}ms")
        Log.i(TAG, "Avg Confidence: ${String.format("%.2f", stats.avgConfidence * 100)}%")
        Log.i(TAG, "Confidence Range: ${String.format("%.2f", stats.minConfidence * 100)}% - ${String.format("%.2f", stats.maxConfidence * 100)}%")
        Log.i(TAG, "Success Rate: ${String.format("%.1f", stats.successRate * 100)}%")
        Log.i(TAG, "Total Inferences: ${stats.totalInferences} (${stats.sampleSize} samples)")
        Log.i(TAG, "===================================")
    }
    
    /**
     * Resets all statistics
     */
    fun reset() {
        detectionTimes.clear()
        classificationTimes.clear()
        totalTimes.clear()
        confidenceScores.clear()
        totalInferences = 0
        successfulInferences = 0
        failedInferences = 0
        Log.d(TAG, "Performance statistics reset")
    }
}

/**
 * Performance statistics data class
 */
data class PerformanceStats(
    val avgDetectionTimeMs: Double,
    val avgClassificationTimeMs: Double,
    val avgTotalTimeMs: Double,
    val avgConfidence: Float,
    val minConfidence: Float,
    val maxConfidence: Float,
    val totalInferences: Long,
    val successRate: Float,
    val sampleSize: Int
)
