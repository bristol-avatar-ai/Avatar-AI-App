package com.example.avatar_ai_app.language

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

/**
 * This enum class contains the language setting strings
 * for each ChatBox component.
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