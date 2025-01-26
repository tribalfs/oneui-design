@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Build.VERSION
import android.view.inputmethod.InputMethodManager

inline val Activity.isInMultiWindowModeCompat get() = VERSION.SDK_INT >= 24 && isInMultiWindowMode

/**Hide the soft input if showing and optionally clear focus on the text input view.*/
@JvmOverloads
inline fun Activity.hideSoftInput(clearFocus: Boolean = true) {
    with (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager) {
        if (isAcceptingText) {
            if (clearFocus) currentFocus?.clearFocus()
            hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }
}

inline val Activity.fitsSystemWindows get() = window.decorView.fitsSystemWindows
