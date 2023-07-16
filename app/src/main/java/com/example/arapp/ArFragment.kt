package com.example.arapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position

class ArFragment : Fragment(R.layout.fragment_ar) {

    private lateinit var sceneView: ArSceneView
    private lateinit var avatarButton: ExtendedFloatingActionButton
    private lateinit var listeningText: TextView
    private lateinit var micButton: ExtendedFloatingActionButton
    private lateinit var textButton: ExtendedFloatingActionButton

    data class Model(
        val fileLocation: String,
        val placementMode: PlacementMode = PlacementMode.BEST_AVAILABLE,
        val scale: Float
    )

    private var modelNode: ArModelNode? = null
    private var avatarIsLoaded: Boolean = false
    private var avatarIsPlaced: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_ar, container, false)
        initArFragment(view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sceneView.destroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initArFragment(view: View){
        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            planeRenderer.isVisible = false
        }
        avatarButton = view.findViewById<ExtendedFloatingActionButton>(R.id.avatarButton).apply {
            setOnClickListener { avatarButtonOnClick() }
        }
        listeningText = view.findViewById(R.id.listening_text)
        micButton = view.findViewById<ExtendedFloatingActionButton>(R.id.micButton).apply {
            setOnTouchListener { _, event -> listenToVoice(event)
            }
        }
        textButton = view.findViewById<ExtendedFloatingActionButton>(R.id.textButton).apply {
            setOnClickListener {
                Navigation.findNavController(view).navigate(R.id.navigate_home_to_text)
            }
        }

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
//        val avatar = Model(
//            fileLocation = "models/robot_playground.glb",
//            placementMode = PlacementMode.DISABLED,
//            scale = 0.8f
//        )
//
//        modelNode = ArModelNode(
//            sceneView.engine,
//            PlacementMode.INSTANT,
//        ).apply {
//            loadModelGlbAsync(
//                glbFileLocation = avatar.fileLocation,
//                centerOrigin = Position(y = -1.0f),
//                scaleToUnits = avatar.scale
//            )
//        }
//        sceneView.addChild(modelNode!!)
//        sceneView.selectedNode = modelNode
//        avatarIsLoaded = true
    }

    private fun listenToVoice(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> listeningText.isVisible = true
            MotionEvent.ACTION_UP -> listeningText.isVisible = false
        }
        return false
    }
}


