package com.example.avatar_ai_app.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [DatabaseViewModelFactory] os a custom ViewModelFactory used to create instances of [DatabaseViewModel].
 */

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