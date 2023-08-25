package com.example.avatar_ai_app.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_app.ar.Edge
import com.example.avatar_ai_app.ar.Graph
import com.example.avatar_ai_app.ar.Properties
import com.example.avatar_ai_app.data.DatabaseViewModelInterface.Status
import com.example.avatar_ai_cloud_storage.database.AppDatabase
import com.example.avatar_ai_cloud_storage.database.entity.Anchor
import com.example.avatar_ai_cloud_storage.database.entity.Feature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DatabaseViewModel"

/**
 * ViewModel providing access to database-related operations and data for the app.
 *
 * @param application The application context.
 * @constructor Creates a [DatabaseViewModel] instance.
 */
class DatabaseViewModel(application: Application) : AndroidViewModel(application),
    DatabaseViewModelInterface {

    private val _status = MutableLiveData<Status>()
    override val status: LiveData<Status> get() = _status

    // Application context.
    private val context get() = getApplication<Application>().applicationContext

    // Database and DAO instances.
    private var database: AppDatabase? = null
    private val anchorDao get() = database?.anchorDao()
    private val pathDao get() = database?.pathDao()
    private val featureDao get() = database?.featureDao()
    private val primaryFeatureDao get() = database?.primaryFeatureDao()
    private val tourFeatureDao get() = database?.tourFeatureDao()

    /*
    * Post the current Status to the _status LiveData.
     */
    private fun postStatus(status: Status) {
        _status.postValue(status)
        if (status != Status.ERROR) {
            Log.i(TAG, "status: $status")
        } else {
            Log.w(TAG, "status: $status")
        }
    }

    /*
    * Reload the database on initialisation.
     */
    init {
        reload()
    }

    /**
     * Reloads the database instance and updates the [status] LiveData.
     */
    override fun reload() {
        postStatus(Status.LOADING)
        AppDatabase.close()
        database = null

        viewModelScope.launch(Dispatchers.IO) {
            // Load database.
            database = AppDatabase.getDatabase(context)
            // Update status.
            postStatus(
                if (database != null) {
                    Status.READY
                } else {
                    Status.ERROR
                }
            )
        }
    }

    /**
     * Retrieves a list of features from the database.
     *
     * @return List of [Feature] objects.
     */
    override suspend fun getFeatures(): List<Feature> {
        return featureDao?.getFeatures() ?: emptyList()
    }

    /**
     * Retrieves a specific feature by its name from the database.
     *
     * @param name The name of the feature to retrieve.
     * @return The [Feature] object with the specified name, or null if not found.
     */
    override suspend fun getFeature(name: String): Feature? {
        return featureDao?.getFeature(name)
    }

    /**
     * Retrieves the primary feature at the given Anchor ID, or null if it does not exist.
     *
     * @param anchorId The [Anchor] ID.
     * @return The primary [Feature] located at the [Anchor], or null if it does not exist.
     */
    override suspend fun getPrimaryFeature(anchorId: String): Feature? {
        val featureName = primaryFeatureDao?.getPrimaryFeature(anchorId)?.feature
        return if (featureName != null) {
            getFeature(featureName)
        } else {
            null
        }
    }

    /**
     * Retrieves a graph representation of anchors and paths from the database.
     *
     * @return A [Graph] mapping anchor IDs to lists of destinationId-distance [Pair]s.
     */
    override suspend fun getGraph(): Graph {
        val graph = Graph()

        // Add anchor IDs and names to graph.
        anchorDao?.getAnchors()?.forEach {
            graph[it.id] = Properties(it.name, mutableListOf())
        }

        // Add paths to graph.
        pathDao?.getPaths()?.forEach {
            graph[it.anchor1]?.edges?.add(Edge(it.anchor2, it.distance))
            graph[it.anchor2]?.edges?.add(Edge(it.anchor1, it.distance))
        }
        return graph
    }

    /**
     * Retrieves an ordered list of features in the tour.
     *
     * @return Ordered list of [Feature] objects in the tour.
     */
    override suspend fun getTourFeatures(): List<Feature> {
        return tourFeatureDao?.getTourFeatures()?.mapNotNull {
            getFeature(it)
        } ?: emptyList()
    }
}