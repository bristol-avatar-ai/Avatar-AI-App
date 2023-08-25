package com.example.avatar_ai_app.ar

import io.github.sceneview.ar.ArSceneView

interface ArViewModelInterface {
    fun setGraph(graph: Graph)

    fun initialiseArScene(arSceneView: ArSceneView)

    //fun addModelToScene(modelType: ArViewModel.ModelType)

    fun loadDirections(destination: String)
}