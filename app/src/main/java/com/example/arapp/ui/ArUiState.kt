package com.example.arapp.ui

data class ArUiState(
    val isAvatarMenuVisible: Boolean = false,
    val avatarIsVisible: Boolean = false,
    val avatarIsAnchored: Boolean = false,
    val isTextInput: Boolean = false,
    val isMicButtonShown: Boolean = true,
    val isListening: Boolean = false,
    val isTextResponse: Boolean = false,
    val responseString: String = ""
)