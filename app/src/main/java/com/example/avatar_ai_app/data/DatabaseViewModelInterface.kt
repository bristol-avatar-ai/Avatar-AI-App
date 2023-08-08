package com.example.avatar_ai_app.data

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.avatar_ai_app.ui.MainViewModel
import com.example.avatar_ai_cloud_storage.database.Exhibition

/**
 * DatabaseViewModelInterface is the interface between [MainViewModel] and the
 * Database ViewModel, which handles all database-related functionality.
 */

interface DatabaseViewModelInterface {
    // Indicates if the database has been initialised.
    val isReady: LiveData<Boolean>

    // Reload the database with the newest version on the server.
    fun reload(context: Context)

    // Gets the current exhibition list.
    fun getExhibitions(): List<Exhibition>

    // Gets the current Cloud Anchor graph.
    fun getGraph(): HashMap<String, MutableList<Pair<String, Int>>>
}