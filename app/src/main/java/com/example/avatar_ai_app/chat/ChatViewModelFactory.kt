package com.example.avatar_ai_app.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [ChatViewModelFactory] os a custom ViewModelFactory used to create instances of [ChatViewModel].
 */

class ChatViewModelFactory(private val context: Context, private val language: Language) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context, language) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}