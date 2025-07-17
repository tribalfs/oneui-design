@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.Dimension
import androidx.annotation.Px
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
