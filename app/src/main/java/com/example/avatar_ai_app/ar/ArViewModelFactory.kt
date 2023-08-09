package com.example.avatar_ai_app.ar

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [ArViewModelFactory] os a custom ViewModelFactory used to create instances of [ArViewModel].
 */

class ArViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}