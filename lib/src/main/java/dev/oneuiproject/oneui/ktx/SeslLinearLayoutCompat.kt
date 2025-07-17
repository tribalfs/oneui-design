@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SeslLinearLayoutCompat

@RequiresApi(Build.VERSION_CODES.Q)
inline fun SeslLinearLayoutCompat.setRoundedCornerColor(@ColorInt color: Int) =
    roundedCorner.let { it.setRoundedCornerColor(it.roundedCorners, color) }


/**
 * Sets the corners to be rounded.
 *
 * This is only available on devices running Android Q or later.
 *
 * @param corners The corners to be rounded. This should be a bitmask of the following flags:
 * - `SeslRoundedCorner.ROUNDED_CORNER_NONE`
 * - `SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT`
 * - `SeslRoundedCorner.ROUNDED_CORNER_TOP_RIGHT`
 * - `SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_LEFT`
 * - `SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_RIGHT`
 * - `SeslRoundedCorner.ROUNDED_CORNER_ALL`
 */
@RequiresApi(Build.VERSION_CODES.Q)
inline fun SeslLinearLayoutCompat.setRoundedCorner(corners: Int) {
    roundedCorner.roundedCorners = corners
}