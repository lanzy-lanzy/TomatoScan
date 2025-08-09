package com.ml.tomatoscan.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LeafOverlayBox(
    val label: String,
    val x1: Int,
    val y1: Int,
    val y2: Int,
    val x2: Int
) : Parcelable

