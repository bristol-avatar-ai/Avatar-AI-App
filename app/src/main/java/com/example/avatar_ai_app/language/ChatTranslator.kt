package com.example.avatar_ai_app.language

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ChatTranslator"

// Number of translators requiring initialisation.
private const val TOTAL_INIT_COUNT = 2

/**
 * Chat translator class for translating messages to and from English.
 *
 * Ensure that the [ChatTranslator] has successfully initialised before attempting a translation.
 *
 * @property language The target language for translation.
 * @property initListener Listener for translator initialisation status.
 * @constructor Initializes the ChatTranslator with the specified language and initListener.
 */
class ChatTranslator(private var language: String, private val initListener: InitListener) {

    /**
     * Listener interface for translator initialisation callbacks.
     *
     * Use [onTranslatorInit] to register the translator's initialisation status.
     *
     * @constructor Initialises the InitListener.
     */
    interface InitListener {
        /**
         * Callback method for translator initialisation status.
         *
         * @param success Indicates if the translator was successfully initialised.
         */
        fun onTranslatorInit(success: Boolean)
    }

    // Translator type options.
    private enum class Type { INPUT, OUTPUT }

    // Translator initialisation conditions.
    private var conditions = DownloadConditions.Builder()
        .requireWifi()
        .build()

    // Translator instances.
    private var inputTranslator: Translator? = null
    private var outputTranslator: Translator? = null

    // Initialised translators counter.
    private var initCount = 0

    init {
        setLanguage(language)
    }

    /**
     * Sets the target language for translation.
     *
     * @param language The target language to set.
     */
    fun setLanguage(language: String) {
        close()
        this.language = language
        if (language == TranslateLanguage.ENGLISH) {
            initListener.onTranslatorInit(true)
        } else {
            setTranslator(Type.INPUT)
            setTranslator(Type.OUTPUT)
        }
    }

    /**
     * Closes the translators and resets the initialization count.
     */
    fun close() {
        inputTranslator?.close()
        outputTranslator?.close()
        inputTranslator = null
        outputTranslator = null
        initCount = 0
    }

    /*
    * Sets up a translator based on the specified type (input or output).
     */
    private fun setTranslator(type: Type) {
        val options = when (type) {
            Type.INPUT -> getTranslatorOptions(language, TranslateLanguage.ENGLISH)
            Type.OUTPUT -> getTranslatorOptions(TranslateLanguage.ENGLISH, language)
        }
        val translator = Translation.getClient(options)

        initialiseTranslator(translator, type)
        if (type == Type.INPUT) {
            inputTranslator = translator
        } else {
            outputTranslator = translator
        }
    }

    /*
    * Returns TranslatorOptions for the specified source and target languages.
     */
    private fun getTranslatorOptions(srcLanguage: String, dstLanguage: String): TranslatorOptions {
        return TranslatorOptions.Builder()
            .setSourceLanguage(srcLanguage)
            .setTargetLanguage(dstLanguage)
            .build()
    }

    /*
    * Initializes a translator and handles the initialization count. Calls the
    * initialisation callback method if all the translators have been initialised.
     */
    private fun initialiseTranslator(translator: Translator, type: Type) {
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.i(TAG, "initialiseTranslator: $type translator initialised")

                if (++initCount >= TOTAL_INIT_COUNT) {
                    if (inputTranslator != null && outputTranslator != null) {
                        Log.i(TAG, "initialiseTranslator: ChatTranslator ready")
                        initListener.onTranslatorInit(true)
                    } else {
                        Log.e(TAG, "initialiseTranslator: ")
                        initListener.onTranslatorInit(false)
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "initialiseTranslator: $type translator initialisation failed", it)
            }
    }

    /**
     * Translates the input message into English.
     *
     * @param message The message to be translated.
     * @return The translated message, or null if translation fails.
     */
    suspend fun translateInput(message: String): String? {
        return getTranslation(message, inputTranslator)
    }

    /**
     * Translates the output response to the set [language].
     *
     * @param response The response to be translated.
     * @return The translated response, or null if translation fails.
     */
    suspend fun translateOutput(response: String): String? {
        return getTranslation(response, outputTranslator)
    }

    /*
    * Retrieves translation using the provided translator.
     */
    private suspend fun getTranslation(message: String, translator: Translator?): String? {
        if (language == TranslateLanguage.ENGLISH) {
            Log.i(TAG, "translate: translation success")
            return message
        } else if (translator == null) {
            Log.e(TAG, "getTranslation: translator not ready")
            return null
        }
        return translate(message, translator)
    }

    /*
    * Performs translation and handles success and failure.
     */
    private suspend fun translate(message: String, translator: Translator): String? {
        return suspendCoroutine { continuation ->
            translator.translate(message)
                .addOnSuccessListener { translatedText ->
                    Log.i(TAG, "translate: translation success")
                    continuation.resume(translatedText)
                }
                .addOnFailureListener {
                    Log.e(TAG, "translate: translation failed", it)
                    continuation.resume(null)
                }
        }
    }

}