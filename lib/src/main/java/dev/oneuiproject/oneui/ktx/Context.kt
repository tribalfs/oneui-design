@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import androidx.appcompat.util.SeslMisc

inline val Context.dpToPxFactor get() = resources.displayMetrics.density

inline fun Context.getThemeAttributeValue(attr: Int): TypedValue? =
    TypedValue().run {
        if (theme.resolveAttribute(attr, this, true)) { this } else null
    }

@SuppressLint("RestrictedApi")
inline fun Context.isLightMode(): Boolean = SeslMisc.isLightTheme(this)
