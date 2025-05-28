@file:Suppress("unused")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.graphics.Insets
import androidx.core.widget.NestedScrollView
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner

/**
 * [NestedScrollView] which rounded corners and edge insets can be set.
 *
 * ## Example usage:
 *```xml
 * <dev.oneuiproject.oneui.widget.RoundedNestedScrollView
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         app:roundedCorners="all"
 *         android:paddingHorizontal="10dp"
 *         app:fillHorizontalPadding="true">
 *
 *        <!-- child view -->

 * </dev.oneuiproject.oneui.widget.RoundedNestedScrollView>
 * ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 *
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 * @see drawOverEdge
 * @see fillHorizontalPadding
 */
open class RoundedNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.core.R.attr.nestedScrollViewStyle
) : NestedScrollView(context, attrs, defStyleAttr),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

    override fun onFinishInflate() {
        seslSetFillHorizontalPaddingEnabled(fillHorizontalPadding,  roundedCornersColor)
        super.onFinishInflate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (fillHorizontalPadding) {
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