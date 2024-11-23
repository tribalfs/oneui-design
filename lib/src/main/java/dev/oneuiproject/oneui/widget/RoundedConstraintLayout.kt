@file:Suppress("unused", "MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate

/**
 * [ConstraintLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 */
open class RoundedConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(
    context, attrs, defStyleAttr, defStyleRes),
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