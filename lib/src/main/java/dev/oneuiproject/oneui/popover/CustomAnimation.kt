package dev.oneuiproject.oneui.popover

import androidx.annotation.AnimRes

/**
 * A data class that represents a custom animation for a popover.
 *
 * @property enterResId The resource ID of the enter animation.
 * @property exitResId The resource ID of the exit animation.
 * @see dev.oneuiproject.oneui.ktx.startPopOverActivityForResult
 * @see dev.oneuiproject.oneui.ktx.startPopOverActivity
 */
data class CustomAnimation(
    @JvmField @field:AnimRes val enterResId: Int,
    @JvmField @field:AnimRes val exitResId: Int
)