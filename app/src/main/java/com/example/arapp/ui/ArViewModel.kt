package com.example.arapp.ui

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
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

    val nodes = mutableStateListOf<ArNode>()

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
        if(_uiState.value.isAvatarMenuVisible) {
            updateState(StateValue.MENU, false)
        } else if(_uiState.value.avatarIsVisible){
            hideAvatar()
        } else {
            showAvatar()
        }
    }

    fun getAvatarButtonIcon(): Int {
        if(_uiState.value.isAvatarMenuVisible) {
            return R.drawable.drop_down_arrow
        } else if(_uiState.value.avatarIsVisible) {
            return R.drawable.hide
        }
        return R.drawable.robot_icon
    }

    fun avatarButtonOnHold() {
        if(_uiState.value.isAvatarMenuVisible) {
            updateState(StateValue.MENU, false)
        } else updateState(StateValue.MENU, true)
    }

    fun anchorOrFollowButtonOnClick() {
        if (_uiState.value.avatarIsAnchored) {
            detachAvatar()
        } else if (!_uiState.value.avatarIsAnchored) {
            anchorAvatar()
        }
    }

    fun getMenuButtonText(): String {
        return if (_uiState.value.avatarIsAnchored) {
            "Follow me!"
        } else {
            "Place me here!"
        }
    }

    fun isSendOrMicIcon(): Int {
        return if (uiState.value.isTextInput) R.drawable.baseline_send_24
        else R.drawable.baseline_mic_24
    }

    fun textFieldOnClick() {
        _uiState.update { currentState ->
            currentState.copy(
                isTextInput = true
            )
        }
    }
}