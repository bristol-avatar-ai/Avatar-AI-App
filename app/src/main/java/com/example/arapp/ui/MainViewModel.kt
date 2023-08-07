package com.example.arapp.ui

import android.Manifest
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arapp.R
import com.example.arapp.audio.AudioRecorder
import com.example.arapp.chat.Controller
import com.example.arapp.network.TranscriptionApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "ArViewModel"
private const val RECORDING_WAIT = 200L

class MainViewModel : ViewModel(),
    TextToSpeech.OnInitListener,
    AudioRecorder.RecordingCompletionListener {

    private val _uiState = MutableStateFlow(ArUiState())
    private val _isCameraEnabled = MutableStateFlow(false)
    private val _isRecordingEnabled = MutableStateFlow(false)
    private val _alertContent = MutableLiveData<Pair<Int, Int>>()
    private var startTime = System.currentTimeMillis()
    private var recordingJob: Job? = null

    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    val isCameraEnabled: StateFlow<Boolean>
        get() = _isCameraEnabled

    val isRecordingEnabled: StateFlow<Boolean>
        get() = _isRecordingEnabled

    val alertContent: LiveData<Pair<Int, Int>>
        get() = _alertContent

    //Queue for storing permission strings
    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    val focusRequester = FocusRequester()
    var touchPosition = mutableStateOf(Offset.Zero)
    val textState = mutableStateOf(TextFieldValue())
    var textFieldFocusState = mutableStateOf(false)

    // Controller containing all non-UI components.
    lateinit var controller: Controller

    enum class ErrorType {
        GENERIC,
        NETWORK,
        RECORDING,
        RECORDING_LENGTH,
        SPEECH
    }

    /*
    * Initialises the controller
    */
    fun initController(context: Context) {
        controller = Controller(this, context, viewModelScope)
    }

    /*
    * This function sets the TextToSpeech language and handles any errors.
    */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setTextToSpeechLanguage()
        } else {
            Log.e(TAG, "Failed to initialise TextToSpeech\n$status")
            generateAlert(ErrorType.SPEECH)
        }
        updateLoadingState(false)
    }

    /*
    * If a permission is not granted, it is added to the queue
    */
    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted) {
            visiblePermissionDialogQueue.add(0, permission)
        }
        updatePermissionStatus(permission, isGranted)
    }

    /*
    * Updates the permission states stored in the live data variables
    */
    fun updatePermissionStatus(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.CAMERA -> _isCameraEnabled.value = isGranted
            Manifest.permission.RECORD_AUDIO -> _isRecordingEnabled.value = isGranted
        }

    }

    /*
    * Dismisses the dialog box for the last permission added to the queue
    */
    fun dismissDialog() {
        visiblePermissionDialogQueue.removeLast()
    }

    /*
    * This function sets the TextToSpeech language and handles any errors.
    */
    private fun setTextToSpeechLanguage() {
        val result = controller.textToSpeech.setLanguage(Locale.UK)
        if (result == TextToSpeech.LANG_MISSING_DATA
            || result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.e(TAG, "Failed to set TextToSpeech language\n$result")
            generateAlert(ErrorType.SPEECH)
        } else {
            setTextToSpeechReady(true)
        }
    }

    /*
    * onRecordingCompleted is called when the AudioRecorder stops recording.
    * The action button is disables and the hint is modified. A reply is
    * generated before the control and hint are reset.
     */
    override fun onRecordingCompleted() {
        setRecordingState(ArUiState.processing)
        updateTextFieldStringResId(R.string.processing_message)
        viewModelScope.launch {
            replyToSpeech()
            setRecordingState(ArUiState.ready)
            updateTextFieldStringResId(R.string.send_message_hint)
        }
    }

    /*
    * This function transcribes the recordingFile into text, deletes the
    * file, and then generates a reply.
     */
    private suspend fun replyToSpeech() {
        val message = TranscriptionApi.transcribe(controller.recordingFile)
        if (controller.recordingFile.exists()) {
            controller.recordingFile.delete()
        }
        if (message != null) {
            generateReply(message)
        } else {
            generateAlert(ErrorType.NETWORK)
            Log.e(TAG, "Wifi error")
        }
    }

    /*
    * Returns a mic or send icon for the send button depending on the focus state
    */
    fun getMicOrSendIcon(): Int {
        return if (!textFieldFocusState.value) {
            R.drawable.mic_icon
        } else {
            R.drawable.send_icon
        }
    }

    private fun updateTextFieldStringResId(resId: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                textFieldStringResId = resId
            )
        }
    }

    fun onSend() {
        generateReply(textState.value.text)
    }

    /*
    * Gets a response to the input message with ChatService. Generates an audio
    * reply and plays it if TextToSpeech has been initialised correctly.
    * Coroutines are used to prevent blocking the main thread.
     */
    private fun generateReply(message: String) {
        viewModelScope.launch {
            // Generate reply with ChatService.
            val reply = controller.chatService.getResponse(message)
            outputMessage(reply)
        }
    }

    private fun outputMessage(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                responsePresent = true,
                responseValue = message
            )
        }
        //clear the text field
        textState.value = TextFieldValue()
        if (uiState.value.isTextToSpeechReady) {
            controller.textToSpeech.speak(
                message,
                TextToSpeech.QUEUE_FLUSH, null, null
            )
        }
    }

    fun dismissTextResponse() {
        _uiState.update { currentState ->
            currentState.copy(
                responsePresent = false
            )
        }
    }

    private fun setTextToSpeechReady(ready: Boolean) {
        _uiState.update {
            it.copy(
                isTextToSpeechReady = ready
            )
        }
    }

    private fun setRecordingState(state: Int) {
        _uiState.update {
            it.copy(
                recordingState = state
            )
        }
    }

    private fun updateLoadingState(loading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = loading
            )
        }
    }


    // Update the user input mode in the uiState based on the textField focus
    fun updateInputMode() {
        _uiState.update { currentState ->
            when (textFieldFocusState.value) {
                true -> currentState.copy(
                    inputMode = ArUiState.text
                )

                false -> currentState.copy(
                    inputMode = ArUiState.speech
                )
            }
        }
    }

    fun sendButtonOnPress() {
        when (uiState.value.inputMode) {
            ArUiState.text -> {
                generateReply(textState.value.text)
            }

            ArUiState.speech -> {
                startTime = System.currentTimeMillis()
                recordingJob = viewModelScope.launch(Dispatchers.IO) {
                    setRecordingState(ArUiState.recording)
                    updateTextFieldStringResId(R.string.recording_message)
                    delay(RECORDING_WAIT)
                    controller.audioRecorder.start()
                }
            }
        }
    }

    fun sendButtonOnRelease() {
        when (uiState.value.inputMode) {
            ArUiState.speech -> {
                if (System.currentTimeMillis() - startTime < RECORDING_WAIT) {
                    recordingJob?.cancel()
                    setRecordingState(ArUiState.ready)
                    updateTextFieldStringResId(R.string.send_message_hint)
                    generateAlert(ErrorType.RECORDING_LENGTH)
                } else {
                    controller.audioRecorder.stop()
                    // Reminder, controller stop is asynchronous, code after this should go in the call-back function.
                }
            }
        }
    }

    fun resetControllerState() {
        _uiState.update { currentState ->
            currentState.copy(
                inputMode = ArUiState.speech,
                recordingState = ArUiState.ready,
                isTextToSpeechReady = false,
            )
        }
    }

    /*
    * Updates the alertContent variable and sets the alertIsShown state to true.
    * This displays an alert with the appropriate dialog
    */
    private fun generateAlert(error: ErrorType) {
        val alertContent = when (error) {
            ErrorType.GENERIC -> Pair(R.string.error_title, R.string.error_message)
            ErrorType.NETWORK -> Pair(R.string.error_title, R.string.network_error_message)
            ErrorType.RECORDING -> Pair(R.string.error_title, R.string.recording_error_message)
            ErrorType.RECORDING_LENGTH -> Pair(
                R.string.error_title,
                R.string.recording_length_error_message
            )

            ErrorType.SPEECH -> Pair(R.string.error_title, R.string.speech_error_message)
        }
        _alertContent.value = alertContent
        _uiState.update { currentState ->
            currentState.copy(
                alertIsShown = true
            )
        }
    }

    /*
    * Dismisses the alert dialog box
    */
    fun alertOnDismiss() {
        _uiState.update { currentState ->
            currentState.copy(
                alertIsShown = false
            )
        }
    }

//    fun generateCoordinates(
//        constraints: BoxWithConstraintsScope,
//        tap: Offset
//    ): IntOffset {
//        val x = tap.x
//        val y = tap.y
//        val width = constraints.constraints.maxWidth
//        val height = constraints.constraints.maxHeight
//
//        val topHalf = y < height / 2
//        val leftHalf = x < width / 2
//
//        return IntOffset(tap.x.toInt(), tap.y.toInt())
//    }
}