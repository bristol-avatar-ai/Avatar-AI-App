package com.example.arapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var sceneView: ArSceneView
    private lateinit var summonAvatarButton: ExtendedFloatingActionButton
    private lateinit var placeAvatarButton: ExtendedFloatingActionButton

    data class Model(
        val fileLocation: String,
        val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
        val scale: Float
    )

    private var modelNode: ArModelNode? = null
    var avatarIsLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }
        summonAvatarButton = findViewById<ExtendedFloatingActionButton>(R.id.summonAvatarButton).apply {
            setOnClickListener {
                summonAvatar()
            }
        }
        placeAvatarButton = findViewById<ExtendedFloatingActionButton>(R.id.placeAvatarButton).apply {
            setOnClickListener { placeAvatar() }
        }
    }

    private fun loadAvatar(){
        val avatar = Model(
            fileLocation = "models/robot_playground.glb",
            placementMode = PlacementMode.INSTANT,
            scale = 0.8f
        )

        modelNode = ArModelNode(
            sceneView.engine,
            PlacementMode.INSTANT,
        ).apply {
            loadModelGlbAsync(
                glbFileLocation = avatar.fileLocation,
                centerOrigin = Position(y = -1.0f),
                scaleToUnits = avatar.scale
            )
        }
        sceneView.addChild(modelNode!!)
        sceneView.selectedNode = modelNode
        avatarIsLoaded = true
    }

    private fun summonAvatar() {
        if(avatarIsLoaded){
            modelNode?.let {
                sceneView.removeChild(it)
                it.destroy()
            }
        }
        loadAvatar()
    }

    private fun placeAvatar() {
        TODO("Not yet implemented")
    }
}