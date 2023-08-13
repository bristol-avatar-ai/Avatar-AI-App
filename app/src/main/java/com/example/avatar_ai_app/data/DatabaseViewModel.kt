package com.example.avatar_ai_app.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    // LiveData indicating whether the database is ready.
    private val _isReady = MutableLiveData<Boolean>()
    override val isReady: LiveData<Boolean> get() = _isReady

    // Database and DAO instances.
    private var database: AppDatabase? = null
    private val anchorDao get() = database?.anchorDao()
    private val pathDao get() = database?.pathDao()
    private val featureDao get() = database?.featureDao()
    private val primaryFeatureDao get() = database?.primaryFeatureDao()
    private val tourFeatureDao get() = database?.tourFeatureDao()

    /*
    * Reload the database on initialisation.
     */
    init {
        reload()
    }

    /**
     * Reloads the database instance and updates the [_isReady] LiveData.
     */
    override fun reload() {
        _isReady.postValue(false)
        AppDatabase.close()
        database = null

        viewModelScope.launch(Dispatchers.IO) {
            // Load database.
            database = AppDatabase.getDatabase(
                getApplication<Application>().applicationContext
            )
            // Update _isReady.
            _isReady.postValue(database != null)
            if (database != null) {
                Log.i(TAG, "reload: success")
            } else {
                Log.e(TAG, "reload: failed")
            }
        }
    }

    /**
     * Retrieves a list of features from the database.
     *
     * @return List of [Feature] objects.
     */
    override fun getFeatures(): List<Feature> {
        return featureDao?.getFeatures() ?: emptyList()
    }

    /**
     * Retrieves a specific feature by its name from the database.
     *
     * @param name The name of the feature to retrieve.
     * @return The [Feature] object with the specified name, or null if not found.
     */
    override fun getFeature(name: String): Feature? {
        return featureDao?.getFeature(name)
    }

    /**
     * Retrieves the primary feature at the given Anchor ID, or null if it does not exist.
     *
     * @param anchorId The [Anchor] ID.
     * @return The primary [Feature] located at the [Anchor], or null if it does not exist.
     */
    override fun getPrimaryFeature(anchorId: String): Feature? {
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
     * @return A [HashMap] mapping anchor IDs to lists of destinationId-distance [Pair]s.
     */
    override fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>> {
        val graph = HashMap<String, MutableList<Pair<String, Int>>>()

        // Add anchors to graph.
        anchorDao?.getAnchors()?.forEach {
            graph[it.id] = mutableListOf()
        }

        // Add paths to graph.
        pathDao?.getPaths()?.forEach {
            graph[it.origin]?.add(Pair(it.destination, it.distance))
        }
        return graph
    }

    /**
     * Retrieves an ordered list of features in the tour.
     *
     * @return Ordered list of [Feature] objects in the tour.
     */
    override fun getTourFeatures(): List<Feature> {
        return tourFeatureDao?.getTourFeatures()?.mapNotNull {
            getFeature(it)
        } ?: emptyList()
    }
}