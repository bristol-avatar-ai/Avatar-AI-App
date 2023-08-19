package com.example.avatar_ai_app.data

import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.ar.Graph
import com.example.avatar_ai_app.chat.ChatViewModelInterface.Status
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_cloud_storage.database.entity.Anchor
import com.example.avatar_ai_cloud_storage.database.entity.Feature

/**
 * [DatabaseViewModelInterface] is the interface between [MainViewModel] and the
 * Database ViewModel, which handles all database-related functionality.
 *
 * [status] indicates if the database is ready.
 */
interface DatabaseViewModelInterface {

    /**
     * Enum class representing the different status states of [DatabaseViewModel].
     *
     * @constructor Creates an instance of [Status].
     */
    enum class Status { LOADING, READY, ERROR }

    /**
     * Indicates if the database is ready.
     */
    val status: LiveData<Status>

    /**
     * Reload the database with the newest version on the server.
     */
    fun reload()

    /**
     * Retrieves a list of features from the database.
     *
     * @return List of [Feature] objects.
     */
    suspend fun getFeatures(): List<Feature>

    /**
     * Retrieves a specific feature by its name from the database.
     *
     * @param name The name of the feature to retrieve.
     * @return The [Feature] object with the specified name, or null if not found.
     */
    suspend fun getFeature(name: String): Feature?

    /**
     * Retrieves the primary feature at the given Anchor ID, or null if it does not exist.
     *
     * @param anchorId The [Anchor] ID.
     * @return The primary [Feature] located at the [Anchor], or null if it does not exist.
     */
    suspend fun getPrimaryFeature(anchorId: String): Feature?

    /**
     * Retrieves a graph representation of anchors and paths from the database.
     *
     * @return A [Graph] mapping anchor IDs to lists of destinationId-distance [Pair]s.
     */
    suspend fun getGraph(): Graph

    /**
     * Retrieves an ordered list of features in the tour.
     *
     * @return Ordered list of [Feature] objects in the tour.
     */
    suspend fun getTourFeatures(): List<Feature>
}