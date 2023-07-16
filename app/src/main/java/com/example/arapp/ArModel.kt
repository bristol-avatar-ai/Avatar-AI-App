package com.example.arapp

import io.github.sceneview.ar.node.PlacementMode

data class ArModel(
    val fileLocation: String,
    val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
    val scale: Float
)