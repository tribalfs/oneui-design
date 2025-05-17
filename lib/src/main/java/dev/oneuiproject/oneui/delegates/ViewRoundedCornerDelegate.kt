@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.delegates

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.content.res.use
import androidx.core.graphics.Insets
import dev.oneuiproject.oneui.design.R

class ViewRoundedCornerDelegate(
    private val context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
): ViewRoundedCorner {

    private var mRoundedCorner: SeslRoundedCorner = SeslRoundedCorner(context)

    override var edgeInsets = Insets.NONE

    override var drawOverEdge = true

    override var fillHorizontalPadding: Boolean = false

    override var roundedCorners: Int
        get() = mRoundedCorner.roundedCorners
        set(value) {
            if (mRoundedCorner.roundedCorners == value) return
            mRoundedCorner.roundedCorners = value
        }

    @ColorInt
    override var roundedCornersColor: Int = -1
        set(value) {
            if (field == value) return
            field = value
            if (roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE) {
                mRoundedCorner.setRoundedCornerColor(roundedCorners, value)
            }
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.RoundedCornerView, defStyleAttr, defStyleRes).use {
            val topCornerInset = it.getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetTop, 0)
            val rightCornerInset = it.getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetRight, 0)
            val bottomCornerInset = it.getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetBottom, 0)
            val leftCornerInset = it.getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetLeft, 0)
            drawOverEdge = it.getBoolean(R.styleable.RoundedCornerView_drawOverEdge, true)
            edgeInsets = Insets.of(leftCornerInset, topCornerInset, rightCornerInset, bottomCornerInset)
            roundedCorners = it.getInt(R.styleable.RoundedCornerView_roundedCorners, SeslRoundedCorner.ROUNDED_CORNER_ALL)
            roundedCornersColor = it.getColor(R.styleable.RoundedCornerView_roundedCornerColor, -1)
            fillHorizontalPadding = it.getBoolean(R.styleable.RoundedCornerView_fillHorizontalPadding, false)
        }
    }

    override fun drawRoundedCorners(canvas: Canvas) {
        if (edgeInsets != Insets.NONE || mRoundedCorner.roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE) {
            mRoundedCorner.drawRoundedCorner(canvas, edgeInsets)
        }
    }

}




interface ViewRoundedCorner{
    /**
     * Set either [SeslRoundedCorner.ROUNDED_CORNER_NONE], [SeslRoundedCorner.ROUNDED_CORNER_ALL],
     * [SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT], [SeslRoundedCorner.ROUNDED_CORNER_TOP_RIGHT],
     * [SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_LEFT] or [SeslRoundedCorner.ROUNDED_CORNER_BOTTOM_RIGHT]
     */
    var roundedCorners: Int
    @get:ColorInt
    var roundedCornersColor: Int

    /**
     * The inset values to be applied to the borders and rounded corners of this view, without changing the paddings.
     * The inset borders will be filled with the rounded corner color.
     * @see [fillHorizontalPadding]
     */
    var edgeInsets: Insets

    /**
     * When set to true, the border and rounded corners will be drawn over the view.
     * Defaults to true.
     */
    var drawOverEdge: Boolean

    /**
     * When set to true and the view has horizontal padding, the border and rounded corners
     * will be inset horizontally by the amount of the horizontal paddings.
     * This overrides the left and right values of the [edgeInsets].
     */
    var fillHorizontalPadding: Boolean
    /**
     * This should be called inside the View's dispatchDraw method
     */
    fun drawRoundedCorners(canvas: Canvas)
}
