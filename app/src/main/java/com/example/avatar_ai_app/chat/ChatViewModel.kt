package com.example.avatar_ai_app.chat

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.ErrorListener
import com.example.avatar_ai_app.audio.AudioRecorder
import com.example.avatar_ai_app.chat.ChatViewModelInterface.Status
import com.example.avatar_ai_app.language.ChatTranslator
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_app.network.TranscriptionApi
import com.example.avatar_ai_app.shared.ErrorType
import com.example.avatar_ai_app.shared.MessageType
import com.example.avatar_ai_cloud_storage.database.entity.Feature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


private const val TAG = "ChatViewModel"

// Audio recording details.
private const val RECORDING_NAME = "recording"
private const val RECORDING_FILE_TYPE = "ogg"

// Number of components requiring confirmation of initialisation.
private const val TOTAL_INIT_COUNT = 2

/**
 * ViewModel for managing chat-related functionality.
 *
 * This ViewModel class handles various chat-related tasks, including translating user input,
 * processing responses, recording and transcribing audio, and handling message history.
 *
 * @property application The application context.
 * @property language The initial language used for translation and speech.
 * @property errorListener The listener for handling error events.
 */
class ChatViewModel(
    application: Application,
    private var language: Language,
    private val errorListener: ErrorListener
) : AndroidViewModel(application),
    ChatViewModelInterface,
    OnInitListener,
    AudioRecorder.RecordingCompletionListener,
    ChatTranslator.InitListener {

    // Application context getter.
    private val context get() = getApplication<Application>().applicationContext

    // File to store recordings in the cache directory.
    private val recordingFile: File =
        File.createTempFile(
            RECORDING_NAME,
            RECORDING_FILE_TYPE,
            context.cacheDir
        )

    // Current ChatViewModel status.
    private val _status = MutableLiveData(Status.INIT)
    override val status: LiveData<Status> get() = _status

    // Message history (newest messages are stored first).
    private val _messages = MutableLiveData<MutableList<ChatMessage>>(mutableListOf())
    override val messages: LiveData<MutableList<ChatMessage>> get() = _messages

    // Initialised components counter.
    private var initCount = 0

    // Instance of ChatService with exposed intent and destinationID LiveData.
    private val chatService = ChatService(context)
    override val intent: LiveData<Intent> get() = chatService.intent
    override val destinationID: LiveData<String> get() = chatService.destinationID

    // Instance of AudioRecorder.
    private val audioRecorder: AudioRecorder = AudioRecorder(
        context,
        recordingFile,
        viewModelScope,
        this
    )

    // Instance of ChatTranslator and readiness flag
    private val chatTranslator = ChatTranslator(language.mlKitLanguage, this)
    private var isChatTranslatorReady = false

    // Instance of TextToSpeech and readiness flag.
    private val textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var isTextToSpeechReady = false

    /**
     * Called when ViewModel is no longer in use.
     *
     * Clears resources and resets status.
     */
    override fun onCleared() {
        super.onCleared()
        _messages.value?.clear()
        chatService.reset()
        audioRecorder.release()
        chatTranslator.close()
        isChatTranslatorReady = false
        textToSpeech.stop()
        textToSpeech.shutdown()
        isTextToSpeechReady = false
        initCount = 0
    }

    /*
     * Increments the initialisation counter and
     * checks if all components are initialised.
     */
    private fun componentInitialised() {
        initCount++
        if (initCount >= TOTAL_INIT_COUNT) {
            _status.postValue(Status.READY)
            Log.i(TAG, "componentInitialised: Status: READY")
        }
    }

    /**
     * Callback indicating the status of [ChatTranslator] initialisation.
     *
     * @param success Indicates whether ChatTranslator was successfully initialised.
     */
    override fun onTranslatorInit(success: Boolean) {
        Log.i(TAG, "onTranslatorInit: ChatTranslator ready")
        isChatTranslatorReady = success
        componentInitialised()
    }

    /**
     * Callback indicating the status of [TextToSpeech] initialisation.
     *
     * @param status The initialisation status code.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setTextToSpeechLanguage()
        } else {
            Log.e(TAG, "onInit: Failed to initialise TextToSpeech")
            errorListener.onError(ErrorType.NETWORK)
        }
    }

    /*
    * Sets the TextToSpeech language and handles errors.
     */
    private fun setTextToSpeechLanguage() {
        val result = textToSpeech.setLanguage(language.locale)

        isTextToSpeechReady = if (result == TextToSpeech.LANG_MISSING_DATA
            || result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.e(TAG, "setTextToSpeechLanguage: Failed to set TextToSpeech language")
            errorListener.onError(ErrorType.SPEECH)
            false
        } else {
            Log.i(TAG, "setTextToSpeechLanguage: TextToSpeech ready")
            true
        }
        componentInitialised()
    }

    /**
     * Sets the language used for translation and speech.
     *
     * @param language The new language to set.
     */
    override fun setLanguage(language: Language) {
        this.language = language
        _status.value = Status.INIT
        Log.i(TAG, "setLanguage: Status: INIT")
        initCount = 0
        setTextToSpeechLanguage()
        chatTranslator.setLanguage(language.mlKitLanguage)
    }

    /**
     * Sets the list of features used by [ChatService].
     *
     * [ChatService] utilises an empty list by default.
     *
     * @param featureList The list of features to set.
     */
    override fun setFeatureList(featureList: List<Feature>) {
        chatService.featureList = featureList
        Log.i(TAG, "setFeatureList: featureList size: ${featureList.size}")
    }

    /**
     * Adds a new message to the message list.
     *
     * The message list [LiveData] is posted to update any observers.
     *
     * @param message The message to add.
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _messages.value ?: mutableListOf()
        currentMessages.add(0, message)
        _messages.postValue(currentMessages)
    }

    /**
     * Translates the output message using [ChatTranslator] if available.
     *
     * @param response The response to translate.
     * @return Translated response if available, otherwise the original response.
     */
    override suspend fun translateOutput(response: String): String {
        return if (isChatTranslatorReady) {
            chatTranslator.translateOutput(response)?.apply {
                Log.i(TAG, "translateOutput: translated response: $this")
                return this
            }
            Log.w(TAG, "translateOutput: translation failed")
            return response
        } else {
            Log.w(TAG, "translateOutput: ChatTranslator not ready")
            response
        }
    }

    /**
     * Processes a new message from the user.
     *
     * Generates a reply, reading and adding it to the message list.
     * Messages and responses are translated if required.
     *
     * @param message The user's message.
     */
    override fun newUserMessage(message: String) {
        Log.i(TAG, "newUserMessage: message: $message")
        viewModelScope.launch(Dispatchers.IO) {
            addMessage(ChatMessage(message, MessageType.USER))

            val englishMessage = chatTranslator.translateInput(message)
            if (englishMessage == null) {
                errorListener.onError(ErrorType.NETWORK)
            } else {
                val englishResponse = chatService.getResponse(englishMessage)
                val response = translateOutput(englishResponse)

                readMessage(response)
                addMessage(ChatMessage(response, MessageType.RESPONSE))
                Log.i(TAG, "newUserMessage: response: $response")
            }
        }
    }

    /*
    * Reads out a message using TextToSpeech if available.
     */
    private fun readMessage(message: String) {
        if (isTextToSpeechReady) {
            textToSpeech.speak(
                message,
                TextToSpeech.QUEUE_FLUSH, null, null
            )
            Log.i(TAG, "readMessage: success")
        }
    }

    /**
     * Starts audio recording.
     */
    override fun startRecording() {
        if (_status.value == Status.READY) {
            try {
                audioRecorder.start()
                _status.postValue(Status.RECORDING)
                Log.i(TAG, "startRecording: Status: RECORDING")
            } catch (_: Exception) {
                errorListener.onError(ErrorType.RECORDING)
            }
        } else {
            Log.w(TAG, "startRecording: cannot start recording when status is ${_status.value}")
        }
    }

    /**
     * Stops audio recording.
     */
    override fun stopRecording() {
        audioRecorder.stop()
        Log.i(TAG, "stopRecording: called")
    }

    /**
     * Callback for when audio recording is completed.
     *
     * Transcribes the recording into text, generates a reply, then passes it to [newUserMessage].
     */
    override fun onRecordingCompleted() {
        _status.postValue(Status.PROCESSING)
        Log.i(TAG, "onRecordingCompleted: Status: PROCESSING")

        viewModelScope.launch(Dispatchers.IO) {
            val message = TranscriptionApi.transcribe(recordingFile, language.ibmModel)
            if (recordingFile.exists()) {
                recordingFile.delete()
            }
            if (message != null) {
                Log.i(TAG, "Transcribed: $message")
                newUserMessage(message)
            } else {
                errorListener.onError(ErrorType.NETWORK)
            }
            _status.postValue(Status.READY)
            Log.i(TAG, "onRecordingCompleted: Status: READY")
        }
    }

    /**
     * Reads a new response and adds it to the chat history.
     *
     * @param response The response to be used.
     */
    override fun newResponse(response: String) {
        Log.i(TAG, "newResponse: $response")
        viewModelScope.launch(Dispatchers.IO) {
            readMessage(response)
            addMessage(ChatMessage(response, MessageType.RESPONSE))
        }
    }

    /**
     * Clears the chat history.
     */
    override fun clearChatHistory() {
        _messages.value?.clear()
    }

}