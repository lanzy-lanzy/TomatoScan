# Configuration Package

This package contains all configuration objects for the Tomato Leaf Analysis AI Agent system.

## Configuration Files

### GeminiConfig.kt
Configuration for Google Gemini AI integration with deterministic parameters.

**Key Parameters:**
- `API_KEY`: Loaded from `local.properties` (gemini.api.key)
- `MODEL_NAME`: "gemini-2.0-flash-exp"
- `TEMPERATURE`: 0.0 (deterministic output)
- `TOP_P`: 0.1 (low nucleus sampling)
- `TOP_K`: 1 (maximum determinism)
- `ENABLE_GEMINI`: Flag to enable/disable Gemini integration

**Purpose:** Ensures consistent, deterministic diagnostic reports by using low-temperature generation parameters.

### ModelConfig.kt
Configuration for ML models (YOLOv11 and TFLite).

**Key Parameters:**
- `YOLO_MODEL_PATH`: Path to YOLOv11 leaf detection model
- `TFLITE_MODEL_PATH`: Path to disease classification model
- `YOLO_INPUT_SIZE`: 640x640 pixels
- `TFLITE_INPUT_SIZE`: 224x224 pixels
- `CONFIDENCE_THRESHOLD`: 0.5 (minimum confidence for predictions)
- `DISEASE_CLASSES`: List of supported disease types

**Purpose:** Centralizes model paths, input sizes, and classification thresholds.

### CacheConfig.kt
Configuration for result caching system.

**Key Parameters:**
- `MAX_CACHE_SIZE`: 100 entries (LRU eviction)
- `CACHE_TTL_DAYS`: 7 days
- `ENABLE_CACHING`: Flag to enable/disable caching
- `HASH_SIMILARITY_THRESHOLD`: 0.95 (perceptual hash matching)

**Purpose:** Ensures consistent results for identical inputs through intelligent caching.

## Setup Instructions

### 1. Configure local.properties

Add the following properties to your `local.properties` file:

```properties
# Gemini AI Configuration
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
enable.gemini=true

# Caching Configuration
enable.caching=true
```

### 2. Obtain Gemini API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Copy the key to `local.properties`

### 3. Add YOLOv11 Model

Place the YOLOv11 TFLite model file at:
```
app/src/main/assets/models/yolov11_tomato_leaf.tflite
```

## BuildConfig Integration

The configuration values are automatically loaded into BuildConfig during the build process:

- `BuildConfig.GEMINI_API_KEY`
- `BuildConfig.ENABLE_GEMINI`
- `BuildConfig.ENABLE_CACHING`
- `BuildConfig.MODEL_VERSION`

## Usage Example

```kotlin
import com.ml.tomatoscan.config.GeminiConfig
import com.ml.tomatoscan.config.ModelConfig
import com.ml.tomatoscan.config.CacheConfig

// Access Gemini configuration
val apiKey = GeminiConfig.API_KEY
val temperature = GeminiConfig.TEMPERATURE

// Access model configuration
val modelPath = ModelConfig.TFLITE_MODEL_PATH
val inputSize = ModelConfig.TFLITE_INPUT_SIZE

// Access cache configuration
val cacheEnabled = CacheConfig.ENABLE_CACHING
val maxCacheSize = CacheConfig.MAX_CACHE_SIZE
```

## Requirements Mapping

This configuration setup addresses the following requirements:

- **Requirement 6.1**: Load Gemini API credentials from secure configuration
- **Requirement 6.2**: Allow configuration of Gemini model parameters
- **Requirement 6.3**: Load TFLite model from configurable path
- **Requirement 6.4**: Validate required models are present
- **Requirement 3.2**: Deterministic parameters for consistency
- **Requirement 4.4**: Caching configuration for result consistency
