@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.util.Log
import android.view.SemWindowManager
import android.view.Window
import androidx.reflect.DeviceInfo
import dev.rikka.tools.refine.Refine

inline fun Window.semAddExtensionFlags(flags: Int) {
    if (DeviceInfo.isOneUI()) {
        try {
            val lp = attributes
            Refine.unsafeCast<SemWindowManager.LayoutParams>(lp)
                .semAddExtensionFlags(flags)
            attributes = lp
        } catch (e: Throwable) {
            Log.e(this::class.simpleName, "semAddExtensionFlags, error: ${e.message}")
        }
    }
}

inline fun Window.semClearExtensionFlags(flags: Int) {
    if (DeviceInfo.isOneUI()) {
        try {
            val lp = attributes
            Refine.unsafeCast<SemWindowManager.LayoutParams>(lp)
                .semClearExtensionFlags(flags)
            attributes = lp
        } catch (e: Throwable) {
            Log.e(this::class.simpleName, "semAddExtensionFlags, error: ${e.message}")
        }
    }
}
