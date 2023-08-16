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
import com.example.avatar_ai_app.ui.AvatarState
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Rotation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import kotlin.math.atan2
import kotlin.math.sqrt

private const val TAG = "ArViewModel"

class ArViewModel(application: Application) : AndroidViewModel(application), ArViewModelInterface {
    private val _application = application
    private lateinit var graph: Graph
    @SuppressLint("StaticFieldLeak")
    lateinit var arSceneView: ArSceneView
    private lateinit var avatarModelNode: ArModelNode

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
            resolveAllAnchors(arSceneView)
        }
        this.arSceneView = arSceneView

        Log.d("ArViewModel", "Initialise Done")
    }

    override fun addModelToScene(modelType: ModelType) {
        when (modelType) {
            ModelType.AVATAR -> {
                avatarModelNode = createModel(ModelType.AVATAR)
                avatarModelNode.isVisible = false
                arSceneView.addChild(avatarModelNode)
            }

            ModelType.CRYSTAL -> {
            }
        }
    }

    private fun createModel(modelType: ModelType): ArModelNode {
        val modelNode: ArModelNode

        when (modelType) {
            ModelType.AVATAR -> {
                val model = avatarModel
                modelNode = ArModelNode(arSceneView.engine).apply {
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
                modelNode = ArModelNode(arSceneView.engine).apply {
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
                arSceneView.selectedNode = modelNode
            }
        }

        return modelNode
    }

    private val resolvedModelNodes = mutableListOf<ArModelNode>()
    private val anchorMap: MutableMap<String, ArModelNode> = mutableMapOf()

    private fun resolveAllAnchors(arSceneView: ArSceneView) {

        Log.d("ArViewModel", "Graph keys 2 : ${graph.keys}")
        Log.d("ArViewModel", "Graph keys 3 : ${graph.keys.toList()}")

        val nodeKeys = graph.keys.toList()
        Log.d("ArViewModel", "nodeKeys: $nodeKeys")


        // Iterate over all node keys
        for (key in nodeKeys) {
            val modelNode = createModel(ModelType.CRYSTAL)

            //need to check if this line is needed
            arSceneView.addChild(modelNode)
            resolveModel(modelNode, key)
            resolvedModelNodes.add(modelNode)
        }

        // Map anchors to model nodes
        resolvedModelNodes.forEachIndexed { index, arModelNode ->
            nodeKeys[index].let { key ->
                anchorMap[key] = arModelNode
            }
        }
    }

    private fun resolveModel(modelNode: ArModelNode, anchorId: String?) {


        if (anchorId != null) {
            modelNode.resolveCloudAnchor(anchorId) { anchor: Anchor, success: Boolean ->
                Log.d(TAG, anchor.trackingState.toString())

                if (success) {
                    // TODO: Need to change back to false
                    modelNode.isVisible = false
                }
                if(!success) {
                    Log.d("ArViewModel", "Failure")
                }


            }
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
                if (nodePose != null && isInView(nodePose)) {
                    val distance = distanceFromAnchor(anchorNode)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestAnchorId = anchorId
                    }
                }
            }

//            if (closestAnchorId == null) {
//                // Sleep for a short duration to avoid overwhelming the CPU
//                Thread.sleep(500)
//            }
        //}

        return closestAnchorId
    }


    private fun distanceFromAnchor(anchorNode: ArModelNode?): Float {
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

//    private fun showPath(path: List<String>?) {
//        Log.d("ArViewModel", path.toString())
//        val pathIterator = path?.listIterator()
//
//        val runnableCode = object : Runnable {
//            override fun run() {
//                val thresholdDistance = 2
//
//                // If there's a next model in the path
//                if (pathIterator?.hasNext() == true) {
//                    val currentModelId = pathIterator.next()
//                    val currentModel = anchorMap[currentModelId]
//
//                    // If there's another next model in the path after the current model
//                    if (pathIterator.hasNext()) {
//                        val nextModelId = pathIterator.next()
//                        val nextModel = anchorMap[nextModelId]
//
//                        if (distanceFromAnchor(currentModel) < thresholdDistance) {
//                            Toast.makeText(context, "Within threshold", Toast.LENGTH_SHORT).show()
//
//                            updateVisibleModel(currentModel, nextModel)
//                            Toast.makeText(context, "Next model visible", Toast.LENGTH_SHORT).show()
//
//                            //moveAvatarToNewAnchor(currentModel, nextModel)
//                        } else {
//                            // If the threshold isn't met, move iterator back to retry the same step
//                            if (pathIterator.hasPrevious()) {
//                                pathIterator.previous() // move back to 'nextModelId'
//                                if (pathIterator.hasPrevious()) {
//                                    pathIterator.previous() // move back to 'currentModelId'
//                                }
//                            }
//                        }
//                        handler.postDelayed(this, 500)
//                    } else {
//                        // Last model reached
//                        if (distanceFromAnchor(currentModel) < thresholdDistance) {
//                            currentModel?.isVisible = false
//                            Toast.makeText(context, "Path completed", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }
//            }
//        }
//
//        anchorMap[path?.elementAtOrNull(0)]?.isVisible = true
//        //moveAvatarToFirstAnchor(anchorMap[path?.elementAtOrNull(1)], anchorMap[path?.elementAtOrNull(0)])
//        handler.post(runnableCode)
//    }

    private var modelIndex = 0

    private fun showPath(path: List<String>?) {
        val runnableCode = object : Runnable {
            override fun run() {

                val thresholdDistance = 1.5

                val model = anchorMap[path!![modelIndex]]

                if (distanceFromAnchor(model) < thresholdDistance) {
                    if (modelIndex < path.size - 1) {
                        updateVisibleModel(anchorMap[path[modelIndex]], anchorMap[path[modelIndex+1]])
                        modelIndex++
                    }
                }
                if (modelIndex < path.size - 1) {
                    handler.postDelayed(this, 500)
                }
            }
        }
        (anchorMap[path!![0]] as ArModelNode).isVisible = true
        handler.post(runnableCode)
    }


    private fun updateVisibleModel(oldModel: ArModelNode?, newModel: ArModelNode?) {
        oldModel?.isVisible = false
        newModel?.isVisible = true
    }


    private fun moveAvatarToFirstAnchor(nextModel: ArModelNode?, newModel: ArModelNode?) {
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
                    val angle = Math.toDegrees(atan2(normalizedDirection.y.toDouble(), normalizedDirection.x.toDouble())).toFloat()

                    // Get the new model's rotation as Euler angles
                    val newModelEulerRotation = newModelPosition.worldRotation

                    // Set the avatar's rotation
                    avatarModelNode.worldRotation = Rotation(newModelEulerRotation.x, newModelEulerRotation.y, angle)
                }
            } ?: run {
                avatarModelNode.worldRotation = newModelPosition.worldRotation
            }
        }
        Toast.makeText(context, "Avatar at first anchor", Toast.LENGTH_SHORT).show()
    }




    private fun moveAvatarToNewAnchor(oldModel: ArModelNode?, newModel: ArModelNode?) {
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
                val angle = Math.toDegrees(atan2(directionVector.y.toDouble(), directionVector.x.toDouble())).toFloat()

                // Convert the angle to Euler angles for yaw (rotation around Z)
                val eulerZRotation = Rotation(0f, 0f, angle)

                // Apply the Euler angles to the world rotation
                avatarModelNode.worldRotation = eulerZRotation
            }
        }
        Toast.makeText(context, "End of move avatar to next model", Toast.LENGTH_SHORT).show()

    }

    override fun loadDirections(destination: String) {
        val currentLocation = closestAnchor()

        if (currentLocation.equals(null)) {
            Toast.makeText(context, "Please point me to nearest anchor", Toast.LENGTH_SHORT).show()
            return
        }

        val (_, paths) = dijkstra(currentLocation!!)
        val path = paths[destination]
        showPath(path)
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
                graph[unvisitedNode]?.forEach { edge ->
                    if (!sptSet.contains(edge.first) && dist.getValue(unvisitedNode) != Int.MAX_VALUE) {
                        val newDist = dist.getValue(unvisitedNode) + edge.second
                        if (newDist < dist.getValue(edge.first)) {
                            dist[edge.first] = newDist
                            prev[edge.first] = unvisitedNode
                            paths[edge.first] = paths.getValue(unvisitedNode) + edge.first
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


