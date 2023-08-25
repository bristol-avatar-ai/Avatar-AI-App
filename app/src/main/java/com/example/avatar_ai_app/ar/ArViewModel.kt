package com.example.avatar_ai_app.ar

import android.annotation.SuppressLint
import android.app.Application
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.data.crystalModel
import com.example.avatar_ai_app.data.signModel
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.PlacementMode
import kotlinx.coroutines.*
import kotlin.math.sqrt

private const val TAG = "ArViewModel"

class ArViewModel(application: Application) : AndroidViewModel(application), ArViewModelInterface {

    // Class Variables
    private val _application = application
    private lateinit var graph: Graph
    @SuppressLint("StaticFieldLeak")
    lateinit var arSceneView: ArSceneView
    private val context get() = _application.applicationContext
    private val anchorMap: MutableMap<String, ModelNode> = mutableMapOf()
    private var pathfindingJob: Job? = null

    fun onDestroy() {
        arSceneView.arSession?.destroy()
        arSceneView.destroy()
    }

    override fun setGraph(graph: Graph) {
        this.graph = graph
    }

    /**
     * Initialises the given AR scene view with specific settings.
     * Sets light estimation mode, enables cloud anchors, resolves all anchors,
     * and starts monitoring the models in the view.
     */
    override fun initialiseArScene(arSceneView: ArSceneView) {
        // Set up the AR scene view with environmental HDR light estimation and enable cloud anchors
        viewModelScope.launch {
            arSceneView.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                cloudAnchorEnabled = true
            }
            delay(1000L)
            resolveAllAnchors()
        }

        // Assign the passed AR scene view to the class property
        this.arSceneView = arSceneView

        Log.d("ArViewModel", "Initialise AR Scene Complete")

