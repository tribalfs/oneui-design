@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner

/**
 * [NestedScrollView]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 */
open class RoundedNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.core.R.attr.nestedScrollViewStyle
) : NestedScrollView(context, attrs, defStyleAttr),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

    override fun dispatchDraw(canvas: Canvas) {
        if (drawOverEdge) {
            super.dispatchDraw(canvas)
            drawRoundedCorners(canvas)
        }else{
            drawRoundedCorners(canvas)
            super.dispatchDraw(canvas)
        }
    }
}