package com.example.avatar_ai_app.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.avatar_ai_app.R
import com.example.avatar_ai_cloud_storage.database.entity.Feature

private const val TAG = "ChatService"

/**
 * Service class for handling user message input and generating responses.
 *
 * @property context The application context.
 * @constructor Creates a [ChatService] instance with the provided context.
 */
class ChatService(private val context: Context) {

    /**
     * Enum class representing different types of user requests.
     *
     * @constructor Creates an instance of the [Request] enum.
     */
    enum class Request { CHAT, NAVIGATION, RECOGNITION }

    // List of features; initialised empty.
    var featureList = emptyList<Feature>()

    // Current user request.
    private val _request = MutableLiveData<Request>()
    val request: LiveData<Request> get() = _request

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
            Intent.GREETING -> returnChatResponse(context.getString(R.string.greeting_message))
            Intent.HELP -> returnChatResponse(context.getString(R.string.help_message))
            Intent.NAVIGATION -> navigationRequested(message)
            Intent.RECOGNITION -> recognitionRequested()
            Intent.INFORMATION -> informationRequested(message)
            else -> returnChatResponse(context.getString(R.string.generic_message))
        }
    }

    /*
    * Parses the user's message to determine the intent.
     */
    private fun parseIntent(message: String): Intent? {
        Intent.values().forEach { intent ->
            intent.triggerPhrases.forEach { phrase ->
                if (isPhraseFoundRegex(message, phrase)) {
                    Log.i(TAG, "parseIntent: $intent")
                    return intent
                }
            }
        }
        Log.i(TAG, "parseIntent: null")
        return null
    }

    /*
    * Checks if a given phrase is found in the user's message using regex.
     */
    private fun isPhraseFoundRegex(message: String, phrase: String): Boolean {
        val regex = Regex("(^|\\s)$phrase(\\s|\$)", RegexOption.IGNORE_CASE)
        return message.contains(regex)
    }

    /*
    * Returns a response for chat-related requests.
     */
    private fun returnChatResponse(response: String): String {
        _request.postValue(Request.CHAT)
        Log.i(TAG, "response: $response")
        return response
    }

    /*
    * Handles navigation-related user intents. Posts values to
    * request and destinationId if a requested feature is found.
     */
    private fun navigationRequested(message: String): String {
        val feature = parseFeature(message)
        return if (feature == null) {
            context.getString(R.string.invalid_feature_message)
        } else {
            _request.postValue(Request.NAVIGATION)
            _destinationID.postValue(feature.anchor)
            context.getString(R.string.navigation_message)
        }
    }

    /*
    * Parses the user's message to determine the requested feature.
     */
    private fun parseFeature(message: String): Feature? {
        featureList.forEach {
            if (isPhraseFoundRegex(message, it.name)) {
                Log.i(TAG, "parseFeature: $it.name")
                return it
            }
        }
        Log.i(TAG, "parseFeature: null")
        return null
    }

    /*
    * Handles navigation-related user intents. Posts value to request.
     */
    private fun recognitionRequested(): String {
        _request.postValue(Request.RECOGNITION)
        return context.getString(R.string.recognition_message)
    }

    /*
    * Handles information-related intents. Returns the description of a
    * feature if it is found.
     */
    private fun informationRequested(message: String): String {
        val feature = parseFeature(message)
        return returnChatResponse(
            feature?.description ?: context.getString(R.string.invalid_feature_message)
        )
    }

    /**
     * Resets the service by clearing the feature list and resetting the request.
     */
    fun reset() {
        featureList = emptyList()
        _request.postValue(Request.CHAT)
    }
}