package com.ml.tomatoscan.utils

/**
 * Documentation of the training augmentation strategy used for the YOLOv11 model.
 * 
 * This information helps developers understand the model's robustness characteristics
 * and expected performance under various conditions.
 * 
 * Training Approach: Dual-Layer Augmentation
 * ==========================================
 * 
 * LAYER 1: Pre-Training Manual Augmentation (Dataset Expansion)
 * -------------------------------------------------------------
 * Applied before training to expand the dataset ~7x:
 * 
 * 1. Rotation: ±15 degrees
 *    - Handles different camera angles
 *    - Simulates leaves at various orientations
 * 
 * 2. Horizontal Flip: 50% probability
 *    - Handles left/right orientation
 *    - Doubles effective dataset size
 * 
 * 3. Zoom In: 10% closer view
 *    - Simulates closer camera distance
 *    - Focuses on leaf details
 * 
 * 4. Brightness: +20% (brighter conditions)
 *    - Handles bright sunlight
 *    - Overexposed images
 * 
 * 5. Darkness: -20% (darker conditions)
 *    - Handles shade/shadow
 *    - Underexposed images
 * 
 * 6. Center Crop: 90% of image
 *    - Different leaf positions
 *    - Partial leaf views
 * 
 * Result: Original dataset expanded from ~10,000 to ~70,000 images
 * 
 * 
 * LAYER 2: Runtime Training Augmentation (On-the-Fly)
 * ---------------------------------------------------
 * Applied during training for additional robustness:
 * 
 * 1. Random Rotation: ±15 degrees
 *    - Additional angle variations
 *    - Complements pre-training rotation
 * 
 * 2. Random Horizontal Flip: 50% probability
 *    - Additional orientation variations
 *    - Complements pre-training flip
 * 
 * 3. Scale Variation: ±10%
 *    - Zoom in/out randomly
 *    - Handles varying distances
 * 
 * 4. Translation: ±15% random crops
 *    - Different leaf positions in frame
 *    - Handles off-center captures
 * 
 * 5. HSV Color Jitter:
 *    - Hue: ±1% (minimal, preserves leaf color)
 *    - Saturation: ±50% (handles color intensity)
 *    - Value (Brightness): ±20% (handles lighting)
 * 
 * 6. Disabled Augmentations:
 *    - Mosaic: 0.0 (not suitable for classification)
 *    - Mixup: 0.0 (not suitable for classification)
 *    - Perspective: 0.0 (maintains leaf shape)
 *    - Shear: 0.0 (maintains leaf shape)
 * 
 * 
 * Model Robustness Characteristics
 * =================================
 * 
 * Based on this dual-layer augmentation, the model is robust to:
 * 
 * ✓ Camera Angles: ±15° rotation tolerance
 * ✓ Lighting Conditions: ±20% brightness variation
 * ✓ Distance Variations: ±10% scale changes
 * ✓ Leaf Positioning: Off-center captures, partial views
 * ✓ Color Variations: Different lighting, camera settings
 * ✓ Orientation: Left/right flipped images
 * 
 * 
 * Expected Performance
 * ====================
 * 
 * - Training Epochs: 20
 * - Expected mAP50: 90-95%
 * - Expected mAP50-95: 85-90%
 * - Inference Time: <500ms on mid-range devices
 * - Model Size: ~20MB (Float32), ~5MB (INT8)
 * 
 * 
 * Recommendations for Production Use
 * ===================================
 * 
 * 1. Image Quality:
 *    - Minimum resolution: 640x640 pixels
 *    - Good lighting (not too bright or dark)
 *    - Clear focus on leaf
 * 
 * 2. Capture Conditions:
 *    - Hold camera steady
 *    - Capture full leaf if possible
 *    - Avoid extreme angles (>15°)
 * 
 * 3. Confidence Thresholds:
 *    - Detection: 0.6 (ensures it's a tomato leaf)
 *    - Classification: 0.5 (disease identification)
 *    - Below threshold: Request better image
 * 
 * 4. Error Handling:
 *    - Low confidence: Suggest retaking photo
 *    - No detection: Ensure leaf is visible
 *    - Multiple detections: Focus on one leaf
 */
object AugmentationInfo {
    
    /**
     * Pre-training augmentation types applied
     */
    val PRE_TRAINING_AUGMENTATIONS = listOf(
        "Rotation ±15°",
        "Horizontal Flip",
        "Zoom In 10%",
        "Brightness +20%",
        "Darkness -20%",
        "Center Crop 90%"
    )
    
    /**
     * Runtime augmentation parameters
     */
    val RUNTIME_AUGMENTATIONS = mapOf(
        "degrees" to 15.0f,
        "scale" to 0.1f,
        "translate" to 0.15f,
        "fliplr" to 0.5f,
        "flipud" to 0.0f,
        "hsv_h" to 0.01f,
        "hsv_s" to 0.5f,
        "hsv_v" to 0.2f,
        "mosaic" to 0.0f,
        "mixup" to 0.0f
    )
    
    /**
     * Dataset expansion factor
     */
    const val DATASET_EXPANSION_FACTOR = 7
    
    /**
     * Training configuration
     */
    const val TRAINING_EPOCHS = 20
    const val BATCH_SIZE = 16
    const val IMAGE_SIZE = 640
    
    /**
     * Expected performance metrics
     */
    const val EXPECTED_MAP50 = 0.95f
    const val EXPECTED_MAP50_95 = 0.90f
    const val MAX_INFERENCE_TIME_MS = 500L
    
    /**
     * Recommended thresholds
     */
    const val DETECTION_CONFIDENCE_THRESHOLD = 0.6f
    const val CLASSIFICATION_CONFIDENCE_THRESHOLD = 0.5f
    
    /**
     * Gets a human-readable description of the augmentation strategy
     */
    fun getAugmentationDescription(): String {
        return """
            Dual-Layer Augmentation Strategy:
            
            Pre-Training (Dataset Expansion ~7x):
            ${PRE_TRAINING_AUGMENTATIONS.joinToString("\n") { "  • $it" }}
            
            Runtime (Training-Time):
            ${RUNTIME_AUGMENTATIONS.entries.joinToString("\n") { "  • ${it.key}: ${it.value}" }}
            
            Result: Highly robust model trained on ~70,000 augmented images
        """.trimIndent()
    }
    
    /**
     * Gets recommendations for image capture
     */
    fun getCaptureRecommendations(): List<String> {
        return listOf(
            "Ensure good lighting (not too bright or dark)",
            "Hold camera steady for clear focus",
            "Capture the full leaf if possible",
            "Avoid extreme angles (keep within ±15°)",
            "Fill the frame with the leaf",
            "Avoid shadows and reflections"
        )
    }
}
