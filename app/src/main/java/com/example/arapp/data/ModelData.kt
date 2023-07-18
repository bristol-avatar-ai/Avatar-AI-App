package com.example.arapp.data

import io.github.sceneview.math.Position

data class Model(
    val fileLocation: String,
    val scale: Float,
    val position: Position,
)

val avatarModel: Model = Model(
    fileLocation = "models/robot_playground.glb",
    scale = 0.8f,
    position = Position(y = -1.0f)
)
