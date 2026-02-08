@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.core.graphics.ColorUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

@Px
inline fun Int.dpToPx(densityScale: Float): Int = (this * densityScale).toInt()

@Px
inline fun Float.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).toInt()

@Px
inline fun Int.dpToPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density).toInt()

@Px
inline fun Int.dpToPx(resources: Resources): Int = toFloat().dpToPx(resources)

@Dimension(unit = Dimension.DP)
inline fun Int.pxToDp(resources: Resources): Float = (this / resources.displayMetrics.density)

@OptIn(ExperimentalContracts::class)
inline fun Int.ifNegativeOrZero(block: () -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this <= 0) block() else this
}

@OptIn(ExperimentalContracts::class)
inline fun Int.ifNegative(block: () -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (this < 0) block() else this
}

/**
 * Returns a copy of this ARGB color with the given alpha in [0f, 1f].
 *
 * @param alpha fractional alpha where 0f is fully transparent and
 * 1f is fully opaque. Values outside the range are clamped.
 *
 * @return a new color int with the updated alpha channel.
 */
@ColorInt
inline fun Int.withAlpha(@FloatRange(from = 0.00, 1.0) alpha: Float): Int =
    withAlpha((alpha * 255).roundToInt ())

/**
 * Returns a copy of this ARGB color with the given alpha component in [0, 255].
 *
 * @param alpha alpha channel value (0 = fully transparent, 255 = fully opaque).
 * Values outside the range are clamped.
 * @return a new color int with the updated alpha channel.
 */
@ColorInt
inline fun Int.withAlpha(@IntRange(from = 0, 255) alpha: Int): Int =
    ColorUtils.setAlphaComponent(this,alpha)