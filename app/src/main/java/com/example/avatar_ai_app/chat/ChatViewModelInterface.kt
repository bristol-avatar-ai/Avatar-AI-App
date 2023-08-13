package com.example.avatar_ai_app.chat

import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.language.ChatTranslator
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_cloud_storage.database.entity.Feature

/**
 * Interface for the [ChatViewModel] that defines the methods and properties
 * for managing chat-related functionality.
 *
 * @constructor Creates a new instance of [ChatViewModelInterface].
 */
interface ChatViewModelInterface {

    /**
     * Enum class representing the different status states of [ChatViewModel].
     *
     * @constructor Creates an instance of [Status].
     */
    enum class Status { INIT, READY, RECORDING, PROCESSING }

    // Current ChatViewModel status.
    val status: LiveData<Status>

    // Message history (newest messages are stored first).
    val messages: LiveData<MutableList<ChatMessage>>

    // Current user request type.
    val request: LiveData<ChatService.Request>

    // Requested navigation destination Id.
    val destinationID: LiveData<String>

    /**
     * Sets the language used for translation and speech.
     *
     * @param language The new language to set.
     */
    fun setLanguage(language: Language)

    /**
     * Sets the list of features used by [ChatService].
     *
     * [ChatService] utilises an empty list by default.
     *
     * @param featureList The list of features to set.
     */
    fun setFeatureList(featureList: List<Feature>)

    /**
     * Translates the output message using [ChatTranslator] if available.
     *
     * @param response The response to translate.
     * @return Translated response if available, otherwise the original response.
     */
    suspend fun translateOutput(response: String): String

    /**
     * Processes a new message from the user.
     *
     * Generates a reply, reading and adding it to the message list.
     * Messages and responses are translated if required.
     *
     * @param message The user's message.
     */
    fun newUserMessage(message: String)

    /**
     * Starts audio recording.
     */
    fun startRecording()

    /**
     * Stops audio recording.
     */
    fun stopRecording()

    /**
     * Reads a new response and adds it to the chat history.
     *
     * @param response The response to be used.
     */
    fun newResponse(response: String)

    /**
     * Clears the chat history.
     */
    fun clearChatHistory()
}