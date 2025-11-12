package com.ml.tomatoscan.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TomatoScanViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TomatoScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TomatoScanViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
