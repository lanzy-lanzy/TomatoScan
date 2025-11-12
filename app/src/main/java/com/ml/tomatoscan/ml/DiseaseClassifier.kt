package com.ml.tomatoscan.ml

import android.graphics.Bitmap
import com.ml.tomatoscan.models.DiseaseClass

/**
 * Interface for tomato leaf disease classification.
 * Implementations should use TFLite models to classify diseases from leaf images.
 */
interface DiseaseClassifier {
    /**
     * Classifies the disease present in the leaf image.
     * The input bitmap should be preprocessed to the model's expected dimensions (224x224).
     *
     * @param bitmap Cropped leaf image (should be 224x224 pixels)
     * @return Classification result with disease class, confidence, and all probabilities
     */
    suspend fun classify(bitmap: Bitmap): ClassificationResult

    /**
     * Gets the list of supported disease classes that this classifier can identify.
     * The order of classes should match the model's output layer.
     *
     * @return List of supported disease classes
     */
    fun getSupportedClasses(): List<DiseaseClass>

    /**
     * Releases resources used by the classifier.
     * Should be called when the classifier is no longer needed.
     */
    fun close()
}
