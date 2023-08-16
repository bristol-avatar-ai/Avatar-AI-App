package com.example.avatar_ai_app.ar

import android.app.Application
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.R
import com.example.avatar_ai_app.data.avatarModel
import com.example.avatar_ai_app.data.crystalModel
import com.example.avatar_ai_app.ui.AvatarState
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val TAG = "ArViewModel"

class ArViewModel(application: Application) : AndroidViewModel(application), ArViewModelInterface {

    private val _uiState = MutableStateFlow(AvatarState())
    private val _application = application
    private var graph: Graph = Graph()

    var arSceneView: ArSceneView? = null
    private val context
        get() = _application.applicationContext

    private val uiState: StateFlow<AvatarState> = _uiState.asStateFlow()

    //Can we delete this??
    val nodes = mutableStateListOf<ArNode>()


    enum class AvatarButtonType {
        VISIBILITY,
        MODE
    }

    enum class ModelType {
        AVATAR,
        CRYSTAL
    }

    private lateinit var avatarModelNode: ArModelNode

    // TODO: Ed please remember to call this
    override fun setGraph(graph: Graph) {
        this.graph = graph
    }

    private fun showAvatar() {
        avatarModelNode.detachAnchor()
        avatarModelNode.isVisible = true
        _uiState.update { currentState ->
            currentState.copy(
                avatarIsAnchored = false,
                avatarIsVisible = true
            )
        }
    }

    private fun hideAvatar() {
        avatarModelNode.isVisible = false
        _uiState.update { currentState ->
            currentState.copy(
                avatarIsVisible = false
            )
        }

    }

    private fun anchorAvatar() {
        avatarModelNode.anchor()
        _uiState.update { currentState ->
            currentState.copy(
                avatarIsAnchored = true
            )
        }
    }

    private fun detachAvatar() {
        avatarModelNode.detachAnchor()
        _uiState.update { currentState ->
            currentState.copy(
                avatarIsAnchored = false
            )
        }
    }
    
    override fun initialiseArScene(arSceneView: ArSceneView) {
        arSceneView.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        resolveAllAnchors(arSceneView)
    }

    override fun addModelToScene(arSceneView: ArSceneView, modelType: ModelType) {
        when (modelType) {
            ModelType.AVATAR -> {
                avatarModelNode = createModel(arSceneView, ModelType.AVATAR)
                avatarModelNode.isVisible = false
                arSceneView.addChild(avatarModelNode)
            }

            ModelType.CRYSTAL -> {

            }
        }
    }

    //Creates a model from the ModelType data class
    private fun createModel(arSceneView: ArSceneView, modelType: ModelType): ArModelNode {
        val modelNode = ArModelNode(arSceneView.engine).apply {
            viewModelScope.launch {
                when (modelType) {
                    ModelType.AVATAR -> {
                        loadModelGlb(
                            context = context,
                            glbFileLocation = avatarModel.fileLocation,
                            scaleToUnits = avatarModel.scale,
                            centerOrigin = avatarModel.position
                        )
                    }

                    ModelType.CRYSTAL -> {
                        loadModelGlb(
                            context = context,
                            glbFileLocation = crystalModel.fileLocation,
                            scaleToUnits = crystalModel.scale,
                            centerOrigin = crystalModel.position
                        )
                    }
                }
            }
        }
        return modelNode
    }

    fun avatarButtonOnClick() {
        _uiState.update { currentState ->
            when (uiState.value.isAvatarMenuVisible) {
                true -> currentState.copy(
                    isAvatarMenuVisible = false
                )

                false -> currentState.copy(
                    isAvatarMenuVisible = true
                )
            }
        }
    }

    fun summonOrHideButtonOnClick() {
        if (uiState.value.avatarIsVisible) {
            hideAvatar()
        } else {
            showAvatar()
        }
    }

    fun anchorOrFollowButtonOnClick() {
        if (uiState.value.avatarIsAnchored) {
            detachAvatar()
        } else if (!uiState.value.avatarIsAnchored) {
            anchorAvatar()
        }
    }

    fun dismissActionMenu() {
        if (uiState.value.isAvatarMenuVisible) {
            _uiState.update { currentState ->
                currentState.copy(
                    isAvatarMenuVisible = false
                )
            }
        }
    }

    fun getActionButtonValues(buttonType: AvatarButtonType):
            Pair<Int, String> {
        return when (buttonType) {
            AvatarButtonType.VISIBILITY -> {
                if (uiState.value.avatarIsVisible) {
                    Pair(R.drawable.hide, "Dismiss Avatar")
                } else Pair(R.drawable.robot_icon, "Summon Avatar")
            }

            AvatarButtonType.MODE -> {
                if (uiState.value.avatarIsAnchored) {
                    Pair(R.drawable.unlock_icon, "Release Avatar")
                } else Pair(R.drawable.lock_icon, "Place Avatar Here")
            }
        }
    }

