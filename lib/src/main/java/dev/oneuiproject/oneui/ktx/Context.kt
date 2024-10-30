package dev.oneuiproject.oneui.ktx

import android.content.Context

inline val Context.dpToPxFactor get() = resources.displayMetrics.density