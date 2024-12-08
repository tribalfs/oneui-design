@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.util.SeslMisc
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.getWidthExcludingSystemInsets

inline val Context.dpToPxFactor get() = resources.displayMetrics.density

inline fun Context.getThemeAttributeValue(attr: Int): TypedValue? =
    TypedValue().run {
        if (theme.resolveAttribute(attr, this, true)) { this } else null
    }

@SuppressLint("RestrictedApi")
inline fun Context.isLightMode(): Boolean = SeslMisc.isLightTheme(this)

val Context.activity: Activity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

val Context.appCompatActivity: AppCompatActivity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

inline val Context.widthExcludingSystemInsets: Int get() = getWidthExcludingSystemInsets(this)