package com.ml.tomatoscan.utils

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

// Extension property to create DataStore
private val Application.localizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "localization_preferences")

enum class SupportedLanguage(val code: String, val displayName: String, val nativeDisplayName: String) {
    ENGLISH("en", "English", "English"),
    FILIPINO("fil", "Filipino", "Filipino"),
    CEBUANO("ceb", "Cebuano", "Cebuano")
}

class LocalizationManager(private val application: Application) {
    
    private val dataStore = application.localizationDataStore
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
    }
    
    suspend fun getCurrentLanguage(): SupportedLanguage {
        return try {
            val savedLanguage = dataStore.data.map { preferences ->
                preferences[LANGUAGE_KEY] ?: SupportedLanguage.ENGLISH.code
            }.first()
            
            SupportedLanguage.values().find { it.code == savedLanguage } 
                ?: SupportedLanguage.ENGLISH
        } catch (e: Exception) {
            SupportedLanguage.ENGLISH
        }
    }
    
    suspend fun setLanguage(language: SupportedLanguage) {
        try {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = language.code
            }
            applyLanguage(language)
        } catch (e: Exception) {
            // Handle error if needed
        }
    }
    
    private fun applyLanguage(language: SupportedLanguage) {
        val locale = when (language) {
            SupportedLanguage.ENGLISH -> Locale("en")
            SupportedLanguage.FILIPINO -> Locale("fil")
            SupportedLanguage.CEBUANO -> Locale("ceb")
        }
        
        Locale.setDefault(locale)
        
        val config = Configuration(application.resources.configuration)
        config.setLocale(locale)
        application.resources.updateConfiguration(config, application.resources.displayMetrics)
    }
    
    suspend fun initializeLanguage() {
        val currentLanguage = getCurrentLanguage()
        applyLanguage(currentLanguage)
    }
    
    fun createLocalizedContext(context: Context, language: SupportedLanguage): Context {
        val locale = when (language) {
            SupportedLanguage.ENGLISH -> Locale("en")
            SupportedLanguage.FILIPINO -> Locale("fil")
            SupportedLanguage.CEBUANO -> Locale("ceb")
        }
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
