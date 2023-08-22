package com.example.avatar_ai_app.ar

import android.annotation.SuppressLint
import android.app.Application
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.data.avatarModel
import com.example.avatar_ai_app.data.crystalModel
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Rotation
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.math.sqrt

private const val TAG = "ArViewModel"

class ArViewModel(application: Application) : AndroidViewModel(application), ArViewModelInterface {
    private val _application = application
    private lateinit var graph: Graph
    private lateinit var unresolvedIds: HashSet<String>

    @SuppressLint("StaticFieldLeak")
    lateinit var arSceneView: ArSceneView
    private lateinit var avatarModelNode: ModelNode
    private var modelIndex: Int = 0


    private val context
        get() = _application.applicationContext

    enum class ModelType {
        AVATAR,
        CRYSTAL
    }

    override fun setGraph(graph: Graph) {
        this.graph = graph
        Log.d("ArViewModel", "graph keys = ${graph.keys}")
    }

    override fun initialiseArScene(arSceneView: ArSceneView) {
        viewModelScope.launch {
            arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            arSceneView.cloudAnchorEnabled = true
            delay(1000L)
            setIdList()
            resolveAllAnchors(arSceneView)
        }
        this.arSceneView = arSceneView

        Log.d("ArViewModel", "Initialise Done")
    }

    override fun addModelToScene(modelType: ModelType) {
        when (modelType) {
            ModelType.AVATAR -> {
                avatarModelNode = createModel(ModelType.AVATAR)
                avatarModelNode.isVisible = true
                arSceneView.addChild(avatarModelNode)
            }

            ModelType.CRYSTAL -> {
            }
        }
    }

    private fun setIdList() {
        unresolvedIds = HashSet()
        graph.keys.forEach { id ->
            unresolvedIds.add(id)
        }
        Log.d(TAG, "Number of anchors = ${unresolvedIds.size}")
    }

    private fun createModel(modelType: ModelType): ModelNode {
        val modelNode: ModelNode

        when (modelType) {
            ModelType.AVATAR -> {
                val model = avatarModel
                modelNode = ModelNode(arSceneView.engine).apply {
                    viewModelScope.launch {
                        loadModelGlb(
                            context = context,
                            glbFileLocation = model.fileLocation,
                            scaleToUnits = model.scale,
                            centerOrigin = model.position
                        )
                    }
                }
            }

            ModelType.CRYSTAL -> {
                val model = crystalModel
                modelNode = ModelNode(arSceneView.engine).apply {
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
                    isVisible = true
                }
                arSceneView.addChild(modelNode)
//                arSceneView.selectedNode = modelNode
            }
        }
        return modelNode
    }

    private val resolvedModelNodes = mutableListOf<ModelNode>()
    private val anchorMap: MutableMap<String, ModelNode> = mutableMapOf()

    private fun resolveAllAnchors(arSceneView: ArSceneView) {
        Log.d(TAG, "resolve all anchors called")
        val nodeKeys = graph.keys.toList()

        // Iterate over all node keys
        for (anchorId in unresolvedIds) {
            val modelNode = createModel(ModelType.CRYSTAL)

            //need to check if this line is needed
//            arSceneView.addChild(modelNode)
            resolveModel(modelNode, anchorId)
            resolvedModelNodes.add(modelNode)
        }

        // Map anchors to model nodes
        resolvedModelNodes.forEachIndexed { index, modelNode ->
            nodeKeys[index].let { key ->
                anchorMap[key] = modelNode
            }
        }
    }

    private fun resolveModel(modelNode: ModelNode, anchorId: String?) {
        Log.d(TAG, "resolve model called $anchorId")
        if (anchorId != null) {
            modelNode.resolveCloudAnchor(anchorId) { anchor: Anchor, success: Boolean ->
                Log.d(TAG, anchor.trackingState.toString())

                if (success) {
                    // TODO: Need to change back to false
                    Log.d(TAG, "Success")
                    Log.d(TAG, "AnchorId = $anchorId")
                    //modelNode.isVisible = true
                    modelNode.isResolved = true
                    //Log.d(TAG, "Cloud anchor state = ${modelNode.cloudAnchorState.name}")

                    unresolvedIds.remove(anchorId)
                    Log.d(TAG, "Unresolved anchors: $unresolvedIds")
                    Log.d(TAG, "Unresolved anchors remaining: ${unresolvedIds.size}")
                }
                if (!success) {
                    Log.d(TAG, "Failure")
                    resolveModel(modelNode, anchorId)
                }


            }
        } else {
            Log.d(TAG, "Null anchor")
        }
    }

