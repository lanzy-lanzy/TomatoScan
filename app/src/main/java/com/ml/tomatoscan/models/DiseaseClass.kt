package com.ml.tomatoscan.models

/**
 * Enum representing all valid tomato leaf disease classifications.
 * Each disease class includes a display name for UI presentation.
 */
enum class DiseaseClass(val displayName: String) {
    /**
     * Early Blight - Caused by Alternaria solani fungus
     */
    EARLY_BLIGHT("Early Blight"),
    
    /**
     * Late Blight - Caused by Phytophthora infestans
     */
    LATE_BLIGHT("Late Blight"),
    
    /**
     * Leaf Mold - Caused by Passalora fulva fungus
     */
    LEAF_MOLD("Leaf Mold"),
    
    /**
     * Septoria Leaf Spot - Caused by Septoria lycopersici fungus
     */
    SEPTORIA_LEAF_SPOT("Septoria Leaf Spot"),
    
    /**
     * Bacterial Speck - Caused by Pseudomonas syringae bacteria
     */
    BACTERIAL_SPECK("Bacterial Speck"),
    
    /**
     * Healthy Leaf - No disease detected
     */
    HEALTHY("Healthy"),
    
    /**
     * Uncertain - Unable to determine disease due to poor image quality or low confidence
     */
    UNCERTAIN("Uncertain");
    
    companion object {
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
