# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# TensorFlow Lite Rules
# ============================================================================
# Keep TFLite classes for model inference
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# Keep TFLite Support library classes
-keep class org.tensorflow.lite.support.** { *; }
-keepclassmembers class org.tensorflow.lite.support.** { *; }

# Keep TFLite GPU delegate
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.gpu.** { *; }

# ============================================================================
# ONNX Runtime Rules (if used for YOLOv11)
# ============================================================================
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }

# ============================================================================
# Google Gemini AI SDK Rules
# ============================================================================
# Keep Gemini Generative AI classes
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }

# Keep Gemini model classes
-keep class com.google.ai.client.generativeai.type.** { *; }

# ============================================================================
# Data Model Classes for Serialization
# ============================================================================
# Keep all data models in the models package
-keep class com.ml.tomatoscan.models.** { *; }
-keepclassmembers class com.ml.tomatoscan.models.** { *; }

# Keep all data classes in the data package
-keep class com.ml.tomatoscan.data.** { *; }
-keepclassmembers class com.ml.tomatoscan.data.** { *; }

# Keep config classes
-keep class com.ml.tomatoscan.config.** { *; }
-keepclassmembers class com.ml.tomatoscan.config.** { *; }

# Keep service interfaces and implementations
-keep class com.ml.tomatoscan.services.** { *; }
-keepclassmembers class com.ml.tomatoscan.services.** { *; }

# ============================================================================
# Room Database Rules
# ============================================================================
# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Room DAOs
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room Database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# ============================================================================
# Kotlin Serialization Rules
# ============================================================================
# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializer classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data class fields for serialization
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# Parcelable Rules
# ============================================================================
# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ============================================================================
# Coroutines Rules
# ============================================================================
# Keep coroutines classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================================
# General Rules
# ============================================================================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BuildConfig
-keep class com.ml.tomatoscan.BuildConfig { *; }