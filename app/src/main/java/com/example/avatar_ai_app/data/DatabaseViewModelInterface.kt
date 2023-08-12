package com.example.avatar_ai_app.data

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_cloud_storage.database.entity.Feature

/**
 * [DatabaseViewModelInterface] is the interface between [MainViewModel] and the
 * Database ViewModel, which handles all database-related functionality.
 */

interface DatabaseViewModelInterface {
    // Indicates if the database has been initialised.
    val isReady: LiveData<Boolean>

    // Reload the database with the newest version on the server.
    fun reload(context: Context)

    // Gets the current feature list.
    fun getFeatures(): List<Feature>

    // Returns the feature with the given name or null if it does not exit.
    fun getFeature(name: String): Feature?

    // Returns the primary feature at a given anchor or null if it does not exist.
    fun getPrimaryFeature(anchorId: String): Feature?

    // Gets the current Cloud Anchor graph.
    fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>>

    // Gets an ordered list of all the features on the tour.
    fun getTourFeatures(): List<Feature>
}