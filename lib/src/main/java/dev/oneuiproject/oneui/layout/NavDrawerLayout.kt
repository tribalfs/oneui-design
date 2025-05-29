@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.layout


import android.content.Context
import android.content.res.Configuration
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.core.view.doOnLayout
import androidx.customview.view.AbsSavedState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.NavDrawerLayout.Companion.DEFAULT_NAV_RAIL_DETAILS_WIDTH
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.layout.internal.widget.SemSlidingPaneLayout
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletLayout
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MarginProvider
import dev.oneuiproject.oneui.layout.DrawerLayout as OneUIDrawerLayout

/**
 * OneUI-styled layout that implements a DrawerLayout interface on smaller devices and a Navigation Rail interface on larger devices,
 * similar to the Samsung Apps experience.
 *
 * **Important:**
 * - This view must be hosted within an [AppCompatActivity][androidx.appcompat.app.AppCompatActivity] as it relies on AppCompat-specific features and theming.
 * Otherwise, it will result to runtime exceptions.
 * - To use this layout, ensure your activity does *not* handle `smallestScreenSize` in `android:configChanges`.
 * Otherwise, layout changes based on screen size will not function correctly.
 *
 * ## XML Attributes
 * The following XML attributes are supported in addition to attributes of [DrawerLayout]:
 *
 * - `app:drawerBackAnimation`: Enable or disable drawer's predictive back animation.
 *       This applies only to api34+. This is set to false by default.
 *
 *  ## Setting the locations of the direct child views
 *  The locations of direct child views can be set by setting any of the following values to the
 *  `app:layout_location` attribute of the child view in addition to the location values allowed in [ToolbarLayout]:
 *
 * - `drawer_header` - Sets the view as a custom header row within the drawer panel.
 *    Serves as the topmost element in the drawer.
 * - `drawer_panel` - Adds the view to inside the drawer panel below the `drawer_header`.
 */
class NavDrawerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OneUIDrawerLayout(context, attrs) {

    @Px private var navRailContentMinSideMargin: Int = 0
    @Px private var navRailContentPreferredWidth: Int = DEFAULT_NAV_RAIL_DETAILS_WIDTH
    private var navRailContentPaneResizeOff = false
    private var hideNavRailDrawerOnCollapse = false

    private var semSlidingPaneLayout: SemSlidingPaneLayout? = null

    /**Returns true if the current interface is in navigation rail mode*/
    val isLargeScreenMode get() = isTabletLayout(resources)

    override fun getDefaultLayoutResource() =
        if (isLargeScreenMode) R.layout.oui_des_layout_navdrawer_main else super.getDefaultLayoutResource()

    override val containerLayout: DrawerLayoutInterface get() =
        semSlidingPaneLayout
            ?: findViewById<SemSlidingPaneLayout>(R.id.sliding_pane_layout)?.also { semSlidingPaneLayout = it }
            ?: super.containerLayout

    override val navButtonsHandler: NavButtonsHandler get() =
        semSlidingPaneLayout
            ?: findViewById<SemSlidingPaneLayout>(R.id.sliding_pane_layout)?.also { semSlidingPaneLayout = it }
            ?: super.navButtonsHandler


    override fun updateOnBackCallbackState() {
        //Don't interrupt animation if has already started.
        if ((backHandler as DrawerLayoutBackHandler<*>).isBackProgressStarted()) return
        super.updateOnBackCallbackState()
    }

    /**
     * Set whether or not back button press or gesture closes the drawer/nav rail pane if it's opened.
     * This applies only when on [largeScreenMode][isLargeScreenMode].
     * This is false by default.
     */
    var closeNavRailOnBack = false
        set(value) {
            if (field == value) return
            field = value
            updateOnBackCallbackState()
        }

    override fun getBackCallbackStateUpdate(): Boolean =
        (containerLayout as? SemSlidingPaneLayout)?.let {
            it.isDrawerOpenOrIsOpening
                    && closeNavRailOnBack && !isDrawerLocked() || isSearchMode || isActionMode
        } ?: super.getBackCallbackStateUpdate()


