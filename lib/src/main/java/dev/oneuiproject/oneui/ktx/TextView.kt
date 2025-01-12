@file:Suppress("unused", "NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.util.Log
import android.widget.SemTextView
import android.widget.TextView
import androidx.reflect.DeviceInfo
import dev.rikka.tools.refine.Refine

/**
 * Enables or disables button background on this [TextView] on OneUI systems
 * and optionally sets the text color to [textColor] if provided.
 *
 * @param enabled
 * @param textColor Optional text color to set.
 */
@JvmOverloads
inline fun TextView.semSetButtonShapeEnabled(enabled: Boolean, textColor: Int = -1) {
    if (DeviceInfo.isOneUI()) {
        try {
            if (textColor != -1) {
                Refine.unsafeCast<SemTextView>(this).semSetButtonShapeEnabled(enabled, textColor)
            }else{
                Refine.unsafeCast<SemTextView>(this).semSetButtonShapeEnabled(enabled)
            }
        } catch (e: Exception){
            Log.e(this::class.simpleName, "semAddExtensionFlags, error: ${e.message}")
        }
    }

}
