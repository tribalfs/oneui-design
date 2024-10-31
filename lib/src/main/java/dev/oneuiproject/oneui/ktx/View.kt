package dev.oneuiproject.oneui.ktx

import android.view.View

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