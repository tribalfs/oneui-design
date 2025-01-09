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

    class SideMarginParams(
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
        if (isAttachedToWindow){
            updateContentSideMargin()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        activity?.updateStatusBarVisibility()
        updateContentSideMargin()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        activity?.updateStatusBarVisibility()
        updateContentSideMargin()
    }

    private fun updateContentSideMargin() {
        val adaptiveViews = adaptiveMarginViews?.also { if (it.isEmpty()) return } ?: return
        val sideMarginParams = marginProviderImpl?.getSideMarginParams(context) ?: return
        for (child in adaptiveViews) {
            @Suppress("UNCHECKED_CAST")
            val origMargins = child.getTag(R.id.tag_init_side_margins) as Pair<Int, Int>
            child.setSideMarginParams(sideMarginParams, origMargins.first, origMargins.second)
        }
        post{ requestLayout() }
    }

    private inline fun View.setSideMarginParams(
        smp: SideMarginParams,
        additionalLeft: Int,
        additionalRight: Int
    ) {
        (layoutParams as? MarginLayoutParams)?.let {
            updateLayoutParams<MarginLayoutParams> {
                if (smp.matchParent) width = ViewGroup.LayoutParams.MATCH_PARENT
                leftMargin = smp.sideMargin + additionalLeft
                rightMargin = smp.sideMargin + additionalRight
            }
        } ?: run {
            setPadding(
                smp.sideMargin + additionalLeft,
                paddingTop,
                smp.sideMargin + additionalRight,
                paddingBottom
            )
        }
    }

    companion object{
        private const val TAG = "ADPCoordinatorLayout"

        @JvmField
        val MARGIN_PROVIDER_ADP_DEFAULT = MarginProvider { context ->
            val config = context.resources.configuration
            val screenWidthDp = config.screenWidthDp
            val screenHeightDp = config.screenHeightDp
            val widthXInsets = context.windowWidthNetOfInsets
            val marginRatio = when {
                (screenWidthDp < 589) -> 0.0f
                (screenHeightDp > 411 && screenWidthDp <= 959) -> 0.05f
                (screenWidthDp >= 960 && screenHeightDp <= 1919) -> 0.125f
                (screenWidthDp >= 1920) -> 0.25f
                else -> 0.0f
            }
            SideMarginParams(
                maxOf((widthXInsets * marginRatio).toInt()),
                screenWidthDp >= 589
            )
        }

        @JvmField
        val MARGIN_PROVIDER_ZERO = MarginProvider { _ -> SideMarginParams(0, true) }
    }

}