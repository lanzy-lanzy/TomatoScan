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

class UserViewModel(application: Application, private val historyRepository: com.ml.tomatoscan.data.HistoryRepository) : ViewModel() {

    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow("Alex") // Default name
    val userName: StateFlow<String> = _userName

    private val _userProfilePictureUri = MutableStateFlow<String?>(null)
    val userProfilePictureUri: StateFlow<String?> = _userProfilePictureUri

    init {
        loadUserName()
        loadUserProfilePictureUri()
    }

    private fun loadUserName() {
        _userName.value = sharedPreferences.getString("user_name", "Alex") ?: "Alex"
    }

    private fun loadUserProfilePictureUri() {
        _userProfilePictureUri.value = sharedPreferences.getString("user_profile_picture_uri", null)
    }

    fun updateUserProfilePictureUri(uri: Uri?) {
        viewModelScope.launch {
            val uriString = uri?.toString()
            _userProfilePictureUri.value = uriString
            with(sharedPreferences.edit()) {
                putString("user_profile_picture_uri", uriString)
                apply()
            }
        }
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            _userName.value = newName
            with(sharedPreferences.edit()) {
                putString("user_name", newName)
                apply()
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Clear SharedPreferences
            with(sharedPreferences.edit()) {
                clear()
                apply()
            }
            // Clear history
            historyRepository.clearHistory()

            // Reset the state
            loadUserName()
            loadUserProfilePictureUri()
        }
    }
}

class UserViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(application, com.ml.tomatoscan.data.HistoryRepository(application)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
