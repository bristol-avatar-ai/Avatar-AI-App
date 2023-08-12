package com.example.avatar_ai_app.language

import com.example.avatar_ai_app.chat.ChatViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

/**
 * Contains the language setting strings for [ChatViewModel] components.
 *
 * @property string For display in the language selection menu.
 * @property ibmModel IBM Speech To Text Model.
 * @property locale Android Text To Speech Locale.
 * @property mlKitLanguage Google MlKit Translate Language.
 * @constructor Create Language instance.
 */

enum class Language(
    val string: String,
    val ibmModel: String,
    val locale: Locale,
    val mlKitLanguage: String
) {
    ENGLISH("English", "en-GB_Telephony", Locale.UK, TranslateLanguage.ENGLISH),
    FRENCH("French", "fr-FR_Telephony", Locale.FRANCE, TranslateLanguage.FRENCH),
    GERMAN("German", "de-DE_Telephony", Locale.GERMANY, TranslateLanguage.GERMAN),
    CHINESE("Chinese", "zh-CN_Telephony", Locale.CHINA, TranslateLanguage.CHINESE)
}