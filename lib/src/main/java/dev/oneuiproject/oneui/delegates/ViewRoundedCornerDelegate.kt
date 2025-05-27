@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.delegates

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.util.SeslRoundedCorner
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.Insets
import dev.oneuiproject.oneui.design.R

/**
 * Delegate class to implement rounded corners for a view.
 *
 * This class handles the initialization of rounded corner attributes from XML and provides
 * a method to draw the rounded corners on a canvas. It relies on [SeslRoundedCorner]
 * for the actual drawing logic.
 *
 * It implements the [ViewRoundedCorner] interface, allowing views to delegate
 * rounded corner functionality to this class.
 *
 * ## Example usage:
 * ```
 * open class RoundedFrameLayout @JvmOverloads constructor(
 *     context: Context,
 *     attrs: AttributeSet? = null,
 *     defStyleAttr: Int = 0,
 *     defStyleRes: Int = 0
 * ) : FrameLayout(context, attrs, defStyleAttr, defStyleRes),
 *     ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, defStyleRes) {
 *
 *     override fun dispatchDraw(canvas: Canvas) {
 *         if (fillHorizontalPadding) {
 *             if (paddingStart > 0 || paddingEnd > 0) {
 *                 edgeInsets = Insets.of(paddingStart, edgeInsets.top, paddingEnd, edgeInsets.bottom)
 *             }
 *         }
 *         if (drawOverEdge) {
 *             super.dispatchDraw(canvas)
 *             drawRoundedCorners(canvas)
 *         }else{
 *             drawRoundedCorners(canvas)
 *             super.dispatchDraw(canvas)
 *         }
 *     }
 * }
 * ```
 *
 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource
 *                     that supplies default values for the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that supplies default values for the view,
 *                    used only if defStyleAttr is 0 or can not be found in the theme. Can be 0 to not
 *                    look for defaults.
 */
class ViewRoundedCornerDelegate(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
): ViewRoundedCorner {

    private var seslRoundedCorner: SeslRoundedCorner = SeslRoundedCorner(context)

    override var edgeInsets = Insets.NONE

    override var drawOverEdge = true

    override var fillHorizontalPadding: Boolean = false

    override var roundedCorners: Int
        get() = seslRoundedCorner.roundedCorners
        set(value) {
            if (seslRoundedCorner.roundedCorners == value) return
            seslRoundedCorner.roundedCorners = value
        }

    @ColorInt
    override var roundedCornersColor: Int = -1
        set(value) {
            if (field == value) return
            field = value
            if (roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE) {
                seslRoundedCorner.setRoundedCornerColor(roundedCorners, value)
            }
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.RoundedCornerView, defStyleAttr, defStyleRes) {
            val topCornerInset = getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetTop, 0)
            val rightCornerInset = getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetRight, 0)
            val bottomCornerInset = getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetBottom, 0)
            val leftCornerInset = getDimensionPixelSize(R.styleable.RoundedCornerView_edgeInsetLeft, 0)
            drawOverEdge = getBoolean(R.styleable.RoundedCornerView_drawOverEdge, true)
            edgeInsets = Insets.of(leftCornerInset, topCornerInset, rightCornerInset, bottomCornerInset)
            roundedCorners = getInt(R.styleable.RoundedCornerView_roundedCorners, SeslRoundedCorner.ROUNDED_CORNER_ALL)
            roundedCornersColor = getColor(R.styleable.RoundedCornerView_roundedCornerColor, -1)
            fillHorizontalPadding = getBoolean(R.styleable.RoundedCornerView_fillHorizontalPadding, false)
        }
    }

    override fun drawRoundedCorners(canvas: Canvas) {
        if (edgeInsets != Insets.NONE || seslRoundedCorner.roundedCorners != SeslRoundedCorner.ROUNDED_CORNER_NONE) {
            seslRoundedCorner.drawRoundedCorner(canvas, edgeInsets)
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
