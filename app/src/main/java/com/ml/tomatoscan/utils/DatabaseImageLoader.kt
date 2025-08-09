package com.ml.tomatoscan.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import android.content.res.Resources
import java.io.File

/**
 * Custom Coil Fetcher for loading images stored as byte arrays in Room database
 */
class DatabaseImageFetcher(
    private val data: String,
    private val options: Options,
    private val context: Context,
    private val historyRepository: com.ml.tomatoscan.data.HistoryRepository
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return try {
            when {
                data.startsWith("file://") -> {
                    // Handle file URI
                    val filePath = data.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(filePath)
                        if (bitmap != null) {
                            DrawableResult(
                                drawable = bitmap.toDrawable(context.resources),
                                isSampled = false,
                                dataSource = DataSource.DISK
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                data.startsWith("file:///android_asset/") -> {
                    val assetPath = data.removePrefix("file:///android_asset/")
                    try {
                        val inputStream = context.assets.open(assetPath)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) {
                            DrawableResult(
                                drawable = bitmap.toDrawable(context.resources),
                                isSampled = false,
                                dataSource = DataSource.DISK
                            )
                        } else {
                            Log.e("DatabaseImageFetcher", "Failed to decode bitmap from asset: $assetPath")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("DatabaseImageFetcher", "Error opening asset: $assetPath", e)
                        null
                    }
                }
                data.startsWith("internal_storage_image_") -> {
                    val timestamp = data.removePrefix("internal_storage_image_").toLongOrNull()
                    if (timestamp != null) {
                        val scanResult = historyRepository.getScanResultByTimestamp(timestamp)
                        val bitmap = scanResult?.imageBitmap
                        if (bitmap != null) {
                            DrawableResult(
                                drawable = bitmap.toDrawable(context.resources),
                                isSampled = false,
                                dataSource = DataSource.DISK
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("DatabaseImageFetcher", "Failed to fetch image", e)
            null
        }
    }

    class Factory(private val context: Context, private val historyRepository: com.ml.tomatoscan.data.HistoryRepository) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.startsWith("file://") || data.startsWith("internal_storage_image_")) {
                DatabaseImageFetcher(data, options, context, historyRepository)
            } else {
                null
            }
        }
    }
}

private fun Bitmap.toDrawable(resources: android.content.res.Resources): android.graphics.drawable.BitmapDrawable {
    return android.graphics.drawable.BitmapDrawable(resources, this)
}
