package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.graphics.Insets
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate

/**
 * [RelativeLayout]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 */
open class RoundedRelativeLayout @JvmOverloads constructor(
    context: Context, attrs:
    AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

    override fun dispatchDraw(canvas: Canvas) {
        if(fillHorizontalPadding){
            if (paddingStart > 0 || paddingEnd > 0) {
                edgeInsets = Insets.of(paddingStart, edgeInsets.top, paddingEnd, edgeInsets.bottom)
            }
        }
        if (drawOverEdge) {
            super.dispatchDraw(canvas)
            drawRoundedCorners(canvas)
        }else{
            drawRoundedCorners(canvas)
            super.dispatchDraw(canvas)
        }
    }
}