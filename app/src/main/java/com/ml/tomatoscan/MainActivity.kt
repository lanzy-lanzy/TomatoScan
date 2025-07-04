package com.ml.tomatoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ml.tomatoscan.ui.theme.TomatoScanTheme
import com.ml.tomatoscan.viewmodels.ThemeViewModel
import com.ml.tomatoscan.viewmodels.ThemeViewModelFactory
import com.ml.tomatoscan.viewmodels.LanguageViewModel
import com.ml.tomatoscan.viewmodels.LanguageViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(application)
            )
            val languageViewModel: LanguageViewModel = viewModel(
                factory = LanguageViewModelFactory(application)
            )

            val themeMode by themeViewModel.themeMode.collectAsState()

            // Initialize language on app start
            languageViewModel.initializeLanguage()

            TomatoScanTheme(themeMode = themeMode) {
                Navigation()
            }
        }
    }
}