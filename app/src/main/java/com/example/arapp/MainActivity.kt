package com.example.arapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var sceneView: ArSceneView
    private lateinit var avatarButton: ExtendedFloatingActionButton
    private lateinit var listeningText: TextView
    private lateinit var micButton: ExtendedFloatingActionButton

    data class Model(
        val fileLocation: String,
        val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
        val scale: Float
    )

    private var modelNode: ArModelNode? = null
    private var avatarIsLoaded: Boolean = false
    private var avatarIsPlaced: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sceneView = findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }
        avatarButton = findViewById<ExtendedFloatingActionButton>(R.id.avatarButton).apply {
            setOnClickListener { avatarButtonOnClick() }
        }
        listeningText = findViewById(R.id.listening_text)
        micButton = findViewById<ExtendedFloatingActionButton>(R.id.micButton).apply {
            setOnTouchListener { _, event -> listenToVoice(event)
            }
        }
        registerForContextMenu(sceneView)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menuInflater.inflate(R.menu.options_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_1 -> return true
            R.id.option_2 -> return true
        }
        return super.onContextItemSelected(item)
    }

    private fun avatarButtonOnClick() {
        if(!avatarIsLoaded || avatarIsPlaced){
            modelNode?.let {
                sceneView.removeChild(it)
                it.destroy()
            }
            loadAvatar()
            avatarButton.text = getString(R.string.place_avatar_button)
            avatarIsPlaced = false
        } else{
            placeAvatar()
            avatarButton.text = getString(R.string.summon_avatar_button)
        }

    }

    private fun placeAvatar() {
        modelNode?.anchor()
        avatarIsPlaced = true
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

    private fun listenToVoice(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> listeningText.isVisible = true
            MotionEvent.ACTION_UP -> listeningText.isVisible = false
        }
        return false
    }

}