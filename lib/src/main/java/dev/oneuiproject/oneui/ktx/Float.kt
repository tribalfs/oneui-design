@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.content.res.Resources
import androidx.annotation.Dimension
import androidx.annotation.Px

@Px
inline fun Float.dpToPx(densityScale: Float): Int = (this * densityScale).toInt()

@Px
inline fun Float.dpToPx(resources: Resources): Int = dpToPx(resources.displayMetrics.density)

@Dimension(unit = Dimension.DP)
inline fun Float.pxToDp(resources: Resources): Float = toInt().pxToDp (resources)