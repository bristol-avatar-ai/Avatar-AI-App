package com.example.avatar_ai_app.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.avatar_ai_cloud_storage.database.Anchor
import com.example.avatar_ai_cloud_storage.database.AppDatabase
import com.example.avatar_ai_cloud_storage.database.Exhibition
import com.example.avatar_ai_cloud_storage.database.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DatabaseViewModel"

class DatabaseViewModel(context: Context) : ViewModel() {

    private val _isReady = MutableLiveData<Boolean>()
    val isReady: LiveData<Boolean>
        get() = _isReady

    private var _database: AppDatabase? = null
    private val database get() = _database!!
    private val anchorDao get() = database.anchorDao()
    private val exhibitionDao get() = database.exhibitionDao()
    private val pathDao get() = database.pathDao()

    init {
        reload(context)
    }

    fun reload(context: Context) {
        AppDatabase.close()
        viewModelScope.launch(Dispatchers.IO) {
            _database = AppDatabase.getDatabase(context)
            _isReady.postValue(_database != null)
        }
    }

    fun getAnchors(): List<Anchor> {
        return anchorDao.getAnchors()
    }

    fun getExhibitions(): List<Exhibition> {
        return exhibitionDao.getExhibitions()
    }

    private fun getPaths(): List<Path> {
        return pathDao.getPaths()
    }

    fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>> {
        val graph = HashMap<String, MutableList<Pair<String, Int>>>()
        getAnchors().forEach {
            graph[it.id] = mutableListOf()
        }
        getPaths().forEach {
            graph[it.origin]?.add(Pair(it.destination, it.distance))
        }
        return graph
    }

}

class DatabaseViewModelFactory(private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}