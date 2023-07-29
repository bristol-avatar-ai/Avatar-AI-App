package com.example.arapp.ui

import com.example.arapp.const.Language


data class ArUiState(
    //Avatar
    val avatarIsVisible: Boolean = false,
    val avatarIsAnchored: Boolean = false,

    //UI
    val isLoading: Boolean = true,
    val isAvatarMenuVisible: Boolean = false,
    val inputMode: Int = speech,

    //Recording
    val isRecordingEnabled: Boolean = false,
    val recordingState: Int = ready,
    val responsePresent: Boolean = false,
    val responseValue: String = "",

    //Text input
    val isTextToSpeechReady: Boolean = false,

    //Language
    val language: Language = Language.English

) {
    companion object {
        const val ready = 0
        const val recording = 1
        const val processing = 2
        const val text = 0
        const val speech = 1
    }
}