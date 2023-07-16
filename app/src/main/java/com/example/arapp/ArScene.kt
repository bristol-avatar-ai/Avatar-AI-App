package com.example.arapp

import android.view.View
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position

class ArScene constructor(view: View){

    private var sceneView: ArSceneView = view.findViewById(R.id.sceneView)
    private var modelNode: ArModelNode? = null

    fun addModelToScene(model: ArModel){
//        modelNode = ArModelNode(
//            sceneView.engine,
//            PlacementMode.INSTANT
//        ).apply{
//            loadModelGlbAsync(
//                glbFileLocation = model.fileLocation,
//                centerOrigin = Position(y= -1.0f),
//                scaleToUnits = model.scale
//            )
//        }
//        sceneView.addChild(modelNode!!)
//        sceneView.selectedNode = modelNode
    }

    fun getSceneView(): ArSceneView{
        return sceneView
    }
}