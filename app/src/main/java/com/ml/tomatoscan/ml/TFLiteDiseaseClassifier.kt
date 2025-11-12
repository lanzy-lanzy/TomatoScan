package com.ml.tomatoscan.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ml.tomatoscan.config.ModelConfig
import com.ml.tomatoscan.models.DiseaseClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TensorFlow Lite implementation of disease classifier.
 * Uses the tomato_disease_model.tflite model to classify tomato leaf diseases.
 *
 * @property context Android context for accessing assets
 */
class TFLiteDiseaseClassifier(private val context: Context) : DiseaseClassifier {

    private var interpreter: Interpreter? = null
    
    companion object {
        private const val TAG = "TFLiteDiseaseClassifier"
        
        /**
         * Supported disease classes in the order they appear in the model output.
         * This order must match the training labels of the TFLite model.
         */
        private val SUPPORTED_CLASSES = listOf(
            DiseaseClass.BACTERIAL_SPECK,
            DiseaseClass.EARLY_BLIGHT,
            DiseaseClass.HEALTHY,
            DiseaseClass.LATE_BLIGHT,
            DiseaseClass.LEAF_MOLD,
            DiseaseClass.SEPTORIA_LEAF_SPOT
        )
    }

    init {
        // NOTE: This classifier is NO LONGER USED!
        // The tomato_leaf_640.tflite YOLO model provides both detection AND classification.
        // This class is kept only for interface compatibility but should not be called.
        Log.d(TAG, "TFLiteDiseaseClassifier initialized (NOT USED - YOLO model handles classification)")
        interpreter = null
    }

    /**
     * DEPRECATED: No longer loads a separate model.
     * Classification is now handled by the YOLO model (tomato_leaf_640.tflite).
     */
    private fun loadModel() {
        // Intentionally empty - we don't load tomato_disease_model.tflite anymore
        Log.d(TAG, "Skipping separate disease model loading - using YOLO classification")
    }

    override suspend fun classify(bitmap: Bitmap): ClassificationResult = withContext(Dispatchers.Default) {
        // This method should NEVER be called!
        // Classification is now handled directly by the YOLO model in AnalysisPipelineImpl
        Log.e(TAG, "ERROR: TFLiteDiseaseClassifier.classify() was called but should not be used!")
        Log.e(TAG, "Classification should come from YOLO model (tomato_leaf_640.tflite) class probabilities")
        
        throw IllegalStateException(
            "TFLiteDiseaseClassifier is deprecated. " +
            "Use YOLO model class probabilities from DetectionResult instead."
        )
    }

    override fun getSupportedClasses(): List<DiseaseClass> {
        return SUPPORTED_CLASSES
    }
    
    /**
     * Converts bitmap to Float32 ByteBuffer for model input
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = ModelConfig.TFLITE_INPUT_SIZE
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // Float32 = 4 bytes
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

    override fun close() {
        try {
            interpreter?.close()
            interpreter = null
            Log.d(TAG, "TFLite interpreter closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}
