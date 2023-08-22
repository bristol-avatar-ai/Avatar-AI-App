package com.example.avatar_ai_app.ar

import com.google.android.filament.Engine
import io.github.sceneview.ar.node.ArModelNode

class ModelNode(engine: Engine, val signName: String?) : ArModelNode(engine) {

    var isResolved: Boolean = false
}