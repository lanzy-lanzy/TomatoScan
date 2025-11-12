package com.ml.tomatoscan.config

import com.ml.tomatoscan.BuildConfig

/**
 * Configuration for Google Gemini AI integration
 * Uses deterministic parameters to ensure consistent outputs
 */
object GeminiConfig {
    /**
     * Gemini API key loaded from BuildConfig
     * Should be configured in local.properties as gemini.api.key
     */
    val API_KEY: String
        get() = BuildConfig.GEMINI_API_KEY

    /**
     * Gemini model name to use for diagnostic report generation
     * Loaded from BuildConfig
     */
    val MODEL_NAME: String
        get() = BuildConfig.GEMINI_MODEL_NAME

    /**
     * Temperature parameter for generation (0.0 = deterministic)
     * Lower values make output more focused and deterministic
     */
    const val TEMPERATURE = 0.0f

    /**
     * Top-P (nucleus sampling) parameter
     * Lower values increase determinism
     */
    const val TOP_P = 0.1f

    /**
     * Top-K parameter for token selection
     * Value of 1 ensures maximum determinism
     */
    const val TOP_K = 1

    /**
     * Maximum number of retry attempts for API calls
     */
    const val MAX_RETRIES = 3

    /**
     * Timeout for API requests in seconds
     */
    const val TIMEOUT_SECONDS = 30L

    /**
     * Flag to enable/disable Gemini integration
     * Can be configured in local.properties as enable.gemini
     */
    val ENABLE_GEMINI: Boolean
        get() = BuildConfig.ENABLE_GEMINI
}
