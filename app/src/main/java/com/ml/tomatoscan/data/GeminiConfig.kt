package com.ml.tomatoscan.data

/**
 * Central place to configure Gemini model and API key for local/dev builds.
 *
 * IMPORTANT: Avoid committing real API keys to source control.
 * For production, prefer injecting via BuildConfig fields, encrypted storage,
 * or remote config. This file is provided for convenience during development.
 */
object GeminiConfig {
    /**
     * Gemini model to use for analysis. You can change this to another compatible
     * multimodal model (e.g., "gemini-1.5-pro") if desired.
     */
    @JvmStatic
    var MODEL: String = "gemini-1.5-flash"

    /**
     * Place your Gemini API key here for local testing, or leave null to fall back
     * to the GEMINI_API_KEY environment variable.
     *
     * Example (NOT REAL): "AIzaSy..."
     */
    @JvmStatic
    var API_KEY: String? = "AIzaSyBD15s-m0ClELhAR7XbbVPRkSFlQzcu_fQ"

    /**
     * Convenience provider that matches GeminiService's constructor signature.
     * Usage:
     *   val geminiService = GeminiService(GeminiConfig::provideApiKey)
     */
    @JvmStatic
    fun provideApiKey(): String? = API_KEY ?: System.getenv("GEMINI_API_KEY")
}

