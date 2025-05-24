package dev.oneuiproject.oneuiexample.ui.main.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shorthand for
 * ```
 * viewLifecycleOwner.lifecycleScope.launch {
 *    viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState){
 *
 *    }
 * }
 * ```
 */
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}
