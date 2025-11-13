package com.ml.tomatoscan.models

/**
 * Enum representing all valid tomato leaf disease classifications.
 * Each disease class includes a display name for UI presentation.
 * 
 * Order matches the YOLOv11 model training data indices:
 * 0: Bacterial Spot
 * 1: Early Blight
 * 2: Late Blight
 * 3: Septoria Leaf Spot
 * 4: Tomato Mosaic Virus
 * 5: Healthy
 */
enum class DiseaseClass(val displayName: String) {
    /**
     * Bacterial Spot - Caused by Xanthomonas bacteria
     * Visual symptoms: Small dark spots with yellow halos on leaves
     * Model index: 0
     */
    BACTERIAL_SPOT("Bacterial Spot"),
    
    /**
     * Early Blight - Caused by Alternaria solani fungus
     * Visual symptoms: Concentric ring patterns (target spots) on leaves
     * Model index: 1
     */
    EARLY_BLIGHT("Early Blight"),
    
    /**
     * Late Blight - Caused by Phytophthora infestans
     * Visual symptoms: Water-soaked lesions with white fungal growth
     * Model index: 2
     */
    LATE_BLIGHT("Late Blight"),
    
    /**
     * Septoria Leaf Spot - Caused by Septoria lycopersici fungus
     * Visual symptoms: Small circular spots with gray centers and dark borders
     * Model index: 3
     */
    SEPTORIA_LEAF_SPOT("Septoria Leaf Spot"),
    
    /**
     * Tomato Mosaic Virus - Viral disease
     * Visual symptoms: Mottled yellow-green patterns, leaf distortion
     * Model index: 4
     */
    TOMATO_MOSAIC_VIRUS("Tomato Mosaic Virus"),
    
    /**
     * Healthy Leaf - No disease detected
     * Visual symptoms: Normal green leaf with no disease symptoms
     * Model index: 5
     */
    HEALTHY("Healthy"),
    
    /**
     * Uncertain - Unable to determine disease due to poor image quality or low confidence
     * Not a model output class
     */
    UNCERTAIN("Uncertain");
    
    companion object {
        /**
         * Get DiseaseClass from model output index
         * @param index The model output index (0-5)
         * @return Matching DiseaseClass or UNCERTAIN if index is invalid
         */
        fun fromModelIndex(index: Int): DiseaseClass {
            return when (index) {
                0 -> BACTERIAL_SPOT
                1 -> EARLY_BLIGHT
                2 -> LATE_BLIGHT
                3 -> SEPTORIA_LEAF_SPOT
                4 -> TOMATO_MOSAIC_VIRUS
                5 -> HEALTHY
                else -> UNCERTAIN
            }
        }
        
        /**
         * Get DiseaseClass from display name string
         * @param displayName The display name to match
         * @return Matching DiseaseClass or UNCERTAIN if not found
         */
        fun fromDisplayName(displayName: String): DiseaseClass {
            return values().find { 
                it.displayName.equals(displayName, ignoreCase = true) 
            } ?: UNCERTAIN
        }
        
        /**
         * Get DiseaseClass from enum name string
         * @param name The enum name to match
         * @return Matching DiseaseClass or UNCERTAIN if not found
         */
        fun fromString(name: String): DiseaseClass {
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                UNCERTAIN
            }
        }
    }
}
