package com.ml.tomatoscan.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.ml.tomatoscan.config.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * YOLOv11-based implementation of LeafDetector for detecting tomato leaves in images.
 * 
 * This class uses TensorFlow Lite to run a YOLOv11 object detection model that identifies
 * tomato leaf regions in images. It handles preprocessing (640x640 resize), inference,
 * and post-processing with Non-Maximum Suppression (NMS) to filter overlapping detections.
 * 
 * Requirements addressed:
 * - 1.1: Load YOLOv11 model and process images for leaf detection
 * - 1.5: Execute detection within performance constraints
 */
class YoloLeafDetector(private val context: Context) : LeafDetector {
    
    private var interpreter: Interpreter? = null
    private val confidenceThreshold = ModelConfig.DETECTION_CONFIDENCE_THRESHOLD
    private val inputSize = ModelConfig.YOLO_INPUT_SIZE
    private val nmsThreshold = ModelConfig.NMS_IOU_THRESHOLD
    
    companion object {
        private const val TAG = "YoloLeafDetector"
        private const val PIXEL_SIZE = 3 // RGB
        private const val BYTES_PER_CHANNEL = 4 // Float32
        private const val NUM_THREADS = 4
    }
    
    init {
        loadModel()
    }
    
    /**
     * Loads the YOLOv11 model from assets
     */
    private fun loadModel() {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, ModelConfig.YOLO_MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)
                setUseNNAPI(false) // Disable NNAPI for better compatibility
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "YOLOv11 model loaded successfully from ${ModelConfig.YOLO_MODEL_PATH}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading YOLOv11 model: ${e.message}", e)
            Log.w(TAG, "YOLOv11 detector will not function until model is available at ${ModelConfig.YOLO_MODEL_PATH}")
            // Don't throw exception - allow app to continue with degraded functionality
            interpreter = null
        }
    }

    
    /**
     * Detects all tomato leaves in the provided image.
     * Performs preprocessing, inference, and NMS post-processing.
     */
    override suspend fun detectLeaves(bitmap: Bitmap): List<DetectionResult> = withContext(Dispatchers.Default) {
        require(bitmap.width > 0 && bitmap.height > 0) {
            "Invalid bitmap: width=${bitmap.width}, height=${bitmap.height}"
        }
        
        val interpreter = interpreter ?: throw IllegalStateException("Model not loaded")
        
        try {
            // Preprocess image to 640x640
            val preprocessedBitmap = preprocessForDetection(bitmap)
            val inputBuffer = bitmapToByteBuffer(preprocessedBitmap)
            
            // Prepare output buffer
            // YOLOv11 output format: [1, num_classes+4, num_detections] 
            // For example: [1, 10, 8400] where 10 = 4 bbox coords + 6 class scores
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.d(TAG, "Model output shape: [${outputShape.joinToString(", ")}]")
            
            val numValues = outputShape[1]  // e.g., 10
            val numDetections = outputShape[2]  // e.g., 8400
            val outputBuffer = Array(1) { Array(numValues) { FloatArray(numDetections) } }
            
            // Run inference
            val startTime = System.currentTimeMillis()
            interpreter.run(inputBuffer, outputBuffer)
            val inferenceTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "YOLOv11 inference completed in ${inferenceTime}ms")
            
            // Parse detections (transpose the output)
            val detections = parseDetectionsTransposed(outputBuffer[0], bitmap)
            
            // Apply Non-Maximum Suppression
            val filteredDetections = applyNMS(detections)
            
            Log.d(TAG, "Detected ${filteredDetections.size} leaves after NMS")
            filteredDetections
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during leaf detection: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Crops the highest confidence leaf from the image
     */
    override suspend fun cropLeaf(bitmap: Bitmap): Bitmap? {
        val detections = detectLeaves(bitmap)
        return detections.firstOrNull()?.croppedBitmap
    }
    
    override fun getConfidenceThreshold(): Float = confidenceThreshold

    
    /**
     * Preprocesses bitmap for YOLOv11 detection (640x640 resize)
     */
    private fun preprocessForDetection(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
    }
    
    /**
     * Converts bitmap to ByteBuffer for model input
     * Normalizes pixel values to [0, 1] range
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            BYTES_PER_CHANNEL * inputSize * inputSize * PIXEL_SIZE
        )
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                
                // Extract RGB values and normalize to [0, 1]
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f) // R
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)  // G
                byteBuffer.putFloat((value and 0xFF) / 255.0f)          // B
            }
        }
        
        return byteBuffer
    }

    
    /**
     * Parses transposed YOLOv11 output into DetectionResult objects
     * Output format: [num_values, num_detections] where values are [x, y, w, h, class_scores...]
     */
    private fun parseDetectionsTransposed(
        output: Array<FloatArray>,
        originalBitmap: Bitmap
    ): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()
        
        val numDetections = output[0].size
        Log.d(TAG, "Total detections from model: $numDetections")
        
        var maxConfidence = 0f
        var detectionsAboveThreshold = 0
        
        // Iterate through each detection
        for (i in 0 until numDetections) {
            // Extract bounding box coordinates
            // YOLOv11 outputs [x1, y1, x2, y2] format (corner coordinates), not center format
            val x1 = output[0][i]
            val y1 = output[1][i]
            val x2 = output[2][i]
            val y2 = output[3][i]
            
            // Get class scores (indices 4 onwards)
            // Find the class with highest confidence
            var confidence = 0f
            var bestClassIdx = 4
            for (j in 4 until output.size) {
                if (output[j][i] > confidence) {
                    confidence = output[j][i]
                    bestClassIdx = j
                }
            }
            
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                // Log the best detection for debugging with all values
                if (i < 3) {  // Only log first few
                    Log.d(TAG, "Detection $i: [0]=${output[0][i]}, [1]=${output[1][i]}, [2]=${output[2][i]}, [3]=${output[3][i]}, [4]=${output[4][i]}, [5]=${output[5][i]}, bestClass=$bestClassIdx, conf=$confidence")
                }
            }
            
            // Filter by confidence threshold
            if (confidence < confidenceThreshold) continue
            
            detectionsAboveThreshold++
            
            // Coordinates are already in corner format [x1, y1, x2, y2]
            val left = x1.coerceIn(0f, 1f)
            val top = y1.coerceIn(0f, 1f)
            val right = x2.coerceIn(0f, 1f)
            val bottom = y2.coerceIn(0f, 1f)
            
            // Extract class probabilities (indices 4-9 are disease classes)
            val classProbabilities = mutableMapOf<Int, Float>()
            for (j in 4 until output.size) {
                classProbabilities[j - 4] = output[j][i]
            }
            
            // Debug high confidence detections
            if (detectionsAboveThreshold <= 3) {
                Log.d(TAG, "High conf detection: x1=$x1, y1=$y1, x2=$x2, y2=$y2 -> bbox=[$left, $top, $right, $bottom], conf=$confidence, classes=$classProbabilities")
            }
            
            val boundingBox = RectF(left, top, right, bottom)
            
            // Crop the leaf region from original bitmap
            val croppedBitmap = cropBoundingBox(originalBitmap, boundingBox)
            
            detections.add(
                DetectionResult(
                    boundingBox = boundingBox,
                    confidence = confidence,
                    croppedBitmap = croppedBitmap,
                    classProbabilities = classProbabilities
                )
            )
        }
        
        Log.d(TAG, "Max confidence found: $maxConfidence, threshold: $confidenceThreshold")
        Log.d(TAG, "Detections above threshold: $detectionsAboveThreshold")
        
        // Sort by confidence (highest first)
        return detections.sortedByDescending { it.confidence }
    }

    
    /**
     * Applies Non-Maximum Suppression to filter overlapping detections
     * Keeps only the highest confidence detection for overlapping boxes
     */
    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        val selected = mutableListOf<DetectionResult>()
        val sorted = detections.sortedByDescending { it.confidence }
        
        for (detection in sorted) {
            var shouldSelect = true
            
            // Check if this detection overlaps significantly with any selected detection
            for (selectedDetection in selected) {
                val iou = calculateIoU(detection.boundingBox, selectedDetection.boundingBox)
                if (iou > nmsThreshold) {
                    shouldSelect = false
                    break
                }
            }
            
            if (shouldSelect) {
                selected.add(detection)
            }
        }
        
        return selected
    }
    
    /**
     * Calculates Intersection over Union (IoU) between two bounding boxes
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionRight < intersectionLeft || intersectionBottom < intersectionTop) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    
    /**
     * Crops a bounding box region from the original bitmap with padding.
     * Adds 10% padding around the detected region and handles edge cases.
     * 
     * Requirements addressed:
     * - 1.1: Extract bounding box region from original image
     * - 1.2: Add padding around detected region (10% margin)
     * - Handle edge cases (detection near image boundaries)
     */
    private fun cropBoundingBox(bitmap: Bitmap, boundingBox: RectF): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Convert normalized coordinates to pixel coordinates
        val left = (boundingBox.left * width).toInt()
        val top = (boundingBox.top * height).toInt()
        val right = (boundingBox.right * width).toInt()
        val bottom = (boundingBox.bottom * height).toInt()
        
        // Calculate box dimensions
        val boxWidth = right - left
        val boxHeight = bottom - top
        
        // Add 10% padding
        val paddingX = (boxWidth * ModelConfig.CROP_PADDING_PERCENT).toInt()
        val paddingY = (boxHeight * ModelConfig.CROP_PADDING_PERCENT).toInt()
        
        // Apply padding while handling edge cases (near image boundaries)
        val paddedLeft = max(0, left - paddingX)
        val paddedTop = max(0, top - paddingY)
        val paddedRight = min(width, right + paddingX)
        val paddedBottom = min(height, bottom + paddingY)
        
        // Calculate final crop dimensions
        val cropWidth = paddedRight - paddedLeft
        val cropHeight = paddedBottom - paddedTop
        
        // Ensure valid dimensions
        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.w(TAG, "Invalid crop dimensions: width=$cropWidth, height=$cropHeight. Using full image.")
            return bitmap
        }
        
        return try {
            Bitmap.createBitmap(bitmap, paddedLeft, paddedTop, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping bitmap: ${e.message}", e)
            bitmap // Return original bitmap on error
        }
    }
    
    /**
     * Releases resources when detector is no longer needed
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "YoloLeafDetector resources released")
    }
}
