package com.example.avatar_ai_app.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.avatar_ai_cloud_storage.database.entity.Feature

private const val TAG = "ChatService"

private const val GREETING_MESSAGE = "Hello!"
private const val HELP_MESSAGE =
    "Feel free to ask me anything! I can recognise features and provide information about them. You can also ask me for directions!"
private const val NAVIGATION_MESSAGE =
    "Please follow the onscreen directions towards your destination!"
private const val INVALID_EXHIBITION_MESSAGE = "Sorry, I don't recognise that feature!"
private const val RECOGNITION_MESSAGE = "Let me have a quick look!"
private const val GENERIC_MESSAGE =
    "Sorry, I can't help you with that. Try asking me about an feature or for directions towards one!"

class ChatService {

    // User request types.
    enum class Request { CHAT, NAVIGATION, RECOGNITION }

    var featureList = emptyList<Feature>()

    private val _request = MutableLiveData<Request>()
    val request: LiveData<Request> get() = _request

    private val _destinationID = MutableLiveData<String>()
    val destinationID: LiveData<String> get() = _destinationID

    fun getResponse(message: String): String {
        Log.i(TAG, "Message: $message")
        return when (parseIntent(message)) {
            Intent.GREETING -> returnResponse(GREETING_MESSAGE)
            Intent.HELP -> returnResponse(HELP_MESSAGE)
            Intent.NAVIGATION -> navigationRequested(message)
            Intent.RECOGNITION -> recognitionRequested()
            Intent.INFORMATION -> informationRequested(message)
            else -> returnResponse(GENERIC_MESSAGE)
        }
    }

    private fun returnResponse(response: String): String {
        _request.postValue(Request.CHAT)
        return response
    }

    private fun parseIntent(message: String): Intent? {
        Intent.values().forEach { intent ->
            intent.triggerPhrases.forEach { phrase ->
                if (isPhraseFoundRegex(message, phrase)) {
                    Log.i(TAG, "Intent: $intent")
                    return intent
                }
            }
        }
        Log.i(TAG, "Intent: null")
        return null
    }

    private fun isPhraseFoundRegex(message: String, phrase: String): Boolean {
        val regex = Regex("(^|\\s)$phrase(\\s|\$)", RegexOption.IGNORE_CASE)
        return message.contains(regex)
    }

    private fun navigationRequested(message: String): String {
        val feature = parseExhibition(message)
        return if (feature == null) {
            INVALID_EXHIBITION_MESSAGE
        } else {
            _request.postValue(Request.NAVIGATION)
            _destinationID.postValue(feature.anchor)
            NAVIGATION_MESSAGE
        }
    }

    private fun parseExhibition(message: String): Feature? {
        featureList.forEach {
            if (isPhraseFoundRegex(message, it.name)) {
                Log.i(TAG, "Feature: $it.name")
                return it
            }
        }
        Log.i(TAG, "Feature: null")
        return null
    }

    private fun recognitionRequested(): String {
        _request.postValue(Request.RECOGNITION)
        return RECOGNITION_MESSAGE
    }

    private fun informationRequested(message: String): String {
        val feature = parseExhibition(message)
        _request.postValue(Request.CHAT)
        return feature?.description ?: INVALID_EXHIBITION_MESSAGE
    }

    fun reset() {
        featureList = emptyList()
        _request.postValue(Request.CHAT)
    }
}