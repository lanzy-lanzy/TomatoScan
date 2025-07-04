package com.ml.tomatoscan.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ml.tomatoscan.utils.LocalizationManager
import com.ml.tomatoscan.utils.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    
    private val localizationManager = LocalizationManager(application)
    
    private val _currentLanguage = MutableStateFlow(SupportedLanguage.ENGLISH)
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage
    
    init {
        loadCurrentLanguage()
    }
    
    private fun loadCurrentLanguage() {
        viewModelScope.launch {
            try {
                val language = localizationManager.getCurrentLanguage()
                _currentLanguage.value = language
            } catch (e: Exception) {
                _currentLanguage.value = SupportedLanguage.ENGLISH
            }
        }
    }
    
    fun setLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            try {
                localizationManager.setLanguage(language)
                _currentLanguage.value = language
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    fun initializeLanguage() {
        viewModelScope.launch {
            try {
                localizationManager.initializeLanguage()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
}

class LanguageViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LanguageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LanguageViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
