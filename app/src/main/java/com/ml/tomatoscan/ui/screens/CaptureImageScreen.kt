package com.ml.tomatoscan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.ml.tomatoscan.viewmodels.TomatoScanViewModel
import java.io.File

@Composable
fun CaptureImageScreen(
    navController: NavController,
    viewModel: TomatoScanViewModel
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { uri ->
                try {
                    bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        // Force software bitmap to avoid HARDWARE config issues
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CaptureImageScreen", "Error loading bitmap", e)
                    // Try alternative method if ImageDecoder fails
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } catch (e2: Exception) {
                        android.util.Log.e("CaptureImageScreen", "Fallback bitmap loading failed", e2)
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "temp_image.jpg")
            val newUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            imageUri = newUri
            cameraLauncher.launch(newUri)
        } else {
            // Handle permission denial
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap == null) {
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val file = File(context.cacheDir, "temp_image.jpg")
                    val newUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    imageUri = newUri
                    cameraLauncher.launch(newUri)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Take Picture")
            }
        }

        bitmap?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(200.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                imageUri?.let { uri ->
                    viewModel.analyzeImage(bmp, uri)
                    navController.navigate("dashboard")
                }
            }) {
                Text("Analyze")
            }
        }
    }
}
