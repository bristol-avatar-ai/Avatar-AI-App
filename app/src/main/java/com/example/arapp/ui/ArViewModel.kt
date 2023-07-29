package com.example.arapp.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arapp.R
import com.example.arapp.audio.AudioRecorder
import com.example.arapp.controller.Controller
import com.example.arapp.data.avatarModel
import com.example.arapp.network.TranscriptionAPIServiceException
import com.example.arapp.network.TranscriptionApi
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class StateValue {
    MENU,
    ANCHOR,
    VISIBLE,
    MIC,
    RESPONSE,
    RECORDING
}

enum class AvatarButtonType {
    VISIBILITY,
    MODE
}

private val _uiState = MutableStateFlow(ArUiState())

private lateinit var modelNode: ArModelNode

private fun updateState(value: StateValue, state: Boolean) {
    _uiState.update { currentState ->
        when (value) {
            StateValue.MENU -> currentState.copy(
                isAvatarMenuVisible = state
            )

            StateValue.VISIBLE -> currentState.copy(
                avatarIsVisible = state
            )

            StateValue.ANCHOR -> currentState.copy(
                avatarIsAnchored = state
            )

            StateValue.MIC -> currentState.copy(
                inputMode = when (state) {
                    true -> ArUiState.recording
                    false -> ArUiState.text
                }

            )

            StateValue.RESPONSE -> currentState.copy(
                responsePresent = state
            )

            StateValue.RECORDING -> currentState.copy(
                isRecordingEnabled = state
            )
        }
    }
}

private fun showAvatar() {
    modelNode.detachAnchor()
    modelNode.isVisible = true
    updateState(StateValue.ANCHOR, false)
    updateState(StateValue.VISIBLE, true)
}

private fun hideAvatar() {
    modelNode.isVisible = false
    updateState(StateValue.VISIBLE, false)
}

private fun anchorAvatar() {
    modelNode.anchor()
    updateState(StateValue.ANCHOR, true)
}

private fun detachAvatar() {
    modelNode.detachAnchor()
    updateState(StateValue.ANCHOR, false)
}

private const val TAG = "ArViewModel"

