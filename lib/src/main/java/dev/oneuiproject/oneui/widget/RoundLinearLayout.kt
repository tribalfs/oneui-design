@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * [LinearLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 */
@Deprecated("Use RoundedLinearLayout instead")
open class RoundLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RoundedLinearLayout(context, attrs, defStyleAttr, defStyleRes)