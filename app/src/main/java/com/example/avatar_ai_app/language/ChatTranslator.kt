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

private const val TOTAL_INIT_COUNT = 2

class ChatTranslator(private var language: String, private val initListener: InitListener) {

    interface InitListener {
        fun onTranslatorInit(success: Boolean)
    }

    private enum class TranslatorType { MESSAGE, RESPONSE }

    private var conditions = DownloadConditions.Builder()
        .requireWifi()
        .build()

    private var messageTranslator: Translator? = null

    private var responseTranslator: Translator? = null

    private var initCount = 0

    init {
        setLanguage(language)
    }

    fun setLanguage(language: String) {
        this.language = language
        close()
        if (language == TranslateLanguage.ENGLISH) {
            initListener.onTranslatorInit(true)
        } else {
            setTranslator(TranslatorType.MESSAGE)
            setTranslator(TranslatorType.RESPONSE)
        }
    }

    fun close() {
        messageTranslator?.close()
        responseTranslator?.close()
        messageTranslator = null
        responseTranslator = null
        initCount = 0
    }

    private fun setTranslator(type: TranslatorType) {
        val options = when (type) {
            TranslatorType.MESSAGE -> getTranslatorOptions(language, TranslateLanguage.ENGLISH)
            TranslatorType.RESPONSE -> getTranslatorOptions(TranslateLanguage.ENGLISH, language)
        }
        val translator = Translation.getClient(options)

        initialiseTranslator(translator, type)
        if (type == TranslatorType.MESSAGE) {
            messageTranslator = translator
        } else {
            responseTranslator = translator
        }
    }

    private fun initialiseTranslator(translator: Translator, type: TranslatorType) {
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.i(TAG, "$type init")
                if (++initCount >= TOTAL_INIT_COUNT) {
                    initListener.onTranslatorInit(
                        messageTranslator != null && responseTranslator != null
                    )
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "initialiseTranslator: $type translator initialisation failed", it)
            }
    }

    private fun getTranslatorOptions(srcLanguage: String, dstLanguage: String): TranslatorOptions {
        return TranslatorOptions.Builder()
            .setSourceLanguage(srcLanguage)
            .setTargetLanguage(dstLanguage)
            .build()
    }

    suspend fun translateMessage(message: String): String? {
        return translate(message, messageTranslator)
    }

    suspend fun translateResponse(response: String): String? {
        return translate(response, responseTranslator)
    }

    private suspend fun translate(message: String, translator: Translator?): String? {
        if (language == TranslateLanguage.ENGLISH) {
            return message
        } else if (translator == null) {
            Log.e(TAG, "translate: translator not ready")
            return null
        }
        return suspendCoroutine { continuation ->
            translator.translate(message)
                .addOnSuccessListener { translatedText ->
                    continuation.resume(translatedText)
                }
                .addOnFailureListener {
                    Log.e(TAG, "translate: translation failed", it)
                    continuation.resume(null)
                }
        }
    }

}