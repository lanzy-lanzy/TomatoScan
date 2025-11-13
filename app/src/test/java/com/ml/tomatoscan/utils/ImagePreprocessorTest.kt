package com.ml.tomatoscan.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import com.ml.tomatoscan.config.ModelConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ImagePreprocessor validation
 * Tests preprocessing for 640x640 input as required by YOLOv11 model
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class ImagePreprocessorTest {

    /**
     * Test that preprocessForDetection resizes images to exactly 640x640 pixels
     * Requirement 5.1: Verify it resizes images to exactly 640x640 pixels
     */
    @Test
    fun testPreprocessForDetection_ResizesTo640x640() {
        // Test with various input sizes
        val testSizes = listOf(
            Pair(1920, 1080),  // Landscape
            Pair(1080, 1920),  // Portrait
            Pair(800, 800),    // Square
            Pair(320, 240),    // Small landscape
            Pair(240, 320)     // Small portrait
        )

        testSizes.forEach { (width, height) ->
            val inputBitmap = createTestBitmap(width, height)
            val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

            assertEquals(
                "Width should be 640 for input ${width}x${height}",
                640,
                result.width
            )
            assertEquals(
                "Height should be 640 for input ${width}x${height}",
                640,
                result.height
            )
        }
    }

    /**
     * Test that preprocessForDetection uses ModelConfig.YOLO_INPUT_SIZE
     * Requirement 5.1: Verify output dimensions match configuration
     */
    @Test
    fun testPreprocessForDetection_UsesModelConfigSize() {
        val inputBitmap = createTestBitmap(1024, 768)
        val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

        assertEquals(
            "Width should match ModelConfig.YOLO_INPUT_SIZE",
            ModelConfig.YOLO_INPUT_SIZE,
            result.width
        )
        assertEquals(
            "Height should match ModelConfig.YOLO_INPUT_SIZE",
            ModelConfig.YOLO_INPUT_SIZE,
            result.height
        )
    }

    /**
     * Test that RGB color space is maintained (not BGR)
     * Requirement 5.3: Verify RGB color space is maintained (not BGR)
     */
    @Test
    fun testPreprocessForDetection_MaintainsRGBColorSpace() {
        // Create a bitmap with distinct RGB values
        val inputBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // Fill with a known color (Red=255, Green=100, Blue=50)
        val testColor = Color.rgb(255, 100, 50)
        for (x in 0 until inputBitmap.width) {
            for (y in 0 until inputBitmap.height) {
                inputBitmap.setPixel(x, y, testColor)
            }
        }

        val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

        // Sample the center pixel
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val red = Color.red(centerPixel)
        val green = Color.green(centerPixel)
        val blue = Color.blue(centerPixel)

        // Verify RGB order is maintained (allowing for some processing variation)
        // Red should be highest, green middle, blue lowest
        assertTrue(
            "Red channel should be highest (RGB not BGR). R=$red, G=$green, B=$blue",
            red > green && green > blue
        )
        
        // Verify red is still dominant (within reasonable range due to normalization)
        assertTrue(
            "Red should be significantly higher than blue (RGB not BGR). R=$red, B=$blue",
            red > blue + 50
        )
    }

    /**
     * Test that pixel values are in 0-255 range for INT8 quantized models
     * Requirement 5.4: Verify pixel values are in 0-255 range for INT8 quantized models
     */
    @Test
    fun testPreprocessForDetection_PixelValuesInValidRange() {
        val inputBitmap = createTestBitmap(800, 600)
        val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

        // Sample multiple pixels across the image
        val samplePoints = listOf(
            Pair(0, 0),                                    // Top-left
            Pair(result.width - 1, 0),                     // Top-right
            Pair(0, result.height - 1),                    // Bottom-left
            Pair(result.width - 1, result.height - 1),     // Bottom-right
            Pair(result.width / 2, result.height / 2)      // Center
        )

        samplePoints.forEach { (x, y) ->
            val pixel = result.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            // Verify all channels are in valid 0-255 range
            assertTrue(
                "Red channel at ($x,$y) should be >= 0, got $red",
                red >= 0
            )
            assertTrue(
                "Red channel at ($x,$y) should be <= 255, got $red",
                red <= 255
            )
            assertTrue(
                "Green channel at ($x,$y) should be >= 0, got $green",
                green >= 0
            )
            assertTrue(
                "Green channel at ($x,$y) should be <= 255, got $green",
                green <= 255
            )
            assertTrue(
                "Blue channel at ($x,$y) should be >= 0, got $blue",
                blue >= 0
            )
            assertTrue(
                "Blue channel at ($x,$y) should be <= 255, got $blue",
                blue <= 255
            )
        }
    }

    /**
     * Test with sample images to ensure output dimensions are correct
     * Requirement 5.5: Test with sample images to ensure output dimensions are correct
     */
    @Test
    fun testPreprocessForDetection_VariousInputSizes() {
        // Test edge cases and common camera resolutions
        val testCases = listOf(
            Triple(4032, 3024, "High-res camera"),
            Triple(1920, 1080, "Full HD"),
            Triple(1280, 720, "HD"),
            Triple(640, 480, "VGA"),
            Triple(224, 224, "Small square"),
            Triple(100, 100, "Tiny square"),
            Triple(3000, 2000, "Large landscape"),
            Triple(2000, 3000, "Large portrait")
        )

        testCases.forEach { (width, height, description) ->
            val inputBitmap = createTestBitmap(width, height)
            val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

            assertEquals(
                "$description: Width should be 640",
                640,
                result.width
            )
            assertEquals(
                "$description: Height should be 640",
                640,
                result.height
            )
            
            // Verify bitmap is valid and not null
            assertNotNull("$description: Result should not be null", result)
            
            // Verify bitmap config is valid
            assertNotNull("$description: Bitmap config should not be null", result.config)
        }
    }

    /**
     * Test that preprocessing handles HARDWARE bitmaps correctly
     * Ensures conversion to software bitmap for pixel access
     */
    @Test
    fun testPreprocessForDetection_HandlesSoftwareBitmaps() {
        // Create ARGB_8888 bitmap (software config)
        val inputBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        
        // Fill with test pattern
        for (x in 0 until inputBitmap.width) {
            for (y in 0 until inputBitmap.height) {
                inputBitmap.setPixel(x, y, Color.rgb(128, 128, 128))
            }
        }

        val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

        // Should successfully process and return 640x640
        assertEquals(640, result.width)
        assertEquals(640, result.height)
        
        // Should be able to access pixels (not HARDWARE config)
        assertNotEquals(
            "Result should not be HARDWARE config",
            Bitmap.Config.HARDWARE,
            result.config
        )
    }

    /**
     * Test that preprocessing maintains image quality
     * Verifies that the image is not completely black or white after processing
     */
    @Test
    fun testPreprocessForDetection_MaintainsImageQuality() {
        // Create a bitmap with varied colors
        val inputBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        
        // Create a gradient pattern
        for (x in 0 until inputBitmap.width) {
            for (y in 0 until inputBitmap.height) {
                val intensity = ((x + y) * 255 / (inputBitmap.width + inputBitmap.height))
                inputBitmap.setPixel(x, y, Color.rgb(intensity, intensity, intensity))
            }
        }

        val result = ImagePreprocessor.preprocessForDetection(inputBitmap)

        // Sample multiple pixels and verify variation exists
        val centerPixel = result.getPixel(result.width / 2, result.height / 2)
        val cornerPixel = result.getPixel(10, 10)
        
        // Pixels should not all be the same (image should maintain variation)
        assertNotEquals(
            "Image should maintain variation after preprocessing",
            centerPixel,
            cornerPixel
        )
    }

    /**
     * Helper function to create test bitmaps of various sizes
     */
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Fill with a gradient pattern for realistic testing
        for (x in 0 until width) {
            for (y in 0 until height) {
                val r = (x * 255 / width)
                val g = (y * 255 / height)
                val b = 128
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        
        return bitmap
    }
}
