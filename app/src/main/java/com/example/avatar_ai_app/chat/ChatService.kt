package com.example.avatar_ai_app.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.avatar_ai_cloud_storage.database.Exhibition

private const val TAG = "ChatService"

private const val GREETING_MESSAGE = "Hello!"
private const val HELP_MESSAGE =
    "Feel free to ask me anything! I can recognise exhibitions and provide information about them. You can also ask me for directions!"
private const val NAVIGATION_MESSAGE =
    "Please follow the onscreen directions towards your destination!"
private const val INVALID_EXHIBITION_MESSAGE = "Sorry, I don't recognise that exhibition!"
private const val RECOGNITION_MESSAGE = "Let me have a quick look!"
private const val GENERIC_MESSAGE =
    "Sorry, I can't help you with that. Try asking me about an exhibition or for directions towards one!"

class ChatService {

    // User request types.
    enum class Request { CHAT, NAVIGATION, RECOGNITION }

    var exhibitionList = emptyList<Exhibition>()

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
        val exhibition = parseExhibition(message)
        return if (exhibition == null) {
            INVALID_EXHIBITION_MESSAGE
        } else {
            _request.postValue(Request.NAVIGATION)
            _destinationID.postValue(exhibition.anchor)
            NAVIGATION_MESSAGE
        }
    }

    private fun parseExhibition(message: String): Exhibition? {
        exhibitionList.forEach {
            if (isPhraseFoundRegex(message, it.name)) {
                Log.i(TAG, "Exhibition: $it.name")
                return it
            }
        }
        Log.i(TAG, "Exhibition: null")
        return null
    }

    private fun recognitionRequested(): String {
        _request.postValue(Request.RECOGNITION)
        return RECOGNITION_MESSAGE
    }

    private fun informationRequested(message: String): String {
        val exhibition = parseExhibition(message)
        _request.postValue(Request.CHAT)
        return exhibition?.description ?: INVALID_EXHIBITION_MESSAGE
    }

    fun reset() {
        exhibitionList = emptyList()
        _request.postValue(Request.CHAT)
    }
}