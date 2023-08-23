package com.example.avatar_ai_app.ar

import android.annotation.SuppressLint
import android.app.Application
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.data.crystalModel
import com.example.avatar_ai_app.data.signModel
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.PlacementMode
import kotlinx.coroutines.*
import kotlin.math.sqrt

private const val TAG = "ArViewModel"

class ArViewModel(application: Application) : AndroidViewModel(application), ArViewModelInterface {

    private val _application = application
    private lateinit var graph: Graph

    @SuppressLint("StaticFieldLeak")
    lateinit var arSceneView: ArSceneView

    private val context
        get() = _application.applicationContext

    override fun setGraph(graph: Graph) {
        this.graph = graph
    }

    override fun initialiseArScene(arSceneView: ArSceneView) {
        viewModelScope.launch {
            arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            arSceneView.cloudAnchorEnabled = true
            delay(1000L)
            resolveAllAnchors()
        }
        this.arSceneView = arSceneView

        Log.d("ArViewModel", "Initialise AR Scene Complete")
    }

    private fun resolveAllAnchors() {
        graph.forEach { anchorId, anchorProperties ->
            val modelNode: ModelNode = if(anchorProperties.name.contains("SIGN")) {
                createSignModel(anchorProperties.name)
            } else {
                createCrystalModel()
            }

            resolveModel(modelNode, anchorId)
            resolvedModelNodes.add(modelNode)
            anchorMap[anchorId] = modelNode
        }
    }

    private fun createCrystalModel(): ModelNode {
        val modelNode: ModelNode

        val model = crystalModel
        modelNode = ModelNode(arSceneView.engine, null).apply {
            viewModelScope.launch {
                loadModelGlb(
                    context = context,
                    glbFileLocation = model.fileLocation,
                    scaleToUnits = model.scale,
                    centerOrigin = model.position
                )
            }
        }
        modelNode.apply {
            placementMode = PlacementMode.INSTANT
            isVisible = false
        }

        arSceneView.addChild(modelNode)
        return modelNode
    }

    private fun createSignModel(anchorName: String): ModelNode {
        val modelNode: ModelNode
        val model = signModel
        modelNode = ModelNode(arSceneView.engine, null).apply {
            viewModelScope.launch {
                loadModelGlb(
                    context = context,
                    glbFileLocation = "models/${anchorName.substring(7)}.glb",
                    scaleToUnits = model.scale,
                    centerOrigin = model.position
                )
            }
        }
        modelNode.apply {
            placementMode = PlacementMode.INSTANT
            isVisible = false
        }
        arSceneView.addChild(modelNode)

        Log.d(TAG, "File Location: " + "models/${anchorName.substring(7)}.glb")


        return modelNode
    }

    private val resolvedModelNodes = mutableListOf<ModelNode>()
    private val anchorMap: MutableMap<String, ModelNode> = mutableMapOf()

    private fun resolveModel(modelNode: ModelNode, anchorId: String?) {
        if (anchorId != null) {
            modelNode.resolveCloudAnchor(anchorId) { anchor: Anchor, success: Boolean ->
                Log.d(TAG, anchor.trackingState.toString())

                if (success) {
                    Log.d(TAG, "Anchor resolved - Id: $anchorId")
                    modelNode.isResolved = true
                    if(modelNode.signName?.contains("SIGN") == true){
                        modelNode.isVisible = true
                    }
                }
                if (!success) {
                    Log.d(TAG, "Anchor failed to resolve, trying again - Id: $anchorId")
                    resolveModel(modelNode, anchorId)
                }
            }
        } else {
            Log.d(TAG, "Null anchor Id")
        }
    }

    override fun loadDirections(destination: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var currentLocation = closestAnchor()
            var snackbar: Snackbar? = null

            while (currentLocation == null) {
                if (snackbar?.isShown == false || snackbar == null) {
                    snackbar = Snackbar.make(arSceneView, "Please move your device around to scan the surroundings",
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackbar.show()
                }

                delay(2000) // Wait for 2 seconds before checking again
                currentLocation = closestAnchor()
            }

            snackbar?.dismiss()

            val (_, paths) = dijkstra(currentLocation)
            val path = paths[destination]

            showPath(path)
        }
    }

    // This function will return the Id of the nearest cloud anchor
    private fun closestAnchor(): String? {
        var closestAnchorId: String? = null

        // This will run until an anchor is found in view and returned
        var minDistance = Float.MAX_VALUE

        for ((anchorId, anchorNode) in anchorMap) {
            val nodePose = anchorNode.anchor?.pose
            if (
                nodePose != null && isInView(nodePose)
                && anchorNode.isResolved && anchorNode.signName.isNullOrEmpty()
            ) {
                val distance = distanceFromAnchor(anchorNode)
                if (distance < minDistance) {
                    minDistance = distance
                    closestAnchorId = anchorId
                }
            }
        }
        return closestAnchorId
    }

