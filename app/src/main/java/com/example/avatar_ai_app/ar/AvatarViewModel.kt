package com.example.avatar_ai_app.ar

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.data.avatarModel
import com.example.avatar_ai_app.ui.AvatarState
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

class AvatarViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(AvatarState())

    val uiState: StateFlow<AvatarState> = _uiState.asStateFlow()
    val nodes = mutableStateListOf<ArNode>()

    enum class AvatarButtonType {
        VISIBILITY,
        MODE
    }

    private lateinit var modelNode: ArModelNode

    private fun showAvatar() {
        modelNode.detachAnchor()
        modelNode.isVisible = true
        _uiState.update{ currentState ->
            currentState.copy(
                avatarIsAnchored = false,
                avatarIsVisible = true
            )
        }
    }

    private fun hideAvatar() {
        modelNode.isVisible = false
        _uiState.update{ currentState ->
            currentState.copy(
                avatarIsVisible = false
            )
        }

    }

    private fun anchorAvatar() {
        modelNode.anchor()
        _uiState.update{ currentState ->
            currentState.copy(
                avatarIsAnchored = true
            )
        }
    }

    private fun detachAvatar() {
        modelNode.detachAnchor()
        _uiState.update{ currentState ->
            currentState.copy(
                avatarIsAnchored = false
            )
        }
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
        _uiState.update { currentState ->
            when(uiState.value.isAvatarMenuVisible) {
                true -> currentState.copy(
                    isAvatarMenuVisible = false
                )
                false -> currentState.copy(
                    isAvatarMenuVisible = true
                )
            }
        }
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
            _uiState.update { currentState ->
                currentState.copy(
                    isAvatarMenuVisible = false
                )
            }
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
}