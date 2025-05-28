package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.TextView
import androidx.appcompat.R as appcompatR

/**
 * This widget is a TextView that can be used to visually separate sections of content.
 * This is typically used to separate groups of rounded-corner views in a list.
 *
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr An attribute in the current theme that contains a
 *        reference to a style resource that supplies default values for
 *        the view. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that
 *        supplies default values for the view, used only if
 *        defStyleAttr is 0 or can not be found in the theme. Can be 0
 *        to not look for defaults.
 */
class Separator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listSeparatorTextViewStyle,
    defStyleRes: Int = appcompatR.style.Widget_AppCompat_Light_TextView_ListSeparator
) : TextView(context, attrs, defStyleAttr, defStyleRes) {

    private val minHeight =
        resources.getDimensionPixelSize(appcompatR.dimen.sesl_list_subheader_min_height)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            if (!text.isNullOrEmpty()) {
                heightMeasureSpec
            } else
                makeMeasureSpec(minHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun canScrollVertically(direction: Int) = false
    override fun canScrollHorizontally(direction: Int): Boolean = false
}
