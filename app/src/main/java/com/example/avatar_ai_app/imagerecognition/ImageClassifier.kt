package com.example.avatar_ai_app.imagerecognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.avatar_ai_app.imagerecognition.tf.Model
import org.tensorflow.lite.support.image.TensorImage

/**
 * An `ImageClassifier` class that handles the process of classifying images using a pretrained
 * TensorFlow Lite model.
 *
 * @property context the application's context used to initialise the model.
 * @property modelPath the path to the machine learning model used for classification; this
 * must be a canonical path
 */
class ImageClassifier(private val context: Context, private val modelPath: String) {

    /**
     * Threshold for determining if a classification is confident enough.
     */
    private val THRESHOLD: Double = 0.7

    /**
     * The machine learning model instance used for image classification.
     */
    private var model: Model? = null

    /**
     * Initializes the machine learning model.
     *
     * @return true if the model is initialised successfully, false otherwise.
     */
    fun initialise(): Boolean {
        try {
            model = Model.newInstance(context, modelPath)
        } catch (e: Exception) {
            Log.d("IMAGE", "Message: " + e.stackTraceToString())
            return false
        }
        return true
    }

    /**
     * Classifies the provided bitmap image and returns the most probable classification name.
     *
     * @param bitmap the image to be classified.
     * @return the name of the recognised exhibit, or null if the confidence is below the threshold.
     */
    fun getExhibitName(bitmap: Bitmap): String? {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val outputs = model?.process(tensorImage)
        val probabilities = outputs?.probabilityAsCategoryList

        var maxProbability: Float? = null
        var maxLabel: String? = null

        if (probabilities != null) {
            for (category in probabilities)
            {
                if (maxProbability == null || category.score > maxProbability) {
                    maxProbability = category.score
                    maxLabel = category.label
                }
            }
        }

        if (maxProbability != null && maxProbability >= THRESHOLD) {
            val cleanedString = maxLabel?.replace("_", " ")
            return cleanedString
        } else {
            return null
        }
    }

    /**
     * Closes the machine learning model when it is no longer needed.
     */
    fun closeModel() {
        model?.close()
    }
}
