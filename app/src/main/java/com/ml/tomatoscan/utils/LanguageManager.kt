package com.ml.tomatoscan.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino"),
    CEBUANO("ceb", "Bisaya")
}

val LocalLanguage = compositionLocalOf { AppLanguage.ENGLISH }

object LanguageManager {
    private const val LANGUAGE_PREF_KEY = "app_language"
    
    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(LANGUAGE_PREF_KEY, language.code).apply()
    }
    
    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedCode = prefs.getString(LANGUAGE_PREF_KEY, AppLanguage.ENGLISH.code)
        return AppLanguage.values().find { it.code == savedCode } ?: AppLanguage.ENGLISH
    }
    
    fun setLocale(context: Context, language: AppLanguage): Context {
        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.FILIPINO -> Locale("fil")
            AppLanguage.CEBUANO -> Locale("ceb")
        }
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun getCurrentLanguageDisplayName(context: Context): String {
        return getSavedLanguage(context).displayName
    }
}

@Composable
fun LanguageProvider(
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLanguage provides language) {
        content()
    }
}

@Composable
fun rememberLanguage(): AppLanguage {
    val context = LocalContext.current
    return remember { LanguageManager.getSavedLanguage(context) }
}
