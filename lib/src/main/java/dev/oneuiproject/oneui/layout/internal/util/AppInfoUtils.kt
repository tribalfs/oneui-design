@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun Context.getApplicationName(): CharSequence?{
    return try {
        applicationInfo.loadLabel(packageManager)
    } catch (_: Exception){
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun Context.getAppVersion(): CharSequence?{
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName
    } catch (_: Exception){
        null
    }
}