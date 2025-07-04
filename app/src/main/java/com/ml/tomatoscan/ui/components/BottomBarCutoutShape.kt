package com.ml.tomatoscan.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

class BottomBarCutoutShape(
    private val cutoutRadius: Dp,
    private val topCornerRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
            val topCornerRadiusPx = with(density) { topCornerRadius.toPx() }

            val cutoutCenterX = size.width / 2

            // Start from the top-left corner
            moveTo(0f, topCornerRadiusPx)

            // Top-left corner arc
            arcTo(
                rect = Rect(left = 0f, top = 0f, right = 2 * topCornerRadiusPx, bottom = 2 * topCornerRadiusPx),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Line to the start of the cutout
            lineTo(cutoutCenterX - cutoutRadiusPx, 0f)

            // The cutout arc for the FAB
            arcTo(
                rect = Rect(
                    left = cutoutCenterX - cutoutRadiusPx,
                    top = -cutoutRadiusPx,
                    right = cutoutCenterX + cutoutRadiusPx,
                    bottom = cutoutRadiusPx
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f, // Corrected sweep angle for an upward curve
                forceMoveTo = false
            )

            // Line from the end of the cutout to the top-right corner
            lineTo(size.width - topCornerRadiusPx, 0f)

            // Top-right corner arc
            arcTo(
                rect = Rect(
                    left = size.width - 2 * topCornerRadiusPx,
                    top = 0f,
                    right = size.width,
                    bottom = 2 * topCornerRadiusPx
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Complete the shape
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
