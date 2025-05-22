@file:Suppress("unused", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.pxToDp
import dev.oneuiproject.oneui.ktx.windowHeight
import dev.oneuiproject.oneui.ktx.windowWidthNetOfInsets
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.updateStatusBarVisibility

/**
 * Extension of [CoordinatorLayout]  which automatically hides the status bar based
 * on the display size and orientation and dynamically updates the side margins
 * of the specified child views.
 */
open class AdaptiveCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.coordinatorlayout.R.attr.coordinatorLayoutStyle,
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    data class SideMarginParams(
        @JvmField @Px var sideMargin: Int,
        @JvmField var matchParent: Boolean = true
    )

    fun interface MarginProvider {
        fun getSideMarginParams(context: Context): SideMarginParams
    }

    private var marginProviderImpl: MarginProvider? = MARGIN_PROVIDER_ADP_DEFAULT

    private var adaptiveMarginViews: Set<View>? = null

    private val activity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity }

    /**
     * Provide a custom implementation of [MarginProvider] to be used
     * for determining the side margins of the specified [childViews].
     *
     * @param provider The custom [MarginProvider] implementation to use.
     * @param childViews The childViews to apply the side margins to.
     */
    fun configureAdaptiveMargin(provider: MarginProvider?, childViews: Set<View>?) {
        restoreMargins(childViews)
        adaptiveMarginViews = childViews
        marginProviderImpl = provider
        if (!isAttachedToWindow) return
        if (computeSideMarginParams()){
            requestLayout()
        }
    }

    /**
     * Provide a custom implementation of [MarginProvider] to be used
     * for determining the side margins of the specified [childView].
     *
     * @param provider The custom [MarginProvider] implementation to use.
     * @param childView The childView to apply the side margins to.
     */
   inline fun configureAdaptiveMargin(provider: MarginProvider?, childView: View) {
        configureAdaptiveMargin(provider, setOf(childView))
    }

    private fun restoreMargins(childViews: Set<View>?) {
        // Restore margins for any previously tracked views
        adaptiveMarginViews?.forEach { child ->
            if (childViews == null || child !in childViews) {
                child.updateLayoutParams<MarginLayoutParams> {
                    @Suppress("UNCHECKED_CAST")
                    val initMargins = child.getTag(R.id.tag_init_side_margins) as Pair<Int, Int>
                    leftMargin =  initMargins.first
                    rightMargin = initMargins.second
                }
            }
        }
    }


    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        child.setTag(R.id.tag_init_side_margins, Pair((params as MarginLayoutParams).leftMargin,  params.rightMargin))
        super.addView(child, index, params)
    }

    override fun onMeasureChild(
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {

        if (lastSideMarginParams != null || computeSideMarginParams()) {
            if (adaptiveMarginViews?.contains(child) == true) {
                @Suppress("UNCHECKED_CAST")
                val origMargins = child.getTag(R.id.tag_init_side_margins) as Pair<Int, Int>
                child.applySideMarginParams(
                    lastSideMarginParams!!,
                    origMargins.first,
                    origMargins.second
                )
            }
        }

        super.onMeasureChild(
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }

    override fun onAttachedToWindow() {
        computeSideMarginParams()
        super.onAttachedToWindow()
        activity?.updateStatusBarVisibility()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        computeSideMarginParams()
        super.onConfigurationChanged(newConfig)
        activity?.updateStatusBarVisibility()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeSideMarginParams()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private var lastSideMarginParams: SideMarginParams? = null

    private fun computeSideMarginParams(): Boolean {
        adaptiveMarginViews?.let {
            marginProviderImpl?.getSideMarginParams(context)?.let {smp ->
                if (smp != lastSideMarginParams) {
                    lastSideMarginParams = smp
                    return true
                }
            }
        }
        return false
    }

    private inline fun View.applySideMarginParams(
        smp: SideMarginParams,
        additionalLeft: Int,
        additionalRight: Int
    ) {
        layoutParams = (layoutParams as LayoutParams).apply{
            if (smp.matchParent) width = ViewGroup.LayoutParams.MATCH_PARENT
            leftMargin = smp.sideMargin + additionalLeft
            rightMargin = smp.sideMargin + additionalRight
        }
    }

    companion object{
        private const val TAG = "ADPCoordinatorLayout"

        @JvmField
        val MARGIN_PROVIDER_ADP_DEFAULT = MarginProvider { context ->
            val widthXInsets = context.windowWidthNetOfInsets
            val pxToDp = 1f.pxToDp(context.resources)

            // Using the following values instead of those from
            // resources.configuration.* due to the latter's
            // delay in being updated.
            val screenWidthDp = widthXInsets * pxToDp
            val screenHeightDp = context.windowHeight * pxToDp

            val marginRatio = when {
                (screenWidthDp < 589) -> 0.0f
                (screenHeightDp > 411 && screenWidthDp <= 959) -> 0.05f
                (screenWidthDp >= 960 && screenHeightDp <= 1919) -> 0.125f
                (screenWidthDp >= 1920) -> 0.25f
                else -> 0.0f
            }
            SideMarginParams(
                (widthXInsets * marginRatio).toInt(),
                screenWidthDp >= 589
            )
        }

        @JvmField
        val MARGIN_PROVIDER_ZERO = MarginProvider { _ -> SideMarginParams(0, true) }
    }

}