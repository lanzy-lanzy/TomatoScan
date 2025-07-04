package com.ml.tomatoscan.ui.screens

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedVectorIcon(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val imageView = android.widget.ImageView(ctx)
            val drawable = ContextCompat.getDrawable(ctx, resId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is AnimatedVectorDrawable) {
                imageView.setImageDrawable(drawable)
                drawable.start()
            } else {
                imageView.setImageDrawable(drawable)
            }
            imageView
        },
        update = {
            val drawable = it.drawable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
        }
    )
}
