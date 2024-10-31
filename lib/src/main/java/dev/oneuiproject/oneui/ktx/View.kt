package dev.oneuiproject.oneui.ktx

import android.view.View
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