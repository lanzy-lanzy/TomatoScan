package com.ml.tomatoscan.config

import com.ml.tomatoscan.BuildConfig

/**
 * Configuration for ML models (YOLOv11 and TFLite)
 * Contains model paths, input sizes, and thresholds
 */
object ModelConfig {
    /**
     * Path to YOLOv11 leaf detection model in assets
     * Using the new 640x640 trained model
     */
    const val YOLO_MODEL_PATH = "tomato_leaf_640.tflite"

    /**
     * Path to TFLite disease classification model in assets
     */
    const val TFLITE_MODEL_PATH = "tomato_disease_model.tflite"

    /**
     * Input image size for YOLOv11 detector (width and height)
     * Model expects 640x640 input
     */
    const val YOLO_INPUT_SIZE = 640

    /**
     * Input image size for TFLite classifier (width and height)
     * Model expects 512x512 input
     */
    const val TFLITE_INPUT_SIZE = 512

    /**
     * Minimum confidence threshold for accepting predictions
     * Predictions below this threshold are marked as UNCERTAIN
     */
    const val CONFIDENCE_THRESHOLD = 0.5f
    
    /**
     * Enable Gemini pre-validation to verify if image contains a tomato leaf
     * before running YOLO detection. Provides better accuracy but requires internet.
     * Set to false for offline-only mode.
     */
    const val ENABLE_GEMINI_PRE_VALIDATION = true

    /**
     * Confidence threshold for YOLOv11 leaf detection
     * Set to 0.6 to ensure only tomato leaves are detected (not other objects)
     */
    const val DETECTION_CONFIDENCE_THRESHOLD = 0.6f

    /**
     * IoU (Intersection over Union) threshold for NMS (Non-Maximum Suppression)
     */
    const val NMS_IOU_THRESHOLD = 0.45f

    /**
     * Padding percentage to add around detected leaf bounding box
     */
    const val CROP_PADDING_PERCENT = 0.1f

    /**
     * Model versions from BuildConfig for tracking and debugging
     */
    val MODEL_VERSION: String = BuildConfig.MODEL_VERSION
    val YOLO_MODEL_VERSION: String = BuildConfig.YOLO_MODEL_VERSION
    val TFLITE_MODEL_VERSION: String = BuildConfig.TFLITE_MODEL_VERSION

    /**
     * Supported disease classes in order
     */
    val DISEASE_CLASSES = listOf(
        "Early Blight",
        "Late Blight",
        "Leaf Mold",
        "Septoria Leaf Spot",
        "Bacterial Speck",
        "Healthy",
        "Uncertain"
    )

    /**
     * Validates that required model files exist in assets
     * Note: This should be called at app startup to ensure models are available
     */
    fun validateModelPaths(): List<String> {
        val missingModels = mutableListOf<String>()
        
        // Note: Actual file existence check should be done with AssetManager
        // This is a placeholder for the validation logic
        // Example: if (!assetManager.list("models")?.contains("yolov11_tomato_leaf.tflite")) { ... }
        
        return missingModels
    }
}
