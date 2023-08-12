package com.example.avatar_ai_app.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_cloud_storage.database.AppDatabase
import com.example.avatar_ai_cloud_storage.database.entity.Feature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DatabaseViewModel"

/**
 * [DatabaseViewModel] provides access to database-related operations and data for the app.
 */

class DatabaseViewModel(context: Context) : ViewModel(), DatabaseViewModelInterface {

    // LiveData to indicate if the database is ready.
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
        reload(context)
    }

    /*
    * Reloads the database instance and updates the _isReady LiveData.
     */
    override fun reload(context: Context) {
        _isReady.postValue(false)
        AppDatabase.close()
        database = null

        viewModelScope.launch(Dispatchers.IO) {
            // Load database.
            database = AppDatabase.getDatabase(context)

            _isReady.postValue(database != null)
            if (database != null) {
                Log.i(TAG, "reload: success")
            } else {
                Log.w(TAG, "reload: failed")
            }
        }
    }

    /*
    * Retrieves a list of features from the database.
     */
    override fun getFeatures(): List<Feature> {
        return featureDao?.getFeatures() ?: emptyList()
    }

    /*
    * Retrieves a specific feature by its name from the database.
     */
    override fun getFeature(name: String): Feature? {
        return featureDao?.getFeature(name)
    }


    /*
    * Retrieves the primary feature at the given Anchor ID or null if it does not exist.
     */
    override fun getPrimaryFeature(anchorId: String): Feature? {
        val featureName = primaryFeatureDao?.getPrimaryFeature(anchorId)
        return if (featureName != null) {
            getFeature(featureName.feature)
        } else {
            null
        }
    }

    /*
    * Retrieves a graph representation of the anchors and paths from the database.
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

    /*
    * Retrieve an ordered list of features in the tour.
     */
    override fun getTourFeatures(): List<Feature> {
        return tourFeatureDao?.getTourFeatures() ?: emptyList()
    }
}