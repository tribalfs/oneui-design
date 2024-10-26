@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.Dimension
import androidx.annotation.Px

@Px
inline fun Float.dpToPx(densityScale: Float): Int = (this * densityScale).toInt()

@Px
inline fun Int.dpToPx(densityScale: Float): Int = (this * densityScale).toInt()

@Px
inline fun Float.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).toInt()

@Px
inline fun Int.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).toInt()

@Px
inline fun Float.dpToPx(resources: Resources): Int = dpToPx(resources.displayMetrics.density)

@Px
inline fun Int.dpToPx(resources: Resources): Int = toFloat().dpToPx(resources)

@Dimension(unit = Dimension.DP)
inline fun Int.pxToDp(resources: Resources): Float = (this / resources.displayMetrics.density)

inline fun Float.pxToDp(resources: Resources): Float = (this / resources.displayMetrics.density)