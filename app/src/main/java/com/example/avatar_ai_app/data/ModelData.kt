package com.example.avatar_ai_app.data

import io.github.sceneview.math.Position

data class Model(
    val fileLocation: String,
    val scale: Float,
    val position: Position,
)

val signModel: Model = Model(
    fileLocation = "",
    scale = 0.8f,
    position = Position(y = -1.0f)
)

val crystalModel: Model = Model(
    fileLocation = "models/crystal.glb",
    scale = 0.8f,
    position = Position(y = -1.0f)
)
