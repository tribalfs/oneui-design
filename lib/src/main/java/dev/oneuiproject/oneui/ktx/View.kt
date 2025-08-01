@file:Suppress("unused", "NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.os.Build
import android.util.Log
import android.view.SemView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.reflect.DeviceInfo
import dev.rikka.tools.refine.Refine


/**
 * Convenience method to register [View.addOnAttachStateChangeListener]
 *
 * @param onChanged
 * @return [View.OnAttachStateChangeListener] created and registered with this view.
 */
inline fun View.doOnAttachedStateChanged(
    crossinline onChanged: (view: View, isAttached: Boolean) -> Unit
) =
    object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = onChanged(view,true)
        override fun onViewDetachedFromWindow(view: View) = onChanged(view,false)
    }.also { addOnAttachStateChangeListener(it) }

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
 * Retrieves the rounded corners of this View.
 *
 * This method is specifically designed for OneUI devices running API level 28 or higher.
 *
 * @return An integer representing the rounded corners of the View.
 *         Returns [ROUNDED_CORNER_NONE] if the method is not applicable or an error occurs.
 * @see SemView.semGetRoundedCorners
 * @see ROUNDED_CORNER_NONE
 */
inline fun View.semGetRoundedCorners(): Int {
    if (Build.VERSION.SDK_INT >= 28 && DeviceInfo.isOneUI()) {
        try {
            return Refine.unsafeCast<SemView>(this).semGetRoundedCorners()
        } catch (e: Throwable){
            Log.e(this::class.simpleName, "semGetRoundedCorners invocation error: ${e.message}")
        }
    } else {
        Log.w(this::class.simpleName, "semGetRoundedCorners method is available only on OneUI with " +
                "api 28 and above"
        )
    }
    return ROUNDED_CORNER_NONE
}

/**
 * Sets the rounded corners of this View.
 *
 * This method only works on OneUI with API level 28 and above.
 *
 * @param corners A bitmask of the corners to round.
 *                See [androidx.appcompat.util.SeslRoundedCorner] for available constants.
 * @param radius The radius of the rounded corners in pixels. If null, the default radius is used.
 */
@JvmOverloads
inline fun View.semSetRoundedCorners(corners: Int, radius: Int? = null) {
    if (Build.VERSION.SDK_INT >= 28 && DeviceInfo.isOneUI()) {
        try {
            radius?.let {
                Refine.unsafeCast<SemView>(this).semSetRoundedCorners(corners, it)
            } ?: Refine.unsafeCast<SemView>(this).semSetRoundedCorners(corners)
        } catch (e: Throwable){
            Log.e(this::class.simpleName, "semSetRoundedCorners invocation error: ${e.message}")
        }
    } else {
        Log.w(this::class.simpleName, "semSetRoundedCorners method is available only on OneUI with " +
                "api 28 and above"
        )
    }

}

/**
 * Sets the rounded corner color for the specified corners of this View.
 *
 * This method only works on OneUI devices with API level 28 and above.
 * If called on other devices or lower API levels, a warning will be logged.
 *
 * @param corners An integer representing the corners to apply the color to.
 *                Use constants from `androidx.appcompat.util.SeslRoundedCorner`
 *                (e.g., `SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT`).
 * @param color The color to set for the rounded corners, as an integer.
 *              Use `@ColorInt` annotation to ensure a valid color is passed.
 */
inline fun View.semSetRoundedCornerColor(
    corners: Int, @ColorInt color: Int
) {
    if (DeviceInfo.isOneUI() && Build.VERSION.SDK_INT >= 28) {
        try {
            return Refine.unsafeCast<SemView>(this).semSetRoundedCornerColor(corners, color)
        } catch (e: Throwable){
            Log.e(this::class.simpleName, "semSetRoundedCornerColor invocation error: ${e.message}")
        }
    } else {
        Log.w(this::class.simpleName, "semSetRoundedCornerColor method is available only on OneUI with " +
                "api 28 and above"
        )
    }
}


/**
 * Traverses up the view hierarchy to find the first ancestor of the specified type.
 *
 * @param T The type of the ancestor to find.
 * @return The first ancestor of type [T], or null if no such ancestor is found.
 */
inline fun <reified T: ViewGroup> View.findAncestorOfType(): T?{
    var targetParent: T? = null
    var parent = this.parent
    while (parent is ViewGroup) {
        if (parent is T) {
            targetParent = parent
            break
        }
        parent = parent.parent
    }
    return targetParent
}

/**
 * Checks if this View is a descendant of the given [parentView].
 *
 * This function traverses up the view hierarchy from the current view and checks if
 * any of its ancestors match the provided [parentView].
 *
 * @param parentView The [ViewGroup] to check if it's an ancestor of this view.
 * @return `true` if this view is a descendant of [parentView], `false` otherwise.
 */
@JvmName("isViewDescendant")
inline fun View.isDescendantOf(parentView: ViewGroup): Boolean {
    var parent = this.parent
    while (parent is ViewGroup) {
        if (parent == parentView) {
            return true
        }
        parent = parent.parent
    }
    return false
}

/**
 * Sets a click listener that invokes the given action only after the view has been clicked
 * a specific number of times within a specified interval.
 *
 * This is useful for implementing features like "Easter eggs" or developer options that
 * are triggered by multiple rapid clicks.
 *
 * @param clickCount The number of clicks required to trigger the action. Defaults to 7.
 * @param maxClickIntervalMillis The maximum time in milliseconds allowed between consecutive clicks
 * for them to be considered part of the same multi-click sequence. If the time between
 * clicks exceeds this value, the click count resets. Defaults to 1000 milliseconds (1 second).
 * @param action The lambda function to be executed when the required number of clicks is reached
 * within the specified interval.
 */
inline fun View.onMultiClick(clickCount: Int = 7, maxClickIntervalMillis: Long = 1_000, crossinline action: () -> Unit) {
    var currentCount = 0
    val resetCount = Runnable { currentCount = 0 }
    isClickable = true
    setOnClickListener {
        removeCallbacks(resetCount)
        currentCount++
        if (currentCount >= clickCount) {
            currentCount = 0
            action()
            return@setOnClickListener
        }
        postDelayed(resetCount, maxClickIntervalMillis)
    }
}