package com.example.avatar_ai_app.chat

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Represents a chat message stored in the ChatBoxViewModel.
 *
 * @property string The content of the chat message.
 * @property sender An identifier representing the sender of the message (USER or AI).
 * @constructor Creates an instance of ChatMessage with the provided content and sender identifier.
 */
data class ChatMessage(val string: String, val sender: Int) {

    /**
     * Enum class representing the sender of a chat message (USER or AI).
     */
    enum class Type { USER, AI }

    companion object {
        // TODO: Switch to enum
        const val USER = 1
        const val AI = 2

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