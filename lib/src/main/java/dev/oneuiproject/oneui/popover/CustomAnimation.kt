package dev.oneuiproject.oneui.popover

import androidx.annotation.AnimRes

data class CustomAnimation(
    @JvmField
    @AnimRes val enterResId: Int,
    @JvmField
    @AnimRes val exitResId: Int
)