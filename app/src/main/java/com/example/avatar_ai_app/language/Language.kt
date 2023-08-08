package com.example.avatar_ai_app.language

import java.util.Locale

/**
 * This enum class contains the language setting strings
 * for each ChatBox component.
 */
enum class Language(val locale: Locale, val string: String) {
    English(Locale.UK, "English"),
    French(Locale.FRENCH, "French")
}