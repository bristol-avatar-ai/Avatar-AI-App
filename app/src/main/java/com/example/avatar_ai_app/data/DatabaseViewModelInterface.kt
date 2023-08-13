package com.example.avatar_ai_app.data

import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_cloud_storage.database.entity.Anchor
import com.example.avatar_ai_cloud_storage.database.entity.Feature

/**
 * [DatabaseViewModelInterface] is the interface between [MainViewModel] and the
 * Database ViewModel, which handles all database-related functionality.
 *
 * [isReady] indicates if the database has been initialised.
 */
interface DatabaseViewModelInterface {
    /**
     * Indicates if the database has been initialised.
     */
    val isReady: LiveData<Boolean>

    /**
     * Reload the database with the newest version on the server.
     */
    fun reload()

    /**
     * Retrieves a list of features from the database.
     *
     * @return List of [Feature] objects.
     */
    fun getFeatures(): List<Feature>

    /**
     * Retrieves a specific feature by its name from the database.
     *
     * @param name The name of the feature to retrieve.
     * @return The [Feature] object with the specified name, or null if not found.
     */
    fun getFeature(name: String): Feature?

    /**
     * Retrieves the primary feature at the given Anchor ID, or null if it does not exist.
     *
     * @param anchorId The [Anchor] ID.
     * @return The primary [Feature] located at the [Anchor], or null if it does not exist.
     */
    fun getPrimaryFeature(anchorId: String): Feature?

    /**
     * Retrieves a graph representation of anchors and paths from the database.
     *
     * @return A [HashMap] mapping anchor IDs to lists of destinationId-distance [Pair]s.
     */
    fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>>

    /**
     * Retrieves an ordered list of features in the tour.
     *
     * @return Ordered list of [Feature] objects in the tour.
     */
    fun getTourFeatures(): List<Feature>
}