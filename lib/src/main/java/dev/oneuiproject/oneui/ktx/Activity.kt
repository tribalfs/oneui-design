@file:Suppress("NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.ktx

import android.app.Activity
import android.os.Build.VERSION

inline val Activity.isInMultiWindowModeCompat get() = VERSION.SDK_INT >= 24 && isInMultiWindowMode
