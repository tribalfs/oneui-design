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
 * [RecyclerView] which rounded corners and edge insets can be set.
 *
 * ## Example usage:
 *```xml
 * <dev.oneuiproject.oneui.widget.RoundedRecyclerView
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         app:roundedCorners="all"
 *         android:paddingHorizontal="10dp"
 *         app:fillHorizontalPadding="true"
 *         android:scrollbarStyle="outsideOverlay"/>
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
open class RoundedRecyclerView @JvmOverloads constructor(
    context: Context, attrs:
    AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr),
    ViewRoundedCorner by ViewRoundedCornerDelegate(context, attrs, defStyleAttr, 0) {

    override fun onFinishInflate() {
        seslSetFillHorizontalPaddingEnabled(fillHorizontalPadding)
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