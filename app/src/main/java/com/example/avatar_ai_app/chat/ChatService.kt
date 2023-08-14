package com.example.avatar_ai_app.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.shared.containsPhraseFuzzy
import com.example.avatar_ai_cloud_storage.database.entity.Feature

private const val TAG = "ChatService"

// Minimum Levenshtein Ratio allowed in fuzzy phrase searching.
// This is around one-in-four characters edits.
private const val MIN_LEVENSHTEIN_RATIO = 0.75

/**
 * Service class for handling user message input and generating responses.
 *
 * @property context The application context.
 * @constructor Creates a [ChatService] instance with the provided context.
 */
class ChatService(private val context: Context) {

    // List of features; initialised empty.
    var featureList = emptyList<Feature>()

    // Current user intent.
    private val _intent = MutableLiveData<Intent>()
    val intent: LiveData<Intent> get() = _intent

    // Currently requested destination.
    private val _destinationID = MutableLiveData<String>()
    val destinationID: LiveData<String> get() = _destinationID

    /**
     * Generates a response based on the user message.
     *
     * @param message The user's message.
     * @return The generated response.
     */
    fun getResponse(message: String): String {
        Log.i(TAG, "getResponse: $message")
        return when (parseIntent(message)) {
            Intent.GREETING -> context.getString(R.string.greeting_message)
            Intent.HELP -> context.getString(R.string.help_message)
            Intent.NAVIGATION -> navigationRequested(message)
            Intent.RECOGNITION -> context.getString(R.string.recognition_message)
            Intent.INFORMATION -> informationRequested(message)
            else -> context.getString(R.string.generic_message)
        }
    }

    /*
    * Parses the user's message to determine the intent.
     */
    private fun parseIntent(message: String): Intent? {
        Intent.values().forEach { intent ->
            intent.triggerPhrases.forEach { phrase ->
                if (message.containsPhraseFuzzy(phrase, MIN_LEVENSHTEIN_RATIO)) {
                    Log.i(TAG, "parseIntent: $intent")
                    _intent.postValue(intent)
                    return intent
                }
            }
        }
        Log.i(TAG, "parseIntent: null")
        _intent.postValue(Intent.HELP)
        return null
    }

    /*
    * Handles navigation-related user intents. Posts values to
    * request and destinationId if a requested feature is found.
     */
    private fun navigationRequested(message: String): String {
        val feature = parseFeature(message)
        return if (feature != null) {
            _destinationID.postValue(feature.anchor)
            context.getString(R.string.navigation_message)
        } else {
            context.getString(R.string.invalid_feature_message)
        }
    }

    /*
    * Parses the user's message to determine the requested feature.
     */
    private fun parseFeature(message: String): Feature? {
        featureList.forEach {
            if (message.containsPhraseFuzzy(it.name, MIN_LEVENSHTEIN_RATIO)) {
                Log.i(TAG, "parseFeature: $it.name")
                return it
            }
        }
        Log.i(TAG, "parseFeature: null")
        return null
    }

    /*
    * Handles information-related intents. Returns the description of a
    * feature if it is found.
     */
    private fun informationRequested(message: String): String {
        val feature = parseFeature(message)
        return feature?.description ?: context.getString(R.string.invalid_feature_message)
    }

    /**
     * Resets the service by clearing the feature list and resetting the request.
     */
    fun reset() {
        featureList = emptyList()
    }
}