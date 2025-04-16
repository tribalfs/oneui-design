package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.TextView
import androidx.appcompat.R as appcompatR

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
