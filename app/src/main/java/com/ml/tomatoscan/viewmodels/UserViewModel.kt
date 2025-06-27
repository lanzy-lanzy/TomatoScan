package com.ml.tomatoscan.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : ViewModel() {

    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow("Alex") // Default name
    val userName: StateFlow<String> = _userName

    init {
        loadUserName()
    }

    private fun loadUserName() {
        _userName.value = sharedPreferences.getString("user_name", "Alex") ?: "Alex"
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