    fun enableActionButton(buttonType: AvatarButtonType): Boolean {
        return when (buttonType) {
            AvatarButtonType.MODE -> {
                (uiState.value.isAvatarMenuVisible and uiState.value.avatarIsVisible)
            }

            AvatarButtonType.VISIBILITY -> {
                uiState.value.isAvatarMenuVisible
            }
        }
    }

    private val resolvedModelNodes = mutableListOf<ArModelNode>()
    private val anchorMap: MutableMap<String, ArModelNode> = mutableMapOf()

    private fun resolveAllAnchors(arSceneView: ArSceneView) {

        val nodeKeys = graph.keys?.toList()

        // Iterate over all node keys
        if (nodeKeys != null) {
            for (key in nodeKeys) {
                val modelNode = createModel(arSceneView, ModelType.CRYSTAL)

                resolveModel(modelNode, key)
                resolvedModelNodes.add(modelNode)
            }
        }

        // Map anchors to model nodes
        resolvedModelNodes.forEachIndexed { index, arModelNode ->
            nodeKeys?.get(index)?.let { key ->
                anchorMap[key] = arModelNode
            }
        }
    }

    private fun resolveModel(modelNode: ArModelNode, anchorId: String?) {

        if (anchorId != null) {
            modelNode.resolveCloudAnchor(anchorId) { _: Anchor, success: Boolean ->
                if (success) {
                    modelNode.isVisible = false
                }
            }
        }
    }

    // This function will return the Id of the nearest cloud anchor
    private fun closestAnchor(arSceneView: ArSceneView): String? {
        var minDistance = Float.MAX_VALUE
        var closestAnchorId: String? = null

        for ((anchorId, anchorNode) in anchorMap) {
            val nodePose = anchorNode.anchor?.pose
            if (nodePose != null && isInView(arSceneView, nodePose)) {
                val distance = distanceFromAnchor(arSceneView, anchorNode)
                if (distance < minDistance) {
                    minDistance = distance
                    closestAnchorId = anchorId
                }
            }
        }
        return closestAnchorId
    }

    private fun distanceFromAnchor(arSceneView: ArSceneView, anchorNode: ArModelNode?): Float {
        val cameraPose = arSceneView.currentFrame?.camera?.pose

        val nodePose = anchorNode?.anchor?.pose

        if (nodePose != null && isInView(arSceneView, nodePose)) {
            val dx = cameraPose?.tx()?.minus(nodePose.tx())
            val dy = cameraPose?.ty()?.minus(nodePose.ty())
            val dz = cameraPose?.tz()?.minus(nodePose.tz())
            return sqrt((dx!! * dx + dy!! * dy + dz!! * dz).toDouble()).toFloat()
        }
        return Float.MAX_VALUE
    }

    private fun isInView(arSceneView: ArSceneView, pose: Pose): Boolean {
        val screenCoord = getScreenCoordinates(arSceneView, pose) ?: return false
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
    private fun getScreenCoordinates(arSceneView: ArSceneView, pose: Pose): Vector3? {
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

    private fun updateVisibleModel(oldModel: ArModelNode?, newModel: ArModelNode?) {
        // TODO: Cesca - change the model on appearance
        oldModel?.isVisible = false
        newModel?.isVisible = true
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun showPath(arSceneView: ArSceneView, path: List<String>?) {
        val pathIterator = path?.listIterator()

        val runnableCode = object : Runnable {
            override fun run() {
                val thresholdDistance = 2

                // If there's a next model in the path
                if (pathIterator?.hasNext() == true) {
                    val currentModelId = pathIterator.next()
                    val currentModel = anchorMap[currentModelId]

                    // If there's another next model in the path after the current model
                    if (pathIterator.hasNext()) {
                        val nextModelId = pathIterator.next()
                        val nextModel = anchorMap[nextModelId]

                        if (distanceFromAnchor(arSceneView, currentModel) < thresholdDistance) {
                            updateVisibleModel(currentModel, nextModel)
                        } else {
                            // If the threshold isn't met, move iterator back to retry the same step
                            if (pathIterator.hasPrevious()) {
                                pathIterator.previous() // move back to 'nextModelId'
                                if (pathIterator.hasPrevious()) {
                                    pathIterator.previous() // move back to 'currentModelId'
                                }
                            }
                        }
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }

        anchorMap[path?.firstOrNull()]?.isVisible = true
        handler.post(runnableCode)
    }

    private fun loadDirections(arSceneView: ArSceneView, destination: String) {
        val currentLocation = closestAnchor(arSceneView)
        if (currentLocation.equals(null)) {
            Toast.makeText(context, "Please point me to nearest anchor", Toast.LENGTH_SHORT).show()
            return
        }

        val (_, paths) = dijkstra(currentLocation!!)
        val path = paths[destination]
        showPath(arSceneView, path)
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


}


