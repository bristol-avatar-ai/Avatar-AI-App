package com.example.avatar_ai_app.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.avatar_ai_app.ErrorListener
import com.example.avatar_ai_app.language.Language

/**
 * Factory class for creating instances of [ChatViewModel].
 *
 * @param application The application context.
 * @param language The initial language for translation and speech.
 * @param errorListener The listener for handling error events.
 */
class ChatViewModelFactory(
    private val application: Application,
    private val language: Language,
    private val errorListener: ErrorListener
) : ViewModelProvider.Factory {
    /**
     * Creates an instance of the requested ViewModel class.
     *
     * @param modelClass The class of the ViewModel to be created.
     * @return An instance of the requested ViewModel class.
     * @throws IllegalArgumentException if the provided class is not [ChatViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, language, errorListener) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}