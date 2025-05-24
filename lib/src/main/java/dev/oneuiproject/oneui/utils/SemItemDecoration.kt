package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.util.SeslRoundedCorner
import androidx.appcompat.util.SeslSubheaderRoundedCorner
import androidx.core.content.withStyledAttributes
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.utils.ItemDecorRule.ALL
import dev.oneuiproject.oneui.utils.ItemDecorRule.NONE
import dev.oneuiproject.oneui.utils.ItemDecorRule.SELECTED


/**
 * @param context
 * @param dividerRule [ItemDecorRule] to apply to dividers.
 * @param subHeaderRule [ItemDecorRule] to apply to rounded sub-headers.
 */
@SuppressLint("PrivateResource", "ResourceType")
open class SemItemDecoration @JvmOverloads constructor(
    private val context: Context,
    private val dividerRule: ItemDecorRule = ALL,
    private val subHeaderRule: ItemDecorRule = ALL,
) : RecyclerView.ItemDecoration() {

    private var divider: Drawable? = null
    @Px
    private var defaultDividerInset: Int = 0
    @Px
    private var dividerInsetStart: Int = 0
    @Px
    private var dividerInsetEnd: Int = 0
    @Px
    private var dividerHeight: Int? = null

    private var subheaderRoundedCorner: SeslSubheaderRoundedCorner? = null

    init {
        when {
            dividerRule !is NONE && subHeaderRule !is NONE -> {
                subheaderRoundedCorner = SeslSubheaderRoundedCorner(context).apply {
                    roundedCorners = SeslRoundedCorner.ROUNDED_CORNER_ALL
                }

                context.withStyledAttributes(
                    null,
                    attrs = intArrayOf(android.R.attr.listDivider, androidx.appcompat.R.attr.roundedCornerColor)
                ) {
                    divider = getDrawable(0)
                    subheaderRoundedCorner!!.setRoundedCornerColor(SeslRoundedCorner.ROUNDED_CORNER_ALL, getColor(1, 0))
                }
                context.resources.apply {
                    defaultDividerInset =  getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_list_divider_inset)
                    getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_list_item_padding_horizontal).let {
                        dividerInsetStart = it
                        dividerInsetEnd = it
                    }
                }
            }
            subHeaderRule !is NONE -> {
                subheaderRoundedCorner = SeslSubheaderRoundedCorner(context).apply {
                    roundedCorners = SeslRoundedCorner.ROUNDED_CORNER_ALL
                    setRoundedCornerColor(
                        SeslRoundedCorner.ROUNDED_CORNER_ALL,
                        context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)!!.data
                    )
                }
            }

            dividerRule !is NONE -> {
                divider = AppCompatResources.getDrawable(context, context.getThemeAttributeValue(android.R.attr.listDivider)!!.resourceId)!!
                context.resources.apply {
                    defaultDividerInset =  getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_list_divider_inset)
                    getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_list_item_padding_horizontal).let {
                        dividerInsetStart = it
                        dividerInsetEnd = it
                    }
                }
            }
        }
    }

    /**
     * Set custom divider insets for start and end positions.
     *
     * @param dividerInsets Custom insets in pixels.
     */
    fun setDividerInsets(dividerInsets: DividerInsets){
        this.dividerInsetStart = dividerInsets.start
        this.dividerInsetEnd = dividerInsets.end
    }

    /**
     * Set custom start inset for divider.
     *
     * @param dividerInsetStart Custom start inset in pixels.
     */
    fun setDividerInsetStart(@Px dividerInsetStart: Int){
        this.dividerInsetStart = dividerInsetStart
    }

    /**
     * Set custom end inset for divider.
     *
     * @param dividerInsetEnd Custom end inset in pixels.
     */
    fun setDividerInsetEnd(@Px dividerInsetEnd: Int){
        this.dividerInsetEnd = dividerInsetEnd
    }

    /**
     * Set custom divider height.
     *
     * @param dividerHeight Custom divider height in pixels.
     */
    fun setDividerHeight(@Px dividerHeight: Int){
        this.dividerHeight = dividerHeight
    }

    override fun onDraw(
        canvas: Canvas, recyclerView: RecyclerView,
        state: RecyclerView.State
    ) {
        if (divider == null) return

        val dividerHeight = dividerHeight ?:divider!!.intrinsicHeight
        val rvPaddingStart = recyclerView.paddingStart
        val rvPaddingEnd = recyclerView.paddingEnd
        val rvWidth = recyclerView.width

        var child: View
        val end: Int
        val start: Int
        var top: Int
        var bottom: Int

        if (recyclerView.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            start = rvPaddingStart + dividerInsetStart - defaultDividerInset
            end = rvWidth - (rvPaddingEnd + dividerInsetEnd) + defaultDividerInset
        } else {
            start = rvPaddingStart + dividerInsetEnd - defaultDividerInset
            end = rvWidth - (rvPaddingEnd + dividerInsetStart) + defaultDividerInset
        }

        for (i in 0 until recyclerView.childCount) {
            child = recyclerView.getChildAt(i)
            val applyDecor = when (dividerRule){
                ALL -> true
                is SELECTED -> {
                    dividerRule.isEnabledFor (recyclerView.getChildViewHolder(child))
                }
                else -> false//not expected here
            }
            if (applyDecor) {
                top = child.bottom + child.marginBottom
                bottom = dividerHeight + top
                divider!!.apply {
                    setBounds(start, top, end, bottom)
                    draw(canvas)
                }
            }
        }
    }

    override fun seslOnDispatchDraw(
        canvass: Canvas,
        recyclerView: RecyclerView,
        state: RecyclerView.State
    ) {

        if (subheaderRoundedCorner == null) return

        var child: View
        for (i in 0 until recyclerView.childCount) {
            child = recyclerView.getChildAt(i)
            val applyDecor = when (subHeaderRule){
                ALL -> true
                is SELECTED -> {
                    subHeaderRule.isEnabledFor(recyclerView.getChildViewHolder(child))
                }
                else -> false//not expected here
            }
            if (applyDecor) {
                subheaderRoundedCorner!!.drawRoundedCorner(child, canvass)
            }
        }
    }
}

/**
 * Choose either [ALL], [SELECTED], [NONE]
 */
sealed class ItemDecorRule{
    /**
     * Apply decoration to all view holders.
     */
    data object ALL: ItemDecorRule()
    /**
     * Apply decoration only to specific view holders.
     * @param isEnabledFor Function lambda to check if decoration should be applied.
     */
    data class SELECTED(@JvmField val isEnabledFor: (vh: ViewHolder) -> Boolean): ItemDecorRule()

    /**
     * Do not apply decoration.
     */
    data object NONE: ItemDecorRule()
}

/**
 * @param start Start inset in pixels.
 * @param end End inset in pixels.
 */
data class DividerInsets(

    @JvmField
    @Px
    val start: Int,

    @JvmField
    @Px
    val end: Int
)