package dev.oneuiproject.oneui.ktx

import android.app.Activity
import android.os.Build.VERSION

inline val Activity.isInMultiWindowModeCompat: Boolean get() = VERSION.SDK_INT >= 24 && isInMultiWindowMode
