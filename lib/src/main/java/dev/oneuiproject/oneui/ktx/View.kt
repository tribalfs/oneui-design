@file:Suppress("unused", "NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.reflect.DeviceInfo
import dev.oneuiproject.oneui.utils.internal.ReflectUtils.genericInvokeMethod


/**
 * Convenience method to register [View.addOnAttachStateChangeListener]
 *
 * @param onChanged
 */
inline fun View.doOnAttachedStateChanged(
    crossinline onChanged: (view: View, isAttached: Boolean) -> Unit
) {
    addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                onChanged(view,true)
            }

            override fun onViewDetachedFromWindow(view: View) {
                onChanged(view,false)
            }
        }
    )
    onChanged(this, isAttachedToWindow)
}

inline val View.isSoftKeyboardShowing
    get() = ViewCompat.getRootWindowInsets(this)?.isVisible(
        WindowInsetsCompat.Type.ime()
    ) == true


/**
 * Sets a click listener that doesn't allow multiple invocations within a given interval.
 * @param interval The interval in milliseconds.
 * @param action The lambda function to perform.
 */
@JvmOverloads
inline fun <T: View>T.onSingleClick(interval: Long = 600, crossinline action: (T) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            action(this)
        }
    }
}

/**
 * Sets OneUI style tooltip text to this View.
 */
inline fun View.semSetToolTipText(toolTipText: CharSequence?) {
    TooltipCompat.setTooltipText(this, toolTipText)
}

/**
 * This method only works on OneUI with api 28 and above
 */
@RequiresApi(Build.VERSION_CODES.P)
inline fun View.semGetRoundedCorners(): Int {
    if (DeviceInfo.isOneUI()) {
        return genericInvokeMethod(
            View::class.java,
            this, "semGetRoundedCorners"
        ) as Int
    } else {
        Log.w("View-ktx", "semGetRoundedCorners method is available only on OneUI with " +
                "api 28 and above"
        )
    }
    return ROUNDED_CORNER_NONE
}

/**
 * This method only works on OneUI with api 28 and above
 */
inline fun View.semSetRoundedCorners( corners: Int) {
    if (DeviceInfo.isOneUI() && Build.VERSION.SDK_INT >= 28) {
        genericInvokeMethod(
            View::class.java,
            this, "semSetRoundedCorners", corners
        )
    } else {
        Log.w("View-ktx", "semSetRoundedCorners method is available only on OneUI with " +
                "api 28 and above"
        )
    }

}

/**
 * This method only works on OneUI with api 28 and above
 */
inline fun View.semGetRoundedCornerColor(corner: Int): Int {
    if (DeviceInfo.isOneUI() && Build.VERSION.SDK_INT >= 28) {
        return genericInvokeMethod(
            View::class.java,
            this, "semGetRoundedCornerColor", corner
        ) as Int
    } else {
        Log.w("View-ktx", "semGetRoundedCornerColor method is available only on OneUI with " +
                "api 28 and above"
        )
    }
    return Color.WHITE
}

/**
 * This method only works on OneUI with api 28 and above
 */
inline fun View.semSetRoundedCornerColor(
    corners: Int, @ColorInt color: Int
) {
    if (DeviceInfo.isOneUI() && Build.VERSION.SDK_INT >= 28) {
        genericInvokeMethod(
            View::class.java,
            this, "semSetRoundedCornerColor", corners, color
        )
    } else {
        Log.w("View-ktx", "semSetRoundedCornerColor method is available only on OneUI with " +
                "api 28 and above"
        )
    }
}

