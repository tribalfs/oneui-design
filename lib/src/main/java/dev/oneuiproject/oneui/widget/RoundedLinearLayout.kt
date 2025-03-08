@file:Suppress("unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner

/**
 * [LinearLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 */
open class RoundedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, defStyleRes) {

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