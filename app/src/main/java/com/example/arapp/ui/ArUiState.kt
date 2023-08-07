package com.example.arapp.ui

import com.example.arapp.R
import com.example.arapp.chat.Language


data class ArUiState(
    //UI
    val isLoading: Boolean = true,
    val inputMode: Int = speech,
    val textFieldStringResId: Int = R.string.send_message_hint,

    //Recording
    val recordingState: Int = ready,
    val responsePresent: Boolean = false,
    val responseValue: String = "",

    //Text input
    val isTextToSpeechReady: Boolean = false,

    //Language
    val language: Language = Language.English,

    //Alert message
    val alertIsShown: Boolean = false,
    val alertResId: Int = R.string.empty_string

) {
    companion object {
        const val ready = 0
        const val recording = 1
        const val processing = 2
        const val text = 0
        const val speech = 1
    }
}