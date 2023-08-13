package com.example.avatar_ai_app.ui

import androidx.compose.runtime.mutableStateListOf
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.chat.ChatMessage
import com.example.avatar_ai_app.language.Language


data class UiState(
    //UI
    val isLoaded: Boolean = false,
    val inputMode: Int = speech,
    val textFieldStringResId: Int = R.string.send_message_hint,

    //Recording
    val recordingState: Int = ready,
    val responsePresent: Boolean = false,
    val responseValue: String = "",

    //Text input
    val isTextToSpeechReady: Boolean = false,

    //Language
    val language: Language = Language.ENGLISH,

    //Alert message
    val alertIsShown: Boolean = false,
    val alertResId: Int = R.string.empty_string,

    //Messages
    val messages: MutableList<ChatMessage> = mutableStateListOf(),
    val messagesAreShown: Boolean = true

) {
    companion object {
        const val ready = 0
        const val recording = 1
        const val processing = 2
        const val text = 0
        const val speech = 1
    }

    fun addMessage(msg: ChatMessage) {
        messages.add(0, msg)
    }
}