@file:Suppress("MemberVisibilityCanBePrivate","unused")
package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.graphics.Insets
import androidx.recyclerview.widget.RecyclerView
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate

/**
 * [RecyclerView]  which rounded corners can be set.
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 */
open class RoundedRecyclerView @JvmOverloads constructor(
    context: Context, attrs:
    AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

    override fun onFinishInflate() {
        seslSetFillHorizontalPaddingEnabled(fillHorizontalPadding)
        if (fillHorizontalPadding){
            edgeInsets = Insets.NONE
        }
        super.onFinishInflate()
    }

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