        // Start monitoring models in the view
        monitorModelsInView()
    }

    /**
     * Iterates over all anchors in the graph. For each anchor, it determines the type of
     * model (sign or crystal) based on its name, resolves the anchor for the model,
     * and adds the model node to the anchor map.
     */
    private fun resolveAllAnchors() {
        graph.forEach { anchorId, anchorProperties ->
            // Determine which type of model to create based on the anchor name
            val modelNode = if (anchorProperties.name.contains("SIGN")) {
                createSignModel(anchorProperties.name)
            } else {
                createCrystalModel(anchorProperties.name)
            }

            // Resolve the anchor for the created model node
            resolveModel(modelNode, anchorId)

            // Add the model node to the anchor map
            anchorMap[anchorId] = modelNode
        }
    }

    /**
     * Creates a crystal model based on the given anchor name, initialises its properties,
     * and adds it to the AR scene. If the anchor name contains "ORIENTATION", the model
     * will be marked as an orientation model.
     */
    private fun createCrystalModel(anchorName: String): ModelNode {
        // Load crystal model properties
        val model = crystalModel

        // Create and initialise model node based on anchor name
        val modelNode = ModelNode(arSceneView.engine, anchorName).apply {
            // Load the associated model asynchronously
            viewModelScope.launch {
                loadModelGlb(
                    context = context,
                    glbFileLocation = model.fileLocation,
                    scaleToUnits = model.scale,
                    centerOrigin = model.position
                )
            }

            // Set model properties
            placementMode = PlacementMode.INSTANT
            isVisible = false

            // Mark as an orientation model if anchor name contains "ORIENTATION"
            if (anchorName.contains("ORIENTATION")) {
                isOrientation = true
            }
        }

        // Add model to AR scene
        arSceneView.addChild(modelNode)

        return modelNode
    }

    /**
     * Creates a sign model based on the given anchor name, initialises it,
     * sets its properties, and adds it to the AR scene.
     */
    private fun createSignModel(anchorName: String): ModelNode {
        // Load sign model properties
        val model = signModel

        // Create and initialise model node based on anchor name
        val modelNode = ModelNode(arSceneView.engine, anchorName).apply {
            // Load the associated model asynchronously
            viewModelScope.launch {
                loadModelGlb(
                    context = context,
                    glbFileLocation = "models/${anchorName.substring(7)}.glb",
                    scaleToUnits = model.scale,
                    centerOrigin = model.position
                )
            }

            // Set model properties
            placementMode = PlacementMode.INSTANT
            isVisible = false
            isSign = true
        }

        // Add model to AR scene
        arSceneView.addChild(modelNode)

        return modelNode
    }


    /**
     * Attempts to resolve the provided model node using a cloud anchor ID.
     * If resolution fails, it recursively tries again.
     */
    private fun resolveModel(modelNode: ModelNode, anchorId: String?) {
        if (anchorId == null) {
            Log.d(TAG, "Null anchor Id")
            return
        }

        modelNode.resolveCloudAnchor(anchorId) { anchor: Anchor, success: Boolean ->
            Log.d(TAG, "Anchor tracking state: ${anchor.trackingState}")

            when {
                success -> {
                    Log.d(TAG, "Successfully resolved anchor: $anchorId")
                    modelNode.isResolved = true
                }
                else -> {
                    Log.w(TAG, "Failed to resolve anchor (Id: $anchorId). Retrying...")
                    resolveModel(modelNode, anchorId)
                }
            }
        }
    }

    /**
     * Checks whether a sign and orientation model are for the same feature.
     */
    private fun sameFeature(signModelNode: ModelNode, orientationModelNode: ModelNode): Boolean {
        val featureName = signModelNode.modelName?.substring(7)
        return orientationModelNode.modelName?.contains(featureName ?: "") == true
    }

    /**
     * Loads and displays the shortest path to the given destination.
     */
    override fun loadDirections(destination: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentLocation = detectCurrentLocation()
            val (_, paths) = dijkstra(currentLocation)
            val path = paths[destination]

            if(path != null) {
                showPath(path)
            } else {
                Log.w(TAG, "Invalid Anchor ID.")
            }
        }
    }

    /**
     * Detects the current location by finding the closest anchor.
     * If no anchor is detected immediately, prompts the user to scan the surroundings.
     */
    private suspend fun detectCurrentLocation(): String {
        var currentLocation = closestAnchor()
        var snackbar: Snackbar? = null

        while (currentLocation == null) {
            snackbar = showSnackbarIfNeeded(snackbar)
            delay(2000) // Wait for 2 seconds before checking again
            currentLocation = closestAnchor()
        }

        snackbar?.dismiss()
        return currentLocation
    }

    /**
     * Displays a snackbar prompting the user to scan surroundings, if not already shown.
     */
    private fun showSnackbarIfNeeded(snackbar: Snackbar?): Snackbar {
        return if (snackbar?.isShown == false || snackbar == null) {
            Snackbar.make(
                arSceneView, "Please move your device around to scan the surroundings",
                Snackbar.LENGTH_INDEFINITE
            ).apply { show() }
        } else {
            snackbar
        }
    }

    /**
     * Finds and returns the ID of the closest cloud anchor that is both in view and resolved, but not a sign.
     */
    private fun closestAnchor(): String? {
        var closestAnchorId: String? = null

        // This will run until an anchor is found in view and returned
        var minDistance = Float.MAX_VALUE

        for ((anchorId, anchorNode) in anchorMap) {
            val nodePose = anchorNode.anchor?.pose
            if (
                nodePose != null && isInView(anchorNode)
                && anchorNode.isResolved && !anchorNode.isSign
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

    /**
    * Calculates the shortest paths and distances from the given source node to all other nodes
     * in the graph using Dijkstra's algorithm.
    */
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
                updateDistancesAndPaths(it, dist, prev, paths, sptSet)
            }
        }
        return Pair(dist, paths)
    }

    /**
     * Updates the distances and paths for the neighboring nodes of a given node.
     */
    private fun updateDistancesAndPaths(node: String, dist: MutableMap<String, Int>, prev: MutableMap<String, String>, paths: MutableMap<String, List<String>>, sptSet: MutableSet<String>) {
        sptSet.add(node)
        graph[node]?.edges?.forEach { edge ->
            if (!sptSet.contains(edge.destination) && dist.getValue(node) != Int.MAX_VALUE) {
                val newDist = dist.getValue(node) + edge.distance
                if (newDist < dist.getValue(edge.destination)) {
                    dist[edge.destination] = newDist
                    prev[edge.destination] = node
                    paths[edge.destination] = paths.getValue(node) + edge.destination
                }
            }
        }
    }

    /**
    * Shows the path defined by the list of model keys.
    */
    private fun showPath(path: List<String>) {
        resetPath()
        var modelIndex = 0
        var withinThreshold = false

        pathfindingJob = viewModelScope.launch(Dispatchers.IO) {
            (anchorMap[path[0]] as ModelNode).isVisible = true

            while(modelIndex < path.size - 1 || withinThreshold) {
                val thresholdDistance = 3

                // Current model
                val model = anchorMap[path[modelIndex]]

                // Next model (if exists)
                val nextModel = if (modelIndex < path.size - 2) anchorMap[path[modelIndex + 1]] else null

                // If not already within threshold, check the distance
                if (!withinThreshold && distanceFromAnchor(model) < thresholdDistance) { withinThreshold = true }

                if(distanceFromAnchor(model) < 2){ model?.isVisible = false }

                if (withinThreshold) {
                    // If the next model is resolved, update visibility and move to next model
                    if (nextModel != null && nextModel.isResolved) {
                        updateVisibleModel(model, nextModel)
                        modelIndex++
                        withinThreshold = false // Reset the flag since we moved to the next anchor
                    }
                }
                delay(500)
            }
        }
    }

    /**
     * Resets the AR path by canceling any ongoing pathfinding job and hiding all non-sign nodes.
     */
    private fun resetPath() {
        pathfindingJob?.cancel()
        for ((_, anchorNode) in anchorMap) {
            if (!anchorNode.isSign) {
                anchorNode.isVisible = false
            }
        }
    }

    /**
     * Computes the distance between the camera and the provided anchor node.
     */
    private fun distanceFromAnchor(anchorNode: ModelNode?): Float {
        val cameraPose = arSceneView.currentFrame?.camera?.pose
        val nodePose = anchorNode?.anchor?.pose

        if (cameraPose != null && nodePose != null && isInView(anchorNode)) {
            return computeDistanceBetweenPoses(cameraPose, nodePose)
        }
        return Float.MAX_VALUE
    }

    /**
     * Computes the Euclidean distance between two poses.
     */
    private fun computeDistanceBetweenPoses(pose1: Pose, pose2: Pose): Float {
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }


    /**
     * Checks if the given anchor node's model is within the boundaries of the AR scene view.
     */
    private fun isInView(anchorNode: ModelNode): Boolean {
        val pose = anchorNode.anchor?.pose ?: return false
        val screenCoord = getScreenCoordinates(pose) ?: return false
        return isWithinScreenBounds(screenCoord)
    }

    /**
     * Checks if the given screen coordinates lie within the screen boundaries.
     */
    private fun isWithinScreenBounds(screenCoord: Vector3): Boolean {
        return screenCoord.x >= 0 && screenCoord.y >= 0 &&
                screenCoord.x <= arSceneView.width && screenCoord.y <= arSceneView.height
    }

    /**
     * Transforms a 3D pose in AR space to 2D screen coordinates.
     */
    private fun getScreenCoordinates(pose: Pose): Vector3? {
        val camera = arSceneView.currentFrame?.camera ?: return null

        val worldToScreenMatrix = computeWorldToScreenMatrix(camera)

        // Transform the anchor's position to normalised device coordinates (NDC)
        val ndcCoord = transformToNDC(pose, worldToScreenMatrix)

        // Convert NDC to actual screen coordinates
        return convertNDCtoScreenCoordinates(ndcCoord)
    }

    /**
     * Computes the transformation matrix from world to screen space using a given camera.
     */
    private fun computeWorldToScreenMatrix(camera: Camera): FloatArray {
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

        val viewMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)

        val worldToScreenMatrix = FloatArray(16)
        Matrix.multiplyMM(worldToScreenMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        return worldToScreenMatrix
    }

    /**
     * Transforms a 3D pose to normalised device coordinates (NDC) using a given transformation matrix.
     */
    private fun transformToNDC(pose: Pose, matrix: FloatArray): FloatArray {
        val ndcCoord = FloatArray(4)
        Matrix.multiplyMV(
            ndcCoord,
            0,
            matrix,
            0,
            floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f),
            0
        )
        return ndcCoord
    }

    /**
     * Converts normalised device coordinates (NDC) to actual screen coordinates.
     */
    private fun convertNDCtoScreenCoordinates(ndc: FloatArray): Vector3 {
        val w = ndc[3]
        val screenX = ((ndc[0] / w + 1.0f) / 2.0f) * arSceneView.width
        val screenY = ((1.0f - ndc[1] / w) / 2.0f) * arSceneView.height

        return Vector3(screenX, screenY, ndc[2] / w)
    }


    private fun updateVisibleModel(oldModel: ModelNode?, newModel: ModelNode?) {
        oldModel?.isVisible = false
        newModel?.isVisible = true
    }

    private fun monitorModelsInView() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(500) // Check every half second (or adjust as per your need)
                processAnchorsInView()
            }
        }
    }

    /**
     * Iterates through each combination of signModelNode and orientationModelNode.
     * For each valid pair, it checks their status and relationships, and then performs
     * necessary UI updates on the main thread.
     */
    private suspend fun processAnchorsInView() {
        anchorMap.forEach { (_, signModelNode) ->
            if (isValidNode(signModelNode, true)) {
                anchorMap.forEach { (_, orientationModelNode) ->
                    if (isValidNode(orientationModelNode, false) && shouldProcess(signModelNode, orientationModelNode)) {
                        updateUI(signModelNode, orientationModelNode)
                    }
                }
            }
        }
    }

    /**
     * Validates the given modelNode based on its 'isSign' status and resolution.
     */
    private fun isValidNode(modelNode: ModelNode, shouldBeSign: Boolean) =
        modelNode.isSign == shouldBeSign && modelNode.isResolved

    /**
     * Determines if two given nodes should be processed together.
     */
    private fun shouldProcess(signModelNode: ModelNode, orientationModelNode: ModelNode) =
        sameFeature(signModelNode, orientationModelNode) &&
                isInView(signModelNode) &&
                isInView(orientationModelNode) &&
                distanceFromAnchor(signModelNode) < 7

    /**
     * Performs UI operations on the main thread.
     * It makes the signModelNode look at the orientationModelNode and makes the signModelNode visible.
     */
    private suspend fun updateUI(signModelNode: ModelNode, orientationModelNode: ModelNode) {
        withContext(Dispatchers.Main) {
            signModelNode.lookAt(orientationModelNode)
            signModelNode.isVisible = true
        }
    }

}