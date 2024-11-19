package dev.oneuiproject.oneui.ktx

import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

