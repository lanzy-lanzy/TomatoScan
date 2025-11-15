package com.ml.tomatoscan.utils

import com.ml.tomatoscan.config.ModelConfig

/**
 * Provides comprehensive information about the trained model for display in the app.
 * 
 * This helps users understand:
 * - What the model can detect
 * - How it was trained
 * - Expected accuracy and performance
 * - Best practices for use
 */
object ModelInfoProvider {
    
    /**
     * Gets a formatted model information string for display
     */
    fun getModelInfo(): String {
        return """
            🍅 TomatoScan Model Information
            
            Model Version: ${ModelConfig.MODEL_VERSION}
            Architecture: ${ModelConfig.MODEL_ARCHITECTURE}
            Parameters: ${ModelConfig.MODEL_PARAMETERS}
            Training Date: ${ModelConfig.MODEL_TRAINING_DATE}
            
            Training Configuration:
            • Epochs: ${ModelConfig.MODEL_TRAINING_EPOCHS}
            • Image Size: ${ModelConfig.YOLO_INPUT_SIZE}x${ModelConfig.YOLO_INPUT_SIZE}
            • Augmentation: ${ModelConfig.TRAINING_AUGMENTATION_STRATEGY}
            • Dataset Expansion: ${ModelConfig.PRE_TRAINING_EXPANSION}
            
            Supported Diseases (6 classes):
            ${ModelConfig.DISEASE_CLASSES.mapIndexed { index, name -> "${index + 1}. $name" }.joinToString("\n")}
            
            Expected Performance:
            • Accuracy (mAP50-95): ${String.format("%.1f%%", ModelConfig.MODEL_MAP50_95 * 100)}
            • Inference Time: <500ms
            • Detection Threshold: ${String.format("%.1f%%", ModelConfig.DETECTION_CONFIDENCE_THRESHOLD * 100)}
            • Classification Threshold: ${String.format("%.1f%%", ModelConfig.CONFIDENCE_THRESHOLD * 100)}
        """.trimIndent()
    }
    
    /**
     * Gets training details for technical users
     */
    fun getTrainingDetails(): String {
        return """
            Training Methodology:
            
            ${AugmentationInfo.getAugmentationDescription()}
            
            Dataset: PlantVillage Tomato Diseases
            Original Images: ~10,000
            Augmented Images: ~70,000
            
            Training Split:
            • Train: 70%
            • Validation: 20%
            • Test: 10%
            
            Augmentation Benefits:
            ✓ Robust to camera angles (±15°)
            ✓ Handles lighting variations (±20%)
            ✓ Works with different distances (±10%)
            ✓ Tolerates off-center captures
            ✓ Adapts to color variations
        """.trimIndent()
    }
    
    /**
     * Gets usage recommendations for users
     */
    fun getUsageRecommendations(): String {
        val recommendations = AugmentationInfo.getCaptureRecommendations()
        return """
            📸 Best Practices for Accurate Results:
            
            ${recommendations.mapIndexed { index, rec -> "${index + 1}. $rec" }.joinToString("\n")}
            
            What to Expect:
            • High confidence (>70%): Reliable diagnosis
            • Medium confidence (50-70%): Consider retaking photo
            • Low confidence (<50%): Retake with better conditions
            
            Model Limitations:
            • Trained only on 6 specific diseases
            • Requires clear leaf images
            • Works best with single leaf in frame
            • May struggle with severely damaged leaves
        """.trimIndent()
    }
    
    /**
     * Gets a short summary for quick reference
     */
    fun getQuickSummary(): String {
        return """
            Model: ${ModelConfig.MODEL_ARCHITECTURE} (${ModelConfig.MODEL_VERSION})
            Trained: ${ModelConfig.MODEL_TRAINING_DATE} (${ModelConfig.MODEL_TRAINING_EPOCHS} epochs)
            Accuracy: ${String.format("%.1f%%", ModelConfig.MODEL_MAP50_95 * 100)}
            Diseases: ${ModelConfig.DISEASE_CLASSES.size} classes
            Augmentation: Dual-layer (7x expansion + runtime)
        """.trimIndent()
    }
    
    /**
     * Gets disease class information
     */
    fun getDiseaseClassInfo(): Map<String, String> {
        return mapOf(
            "Bacterial Spot" to "Small dark spots with yellow halos on leaves",
            "Early Blight" to "Concentric ring patterns (target spots) on older leaves",
            "Late Blight" to "Water-soaked lesions with white fuzzy mold",
            "Septoria Leaf Spot" to "Small circular spots with gray centers",
            "Tomato Mosaic Virus" to "Mottled yellow-green patterns, leaf distortion",
            "Healthy" to "Uniform green leaves with no disease symptoms"
        )
    }
    
    /**
     * Gets performance expectations based on training
     */
    fun getPerformanceExpectations(): String {
        return """
            Expected Performance Metrics:
            
            Detection Stage:
            • Speed: 200-300ms
            • Confidence: >60% for valid tomato leaves
            • False Positives: <5% (rejects non-tomato objects)
            
            Classification Stage:
            • Speed: 100-200ms
            • Accuracy: 90-95% on clear images
            • Confidence: >50% for reliable diagnosis
            
            Total Pipeline:
            • End-to-End: <500ms (without Gemini)
            • With Gemini: 2-3 seconds (includes validation)
            • Memory Usage: <50MB
            
            Robustness:
            • Lighting: Handles ±20% brightness variation
            • Angles: Tolerates ±15° rotation
            • Distance: Works with ±10% scale changes
            • Position: Handles off-center captures
        """.trimIndent()
    }
}
