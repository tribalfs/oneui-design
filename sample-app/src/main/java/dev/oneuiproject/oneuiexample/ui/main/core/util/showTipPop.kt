package dev.oneuiproject.oneuiexample.ui.main.core.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneui.widget.TipPopup.Direction
import dev.oneuiproject.oneui.widget.TipPopup.Mode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

inline fun Fragment.showTipPopup(
    message: String,
    expanded: Boolean = false,
    mode: Mode = Mode.TRANSLUCENT,
    direction: Direction = Direction.DEFAULT,
    /**
     * Auto dismiss when Fragment's lifecycle is onPause or below
     */
    dismissOnPaused: Boolean = true,
    crossinline getAnchor: () -> View?,
    delayMillis: Long = 0,
    crossinline onCreate: TipPopup.() -> Unit = {}
) {
    lifecycleScope.launch {
        delay(delayMillis)
        if (!isActive || !this@showTipPopup.isResumed) return@launch
        val anchor = getAnchor() ?: return@launch
        TipPopup(anchor, mode).apply {
            setMessage(message)
            onCreate()
            setExpanded(expanded)
            if (dismissOnPaused) {
                viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onPause(owner: LifecycleOwner) {
                        this@apply.dismiss(true)
                        owner.lifecycle.removeObserver(this)
                    }
                })
            }
            show(direction)
        }
    }
}