    override val shouldCloseDrawer: Boolean get() =
        (containerLayout as? SemSlidingPaneLayout)?.let {
            it.isDrawerOpenOrIsOpening
                    && closeNavRailOnBack && !isDrawerLocked() && !isActionMode && !isSearchMode
        } ?: super.shouldCloseDrawer



    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateContentWidth()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        updateContentWidth()
    }

    /**
     * Set minimum side margin for details pane when on [largeScreenMode][isLargeScreenMode].
     *
     * @see setAdaptiveMarginProvider
     * @see setNavRailContentPreferredWidth
     */
    fun setNavRailContentMinSideMargin(@Px minSideMargin: Int) {
        if (navRailContentMinSideMargin == minSideMargin) return
        navRailContentMinSideMargin = minSideMargin
        if (isAttachedToWindow) updateContentWidth()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private fun updateContentWidth() {
        (containerLayout as? SemSlidingPaneLayout)?.apply {
            updateContentMinSidePadding(navRailContentMinSideMargin)
            if (navRailContentPaneResizeOff){
                doOnLayout { seslSetResizeOff(true) }
            }else {
                if (seslGetPreferredContentPixelSize() != navRailContentPreferredWidth) {
                    seslRequestPreferredContentPixelSize(navRailContentPreferredWidth)
                }else{
                    resizeSlideableView(drawerOffset)
                }
            }
        }
    }

    /**
     * Override the preferred/maximum width of the contents in the details pane
     * when on [largeScreenMode][isLargeScreenMode].
     *
     * @param preferredWidth The preferred width in pixels.
     * Set [DEFAULT_NAV_RAIL_DETAILS_WIDTH] to restore default.
     *
     * @see setNavRailContentMinSideMargin
     * @see setAdaptiveMarginProvider
     */
    fun setNavRailContentPreferredWidth(@Px preferredWidth: Int) {
        if (navRailContentPreferredWidth == preferredWidth) return
        navRailContentPreferredWidth = preferredWidth
        (containerLayout as? SemSlidingPaneLayout)?.seslRequestPreferredContentPixelSize(preferredWidth)
    }

    /**
     * Setting this to true disables auto resizing of the views width inside the details pane
     * when on [largeScreenMode][isLargeScreenMode]. The views will just slide out when
     * the drawer pane is expanded.
     *
     * This is false by default.
     */
    fun setNavRailContentPaneResizeOff(resizeOff: Boolean){
        if (navRailContentPaneResizeOff == resizeOff) return
        navRailContentPaneResizeOff = resizeOff
        (containerLayout as? SemSlidingPaneLayout)?.apply {
            if (seslGetResizeOff() != resizeOff) {
                doOnLayout { seslSetResizeOff(resizeOff) }
            }
        }
    }

    /**
     * Assigns a custom implementation of [MarginProvider] to be used
     * for determining the side margin of the content pane when UI is not on [largeScreenMode][isLargeScreenMode]
     * (i.e. in DrawerLayout mode).
     *
     * This will override the default behavior provided by
     * [MARGIN_PROVIDER_ADP_DEFAULT][dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ADP_DEFAULT].
     *
     * @param provider The custom [MarginProvider] implementation to use.
     * Set [MARGIN_PROVIDER_ADP_DEFAULT][dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ADP_DEFAULT] to restore default.
     *
     * @see [dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ZERO]
     * @see setNavRailContentPreferredWidth
     * @see setNavRailContentMinSideMargin
     *
     */
    @Suppress("RedundantOverride")
    override fun setAdaptiveMarginProvider(provider: MarginProvider){
        super.setAdaptiveMarginProvider(provider)
    }

    override fun getAdaptiveChildViews() =
        if(containerLayout is SemSlidingPaneLayout) null else super.getAdaptiveChildViews()

    override fun updateDrawerLock() {
        (containerLayout as? SemSlidingPaneLayout)?.apply {
            val newLockState = lockNavRailOnActionMode && isActionMode
                    || lockNavRailOnSearchMode && isSearchMode
                    || !drawerEnabled
            if (isLocked != newLockState) {
                isLocked = newLockState
                drawerLockListener?.onLockChanged(newLockState)
            }
           } ?: super.updateDrawerLock()
    }

    /**
     * Sets whether to lock the navigation rail or not when action mode is active.
     * When the navigation rail is locked, it can't be closed or opened by the user.
     * This applies only when in [largeScreenMode][isLargeScreenMode].
     *
     * **Important:** The management of enabling and disabling descendant views within the
     * drawer pane (such as drawer items) is not handled by this component.
     * This responsibility must be managed separately. For instance, if drawer items
     * are implemented using a RecyclerView, this task can be delegated to the RecyclerView adapter.
     *
     * @see setDrawerLockListener
     * @see lockNavRailOnSearchMode
     * @see isActionMode
     * @see startActionMode
     */
    var lockNavRailOnActionMode = false
        set(value) {
            if (value == field) return
            field = value
            if (isLargeScreenMode) {
                updateDrawerLock()
            }
        }

    /**
     * Sets whether to lock the navigation rail when search mode is active.
     * When the navigation rail is locked, it can't be closed or opened by the user.
     * This applies only when in [largeScreenMode][isLargeScreenMode].
     *
     * **Important:** The management of enabling and disabling descendant views within the
     * drawer pane (such as drawer items) is not handled by this component.
     * This responsibility must be managed separately. For instance, if drawer items
     * are implemented using a RecyclerView, this task can be delegated to the RecyclerView adapter.
     *
     * @see setDrawerLockListener
     * @see lockNavRailOnActionMode
     * @see isSearchMode
     * @see startSearchMode
     */
    var lockNavRailOnSearchMode = false
        set(value) {
            if (value == field) return
            field = value
            if (isLargeScreenMode) {
                updateDrawerLock()
            }
        }

    /**
     * Open or close the drawer panel.
     *
     * @param open Set to `true` to open, `false` to close
     * @param animate (Optional) whether or not to animate the opening and closing. Default value is `true`.
     * @param ignoreOnNavRailMode (Optional) Don't apply when [largeScreenMode][isLargeScreenMode] is `true`.
     * Default value is set to `false`.
     */
    fun setDrawerOpen(open: Boolean, animate: Boolean = true,
                      ignoreOnNavRailMode: Boolean = false) {
        if (isLargeScreenMode && ignoreOnNavRailMode) return
        setDrawerOpen(open, animate)
    }

    /**The slide range of the nav rail from the closed state to the open state or vice versa.
     * This applies only when in [largeScreenMode][isLargeScreenMode].
     * Otherwise, it will always return 0.*/
    fun getNavRailSlideRange(): Int =
        (containerLayout as? SemSlidingPaneLayout)?.getSlideRange() ?: 0


    fun setHideNavRailDrawerOnCollapse(hideOnCollapse: Boolean){
        if (this.hideNavRailDrawerOnCollapse == hideOnCollapse) return
        this.hideNavRailDrawerOnCollapse = hideOnCollapse
        updateDrawerState()
    }

    override fun updateDrawerState(animate: Boolean) {
        (containerLayout as? SemSlidingPaneLayout)?.apply {
            setDrawerEnabled(drawerEnabled, hideNavRailDrawerOnCollapse, animate)
            if (drawerEnabled) {
                updateDrawerLock()
            }else{
                seslSetLock(true)
            }
            return
        }
        super.updateDrawerState(animate)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.navRailContentPaneResizeOff = navRailContentPaneResizeOff
        state.hideNavRailDrawerOnCollapse = hideNavRailDrawerOnCollapse
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        setNavRailContentPaneResizeOff(state.navRailContentPaneResizeOff)
        setHideNavRailDrawerOnCollapse(state.hideNavRailDrawerOnCollapse)
        super.onRestoreInstanceState(state.superState)
    }

    private class SavedState : AbsSavedState {
        var navRailContentPaneResizeOff = false
        var hideNavRailDrawerOnCollapse = false

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            navRailContentPaneResizeOff = parcel.readInt() != 0
            hideNavRailDrawerOnCollapse = parcel.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (navRailContentPaneResizeOff) 1 else 0)
            out.writeInt(if (hideNavRailDrawerOnCollapse) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object :
                    ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(parcel: Parcel,
                                                  loader: ClassLoader): SavedState = SavedState(parcel, loader)
                    override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel, SavedState::class.java.classLoader)
                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }

    companion object {
        const val DEFAULT_NAV_RAIL_DETAILS_WIDTH = -1
    }

    private object Constants{
        const val TAG = "NavDrawerLayout"
        const val DRAWER_HEADER = 4
    }

}


