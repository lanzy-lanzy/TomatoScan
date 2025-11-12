package com.ml.tomatoscan.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import android.net.Uri

class UserViewModel(application: Application) : ViewModel() {

    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Keys
    private val KEY_USER_NAME = "user_name"
    private val KEY_USER_PICTURE = "user_profile_picture_uri"
    private val KEY_THEME_MODE = "theme_mode" // system | light | dark
    private val KEY_APP_LANGUAGE = "app_language" // system | en | it
    private val KEY_GEMINI_PRE_VALIDATION = "gemini_pre_validation" // true | false

    // User name
    private val _userName = MutableStateFlow("Alex") // Default name
    val userName: StateFlow<String> = _userName

    // Profile picture
    private val _userProfilePictureUri = MutableStateFlow<String?>(null)
    val userProfilePictureUri: StateFlow<String?> = _userProfilePictureUri

    // Theme mode
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode

    // App language
    private val _appLanguage = MutableStateFlow("system")
    val appLanguage: StateFlow<String> = _appLanguage

    // Gemini pre-validation
    private val _geminiPreValidationEnabled = MutableStateFlow(true) // Default enabled
    val geminiPreValidationEnabled: StateFlow<Boolean> = _geminiPreValidationEnabled

    init {
        loadUserName()
        loadUserProfilePictureUri()
        loadThemeMode()
        loadAppLanguage()
        loadGeminiPreValidation()
    }

    private fun loadUserName() {
        _userName.value = sharedPreferences.getString(KEY_USER_NAME, "Alex") ?: "Alex"
    }

    private fun loadUserProfilePictureUri() {
        _userProfilePictureUri.value = sharedPreferences.getString(KEY_USER_PICTURE, null)
    }

    private fun loadThemeMode() {
        _themeMode.value = sharedPreferences.getString(KEY_THEME_MODE, "system") ?: "system"
    }

    private fun loadAppLanguage() {
        _appLanguage.value = sharedPreferences.getString(KEY_APP_LANGUAGE, "system") ?: "system"
    }

    private fun loadGeminiPreValidation() {
        _geminiPreValidationEnabled.value = sharedPreferences.getBoolean(KEY_GEMINI_PRE_VALIDATION, true)
    }

    fun updateUserProfilePictureUri(uri: Uri?) {
        viewModelScope.launch {
            val uriString = uri?.toString()
            _userProfilePictureUri.value = uriString
            with(sharedPreferences.edit()) {
                putString(KEY_USER_PICTURE, uriString)
                apply()
            }
        }
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            _userName.value = newName
            with(sharedPreferences.edit()) {
                putString(KEY_USER_NAME, newName)
                apply()
            }
        }
    }

    fun updateThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            "light", "dark" -> mode.lowercase()
            else -> "system"
        }
        viewModelScope.launch {
            _themeMode.value = normalized
            with(sharedPreferences.edit()) {
                putString(KEY_THEME_MODE, normalized)
                apply()
            }
        }
    }

    fun updateAppLanguage(lang: String) {
        val normalized = when (lang.lowercase()) {
            "en", "tl", "ceb" -> lang.lowercase()
            else -> "system"
        }
        viewModelScope.launch {
            _appLanguage.value = normalized
            with(sharedPreferences.edit()) {
                putString(KEY_APP_LANGUAGE, normalized)
                apply()
            }
        }
    }

    fun updateGeminiPreValidation(enabled: Boolean) {
        viewModelScope.launch {
            _geminiPreValidationEnabled.value = enabled
            with(sharedPreferences.edit()) {
                putBoolean(KEY_GEMINI_PRE_VALIDATION, enabled)
                apply()
            }
        }
    }

    fun clearAllPreferences() {
        viewModelScope.launch {
            with(sharedPreferences.edit()) {
                clear()
                apply()
            }
            // Reset to defaults
            _userName.value = "Alex"
            _userProfilePictureUri.value = null
            _themeMode.value = "system"
            _appLanguage.value = "system"
            _geminiPreValidationEnabled.value = true
        }
    }
}

class UserViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
