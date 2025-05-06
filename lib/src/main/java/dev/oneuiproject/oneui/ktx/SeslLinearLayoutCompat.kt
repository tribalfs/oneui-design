@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SeslLinearLayoutCompat

@RequiresApi(Build.VERSION_CODES.Q)
inline fun SeslLinearLayoutCompat.setRoundedCornerColor(@ColorInt color: Int) =
    roundedCorner.let { it.setRoundedCornerColor(it.roundedCorners, color) }


@RequiresApi(Build.VERSION_CODES.Q)
inline fun SeslLinearLayoutCompat.setRoundedCorner(corners: Int) {
    roundedCorner.roundedCorners = corners
}