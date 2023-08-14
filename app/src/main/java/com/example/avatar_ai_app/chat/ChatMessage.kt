package com.example.avatar_ai_app.chat

import android.annotation.SuppressLint
import com.example.avatar_ai_app.shared.MessageType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Represents a chat message stored in the ChatBoxViewModel.
 *
 * @property string The content of the chat message.
 * @property type An identifier representing the type of the message (USER or AI).
 * @constructor Creates an instance of ChatMessage with the provided content and type identifier.
 */
data class ChatMessage(val string: String, val type: MessageType) {
    companion object {
        // Format for timestamp: 'HH:mm'.
        @SuppressLint("ConstantLocale")
        val timeStampFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    // Save timestamp on construction.
    private val timeStamp = Calendar.getInstance().time

    /**
     * Returns a formatted string of the message's timestamp in the 'HH:mm' format.
     *
     * @return The formatted timestamp string.
     */
    fun getTimeStampString(): String {
        return timeStampFormat.format(timeStamp)
    }

}