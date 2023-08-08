package com.example.avatar_ai_app.chat

import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_cloud_storage.database.Exhibition

/**
 * ChatViewModelInterface is the interface between [MainViewModel] and the
 * Chat ViewModel, which handles all non-UI chat functionality.
 */

interface ChatViewModelInterface {
    // ViewModel status types.
    enum class Status { INIT, READY, RECORDING, PROCESSING }

    // ViewModel status.
    val status: LiveData<Status>

    // Current user request type.
    val request: LiveData<ChatService.Request>

    // Requested navigation destination Id.
    val destinationID: LiveData<String>

    // Informs MainViewModel of error messages.
    val error: LiveData<MainViewModel.ErrorType>

    // Chat history - newest messages are stored first.
    val messages: LiveData<MutableList<ChatMessage>>

    // Sets the chat language.
    fun setLanguage(language: Language)

    // Sets the current exhibition list - an empty list is used by default.
    fun setExhibitionList(exhibitionList: List<Exhibition>)

    // Provides a new user message to the ChatService.
    fun newUserMessage(message: String)

    // Starts recording a user message.
    fun startRecording()

    // Stops recording a user message.
    fun stopRecording()

    // Uses the given string as a new chat response.
    fun newResponse(response: String)

    // Clears the chat history.
    fun clearChatHistory()
}