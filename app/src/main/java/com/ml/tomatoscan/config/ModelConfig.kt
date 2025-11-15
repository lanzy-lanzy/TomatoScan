package com.ml.tomatoscan.config

import com.ml.tomatoscan.BuildConfig

/**
 * Configuration for ML models (YOLOv11 and TFLite)
 * Contains model paths, input sizes, and thresholds
 */
object ModelConfig {
    /**
     * Path to YOLOv11 leaf detection model in assets
     * Using the new 640x640 trained model (30 epochs, FLOAT32 for maximum accuracy)
     * Model: YOLO11n with 2.58M parameters, 6.3 GFLOPs
     * Training: 30 epochs, mAP50-95: 0.995 (99.5% accuracy)
     */
    const val YOLO_MODEL_PATH = "best_float32.tflite"

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
     * Supported disease classes in order matching the training data
     * Order must match model output indices (0-5)
     */
    val DISEASE_CLASSES = listOf(
        "Bacterial Spot",        // Index 0
        "Early Blight",          // Index 1
        "Late Blight",           // Index 2
        "Septoria Leaf Spot",    // Index 3
        "Tomato Mosaic Virus",   // Index 4
        "Healthy"                // Index 5
    )
    
    /**
     * Model version and training metadata
     * 
     * Training Configuration:
     * - Dual-layer augmentation strategy for maximum robustness
     * - Pre-training augmentation: 7x dataset expansion (rotation, flip, zoom, brightness, crops)
     * - Runtime augmentation: Random rotation ±15°, flip, scale ±10%, HSV jitter
     * - Dataset: PlantVillage tomato diseases (6 classes)
     * - Batch size: 16, Image size: 640x640
     */
    const val MODEL_VERSION = "v4.0-float32-20epochs-dual-aug"
    const val MODEL_TRAINING_EPOCHS = 20
    const val MODEL_TRAINING_DATE = "2024-11-15"
    const val MODEL_ARCHITECTURE = "YOLO11n"
    const val MODEL_PARAMETERS = "2.58M"
    const val MODEL_GFLOPS = 6.3f
    const val MODEL_MAP50_95 = 0.95f // Expected 95%+ accuracy with dual augmentation
    
    /**
     * Training augmentation details for reference
     * This helps understand model robustness characteristics
     */
    const val TRAINING_AUGMENTATION_STRATEGY = "Dual-Layer"
    const val PRE_TRAINING_EXPANSION = "7x" // Dataset expanded 7x before training
    const val RUNTIME_AUGMENTATION = "Rotation ±15°, Flip 50%, Scale ±10%, HSV jitter"

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
