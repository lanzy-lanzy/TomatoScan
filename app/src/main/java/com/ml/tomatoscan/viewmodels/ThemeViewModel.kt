package com.ml.tomatoscan.viewmodels

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Extension property to create DataStore
private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStore = application.dataStore
    
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
    
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode
    
    init {
        loadThemeMode()
    }
    
    private fun loadThemeMode() {
        viewModelScope.launch {
            try {
                val savedTheme = dataStore.data.map { preferences ->
                    preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
                }.first()
                
                _themeMode.value = ThemeMode.valueOf(savedTheme)
            } catch (e: Exception) {
                // If there's an error, default to SYSTEM
                _themeMode.value = ThemeMode.SYSTEM
            }
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[THEME_MODE_KEY] = mode.name
                }
                _themeMode.value = mode
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
}

class ThemeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
