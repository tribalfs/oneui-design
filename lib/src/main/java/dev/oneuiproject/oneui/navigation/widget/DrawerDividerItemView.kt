package dev.oneuiproject.oneui.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.util.SeslMisc
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx

/**
 * @hide
 */
@SuppressLint("RestrictedApi")
internal class DrawerDividerItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var offset: Float = -1f
    private var getNavRailSlideRange: () -> Int = { 0 }

    private val defaultWidth: Int
    private val defaultMargin: Int
    private val dividerView: View


    init {
        val res = context.resources
        defaultWidth = res.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_item_icon_size)
        defaultMargin =
            res.getDimensionPixelSize(R.dimen.oui_des_drawer_menu_divider_padding_start_end)
        dividerView = View(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5f.dpToPx(res)).apply {
                marginStart = defaultMargin
                marginEnd = defaultMargin
            }
            setBackgroundResource(R.drawable.oui_des_drawer_menu_dotted_separator)
            alpha = if (SeslMisc.isLightTheme(context)) 0.87f else 0.5f
        }
        addView(dividerView)
        isClickable = false
        isFocusable = false
    }

    fun setNavRailSlideRangeProvider(provider: () -> Int) {
        this.getNavRailSlideRange = provider
    }

    fun applyOffset(offset: Float) {
        if (this.offset == offset) return
        this.offset = offset
        updateDividerMargin()

        if (isInEditMode && offset == 0f) {
            dividerView.apply {
                updateLayoutParams<LayoutParams> {
                    width = defaultWidth
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        post { updateDividerMargin() }
    }

    /**
     * Updates the divider's end margin based on the current offset and slide range.
     */
    private fun updateDividerMargin() {
        dividerView.updateLayoutParams<LayoutParams> {
            marginEnd = defaultMargin + (getNavRailSlideRange() * (1f - offset)).toInt()
        }
    }
}