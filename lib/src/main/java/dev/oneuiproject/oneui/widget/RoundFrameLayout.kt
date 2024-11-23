@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * [FrameLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 */
@Deprecated("Use RoundedFrameLayout instead")
open class RoundFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RoundedFrameLayout(context, attrs, defStyleAttr, defStyleRes)