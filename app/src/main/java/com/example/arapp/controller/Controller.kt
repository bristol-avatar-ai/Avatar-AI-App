package com.example.arapp.controller

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.arapp.audio.AudioRecorder
import com.example.arapp.model.ChatService
import com.example.arapp.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import java.io.File

private const val RECORDING_NAME = "recording"
private const val RECORDING_FILE_TYPE = "ogg"

/**
 * The Controller serves as a wrapper for all non-UI related classes
 * and variables in the ChatBox Fragment.
 */
class Controller(
    listener: MainViewModel,
    context: Context,
    scope: CoroutineScope
) {

    // Initialise ChatService for message responses.
    val chatService = ChatService()

    // Initialise TextToSpeech class for audio responses.
    val textToSpeech: TextToSpeech = TextToSpeech(context, listener)

    // Save the recordings filepath to the cache directory.
    val recordingFile: File =
        File.createTempFile(RECORDING_NAME, RECORDING_FILE_TYPE, context.cacheDir)

    // Initialise AudioRecorder class for recording audio input.
    val audioRecorder: AudioRecorder =
        AudioRecorder(context, recordingFile, scope, listener)

    /*
    * This function releases all resources initialised by the Controller
    * and should be called in onDestroy/onDestroyView of the enclosing
    * Activity/Fragment.
     */
    fun release() {
        chatService.reset()
        audioRecorder.release()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

}