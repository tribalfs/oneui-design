@file:Suppress("MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.CallSuper
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.findAncestorOfType
import dev.oneuiproject.oneui.ktx.isDescendantOf
import dev.oneuiproject.oneui.ktx.tabViewGroup
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isDisplayTypeSub
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isLandscape

/**
 * An extension of [TabLayout] that dynamically adjusts its width to fit the parent
 * by setting appropriate side margins. It ensures that the selected tab is always
 * visible by automatically scrolling to it.
 *
 * This class provides a mechanism to customize the tab dimensions through the
 * [setCustomTabDimen] method, allowing for flexible layout adjustments.
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 */
open class MarginsTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.tabStyle
) : TabLayout(context, attrs, defStyleAttr) {

    @JvmField
    internal var tabDimens: TabDimen? = null
    @JvmField
    internal var tabTextWidthsList: ArrayList<Float> = arrayListOf()
    @JvmField
    internal val defaultTabPadding = context.resources.getDimension(R.dimen.oui_des_tab_layout_default_tab_padding)

    internal var sideMarginChanged = false
    @JvmField
    internal var sideMargin: Int = 0
    @JvmField
    internal var containerWidth: Int? = null

    @JvmField
    internal var recalculateTextWidths = false

    private var referenceContainer: ViewParent? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        updateLayoutParams()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        updateLayoutParams()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) {
            ensureReferenceContainer()
            calculateMarginsInternal()
        }
        super.onVisibilityChanged(changedView, visibility)
    }


    override fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
        recalculateTextWidths = true
        super.addTab(tab, position, setSelected)
    }

    override fun removeTab(tab: Tab) {
        recalculateTextWidths = true
        super.removeTab(tab)
    }

    private fun recalculateTextWidths() {
        tabTextWidthsList.clear()
        val tabViewGroup = tabViewGroup ?: return
        for (i in 0 until tabCount) {
            val tab = getTabAt(i)
            val viewGroup = tabViewGroup.getChildAt(i) as? ViewGroup
            if (tab != null && viewGroup != null) {
                val textView = tab.seslGetTextView()
                val subTextView = tab.seslGetSubTextView()
                val tabTextWidth = textView?.paint?.measureText(textView.getText().toString())
                    ?.coerceAtLeast(subTextView?.paint?.measureText(textView.getText().toString()) ?: 0f)
                    ?: 0f
                tabTextWidthsList.add(tabTextWidth)
            }
        }
    }

    override fun onAttachedToWindow() {
        ensureReferenceContainer()
        calculateMarginsInternal()
        setScrollPosition(selectedTabPosition, 0.0f, true)
        super.onAttachedToWindow()
    }

    private fun ensureReferenceContainer(){
        if (referenceContainer == null){
            // Attempt to use a consistently visible reference parent for determining the container's width.
            // This helps prevent width "jumping" issues during visibility transitions
            // (from 'gone' to 'visible'). Such issues arise due to delayed width updates when
            // the reference container was previously set to 'gone'.
            referenceContainer = findAncestorOfType<ToolbarLayout>()?.let{
                if (isDescendantOf(it.mainContainer)){
                    it.mainContainer
                }else if (isDescendantOf(it.footerParent)){
                    it.footerParent
                }else if (isDescendantOf(it.appBarLayout.parent as ViewGroup)){
                    it.appBarLayout.parent
                } else null
            } ?: parent
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (isAttachedToWindow) {
            calculateMarginsInternal()
            setScrollPosition(selectedTabPosition, 0.0f, true)
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }


    private fun calculateMarginsInternal(): Boolean {
        val parentWidth = with(referenceContainer!! as ViewGroup) { width - paddingStart - paddingEnd }
        if (parentWidth != 0 && containerWidth != parentWidth) {
            containerWidth = parentWidth
            if (tabDimens == null) {
                tabDimens = TabDimenDefault(defaultTabPadding)
            }
            calculateMargins()
            return true
        }
        return false
    }

    internal val requestLayoutRunnable = Runnable { requestLayout() }

    @CallSuper
    internal open fun calculateMargins(){
        if (recalculateTextWidths || isInEditMode) {
            recalculateTextWidths()
            recalculateTextWidths = false
        }

        tabDimens!!.getSideMargin(this, containerWidth!!, tabTextWidthsList.sum()).toInt().let {
            if (it != sideMargin){
                sideMargin = it
                sideMarginChanged = true
                post(requestLayoutRunnable)
            }
        }
    }


    open fun updateLayoutParams() {
        if (!sideMarginChanged) return
        sideMarginChanged = false
        layoutParams = (layoutParams as MarginLayoutParams).apply {
            marginStart = sideMargin
            marginEnd = sideMargin
        }
    }

    /**
     * Sets a custom implementation for calculating tab dimensions.
     * This allows for more granular control over how the side margins of the tabs are determined.
     *
     * @param tabDimenImpl An instance of [TabDimen] that defines the custom logic
     *                     for calculating tab dimensions.
     */
    open fun setCustomTabDimen(tabDimenImpl: TabDimen){
        tabDimens = tabDimenImpl
        requestLayout()
    }

    /**
     * Functional interface for calculating the side margin of the [TabLayout].
     * This allows for custom logic to determine how the tabs are spaced within
     * their container.
     */
    fun interface TabDimen{
        fun getSideMargin(tabLayout: TabLayout, containerWidthPixels: Int, totalTabTextsWidth: Float): Float
    }

    /**
     * Default implementation of [TabDimen] for calculating tab side margins.
     * This class provides a standard way to determine the side margins for tabs based on
     * various device and layout configurations, such as screen orientation, display type,
     * and screen width.
     *
     * @property defaultTabPadding The default padding to be applied to each tab. This value
     * is used in the calculation of total tab paddings.
     */
    class TabDimenDefault(val defaultTabPadding: Float): TabDimen{
        override fun getSideMargin(tabLayout: TabLayout, containerWidthPixels: Int, totalTabTextsWidth: Float): Float {

            val res = tabLayout.resources
            val configuration = res.configuration

            val tabLayoutPaddingMin = res.getDimension(R.dimen.oui_des_tab_layout_default_side_margin)

            if (isLandscape(configuration) && isDisplayTypeSub(configuration)) {
                return tabLayoutPaddingMin / 2f
            }

            if (configuration.smallestScreenWidthDp <= 480) {
                return tabLayoutPaddingMin
            }

            val tabLayoutPaddingMax = maxOf(containerWidthPixels * 0.125f, tabLayoutPaddingMin)
            val totalTabPaddings = defaultTabPadding * tabLayout.tabCount * 2

            return ((containerWidthPixels - totalTabTextsWidth - totalTabPaddings) / 2f)
                .coerceIn(tabLayoutPaddingMin, tabLayoutPaddingMax)
        }
    }

    companion object{
        private const val DEPTH_TYPE_MAIN = 1
        private const val DEPTH_TYPE_SUB = 2

        private const val TAG = "MarginsTabLayout"
    }


}

