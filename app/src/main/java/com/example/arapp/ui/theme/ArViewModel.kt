package com.example.arapp.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.arapp.data.avatarModel
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val _uiState = MutableStateFlow(ArUiState())

private lateinit var modelNode: ArModelNode

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

    fun hideAvatar() {
        modelNode.isVisible = false
    }

    fun anchorAvatar() {
        modelNode.anchor()
    }

    fun detachAvatar() {
        modelNode.detachAnchor()
    }

    fun summonAvatar() {
        modelNode.let {
            it.isVisible = true
            if(it.isAnchored) it.detachAnchor()
            it.position = avatarModel.position
        }
    }
}