class ArViewModel : ViewModel(), TextToSpeech.OnInitListener,
    AudioRecorder.RecordingCompletionListener {
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()
    val focusRequester = FocusRequester()
    val nodes = mutableStateListOf<ArNode>()
    var touchPosition = mutableStateOf(Offset.Zero)
    val textState = mutableStateOf(TextFieldValue())
    var textFieldFocusState = mutableStateOf(false)

    // Controller containing all non-UI components.
    lateinit var controller: Controller

    fun initController(context: Context) {
        // Initialise controller.
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
            generateErrorMessage(R.string.speech_error_message)
        }
        setLoadingState(false)
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
            generateErrorMessage(R.string.speech_error_message)
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
        viewModelScope.launch {
            replyToSpeech()
            setRecordingState(ArUiState.ready)
        }
    }

    /*
    * This function transcribes the recordingFile into text, deletes the
    * file, and then generates a reply.
     */
    private suspend fun replyToSpeech() {
        try {
            val message = TranscriptionApi.transcribe(controller.recordingFile)
            controller.recordingFile.delete()
            generateReply(message)
        } catch (e: TranscriptionAPIServiceException.NoInternetException) {
            generateErrorMessage(R.string.network_error_message)
        } catch (e: Exception) {
            generateErrorMessage(R.string.error_message)
        }
    }

    private fun generateErrorMessage(resId: Int) {
        // TODO: Output the corresponding error message to the user.
        //outputMessage()
    }

    fun addAvatarToScene(arSceneView: ArSceneView, scope: CoroutineScope, context: Context) {
        arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        modelNode = ArModelNode(arSceneView.engine).apply {
            scope.launch {
                //load the avatar model from ModelData
                loadModelGlb(
                    context = context,
                    glbFileLocation = avatarModel.fileLocation,
                    scaleToUnits = avatarModel.scale,
                    centerOrigin = avatarModel.position
                )
            }
            isVisible = false
        }
        arSceneView.addChild(modelNode)
    }

    fun avatarButtonOnClick() {
        if (uiState.value.isAvatarMenuVisible) {
            updateState(StateValue.MENU, false)
        } else updateState(StateValue.MENU, true)
    }

    fun summonOrHideButtonOnClick() {
        if (uiState.value.avatarIsVisible) {
            hideAvatar()
        } else {
            showAvatar()
        }
    }

    fun anchorOrFollowButtonOnClick() {
        if (uiState.value.avatarIsAnchored) {
            detachAvatar()
        } else if (!uiState.value.avatarIsAnchored) {
            anchorAvatar()
        }
    }

    fun dismissActionMenu() {
        if (uiState.value.isAvatarMenuVisible) {
            updateState(StateValue.MENU, false)
        }
    }

    fun getActionButtonValues(buttonType: AvatarButtonType):
            Pair<Int, String> {
        return when (buttonType) {
            AvatarButtonType.VISIBILITY -> {
                if (uiState.value.avatarIsVisible) {
                    Pair(R.drawable.hide, "Dismiss Avatar")
                } else Pair(R.drawable.robot_icon, "Summon Avatar")
            }

            AvatarButtonType.MODE -> {
                if (uiState.value.avatarIsAnchored) {
                    Pair(R.drawable.unlock_icon, "Release Avatar")
                } else Pair(R.drawable.lock_icon, "Place Avatar Here")
            }
        }
    }

    fun enableActionButton(buttonType: AvatarButtonType): Boolean {
        return when (buttonType) {
            AvatarButtonType.MODE -> {
                (uiState.value.isAvatarMenuVisible and uiState.value.avatarIsVisible)
            }

            AvatarButtonType.VISIBILITY -> {
                uiState.value.isAvatarMenuVisible
            }
        }
    }

    fun getMicOrSendIcon(): Int {
        return if (!textFieldFocusState.value) {
            updateState(StateValue.MIC, true)
            R.drawable.mic_icon
        } else {
            updateState(StateValue.MIC, false)
            R.drawable.send_icon
        }
    }

    fun micAndSendButtonOnClick() {
        when (uiState.value.inputMode) {
            ArUiState.text -> onSend()
        }
    }

    fun onSend() {
        //TODO("send textfieldvalue and generate response")
        textState.value = TextFieldValue()
        updateState(StateValue.RESPONSE, true)
    }

    /*
    * Gets a response to the input message with ChatService. Generates an audio
    * reply and plays it if TextToSpeech has been initialised correctly.
    * Coroutines are used to prevent blocking the main thread.
     */
    fun generateReply(message: String) {
        viewModelScope.launch {
            // Generate reply with ChatService.
            val reply = controller.chatService.getResponse(message)
            outputMessage(reply)
        }
    }

    fun outputMessage(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                responseValue = message
            )
        }
        if (uiState.value.isTextToSpeechReady) {
            controller.textToSpeech.speak(
                message,
                TextToSpeech.QUEUE_FLUSH, null, null
            )
        }
    }

    fun dismissTextResponse() {
        updateState(StateValue.RESPONSE, false)
    }

    fun setRecordingEnabled(enabled: Boolean) {
        updateState(StateValue.RECORDING, enabled)
    }

    fun setTextToSpeechReady(ready: Boolean) {
        _uiState.update {
            it.copy(
                isTextToSpeechReady = ready
            )
        }
    }

    fun setRecordingState(state: Int) {
        _uiState.update {
            it.copy(
                recordingState = state
            )
        }
    }

    fun resetControllerState() {
        _uiState.update { currentState ->
            currentState.copy(
                inputMode = ArUiState.speech,
                isRecordingEnabled = false,
                recordingState = ArUiState.ready,
                isTextToSpeechReady = false,
            )
        }
    }

    fun setLoadingState(loading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = loading
            )
        }
    }

    private var startTime = System.currentTimeMillis()
    private var recordingJob: Job? = null

    // TODO: Refactor, move const outside class
    private val RECORDING_WAIT = 200L

    fun sendButtonOnPress() {
        when (uiState.value.inputMode) {
            ArUiState.text -> {
                generateReply(textState.value.text)
            }

            ArUiState.speech -> {
                startTime = System.currentTimeMillis()
                recordingJob = viewModelScope.launch(Dispatchers.IO) {
                    setRecordingState(ArUiState.recording)
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
                    // TODO: Give hint on how to record
                } else {
                    controller.audioRecorder.stop()
                    // Reminder, controller stop is asynchronous, code after this should go in the call-back function.
                }
            }
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