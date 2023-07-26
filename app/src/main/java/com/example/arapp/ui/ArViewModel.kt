package com.example.arapp.ui

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.example.arapp.R
import com.example.arapp.data.avatarModel
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StateValue {
    MENU,
    ANCHOR,
    VISIBLE
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

class ArViewModel : ViewModel() {
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()
    val focusRequester = FocusRequester()
    val nodes = mutableStateListOf<ArNode>()
    var touchPosition = mutableStateOf(Offset.Zero)
    val textState = mutableStateOf(TextFieldValue())
    var textFieldFocusState = mutableStateOf(false)

    fun addAvatarToScene(arSceneView: ArSceneView, coroutine: CoroutineScope, context: Context) {
        arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        modelNode = ArModelNode(arSceneView.engine).apply {
            coroutine.launch {
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
        if(uiState.value.isAvatarMenuVisible) {
            updateState(StateValue.MENU, false)
        }
    }

    fun getActionButtonValues(buttonType: AvatarButtonType):
    Pair<Int, String>{
        return when(buttonType) {
            AvatarButtonType.VISIBILITY -> {
                if(uiState.value.avatarIsVisible) {
                    Pair(R.drawable.hide, "Dismiss Avatar")
                } else Pair(R.drawable.robot_icon, "Summon Avatar")
            }

            AvatarButtonType.MODE -> {
                if(uiState.value.avatarIsAnchored) {
                    Pair(R.drawable.unlock_icon, "Release Avatar")
                } else Pair(R.drawable.lock_icon, "Place Avatar Here")
            }
        }
    }

    fun enableActionButton(buttonType: AvatarButtonType): Boolean{
        return when(buttonType) {
            AvatarButtonType.MODE -> {
                (uiState.value.isAvatarMenuVisible and uiState.value.avatarIsVisible)
            }
            AvatarButtonType.VISIBILITY -> {
                uiState.value.isAvatarMenuVisible
            }
        }
    }

    fun getMicOrSendIcon(): Int {
        return if(textState.value.text.isEmpty()) {
            R.drawable.mic_icon
        } else {
            R.drawable.send_icon
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