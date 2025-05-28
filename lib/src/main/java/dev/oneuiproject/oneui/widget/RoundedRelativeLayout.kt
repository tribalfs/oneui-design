package dev.oneuiproject.oneui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.graphics.Insets
import dev.oneuiproject.oneui.delegates.ViewRoundedCorner
import dev.oneuiproject.oneui.delegates.ViewRoundedCornerDelegate

/**
 * [RelativeLayout] which rounded corners and edge insets can be set.
 *
 * ## Example usage:
 *```xml
 * <dev.oneuiproject.oneui.widget.RoundedRelativeLayout
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         app:roundedCorners="all">
 *
 *        <!-- child views -->

 * </dev.oneuiproject.oneui.widget.RoundedRelativeLayout>
 * ```
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 * @param defStyleRes (Optional) A resource identifier of a style resource that
 * supplies default values for the view, used only if defStyleAttr is not provided
 * or cannot be found in the theme.
 *
 * @see roundedCorners
 * @see roundedCornersColor
 * @see edgeInsets
 * @see drawOverEdge
 * @see fillHorizontalPadding
 */
open class RoundedRelativeLayout @JvmOverloads constructor(
    context: Context, attrs:
    AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

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