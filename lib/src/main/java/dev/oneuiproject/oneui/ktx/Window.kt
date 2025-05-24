@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.util.Log
import android.view.SemWindowManager
import android.view.Window
import androidx.reflect.DeviceInfo
import dev.rikka.tools.refine.Refine

/**
 * Adds the specified Samsung extension flags to this [Window]'s layout parameters.
 * This function only has an effect on devices running OneUI.
 *
 * @param flags The extension flags to add.
 *
 * @see semClearExtensionFlags
 */
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

/**
 * Clears the specified Samsung extension flags from this [Window]'s layout parameters.
 * This function only has an effect on devices running OneUI.
 *
 * @param flags The extension flags to clear.
 *
 * @see semAddExtensionFlags
 */
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
