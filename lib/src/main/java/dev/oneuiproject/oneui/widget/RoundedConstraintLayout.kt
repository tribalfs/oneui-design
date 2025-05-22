@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate

/**
 * [ConstraintLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 * @see drawOverEdge
 * @see fillHorizontalPadding
 */
open class RoundedConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(
    context, attrs, defStyleAttr, defStyleRes
), ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, defStyleRes) {

    override fun dispatchDraw(canvas: Canvas) {
        if (fillHorizontalPadding) {
            if (paddingStart > 0 || paddingEnd > 0) {
                edgeInsets = Insets.of(paddingStart, edgeInsets.top, paddingEnd, edgeInsets.bottom)
            }
        }
        if (drawOverEdge) {
            super.dispatchDraw(canvas)
            drawRoundedCorners(canvas)
        } else {
            drawRoundedCorners(canvas)
            super.dispatchDraw(canvas)
        }
    }

}