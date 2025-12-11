package com.example.chalkak.domain.detection

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetectionResultItem(
    val label: String,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) : Parcelable