    // This function will return the Id of the nearest cloud anchor
    private fun closestAnchor(): String? {
        var closestAnchorId: String? = null

        // This will run until an anchor is found in view and returned
        //while (closestAnchorId == null) {
        // TODO: DISPLAY A MESSAGE ASKING THE USER TO PAN AROUND?
        var minDistance = Float.MAX_VALUE

        for ((anchorId, anchorNode) in anchorMap) {
            val nodePose = anchorNode.anchor?.pose
            if (nodePose != null && isInView(nodePose) && anchorNode.isResolved && !anchorNode.isSign) {
                val distance = distanceFromAnchor(anchorNode)
                if (distance < minDistance) {
                    minDistance = distance
                    closestAnchorId = anchorId
                }
            }
        }
        return closestAnchorId
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
     *
     * arSceneView - The current AR Scene View.
     * pose - The 3D position in AR space to be transformed.
     * returns Vector3 containing the x, y screen coordinates, and z depth.
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

    private val handler = Handler(Looper.getMainLooper())

    private fun showPath(path: List<String>?) {
        Log.d(TAG, "Path = $path")
        val runnableCode = object : Runnable {
            override fun run() {

                val thresholdDistance = 3

                val model = anchorMap[path!![modelIndex]]
                val nextModel = anchorMap[path[modelIndex + 1]]

                //if (distanceFromAnchor(model) < thresholdDistance) {
                if (nextModel?.isResolved == true) {
                    if (modelIndex < path.size - 1) {
                        Toast.makeText(
                            context,
                            "Current anchor ${path[modelIndex + 1]}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateVisibleModel(
                            model, nextModel
                        )
                        modelIndex++
                    }
                }
                // }
                if (modelIndex < path.size - 1) {
                    handler.postDelayed(this, 500)
                }
            }
        }

        (anchorMap[path!![0]] as ModelNode).isVisible = true
        handler.post(runnableCode)
    }


    private fun updateVisibleModel(oldModel: ModelNode?, newModel: ModelNode?) {
        oldModel?.isVisible = false
        newModel?.isVisible = true
    }


    private fun moveAvatarToFirstAnchor(nextModel: ModelNode?, newModel: ModelNode?) {
        avatarModelNode.isVisible = true

        newModel?.let { newModelPosition ->
            avatarModelNode.worldPosition = newModelPosition.worldPosition
            avatarModelNode.worldScale = newModelPosition.worldScale

            nextModel?.let { nextModelPosition ->
                val directionVector = Vector3(
                    nextModelPosition.worldPosition.x - newModelPosition.worldPosition.x,
                    nextModelPosition.worldPosition.y - nextModelPosition.worldPosition.y,
                    0f // Ignore z coordinate
                )

                if (!directionVector.equals(0)) {
                    val normalizedDirection = directionVector.normalized()
                    val angle = Math.toDegrees(
                        atan2(
                            normalizedDirection.y.toDouble(),
                            normalizedDirection.x.toDouble()
                        )
                    ).toFloat()

                    // Get the new model's rotation as Euler angles
                    val newModelEulerRotation = newModelPosition.worldRotation

                    // Set the avatar's rotation
                    avatarModelNode.worldRotation =
                        Rotation(newModelEulerRotation.x, newModelEulerRotation.y, angle)
                }
            } ?: run {
                avatarModelNode.worldRotation = newModelPosition.worldRotation
            }
        }
        Toast.makeText(context, "Avatar at first anchor", Toast.LENGTH_SHORT).show()
    }


    private fun moveAvatarToNewAnchor(oldModel: ModelNode?, newModel: ModelNode?) {
        // Ensure the avatarModelNode is visible
        avatarModelNode.isVisible = true

        // Move the avatar to the position of the newModel
        newModel?.let { newModelPosition ->
            avatarModelNode.worldPosition = newModelPosition.worldPosition
            avatarModelNode.worldScale = newModelPosition.worldScale

            // If there's an oldModel, calculate direction to oldModel in the x-y plane
            oldModel?.let { oldModelPosition ->
                val directionVector = Vector3(
                    oldModelPosition.worldPosition.x - newModelPosition.worldPosition.x,
                    oldModelPosition.worldPosition.y - newModelPosition.worldPosition.y,
                    0f // Ignore z coordinate
                ).normalized()

                // Get the angle from the direction vector in degrees
                val angle = Math.toDegrees(
                    atan2(
                        directionVector.y.toDouble(),
                        directionVector.x.toDouble()
                    )
                ).toFloat()

                // Convert the angle to Euler angles for yaw (rotation around Z)
                val eulerZRotation = Rotation(0f, 0f, angle)

                // Apply the Euler angles to the world rotation
                avatarModelNode.worldRotation = eulerZRotation
            }
        }
        Toast.makeText(context, "End of move avatar to next model", Toast.LENGTH_SHORT).show()

    }

//    override fun loadDirections(destination: String) {
//        val currentLocation = closestAnchor()
//
//        if (currentLocation.equals(null)) {
//            Toast.makeText(context, "Please point me to nearest anchor", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val (_, paths) = dijkstra(currentLocation!!)
//        val path = paths[destination]
//
//        showPath(path)
//        modelIndex = 0
//    }
//override fun loadDirections(destination: String) {
//    CoroutineScope(Dispatchers.Main).launch {
//        var currentLocation = closestAnchor()
//
//        while (currentLocation == null) {
//            Toast.makeText(context, "Please point me to nearest anchor", Toast.LENGTH_SHORT).show()
//            delay(2000) // Wait for 2 seconds before checking again
//            currentLocation = closestAnchor()
//        }
//
//        val (_, paths) = dijkstra(currentLocation)
//        val path = paths[destination]
//
//        showPath(path)
//        modelIndex = 0
//    }
//}

    override fun loadDirections(destination: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var currentLocation = closestAnchor()
            var snackbar: Snackbar? = null

            while (currentLocation == null) {
                if (snackbar?.isShown == false || snackbar == null) {
                    snackbar = Snackbar.make(
                        arSceneView, // Replace with the view you want the Snackbar to be attached to
                        "Please point me to nearest anchor",
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
            modelIndex = 0
        }
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
                graph[unvisitedNode]?.edges?.forEach() { edge ->
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

    fun onDestroy() {
        arSceneView.arSession?.destroy()
        arSceneView.destroy()
    }
}


