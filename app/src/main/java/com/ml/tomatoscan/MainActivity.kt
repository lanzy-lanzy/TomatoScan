package com.ml.tomatoscan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ml.tomatoscan.ui.theme.TomatoScanTheme
import java.util.Locale
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language before super.onCreate to ensure correct resources
        applyLanguageFromPrefs()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val themeMode = prefs.getString("theme_mode", "system") ?: "system"

        setContent {
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            TomatoScanTheme(darkTheme = darkTheme) {
                Navigation()
            }
        }
    }

    private fun applyLanguageFromPrefs() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        when (val lang = prefs.getString("app_language", "system") ?: "system") {
            "system" -> return
            else -> applyLocale(lang)
        }
    }

    private fun applyLocale(languageCode: String) {
        try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (_: Exception) {
            // Fallback silently if locale application fails
        }
    }
}