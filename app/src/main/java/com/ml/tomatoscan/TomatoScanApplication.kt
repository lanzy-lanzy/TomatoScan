package com.ml.tomatoscan

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class TomatoScanApplication : Application(), CameraXConfig.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash handler for debugging
        try {
            com.ml.tomatoscan.utils.CrashHandler.getInstance().init(this)
            Log.d("TomatoScanApplication", "Crash handler initialized")
        } catch (e: Exception) {
            Log.e("TomatoScanApplication", "Failed to initialize crash handler", e)
        }
    }
    
    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.WARN)
            .build()
    }
}
