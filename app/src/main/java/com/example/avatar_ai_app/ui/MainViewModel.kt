package com.example.avatar_ai_app.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
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
import io.github.sceneview.ar.ArSceneView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "MainViewModel"
private const val RECORDING_WAIT = 100L

class MainViewModel(
    private val chatViewModel: ChatViewModelInterface,
    private val databaseViewModel: DatabaseViewModelInterface,
    private val arViewModel: ArViewModelInterface,
    private val imageViewModel: ImageRecognitionViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    private val _isCameraEnabled = MutableStateFlow(false)
    private val _isRecordingEnabled = MutableStateFlow(false)
    private val _isRecordingReady = MutableStateFlow(true)
    private var startTime = System.currentTimeMillis()
    private var recordingJob: Job? = null
    val isChatViewModelLoaded = MutableStateFlow(false)
    val isDatabaseViewModelLoaded = MutableStateFlow(false)
    val isImageViewModelLoaded = MutableStateFlow(false)

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
    //val textState = mutableStateOf(TextFieldValue())
    var textFieldFocusState = mutableStateOf(false)

    enum class AlertType {
        CLEAR_CHAT,
        HELP
    }

    /**
     * Initialises the observers for the different viewModels, so that
     * mainViewModel can respond to their status.
     * Called everytime the mainActivity is created.
     * @param lifecycleOwner lifeCycle of the main activity
     */
    fun initialiseObservers(lifecycleOwner: LifecycleOwner) {
        chatViewModel.status.observe(lifecycleOwner) { status ->
            when (status) {
                ChatViewModelInterface.Status.LOADING, null -> {
                    setTextToSpeechReady(false)
                    isChatViewModelLoaded.value = false
                    Log.i(TAG, "chatViewModel status: loading")
                }
                ChatViewModelInterface.Status.READY -> {
                    setTextToSpeechReady(true)
                    setRecordingState(UiState.ready)
                    updateTextFieldStringResId(R.string.send_message_hint)
                    _isRecordingReady.value = true
                    isChatViewModelLoaded.value = true
                    Log.i(TAG, "chatViewModel status: ready")
                }
                ChatViewModelInterface.Status.RECORDING -> {
                    setRecordingState(UiState.recording)
                    updateTextFieldStringResId(R.string.recording_message)
                    _isRecordingReady.value = false
                    Log.i(TAG, "chatViewModel status: recording")
                }
                ChatViewModelInterface.Status.PROCESSING -> {
                    setRecordingState(UiState.processing)
                    updateTextFieldStringResId(R.string.processing_message)
                    _isRecordingReady.value = false
                    Log.i(TAG, "chatViewModel status: processing")
                }
            }

            databaseViewModel.status.observe(lifecycleOwner) {
                when (it) {
                    DatabaseViewModelInterface.Status.LOADING -> {
                        isDatabaseViewModelLoaded.value = false
                        Log.i(TAG, "databaseViewModel status: loading")
                    }
                    DatabaseViewModelInterface.Status.READY -> {
                        isDatabaseViewModelLoaded.value = true
                        setFeatureList()
                        Log.i(TAG, "databaseViewModel status: ready")
                    }
                    DatabaseViewModelInterface.Status.ERROR, null -> {
                        isDatabaseViewModelLoaded.value = false
                        Log.i(TAG, "databaseViewModel status: error")
                    }
                }
            }
        }

        imageViewModel.status.observe(lifecycleOwner) {
            when (it) {
                null -> {}
                ImageRecognitionViewModel.Status.INIT -> {
                    isImageViewModelLoaded.value = false
                    Log.i(TAG, "imageViewModel status: init")
                }
                ImageRecognitionViewModel.Status.READY -> {
                    isImageViewModelLoaded.value = true
                    Log.i(TAG, "databaseViewModel status: ready")
                }
                ImageRecognitionViewModel.Status.ERROR -> {
                    isImageViewModelLoaded.value = false
                    Log.i(TAG, "databaseViewModel status: error")
                }

                ImageRecognitionViewModel.Status.PROCESSING -> {
                //TODO - prevent user from typing anything

                }

            }
        }

        chatViewModel.messages.observe(lifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                displayMessages(it)
                Log.i(TAG, "chatViewModel has messages")
            }
        }

        chatViewModel.intent.observe(lifecycleOwner) { intent ->
            when (intent) {
                Intent.RECOGNITION -> {
                    processRecognitionRequest()
                    Log.i(TAG, "chatViewModel intent: recognition")
                }
                Intent.NAVIGATION -> {
                    processNavigationRequest()
                    Log.i(TAG, "chatViewModel intent: navigation")
                }
                else -> {}
            }
        }
    }

    private fun setFeatureList() {
        viewModelScope.launch(Dispatchers.IO) {
            chatViewModel.setFeatureList(databaseViewModel.getFeatures())
        }
    }

    private fun processRecognitionRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            val featureName = imageViewModel.recogniseFeature()
            if (featureName != null) {
                val feature = databaseViewModel.getFeature(featureName)
                if (feature != null) {
                    chatViewModel.newResponse(feature.description)
                    Log.i(TAG, "Feature description: ${feature.description}")
                } else {
                    chatViewModel.newResponse("Sorry, I don't recognise this feature!")
                    Log.i(TAG, "Feature is null")
                }
            } else {
                //TODO implement get nearest cloud anchor from ArViewModel
                Log.i(TAG, "Feature not recognised")
                chatViewModel.newResponse("Sorry, I don't recognise this feature!")
            }
        }
    }

    private fun processNavigationRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            val destination = chatViewModel.destinationID.value
            if (!destination.isNullOrEmpty()) {
                arViewModel.loadDirections(destination)
            }
        }
    }

    /**
     * Adds newest messages to the uiState message list for display in the chatBox
     * @param messages the list of ChatMessages
     */
    private fun displayMessages(messages: List<ChatMessage>) {
        val uiMessageList = uiState.value.messages
        val diff = messages.size - uiMessageList.size

        for (i in diff - 1 downTo 0) {
            val message = messages[i]
            if (message.string.isNotEmpty())
                uiState.value.addMessage(message)
        }

        _uiState.update { currentState ->
            currentState.copy(
                messagesAreShown = true
            )
        }
    }

    /**
     * If a permission is not granted, it is added to the queue
     * @param permission the Manifest.permission being requested
     * @param isGranted whether the permission is granted
     */
    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted) {
            visiblePermissionDialogQueue.add(0, permission)
        }
        updatePermissionStatus(permission, isGranted)
    }

    /**
     * Updates the permission states stored in the live data variables
     * @param permission the permission string being granted
     * @param isGranted permission granted state
     */
    fun updatePermissionStatus(permission: String, isGranted: Boolean) {
        when (permission) {
            Manifest.permission.CAMERA -> _isCameraEnabled.value = isGranted
            Manifest.permission.RECORD_AUDIO -> _isRecordingEnabled.value = isGranted
        }

    }

    /**
     * Dismisses the dialog box for the last permission added to the queue
     */
    fun dismissDialog() {
        visiblePermissionDialogQueue.removeLast()
    }

    /**
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

    /**
     * Update the user input mode in the uiState based on the textField focus.
     */
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

    fun sendButtonOnPress(message: String) {
        when (uiState.value.inputMode) {
            UiState.text -> {
                chatViewModel.newUserMessage(message)
            }

            UiState.speech -> {
                if (uiState.value.recordingState == UiState.ready) {
                    startTime = System.currentTimeMillis()
                    recordingJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(RECORDING_WAIT)
                        chatViewModel.startRecording()
                        Log.d("SendButton", "Main viewModel - recording started")
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
                    Log.d("SendButton", "Main viewModel - recording stopped")
                    chatViewModel.stopRecording()
                    // Reminder, controller stop is asynchronous, code after this should go in the call-back function.
                }
            }
        }
    }

    /**
     * Sets the language for the application
     * @param language
     */
    fun onLanguageSelectionResult(context: Context, language: Language) {
        chatViewModel.setLanguage(language)
        _uiState.update { currentState ->
            currentState.copy(
                language = language
            )
        }
        updateLocale(context, language.locale)
    }

    /**
     * Updates the locale for the given context
     * @param context
     * @param locale
     */
    @Suppress("DEPRECATION")
    private fun updateLocale(context: Context, locale: Locale) {
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Shows the language menu and dismisses the settings menu
     */
    fun languageSettingsButtonOnClick() {
        _uiState.update { currentState ->
            currentState.copy(
                isLanguageMenuShown = true
            )
        }
        dismissSettingsMenu()
    }

    /**
     * Dismisses the language menu
     */
    fun dismissLanguageMenu() {
        _uiState.update { currentState ->
            currentState.copy(
                isLanguageMenuShown = false
            )
        }
    }

    fun clearChatButtonOnClick() {
        generateAlert(AlertType.CLEAR_CHAT)
        dismissSettingsMenu()
    }

    fun helpButtonOnClick() {
        generateAlert(AlertType.HELP)
        dismissSettingsMenu()
    }

    private fun generateAlert(alertType: AlertType) {
        _uiState.update { currentState ->
            when (alertType) {
                AlertType.HELP -> {
                    currentState.copy(
                        alertIsShown = true,
                        alertResId = R.string.not_implemented_message,
                        alertIntent = UiState.help
                    )
                }

                AlertType.CLEAR_CHAT -> {
                    currentState.copy(
                        alertIsShown = true,
                        alertResId = R.string.clear_chat_message,
                        alertIntent = UiState.clear
                    )
                }
            }
        }
    }

    fun alertOnClick() {
        when (uiState.value.alertIntent) {
            UiState.clear -> clearChatHistory()

            UiState.help -> dismissAlertDialogue()
        }
    }

    fun dismissAlertDialogue() {
        _uiState.update { currentState ->
            currentState.copy(
                alertIsShown = false
            )
        }
    }

    /**
     * Wipes the chat history and deletes all messages from the uiState
     */
    private fun clearChatHistory() {
        chatViewModel.clearChatHistory()
        uiState.value.clearMessages()
        dismissAlertDialogue()
    }

    /**
     * Updates the ui based on swipe inputs
     * @param pan distance moved across the Y axis
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun handleSwipe(pan: Float, keyboardController: SoftwareKeyboardController?) {
        if(!uiState.value.messagesAreShown && pan > 0) {
            dismissLanguageMenu()
            focusRequester.requestFocus()
            keyboardController?.hide()

        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    messagesAreShown = pan <= 0,
                )
            }
        }
    }

    private fun dismissOrShowMessages(pan: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                messagesAreShown = pan <= 0,
            )
        }
    }

    fun initialiseArScene(arSceneView: ArSceneView) {
        viewModelScope.launch(Dispatchers.IO) {
            arViewModel.setGraph(databaseViewModel.getGraph())
            //TODO arViewModel.setFeatures(databaseViewModel.getFeatures())
            arViewModel.initialiseArScene(arSceneView)
            arViewModel.addModelToScene(ArViewModel.ModelType.AVATAR)
        }
    }

    fun settingsMenuButtonOnClick() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsMenuShown = true,
                isLanguageMenuShown = false
            )
        }
    }

    fun dismissSettingsMenu() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsMenuShown = false
            )
        }
    }
}


class MainViewModelFactory(
    private val chatViewModel: ChatViewModel,
    private val databaseViewModel: DatabaseViewModel,
    private val arViewModel: ArViewModel,
    private val imageRecognitionViewModel: ImageRecognitionViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                chatViewModel,
                databaseViewModel,
                arViewModel,
                imageRecognitionViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
