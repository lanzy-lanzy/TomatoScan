package com.ml.tomatoscan.ml

import com.ml.tomatoscan.models.DiseaseClass

/**
 * Result of disease classification from TFLite model.
 * Contains the predicted disease class, confidence score, and all class probabilities.
 *
 * @property diseaseClass The predicted disease class
 * @property confidence Confidence score for the prediction (0.0 to 1.0)
 * @property allProbabilities Map of all disease classes to their probability scores
 */
data class ClassificationResult(
    val diseaseClass: DiseaseClass,
    val confidence: Float,
    val allProbabilities: Map<DiseaseClass, Float>
)
