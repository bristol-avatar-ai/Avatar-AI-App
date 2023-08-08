package com.example.avatar_ai_app.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_cloud_storage.database.AppDatabase
import com.example.avatar_ai_cloud_storage.database.Exhibition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DatabaseViewModel"

class DatabaseViewModel(context: Context) : ViewModel(), DatabaseViewModelInterface {

    private val _isReady = MutableLiveData<Boolean>()
    override val isReady: LiveData<Boolean> get() = _isReady

    private var database: AppDatabase? = null
    private val anchorDao get() = database?.anchorDao()
    private val exhibitionDao get() = database?.exhibitionDao()
    private val pathDao get() = database?.pathDao()

    init {
        reload(context)
    }

    override fun reload(context: Context) {
        AppDatabase.close()
        viewModelScope.launch(Dispatchers.IO) {
            database = AppDatabase.getDatabase(context)
            _isReady.postValue(database != null)
        }
    }

    override fun getExhibitions(): List<Exhibition> {
        return exhibitionDao?.getExhibitions() ?: emptyList()
    }

    override fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>> {
        val graph = HashMap<String, MutableList<Pair<String, Int>>>()
        anchorDao?.getAnchors()?.forEach {
            graph[it.id] = mutableListOf()
        }
        pathDao?.getPaths()?.forEach {
            graph[it.origin]?.add(Pair(it.destination, it.distance))
        }
        return graph
    }

}