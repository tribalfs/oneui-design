@file:Suppress("unused", "MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE")
package dev.oneuiproject.oneui.layout


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewOutlineProvider
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.widget.TooltipCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.customview.widget.Openable
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector.getField_TYPE_NONE
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.ifNegativeOrZero
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.NavDrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.widget.NavSlidingPaneLayout
import dev.oneuiproject.oneui.layout.internal.widget.NavSlidingPaneLayout.Companion.DEFAULT_OVERHANG_SIZE
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletLayout
import dev.oneuiproject.oneui.layout.DrawerLayout as OneUIDrawerLayout


/**
 * OneUI-styled layout that implements a DrawerLayout interface on smaller devices and a Navigation Rail interface on larger devices,
 * similar to the Samsung Apps experience.

 * **Important:** To use this layout, ensure your activity does *not* handle `smallestScreenSize` in `android:configChanges`.
 * Otherwise, layout changes based on screen size will not function correctly.
 */
@SuppressLint("RestrictedApi", "PrivateResource")
class NavDrawerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
): OneUIDrawerLayout(context, attrs), Openable{

    private lateinit var mDrawerPane: LinearLayout
    internal val drawerPane get() = mDrawerPane

    private lateinit var mainDetailsPane: LinearLayout
    private var mSplitDetailsPane: FrameLayout? = null
    private var mSlidingPaneLayout: NavSlidingPaneLayout? = null
    private var mNavRailDrawerButtonBadge: TextView? = null
    private var navRailDrawerButton: ImageButton? = null
    private var mSlideViewPane: FrameLayout? = null
    private var mNavRailSlideViewContent: LinearLayout? = null
    private var mDrawerHeaderLayout: View? = null
    private var mDrawerHeaderButton: ImageButton? = null
    private var mDrawerHeaderButtonBadgeView: TextView? = null
    private var mDrawerItemsContainer: FrameLayout? = null
    private var mSystemBarsColor: Int? = null
    private var mHsv: FloatArray? = null
    private var mScrimAlpha: Float? = null

    private var mDrawerHeaderButtonBadge: Badge = Badge.NONE

    private var mOnNavRailConfigChangedCallback: (() -> Unit)? = null

    @Volatile
    private var mCurrentState: DrawerState = DrawerState.CLOSE

    @Dimension
    private var navRailContentMinSideMargin: Int = 0
    private var mTopSystemBarsInset: Int = 0

    /**
     * Returns true if the current interface is in navigation rail mode
     */
    val isLargeScreenMode get() = isTabletLayout(resources)

    /**
     * Animate closing and opening of drawer in navigation rail mode
     */
    @JvmField
    var animateDrawer = true

    init {
        if (isInEditMode) {
            ensureLayoutPreview(attrs)
        }else{
            updateOnBackCallbackState()
        }
    }

    override fun initViews() {
        if (isLargeScreenMode) {
            initNavRailModeViews()
        }else{
            super.initViews()
        }
    }

    override val handleInsets get()  = !isLargeScreenMode && super.handleInsets

    private fun ensureLayoutPreview(attrs: AttributeSet?){
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.NavDrawerLayout, 0, 0).use { a ->
                if (a.getBoolean(R.styleable.NavDrawerLayout_isOpen, false)) {
                    mSlideOffset = 1f
                    setDrawerOpen(open = true)
                    if (!isLargeScreenMode) {
                        val width = mDrawerPane.width.ifNegativeOrZero { 356.dpToPx(resources) }
                        mDrawerPane.updateLayoutParams {
                            this.width = width
                        }
                        mSlideViewPane!!.translationX = width * if (isLayoutRTL) -1f else 1f
                    }else{
                        if (!mSlidingPaneLayout!!.seslGetResizeOff()) {
                            mNavRailSlideViewContent!!.updatePadding (
                                right = mDrawerPane.layoutParams.width
                                        - ((DEFAULT_OVERHANG_SIZE + 40) * context.dpToPxFactor + 0.5f).toInt()
                            )
                        }
                    }
                }else{
                    if (isLargeScreenMode) {
                        mSlideOffset = 0f
                    }
                }
            }
    }

    override fun updateOnBackCallbackState() {
        //Don't interrupt animation if has already started.
        if ((backHandler as DrawerLayoutBackHandler<*>).isBackProgressStarted()) return
        super.updateOnBackCallbackState()
    }

    var closeNavRailOnBack = false
        set(value) {
            if (field == value) return
            field = value
            updateOnBackCallbackState()
        }

    override fun getBackCallbackStateUpdate(): Boolean {
        return if (isLargeScreenMode){
            isDrawerOpenOrOpening && closeNavRailOnBack && !isDrawerLocked() || isSearchMode || isActionMode
        }else{
            super.getBackCallbackStateUpdate()
        }.also {
            Log.d(TAG, "getBackCallbackStateUpdate: $it")
        }
    }

    override val shouldCloseDrawer: Boolean
        get() = if (isLargeScreenMode) {
            closeNavRailOnBack && isDrawerOpenOrOpening && !isDrawerLocked() && !isActionMode && !isSearchMode
        } else super.shouldCloseDrawer

    override val backHandler: BackHandler
        get() = if (isLargeScreenMode) {
            DrawerLayoutBackHandler(this@NavDrawerLayout, NavDrawerBackAnimator(mDrawerPane, mSlideViewPane!!))
        } else super.backHandler

    override fun getDefaultLayoutResource() =
        if (isLargeScreenMode) { R.layout.oui_layout_navdrawer_main } else super.getDefaultLayoutResource()

    /**
     * Set the navigation icon to show on the NavRail/Drawer Panel.
     * It's recommended to also set a relevant tooltip.
     *
     * @see setNavigationButtonTooltip
     */
    override fun setNavigationButtonIcon(icon: Drawable?) {
        Log.d(TAG, "isNavRailMode: $isLargeScreenMode, setNavigationButtonIcon: $icon")
        if (isLargeScreenMode) {
            navRailDrawerButton!!.apply {
                isVisible = true
                setImageDrawable(icon)
                invalidate()
            }
        }else{
            super.setNavigationButtonIcon(icon)
        }
    }

    override fun setNavigationButtonOnClickListener(listener: OnClickListener?) {
        if (isLargeScreenMode) {
            navRailDrawerButton!!.setOnClickListener {
                listener?.onClick(it)
            }
        }else{
            toolbar.setNavigationOnClickListener(listener)
        }
    }

    override fun setNavigationButtonBadge(badge: Badge){
        if (isLargeScreenMode){
            if (mNavRailDrawerButtonBadge != null) {
                updateBadgeView(mNavRailDrawerButtonBadge!!, badge)
            }
        }else{
            super.setNavigationButtonBadge(badge)
        }
    }

    /**
     * Set the navigation button tooltip to show on the NavRail/Drawer Panel.
     *
     * @see setNavigationButtonIcon
     */
    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        navRailDrawerButton?.apply {
            contentDescription = tooltipText
            TooltipCompat.setTooltipText(this, tooltipText)
        } ?: super.setNavigationButtonTooltip(tooltipText)

    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (!isLargeScreenMode || mSlideViewPane == null || mDrawerItemsContainer == null) {
            super.addView(child, index, params)
        } else {
            when ((params as ToolbarLayoutParams).layoutLocation) {
                DRAWER_HEADER -> {
                    (mDrawerHeaderLayout as ViewGroup).apply {
                        removeView(mDrawerHeaderButton)
                        removeView(mDrawerHeaderButtonBadgeView)
                    }
                    mDrawerPane.addView(child, 1, params)
                    mDrawerHeaderButton = child.findViewById(R.id.drawer_header_button)
                    mDrawerHeaderButtonBadgeView = child.findViewById(R.id.drawer_header_button_badge)
                    navRailDrawerButton = child.findViewById<ImageButton>(R.id.navRailDrawerButton).apply {
                        isVisible = true
                    }
                    mNavRailDrawerButtonBadge = child.findViewById(R.id.navRailDrawerButtonBadge)

                    if (mDrawerHeaderButton == null) {
                        Log.e(TAG, "`drawerHeaderButton` id is missing or is not an ImageButton")
                    }
                    if (mDrawerHeaderButtonBadgeView == null) {
                        Log.e(TAG, "`drawerHeaderButtonBadgeView` id is missing or is not a TextView")
                    }
                }

                DRAWER_PANEL -> mDrawerItemsContainer!!.addView(child, params)

                else -> {
                    super.addView(child, index, params)
                }
            }
        }
    }

    override fun lockDrawerIfAvailable(lock: Boolean) {
        if (isLargeScreenMode) {
            lockNavRail(isActionMode && lockNavRailOnActionMode || isSearchMode && lockNavRailOnSearchMode)
        }else {
            super.lockDrawerIfAvailable(lock)
        }
    }

    @get:Dimension
    var preferredNavRailWidth: Int?
        get() = mSlidingPaneLayout?.seslGetPreferredDrawerPixelSize()
        set (value) {
            if (isLargeScreenMode) {
                if (mSlidingPaneLayout!!.seslGetPreferredDrawerPixelSize() == value) return
                mSlidingPaneLayout!!.seslRequestPreferredDrawerPixelSize(value?.dpToPx(resources) ?: -1)
            } else {
                Log.w(TAG, "setPreferredNavRailWidth is only available in NavRailMode")
            }

        }

    var contentPaneResizeOff: Boolean?
        get() = mSlidingPaneLayout?.seslGetResizeOff()
        set(value) {
            if (isLargeScreenMode) {
                mSlidingPaneLayout!!.apply {
                    if (seslGetResizeOff() != value) {
                        seslSetResizeOff(value ?: false)
                    }
                }
            } else Log.w(TAG, "contentPaneResizeOff only applies when on NavRailMode")
        }


    /**
     * Set minimum side margin for content pane when on nav rail mode
     */
    fun setNavRailContentMinSideMargin(@Dimension minSideMargin: Int){
        if (navRailContentMinSideMargin == minSideMargin) return
        navRailContentMinSideMargin = minSideMargin
        updateContentSideMargin()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun updateContentSideMargin(){
        if (isLargeScreenMode){
            mSlideViewPane!!.updatePadding(left = navRailContentMinSideMargin, right = navRailContentMinSideMargin)
        }else{
            super.updateContentSideMargin()
        }
    }

    override fun updateDrawerWidth(){
        if (isLargeScreenMode) return
        super.updateDrawerWidth()
    }

    @SuppressLint("RestrictedApi")
    private inline fun initNavRailModeViews(){

        mSlidingPaneLayout = rootView.findViewById<NavSlidingPaneLayout>(R.id.sliding_pane_layout)
            .also {
                mSlideViewPane = it.findViewById(R.id.slideable_view)
                mDrawerPane = it.findViewById(R.id.drawer_panel)
                mNavRailSlideViewContent = it.findViewById(R.id.slide_contents)
                mainDetailsPane = mNavRailSlideViewContent!!.findViewById(R.id.tbl_main_content_root)
            }.apply {
                addPanelSlideListener(SlidingPaneSlideListener())
            }

        if (navRailDrawerButton == null) {
            navRailDrawerButton = mDrawerPane.findViewById<ImageButton?>(R.id.navRailDrawerButton).apply { isVisible = true }
            mNavRailDrawerButtonBadge = mDrawerPane.findViewById(R.id.navRailDrawerButtonBadge)

            setNavigationButtonOnClickListener {
                if (mSlidingPaneLayout!!.isOpen) {
                    mSlidingPaneLayout!!.seslClosePane(animateDrawer)
                }else{
                    mSlidingPaneLayout!!.seslOpenPane(animateDrawer)
                }
            }
            SeslViewReflector.semSetHoverPopupType(
                navRailDrawerButton!!,
                getField_TYPE_NONE()
            )
        }

        mDrawerHeaderLayout = mDrawerPane.findViewById(R.id.header_layout)
        mDrawerItemsContainer = mDrawerPane.findViewById(R.id.drawer_items_container)
        mDrawerHeaderButton = mDrawerHeaderLayout!!.findViewById(R.id.drawer_header_button)
        mDrawerHeaderButtonBadgeView = mDrawerHeaderLayout!!.findViewById(R.id.drawer_header_button_badge)

        val cornerRadius = DEFAULT_DRAWER_RADIUS.dpToPx(resources)
        mSlidingPaneLayout!!.seslSetRoundedCornerOn(cornerRadius)
        mDrawerPane.apply{
            outlineProvider = DrawerOutlineProvider(cornerRadius)
            clipToOutline = true
        }

        setNavigationButtonTooltip(context.getText(R.string.oui_navigation_drawer))
        setDualDetailPane(false)
    }

    fun setDualDetailPane(enable: Boolean){
        if (isLargeScreenMode) {
            mSlidingPaneLayout!!.apply {
                if (enable){
                    if (mSplitDetailsPane == null){
                        mSplitDetailsPane = mSlideViewPane!!.findViewById<ViewStub>(R.id.viewstub_split_details_container)
                            .inflate() as FrameLayout
                    }
                    seslSetResizeChild()
                    mainDetailsPane.updateLayoutParams<LayoutParams> {width = 0}
                    mCoordinatorLayout.findViewById<LinearLayout>(R.id.tbl_main_content)
                        .updateLayoutParams<CoordinatorLayout.LayoutParams> {width = MATCH_PARENT }

                }else {
                    mainDetailsPane.updateLayoutParams<LayoutParams> {width = MATCH_PARENT }
                    if (!seslGetResizeOff()){
                        seslSetResizeChild(
                            mCoordinatorLayout.findViewById<LinearLayout>(R.id.tbl_main_content),
                            mBottomRoundedCorner,
                            mFooterParent
                        )
                        resizeSlideableView(mSlideOffset)
                    }
                    mSplitDetailsPane?.isGone = true
                }
            }
        }
    }

    var lockNavRailOnActionMode = false
        set(value) {
            if (value == field) return
            field = value
            if (isLargeScreenMode && isActionMode) {
                lockNavRail(value)
            }
        }

    var lockNavRailOnSearchMode = false
        set(value) {
            if (value == field) return
            field = value
            if (isLargeScreenMode && isSearchMode) {
                lockNavRail(value)
            }
        }

    private fun lockNavRail(lock: Boolean) {
        mSlidingPaneLayout!!.seslSetLock(lock)
        navRailDrawerButton!!.isEnabled = !lock
        mDrawerItemsContainer?.let { container ->
            fun disableViewGroup(viewGroup: ViewGroup) {
                for (i in 0 until viewGroup.childCount) {
                    val child = viewGroup.getChildAt(i)
                    child.isEnabled = !lock
                    if (child is ViewGroup) {
                        disableViewGroup(child)
                    }
                }
            }
           disableViewGroup(container)
        }
    }

    override fun isDrawerLocked(): Boolean  =  mSlidingPaneLayout?.seslGetLock() ?: super.isDrawerLocked()

    override fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int?) {
        if (isLargeScreenMode) {
            if (mDrawerHeaderButton != null) {
                mDrawerHeaderButton!!.apply {
                    setImageDrawable(icon)
                    imageTintList = ColorStateList.valueOf(
                        tint ?: ContextCompat.getColor(context, R.color.oui_drawerlayout_header_icon_tint))
                }
            } else {
                Log.e(TAG, "setHeaderButtonIcon: `drawer_header_button` id is not set in custom header view")
            }
        } else super.setHeaderButtonIcon(icon, tint)
    }

    /**
     * Set the tooltip of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     */
    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        mDrawerHeaderButton?.let { TooltipCompat.setTooltipText(it, tooltipText) }
            ?: run { super.setHeaderButtonTooltip(tooltipText) }
    }

    /**
     * Set the click listener of the drawer header button.
     * The header button is the button in the top right corner of the drawer panel.
     */
    override fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        mDrawerHeaderButton?.setOnClickListener(listener)
            ?: run { super.setHeaderButtonOnClickListener(listener) }
    }

    override fun setHeaderButtonBadge(badge: Badge) {
        if (isLargeScreenMode) {
            if (mDrawerHeaderButtonBadgeView != null) {
                if (mDrawerHeaderButtonBadge == badge) return
                mDrawerHeaderButtonBadge = badge
                updateBadgeView(mDrawerHeaderButtonBadgeView!!, badge)
            } else {
                Log.e(
                    TAG,
                    "setHeaderButtonBadge: `drawerSettingsIconBadge` id is not set in custom header view"
                )
            }
        }else{
            super.setHeaderButtonBadge(badge)
        }
    }

    override fun isOpen(): Boolean = if (isLargeScreenMode) mSlidingPaneLayout!!.isOpen else super.isOpen()

    /**
     * Open or close the drawer panel.
     *
     * @param open Set to `true` to open, `false` to close
     * @param animate (Optional) whether or not to animate the opening and closing. Default value is [animateDrawer].
     * @param ignoreOnNavRailMode (Optional) Don't apply when [isLargeScreenMode] is `true`. Default value is set to `false`.
     */
    fun setDrawerOpen(open: Boolean, animate: Boolean = animateDrawer, ignoreOnNavRailMode: Boolean = false) {
        if (isLargeScreenMode && ignoreOnNavRailMode) return
        setDrawerOpen(open, animate)
    }

    /**
     * Open or close the drawer panel.
     *
     * @param open Set to `true` to open, `false` to close
     * @param animate (Optional) whether or not to animate the opening and closing. Default value is [animateDrawer].
     */
    override fun setDrawerOpen(open: Boolean, animate: Boolean) {
        if (isLargeScreenMode){
            if (open) {
                mSlidingPaneLayout!!.seslOpenPane(animate)
            }else{
                mSlidingPaneLayout!!.seslClosePane(animate)
                if (isLayoutRTL) simulateTouch()
            }
        }else {
            super.setDrawerOpen(open, animate)
        }
    }

    //Workaround for drawer pane not closing
    //on RTL and when swiped from the left
    //unless this view or any of its child is touched.
    private fun simulateTouch(){
        val downTime = SystemClock.uptimeMillis()
        val eventTime = downTime + 10
        val location = intArrayOf(0,0)

        navRailDrawerButton!!.getLocationInWindow(location)
        val x = location[0].toFloat()
        val y = location[1].toFloat()

        var event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        dispatchTouchEvent(event)
        event.recycle()

        event = MotionEvent.obtain(downTime, eventTime + 10, MotionEvent.ACTION_UP, x, y, 0)
        dispatchTouchEvent(event)
        event.recycle()
    }

    val drawerOffset get() = mSlideOffset

    fun getNavRailSlideRange(): Int = mSlidingPaneLayout?.getSlideRange() ?: 0


    private inner class SlidingPaneSlideListener : SlidingPaneLayout.PanelSlideListener {
        override fun onPanelClosed(panel: View) {
            mSlideOffset = 0f
            dispatchDrawerStateChange()
        }

        override fun onPanelOpened(panel: View) {
            mSlideOffset = 1f
            dispatchDrawerStateChange()
        }

        override fun onPanelSlide(panel: View, slideOffset: Float) {
            mSlideOffset = slideOffset
            dispatchDrawerStateChange()
        }
    }

    private inner class DrawerOutlineProvider(@param:Px private val mCornerRadius: Int) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.apply {
                isLayoutRTL.let {
                    setRoundRect(
                        if (it) 0 else -mCornerRadius,
                        0,
                        if (it) view.width + mCornerRadius else view.width,
                        view.height,
                        mCornerRadius.toFloat()
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "NavDrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 16F
        private const val DRAWER_HEADER = 4
        private const val KEY_IS_DRAWER_OPENED = "dlkey1"
        private const val KEY_SUPERSTATE = "dlSuperState"
    }

}


