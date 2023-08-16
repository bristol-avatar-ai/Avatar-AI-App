package com.example.avatar_ai_app.ui

import android.Manifest
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.ar.ArViewModel
import com.example.avatar_ai_app.ar.ArViewModelInterface
import com.example.avatar_ai_app.chat.ChatMessage
import com.example.avatar_ai_app.chat.ChatViewModel
import com.example.avatar_ai_app.chat.ChatViewModelInterface
import com.example.avatar_ai_app.chat.Intent
import com.example.avatar_ai_app.data.DatabaseViewModel
import com.example.avatar_ai_app.data.DatabaseViewModelInterface
import com.example.avatar_ai_app.imagerecognition.ImageRecognitionViewModel
import com.example.avatar_ai_app.language.Language
import com.example.avatar_ai_app.shared.MessageType
import io.github.sceneview.ar.ArSceneView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ArViewModel"
private const val RECORDING_WAIT = 100L

class MainViewModel(
    private val chatViewModel: ChatViewModelInterface,
    private val databaseViewModel: DatabaseViewModelInterface,
    private val arViewModel: ArViewModelInterface,
    private val imageViewModel: ImageRecognitionViewModel,
    lifecycleOwner: LifecycleOwner
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    private val _isCameraEnabled = MutableStateFlow(false)
    private val _isRecordingEnabled = MutableStateFlow(false)
    private val _isRecordingReady = MutableStateFlow(true)
    private var startTime = System.currentTimeMillis()
    private var recordingJob: Job? = null

    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isCameraEnabled: StateFlow<Boolean>
        get() = _isCameraEnabled

    val isRecordingEnabled: StateFlow<Boolean>
        get() = _isRecordingEnabled

    val isRecordingReady: StateFlow<Boolean>
        get() = _isRecordingReady

    //Queue for storing permission strings
    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    val focusRequester = FocusRequester()
    var touchPosition = mutableStateOf(Offset.Zero)
    val textState = mutableStateOf(TextFieldValue())
    var textFieldFocusState = mutableStateOf(false)

    init {
        chatViewModel.status.observe(lifecycleOwner) {
            when (it) {
                ChatViewModelInterface.Status.INIT -> {
                    setTextToSpeechReady(false)
                    updateLoadingState(false)
                }

                ChatViewModelInterface.Status.READY -> {
                    setTextToSpeechReady(true)
                    updateLoadingState(true)
                    setRecordingState(UiState.ready)
                    updateTextFieldStringResId(R.string.send_message_hint)
                    _isRecordingReady.value = true
                }

                ChatViewModelInterface.Status.RECORDING -> {
                    setRecordingState(UiState.recording)
                    updateTextFieldStringResId(R.string.recording_message)
                    _isRecordingReady.value = false
                }

                ChatViewModelInterface.Status.PROCESSING -> {
                    setRecordingState(UiState.processing)
                    updateTextFieldStringResId(R.string.processing_message)
                    _isRecordingReady.value = false
                }

                else -> {}
            }
        }

        // TODO: fill this out
        imageViewModel.status.observe(lifecycleOwner) {
            when (it) {
                null -> {}
                ImageRecognitionViewModel.Status.INIT -> {}
                ImageRecognitionViewModel.Status.READY -> {}
                ImageRecognitionViewModel.Status.ERROR -> {}
            }
        }

        chatViewModel.messages.observe(lifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                displayMessages(it)
            }
            //clear the text field
            textState.value = TextFieldValue()
        }

        // TODO: fill this out
        chatViewModel.intent.observe(lifecycleOwner) {
            when (it) {
                Intent.RECOGNITION -> processRecognitionRequest()
                else -> {}
            }
        }
    }

    private fun processRecognitionRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            val featureName = imageViewModel.recogniseFeature()
            if (featureName != null) {
                val feature = databaseViewModel.getFeature(featureName)
                if (feature != null) {
                    chatViewModel.newResponse(feature.description)
                } else {
                    chatViewModel.newResponse("Sorry, I don't recognise this feature!")
                }
            } else {
                chatViewModel.newResponse("Sorry, I don't recognise this feature!")
            }
        }
    }

    //TODO rework this function so that it accounts for multiple messages from one sender, and empty messages
    private fun displayMessages(messages: List<ChatMessage>) {
        //check that there is both a message and a response
        if (messages.size % 2 != 0) return

        viewModelScope.launch {
            if (messages[1].type == MessageType.USER) {
                uiState.value.addMessage(messages[1])
            }
            if (messages[0].type == MessageType.RESPONSE) {
                uiState.value.addMessage(messages[0])
            }
        }

        _uiState.update { currentState ->
            currentState.copy(
                messagesAreShown = true
            )
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
                isLoaded = loading
            )
        }
    }


    // Update the user input mode in the uiState based on the textField focus
    fun updateInputMode() {
        _uiState.update { currentState ->
            when (textFieldFocusState.value) {
                true -> currentState.copy(
                    inputMode = UiState.text
                )

                false -> currentState.copy(
                    inputMode = UiState.speech
                )
            }
        }
    }

    fun sendButtonOnPress() {
        when (uiState.value.inputMode) {
            UiState.text -> {
                chatViewModel.newUserMessage(textState.value.text)
            }

            UiState.speech -> {
                if (uiState.value.recordingState == UiState.ready) {
                    startTime = System.currentTimeMillis()
                    recordingJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(RECORDING_WAIT)
                        chatViewModel.startRecording()
                    }
                }
            }
        }
    }

    fun sendButtonOnRelease() {
        when (uiState.value.inputMode) {
            UiState.speech -> {
                if (System.currentTimeMillis() - startTime < RECORDING_WAIT) {
                    recordingJob?.cancel()
                    viewModelScope.launch {
                        updateTextFieldStringResId(R.string.recording_length_error_message)
                        delay(1000L)
                        updateTextFieldStringResId(R.string.send_message_hint)
                    }
                } else {
                    chatViewModel.stopRecording()
                    // Reminder, controller stop is asynchronous, code after this should go in the call-back function.
                }
            }
        }
    }

    fun onLanguageSelectionResult(language: Language) {
        _uiState.update { currentState ->
            currentState.copy(
                language = language
            )
        }
        chatViewModel.setLanguage(language)
    }

    fun handleSwipe(pan: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                messagesAreShown = pan <= 0
            )
        }
    }

    fun initialiseArScene(arSceneView: ArSceneView) {
        arViewModel.initialiseArScene(arSceneView)
    }

    fun addModelToScene(arSceneView: ArSceneView, modelType: ArViewModel.ModelType) {
        arViewModel.addModelToScene(arSceneView, modelType)
    }

    fun setGraph() {
        //databaseViewModel.getGraph()
        viewModelScope.launch(Dispatchers.IO) {
            arViewModel.setGraph(databaseViewModel.getGraph())
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

class MainViewModelFactory(
    private val chatViewModel: ChatViewModel,
    private val databaseViewModel: DatabaseViewModel,
    private val arViewModel: ArViewModel,
    private val imageRecognitionViewModel: ImageRecognitionViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                chatViewModel,
                databaseViewModel,
                arViewModel,
                imageRecognitionViewModel,
                lifecycleOwner
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}