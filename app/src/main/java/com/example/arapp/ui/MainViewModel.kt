package com.example.arapp.ui

import android.Manifest
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.arapp.R
import com.example.arapp.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ArViewModel"
private const val RECORDING_WAIT = 200L

class MainViewModel(private val chatViewModel: ChatViewModel,
private val lifecycleOwner: LifecycleOwner) : ViewModel() {

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

    enum class ErrorType {
        GENERIC,
        NETWORK,
        RECORDING,
        RECORDING_LENGTH,
        SPEECH
    }

    init {
        chatViewModel.status.observe(lifecycleOwner) {
            when(it){
                ChatViewModel.Status.INIT -> {

                }
                ChatViewModel.Status.READY -> {
                    setTextToSpeechReady(true)
                    updateLoadingState(false)
                    setRecordingState(ArUiState.ready)
                    updateTextFieldStringResId(R.string.send_message_hint)
                }
                ChatViewModel.Status.RECORDING -> {
                    setRecordingState(ArUiState.recording)
                    updateTextFieldStringResId(R.string.recording_message)
                }
                ChatViewModel.Status.PROCESSING -> {
                    setRecordingState(ArUiState.processing)
                    updateTextFieldStringResId(R.string.processing_message)
                }
                else -> {}
            }
        }

        chatViewModel.messages.observe(lifecycleOwner) {
            //TODO
            var messages: String? = null
            if(!it.isNullOrEmpty()){
                messages = it[0].string
            }
            _uiState.update { currentState ->
                currentState.copy(
                    responsePresent = messages != null,
                    responseValue = messages ?: ""
                )

            }
            //clear the text field
            textState.value = TextFieldValue()
        }
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
        chatViewModel.newUserMessage(textState.value.text)
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
                chatViewModel.newUserMessage(textState.value.text)
            }

            ArUiState.speech -> {
                startTime = System.currentTimeMillis()
                recordingJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(RECORDING_WAIT)
                    chatViewModel.startRecording()
                }
            }
        }
    }

    fun sendButtonOnRelease() {
        when (uiState.value.inputMode) {
            ArUiState.speech -> {
                if (System.currentTimeMillis() - startTime < RECORDING_WAIT) {
                    recordingJob?.cancel()
                    generateAlert(ErrorType.RECORDING_LENGTH)
                } else {
                    chatViewModel.stopRecording()
                    // Reminder, controller stop is asynchronous, code after this should go in the call-back function.
                }
            }
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

class MainViewModelFactory(private val chatViewModel: ChatViewModel, private val lifecycleOwner: LifecycleOwner) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(chatViewModel, lifecycleOwner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}