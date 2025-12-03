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
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.ktx.pxToDp
import dev.oneuiproject.oneui.ktx.windowHeight
import dev.oneuiproject.oneui.ktx.windowWidthNetOfInsets
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.updateStatusBarVisibility

/**
 * Extension of [CoordinatorLayout]  which automatically hides the status bar based
 * on the display size and orientation and dynamically updates the side margins
 * of the specified child views.
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 * @param defStyleAttr (Optional) An attribute in the current theme that contains a
 * reference to a style resource that supplies default values for the view.
 *
 * @see configureAdaptiveMargin
 * @see MARGIN_PROVIDER_ADP_DEFAULT
 * @see MARGIN_PROVIDER_ZERO
 */
open class AdaptiveCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.coordinatorlayout.R.attr.coordinatorLayoutStyle,
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    @Px
    private var _landscapeHeightForStatusBar = 420

    /**
     * Data class to store the side margin parameters for child views.
     *
     * @property sideMargin The side margin value in pixels.
     * @property matchParent Whether the child view should match the parent's width.
     */
    data class SideMarginParams(
        @JvmField @field:Px var sideMargin: Int,
        @JvmField var matchParent: Boolean = true
    )

    /**
     * Interface for providing side margin parameters to [AdaptiveCoordinatorLayout].
     * Can be used to implement custom logic for determining the side margins
     * of child views.
     */
    fun interface MarginProvider {
        fun getSideMarginParams(context: Context): SideMarginParams
    }

    private var marginProviderImpl: MarginProvider? = MARGIN_PROVIDER_ADP_DEFAULT

    private var adaptiveMarginViews: Set<View>? = null

    private val activity by lazy(LazyThreadSafetyMode.NONE) { context.activity }

    /**
     * This function allows you to define custom logic for calculating
     * side margins for specified [childViews] within this layout.
     *
     * When this function is called:
     * - Any previously applied adaptive margins on the `childViews` (or views
     * that were previously part of `adaptiveMarginViews` but are not in the
     * new `childViews` set) are restored to their initial values.
     * - The `adaptiveMarginViews` set is updated with the new `childViews`.
     *
     * @param provider The custom [MarginProvider] implementation to use.
     * To remove adaptive margins entirely for these views, you might
     * pass `null` for `childViews` or use [MARGIN_PROVIDER_ZERO]
     * if you want them to match parent with zero margins.
     * @param childViews The set of child [View]s to apply the side margins to.
     * If `null`, previously tracked views will have their margins restored,
     * and no new views will be tracked for adaptive margins.
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
     * Sets the screen height threshold for hiding the status bar in landscape mode.
     *
     * In landscape mode, the status bar will be hidden if the screen height in px
     * is lesser than or equal to this threshold. The default value is 420.
     *   *
     * @param threshold The screen height in px lesser or equal which the status bar should be hidden
     * in landscape mode.  Pass `0` to always show the status bar, or
     * a large value (like `Integer.MAX_VALUE`) to effectively disable hiding.
     */
    fun setLandscapeHeightForStatusBar(@Px threshold: Int) {
        if (_landscapeHeightForStatusBar == threshold) return
        _landscapeHeightForStatusBar = threshold
        if (isAttachedToWindow) {
            activity?.updateStatusBarVisibility(_landscapeHeightForStatusBar)
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Compute side margin parameters before measuring children.
        computeSideMarginParams()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onMeasureChild(
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {

        if (lastSideMarginParams != null && adaptiveMarginViews?.contains(child) == true) {
            @Suppress("UNCHECKED_CAST")
            val origMargins = child.getTag(R.id.tag_init_side_margins) as Pair<Int, Int>
            child.applySideMarginParams(
                lastSideMarginParams!!,
                origMargins.first,
                origMargins.second
            )
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
        activity?.updateStatusBarVisibility(_landscapeHeightForStatusBar)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        computeSideMarginParams()
        super.onConfigurationChanged(newConfig)
        activity?.updateStatusBarVisibility(_landscapeHeightForStatusBar)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw) computeSideMarginParams()
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
        // DO NOT assign a new LayoutParams object.
        // Instead, modify the properties of the existing one
        // so we wouldn't trigger a requestLayout.
        (layoutParams as LayoutParams).apply{
            if (smp.matchParent) width = ViewGroup.LayoutParams.MATCH_PARENT
            leftMargin = smp.sideMargin + additionalLeft
            rightMargin = smp.sideMargin + additionalRight
        }
    }

    companion object{
        private const val TAG = "ADPCoordinatorLayout"

        /**
         * Default margin provider for adaptive layout.
         *
         * The margin is calculated based on the screen width and height:
         * - If screen width < 589dp, margin is 0.
         * - If screen height > 411dp and screen width <= 959dp, margin is 5% of screen width.
         * - If screen width >= 960dp and screen height <= 1919dp, margin is 12.5% of screen width.
         * - If screen width >= 1920dp, margin is 25% of screen width.
         *
         * The view width will be set to MATCH_PARENT if screen width >= 589dp.
         */
        @JvmField
        val MARGIN_PROVIDER_ADP_DEFAULT = MarginProvider { context ->
            // Using the following values instead of those from
            // resources.configuration.* due to the latter's
            // delay in being updated.
            val metrics = context.resources.displayMetrics
            val density = metrics.density

            val widthPixels = metrics.widthPixels.toFloat()
            val heightPixels = metrics.heightPixels.toFloat()

            val screenWidthDp = widthPixels / density
            val screenHeightDp = heightPixels / density

            val marginRatio = when {
                (screenWidthDp < 589) -> 0.0f
                (screenHeightDp > 411 && screenWidthDp <= 959) -> 0.05f
                (screenWidthDp >= 960 && screenHeightDp <= 1919) -> 0.125f
                (screenWidthDp >= 1920) -> 0.25f
                else -> 0.0f
            }
            SideMarginParams(
                (widthPixels * marginRatio).toInt(),
                screenWidthDp >= 589
            )
        }

        /** A [MarginProvider] that provides zero side margins and match_parent width. */
        @JvmField
        val MARGIN_PROVIDER_ZERO = MarginProvider { _ -> SideMarginParams(0, true) }
    }

}