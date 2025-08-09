package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class TFLiteClassifier(
    private val context: Context,
    private val modelPath: String,
    private val maxResults: Int = 1
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(0.1f)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().useGpu()

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelPath,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            Log.e("TFLiteClassifier", "Error creating classifier", e)
        }
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        if (imageClassifier == null) {
            setupClassifier()
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val results = imageClassifier?.classify(tensorImage)

        return results?.map { classification ->
            classification.categories.map { category ->
                Pair(category.label, category.score)
            }
        }?.flatten() ?: emptyList()
    }
}