    private fun dijkstra(src: String): Pair<Map<String, Int>, Map<String, List<String>>> {
        val sptSet = mutableSetOf<String>()
        val dist = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val prev = mutableMapOf<String, String>()
        val paths = mutableMapOf<String, List<String>>()

        dist[src] = 0
        paths[src] = listOf(src)

        for (count in 0 until graph.size) {
            val unvisitedNode =
                dist.filter { !sptSet.contains(it.key) }.minByOrNull { it.value }?.key
            unvisitedNode?.let {
                sptSet.add(unvisitedNode)
                graph[unvisitedNode]?.edges?.forEach { edge ->
                    if (!sptSet.contains(edge.destination) && dist.getValue(unvisitedNode) != Int.MAX_VALUE) {
                        val newDist = dist.getValue(unvisitedNode) + edge.distance
                        if (newDist < dist.getValue(edge.destination)) {
                            dist[edge.destination] = newDist
                            prev[edge.destination] = unvisitedNode
                            paths[edge.destination] =
                                paths.getValue(unvisitedNode) + edge.destination
                        }
                    }
                }
            }
        }
        return Pair(dist, paths)
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun showPath(path: List<String>?) {
        var modelIndex = 0
        var withinThreshold = false

        val runnableCode = object : Runnable {
            override fun run() {
                val thresholdDistance = 3

                // Current model
                val model = anchorMap[path!![modelIndex]]

                // Next model (if exists)
                val nextModel = if (modelIndex < path.size - 1) anchorMap[path[modelIndex + 1]] else null

                // If not already within threshold, check the distance
                if (!withinThreshold && distanceFromAnchor(model) < thresholdDistance) {
                    withinThreshold = true
                }

                // If we're within the threshold
                if (withinThreshold) {
                    // If it's the last anchor, hide it
                    if (modelIndex == path.size - 1) {
                        model?.isVisible = false
                    } else {
                        // If the next model is resolved, update visibility and move to next model
                        if (nextModel != null && nextModel.isResolved) {
                            updateVisibleModel(model, nextModel)
                            modelIndex++
                            withinThreshold = false // Reset the flag since we moved to the next anchor
                        }
                    }
                }

                // Continue the loop until the end of the path
                if (modelIndex < path.size - 1 || withinThreshold) {
                    handler.postDelayed(this, 500)
                }
            }
        }

        (anchorMap[path!![0]] as ModelNode).isVisible = true
        handler.post(runnableCode)
    }



    private fun distanceFromAnchor(anchorNode: ModelNode?): Float {
        val cameraPose = arSceneView.currentFrame?.camera?.pose

        val nodePose = anchorNode?.anchor?.pose

        if (nodePose != null && isInView(nodePose)) {
            val dx = cameraPose?.tx()?.minus(nodePose.tx())
            val dy = cameraPose?.ty()?.minus(nodePose.ty())
            val dz = cameraPose?.tz()?.minus(nodePose.tz())
            return sqrt((dx!! * dx + dy!! * dy + dz!! * dz).toDouble()).toFloat()
        }
        return Float.MAX_VALUE
    }

    private fun isInView(pose: Pose): Boolean {
        val screenCoord = getScreenCoordinates(pose) ?: return false
        return screenCoord.x >= 0 && screenCoord.y >= 0 && screenCoord.x <= arSceneView.width && screenCoord.y <= arSceneView.height
    }

    /*
     * Transforms a 3D pose in AR space to 2D screen coordinates.
     * This is useful for overlaying 2D UI elements over 3D AR content.
     */
    private fun getScreenCoordinates(pose: Pose): Vector3? {
        val camera = arSceneView.currentFrame?.camera ?: return null

        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

        val viewMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)

        // Compute the transformation from world to screen space
        val worldToScreenMatrix = FloatArray(16)
        Matrix.multiplyMM(worldToScreenMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Transform anchor's position to normalized device coordinates (NDC)
        val ndcCoord = FloatArray(4)
        Matrix.multiplyMV(
            ndcCoord,
            0,
            worldToScreenMatrix,
            0,
            floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f),
            0
        )

        // Convert NDC to actual screen coordinates
        val w = ndcCoord[3]
        val screenX = ((ndcCoord[0] / w + 1.0f) / 2.0f) * arSceneView.width
        val screenY = ((1.0f - ndcCoord[1] / w) / 2.0f) * arSceneView.height

        return Vector3(screenX, screenY, ndcCoord[2] / w)
    }

    private fun updateVisibleModel(oldModel: ModelNode?, newModel: ModelNode?) {
        oldModel?.isVisible = false
        newModel?.isVisible = true
    }

    fun onDestroy() {
        arSceneView.arSession?.destroy()
        arSceneView.destroy()
    }
}