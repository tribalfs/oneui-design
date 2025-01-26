package dev.oneuiproject.oneui.layout.internal.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector.getField_TYPE_NONE
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.fitsSystemWindows
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.ktx.setEnableRecursive
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.internal.NavigationBadgeIcon
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.NavDrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.getDrawerStateUpdate
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.updateBadgeView
import dev.oneuiproject.oneui.layout.internal.util.DrawerOutlineProvider
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import androidx.slidingpanelayout.R as splR

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SemSlidingPaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SlidingPaneLayout(context, attrs, defStyle), DrawerLayoutInterface, NavButtonsHandler {

    private lateinit var mToolbar: Toolbar
    private lateinit var mDrawerPane: LinearLayout
    private lateinit var mHeaderView: View
    private var mDrawerHeaderButton: ImageButton? = null
    private lateinit var mDrawerContainer: FrameLayout
    private var mNavRailSlideViewContent: LinearLayout? = null
    private lateinit var mainDetailsPane: LinearLayout
    private var mDrawerHeaderLayout: View? = null
    private var mDrawerItemsContainer: FrameLayout? = null
    private var mDrawerHeaderButtonBadgeView: TextView? = null
    private var mSplitDetailsPane: LinearLayout? = null
    private var navDrawerButtonBadge: Badge = Badge.NONE
    private var headerButtonBadge: Badge = Badge.NONE
    private lateinit var mSlideViewPane: FrameLayout

    private val defaultDrawerTopMargin = resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_action_bar_top_padding)

    private val badgeIcon by lazy(LazyThreadSafetyMode.NONE) { NavigationBadgeIcon(context) }

    @JvmField
    internal var navRailDrawerButton: ImageButton? = null

    @JvmField
    internal var navRailDrawerButtonBadgeView: TextView? = null

    @Volatile
    private var mCurrentState: DrawerState = DrawerState.CLOSE

    @Volatile
    private var sSlideOffset = 0f

    private var drawerCornerRadius = -1
    private var isDualDetails = false
    private var drawerEnabled = true
    private var hideDrawerOnCollapse = false

    private val activity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity }

    init { setOverhangSize(DEFAULT_OVERHANG_SIZE) }

    override fun onConfigurationChanged(configuration: Configuration) {
        seslSetPendingAction(if (isOpen) PENDING_ACTION_EXPANDED else PENDING_ACTION_COLLAPSED)
        super.onConfigurationChanged(configuration)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        mToolbar = findViewById(R.id.toolbarlayout_main_toolbar)
        mSlideViewPane = findViewById(R.id.slideable_view)
        mDrawerPane = findViewById(R.id.drawer_panel)
        mNavRailSlideViewContent = findViewById(R.id.slide_contents)
        mainDetailsPane = mNavRailSlideViewContent!!.findViewById(R.id.tbl_main_content_root)

        addPanelSlideListener(SlidingPaneSlideListener())

        if (navRailDrawerButton == null) {
            navRailDrawerButton =
                mDrawerPane.findViewById<ImageButton>(R.id.navRailDrawerButton).apply {
                    isVisible = true
                    setOnClickListener {
                        if (isDrawerOpen) seslClosePane(true) else seslOpenPane(true)
                    }
                }

            navRailDrawerButtonBadgeView = mDrawerPane.findViewById(R.id.navRailDrawerButtonBadge)

            SeslViewReflector.semSetHoverPopupType(
                navRailDrawerButton!!,
                getField_TYPE_NONE()
            )
        }

        mDrawerHeaderLayout = mDrawerPane.findViewById(R.id.header_layout)
        mDrawerItemsContainer = mDrawerPane.findViewById(R.id.drawer_items_container)
        mDrawerHeaderButton = mDrawerHeaderLayout!!.findViewById(R.id.drawer_header_button)
        mDrawerHeaderButtonBadgeView =
            mDrawerHeaderLayout!!.findViewById(R.id.drawer_header_button_badge)

        setNavigationButtonTooltip(context.getText(R.string.oui_navigation_drawer))
    }

    override fun onAttachedToWindow() {
        //Apply defaults if custom not set
        if (drawerCornerRadius == -1) applyDrawerCornerRadius(drawerCornerRadius)
        if (!isDualDetails) configDetailsPane(false)

        super.onAttachedToWindow()
    }

    override fun open(animate: Boolean) = seslOpenPane(animate).also {
        if (isInEditMode) ensureLayoutPreview()
    }

    override fun close(animate: Boolean) {
        seslClosePane(animate)
        if (layoutDirection == LAYOUT_DIRECTION_RTL) simulateTouch()
    }

    private fun ensureLayoutPreview() {
        sSlideOffset = 1f
        if (!seslGetResizeOff()) {
            mNavRailSlideViewContent!!.updatePadding(
                right = mDrawerPane.layoutParams.width
                        - ((DEFAULT_OVERHANG_SIZE + 40) * context.dpToPxFactor + 0.5f).toInt()
            )
        }
    }

    internal fun setDualDetailPane(enable: Boolean) {
        if (isDualDetails == enable) return
        isDualDetails = enable
        configDetailsPane(enable)
    }

    private fun configDetailsPane(isDualDetails: Boolean){
        if (isDualDetails) {
            if (mSplitDetailsPane == null) {
                mSplitDetailsPane =
                    mSlideViewPane.findViewById<ViewStub>(R.id.viewstub_split_details_container)
                        .inflate() as LinearLayout
            }
            seslSetResizeChild()
            mainDetailsPane.updateLayoutParams<LinearLayout.LayoutParams> { width = 0 }
            findViewById<LinearLayout>(R.id.tbl_main_content_parent)
                .updateLayoutParams<CoordinatorLayout.LayoutParams> { width = MATCH_PARENT }

        } else {
            mainDetailsPane.updateLayoutParams<LinearLayout.LayoutParams> { width = MATCH_PARENT }
            if (!seslGetResizeOff()) {
                seslSetResizeChild(
                    findViewById<LinearLayout>(R.id.tbl_main_content_parent),
                    findViewById(R.id.tbl_bottom_corners),
                    findViewById(R.id.tbl_footer_parent)
                )
                resizeSlideableView(sSlideOffset)
            }
            mSplitDetailsPane?.isGone = true
        }
    }


    //Workaround for drawer pane not closing
    //on RTL and when swiped from the left
    //unless this view or any of its child is touched.
    private fun simulateTouch() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = downTime + 10
        val location = intArrayOf(0, 0)

        mDrawerPane.getLocationInWindow(location)
        val x = location[0].toFloat()
        val y = location[1].toFloat()

        var event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        dispatchTouchEvent(event)
        event.recycle()

        event = MotionEvent.obtain(downTime, eventTime + 10, MotionEvent.ACTION_UP, x, y, 0)
        dispatchTouchEvent(event)
        event.recycle()
    }

    override fun setDrawerCornerRadius(@Dimension dp: Float) {
        setDrawerCornerRadius(dp.dpToPx(resources))
    }

    /**
     * Set a custom radius for the drawer panel's edges.
     */
    override fun setDrawerCornerRadius(@Px px: Int) {
        if (drawerCornerRadius == px) return
        drawerCornerRadius = px
        applyDrawerCornerRadius(px)
    }

    private fun applyDrawerCornerRadius(@Px px: Int){
        val cornerRadius = if (px == -1) DEFAULT_DRAWER_RADIUS.dpToPx(resources) else px
        (mDrawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            it.cornerRadius = cornerRadius
        } ?: run {
            mDrawerPane.outlineProvider = DrawerOutlineProvider(cornerRadius)
            mDrawerPane.clipToOutline = true
        }
        seslSetRoundedCornerOn(cornerRadius)
    }

    override fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams) {
        (mDrawerHeaderLayout as ViewGroup).apply {
            removeView(mDrawerHeaderButton)
            removeView(mDrawerHeaderButtonBadgeView)
        }
        mDrawerPane.addView(headerView, 1, params)
        mDrawerHeaderButton = headerView.findViewById(R.id.drawer_header_button)
        mDrawerHeaderButtonBadgeView = headerView.findViewById(R.id.drawer_header_button_badge)
        navRailDrawerButton = headerView.findViewById<ImageButton>(R.id.navRailDrawerButton).apply {
            isVisible = true
        }
        navRailDrawerButtonBadgeView = headerView.findViewById(R.id.navRailDrawerButtonBadge)

        if (mDrawerHeaderButton == null) {
            Log.e(TAG, "`drawer_header_button` id is missing or is not an ImageButton")
        }
        if (mDrawerHeaderButtonBadgeView == null) {
            Log.e(TAG, "`drawer_header_button_badge` id is missing or is not a TextView")
        }
    }

    override fun addDrawerContent(child: View, params: ViewGroup.LayoutParams) =
        mDrawerItemsContainer!!.addView(child, params)

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        navRailDrawerButton!!.apply {
            contentDescription = tooltipText
            semSetToolTipText(tooltipText)
        }
    }

    override fun getDrawerPane(): View = mDrawerPane

    override fun getContentPane(): View = mSlideViewPane

    private val drawerItemsDisabler = Runnable { mDrawerItemsContainer?.setEnableRecursive(!isLocked) }

    override var isLocked: Boolean
        get() = seslGetLock()
        set(value) {
            if (seslGetLock() == value) return
            seslSetLock(value || !drawerEnabled)
            navRailDrawerButton!!.isEnabled = !value
            removeCallbacks(drawerItemsDisabler)
            postDelayed(drawerItemsDisabler, 50)
        }

    override val isDrawerOpen: Boolean get() = mCurrentState == DrawerState.OPEN

    override val isDrawerOpenOrIsOpening: Boolean
        get() = mCurrentState == DrawerState.OPEN || mCurrentState == DrawerState.OPENING

    private var mDrawerStateListener: ((state: DrawerState) -> Unit)? = null

    override fun setOnDrawerStateChangedListener(listener: ((DrawerState) -> Unit)?) {
        mDrawerStateListener = listener
    }

    override fun getDrawerSlideOffset(): Float = sSlideOffset

    private fun dispatchDrawerStateChange(newSlideOffset: Float) {
        if (sSlideOffset == newSlideOffset) return
        val newState = getDrawerStateUpdate(sSlideOffset, newSlideOffset)
        sSlideOffset = newSlideOffset

        if (newState != mCurrentState) {
            mCurrentState = newState
            mDrawerStateListener?.invoke(newState)
        }

        if (navDrawerButtonBadge == Badge.NONE && headerButtonBadge != Badge.NONE) {
            updateNavBadgeScale()
        }

        if (drawerEnabled && hideDrawerOnCollapse) {
            mToolbar.apply {
                if (navigationIcon == null){
                    navigationIcon = createBadgeNavIcon()
                }
                navigationIcon?.apply {
                    (32.dpToPx(resources) * sSlideOffset).let{
                        titleTextView?.translationX = -it
                        subtitleTextView?.translationX = -it
                    }
                    (255 * (1f - (sSlideOffset * 15f)).coerceIn(0f, 1f)).toInt().let {
                        alpha = it
                        navRailDrawerButton?.alpha =  1f - it
                        navRailDrawerButtonBadgeView?.alpha = 1f - it
                    }
                }
                if (newSlideOffset == 1f){
                    badgeIcon.setBadge(if (navDrawerButtonBadge != Badge.NONE) navDrawerButtonBadge else headerButtonBadge)
                }
            }
        }
    }

    private fun createBadgeNavIcon() =
        LayerDrawable(arrayOf(navRailDrawerButton!!.drawable.constantState!!.newDrawable(), badgeIcon)).apply {
            setId(0, R.id.nav_button_icon_layer_id)
        }

    private fun updateNavBadgeScale() {
        navRailDrawerButtonBadgeView!!.apply {
            val scale = 1f - sSlideOffset
            scaleX = scale
            scaleY = scale
        }
    }

    internal fun updateContentMinSidePadding(@Px padding: Int) =
        mSlideViewPane.updatePadding(left = padding, right = padding)

    /**Note: This doesn't check for the current value*/
    override var showNavigationButtonAsBack = false
        set(value) {
            field = value
            updateNavButton()
        }

    @SuppressLint("VisibleForTests")
    private fun updateNavButton() {
        this.activity?.apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(showNavigationButtonAsBack)
            if (showNavigationButtonAsBack) {
                mToolbar.apply {
                    setNavigationOnClickListener {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            } else {
                mToolbar.apply {
                    titleTextView?.translationX = 0f
                    subtitleTextView?.translationX = 0f
                }
                if (drawerEnabled && hideDrawerOnCollapse) {
                    mToolbar.apply {
                        navigationContentDescription = resources.getText(R.string.oui_navigation_drawer)
                        setNavigationOnClickListener{
                            navigationIcon = null
                            open(true)
                        }
                        if (mCurrentState == DrawerState.OPEN) {
                            navigationIcon = null
                        } else {
                            if (navigationIcon == null) {
                                navigationIcon = createBadgeNavIcon()
                            }
                        }
                        badgeIcon.setBadge(if (navDrawerButtonBadge != Badge.NONE) navDrawerButtonBadge else headerButtonBadge)
                    }
                }else{
                    navRailDrawerButton?.alpha = 1f
                    mToolbar.apply {
                        navigationIcon = null
                        navigationContentDescription = null
                        setNavigationOnClickListener(null)
                    }
                }
            }
        }
    }

    override fun setNavigationButtonBadge(badge: Badge) {
        if (navDrawerButtonBadge == badge) return
        navDrawerButtonBadge = badge
        navRailDrawerButtonBadgeView!!.apply {
            if (badge != Badge.NONE) {
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
                updateBadgeView(badge)
            }else if (!isOpen && headerButtonBadge != Badge.NONE){
                updateBadgeView(headerButtonBadge)
                updateNavBadgeScale()
            }
        }
    }

    override var showNavigationButton: Boolean = true

    override fun setNavigationButtonOnClickListener(listener: OnClickListener?) = Unit

    override fun setNavigationButtonIcon(icon: Drawable?) {
        navRailDrawerButton!!.apply {
            setImageDrawable(icon)
            invalidate()
        }
    }

    override fun setHeaderButtonIcon(icon: Drawable?, tint: Int?) {
        mDrawerHeaderButton?.apply {
            setImageDrawable(icon)
            imageTintList = ColorStateList.valueOf(
                tint ?: ContextCompat.getColor(context, R.color.oui_drawerlayout_header_icon_tint))
        } ?: Log.e(TAG, "setHeaderButtonIcon: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        mDrawerHeaderButton?.semSetToolTipText(tooltipText)
            ?: Log.e(TAG, "setHeaderButtonTooltip: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        mDrawerHeaderButton?.setOnClickListener(listener)
            ?: Log.e(TAG, "setHeaderButtonOnClickListener: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonBadge(badge: Badge) {
        if (mDrawerHeaderButtonBadgeView != null) {
            if (headerButtonBadge == badge) return
            headerButtonBadge = badge
            mDrawerHeaderButtonBadgeView!!.updateBadgeView(badge)
            if (navDrawerButtonBadge == Badge.NONE) {
                navRailDrawerButtonBadgeView!!.updateBadgeView(badge)
                updateNavBadgeScale()
                if (drawerEnabled && hideDrawerOnCollapse){
                    badgeIcon.setBadge(badge)
                }
            }
        } else {
            Log.e(TAG, "setHeaderButtonBadge: `drawer_header_button_badge` id is not set in custom header view")
        }
    }

    private var _backHandler: DrawerLayoutBackHandler<OneUIDrawerLayout>? = null

    override fun getOrCreateBackHandler(drawerLayout: OneUIDrawerLayout) : DrawerLayoutBackHandler<OneUIDrawerLayout> {
        return _backHandler
            ?: DrawerLayoutBackHandler(drawerLayout, NavDrawerBackAnimator(this@SemSlidingPaneLayout))
                .also { _backHandler = it }
    }

    override fun applyWindowInsets(insets: WindowInsetsCompat, isImmersiveActive: Boolean){
        val activity = activity ?: return
        val imeInsetBottom = insets.getInsets(ime()).bottom
        val systemBarsInsets = insets.getInsets(systemBars())

        if (isImmersiveActive) {
            setPadding(systemBarsInsets.left, 0, systemBarsInsets.right, imeInsetBottom)
            seslSetDrawerMarginTop(systemBarsInsets.top + defaultDrawerTopMargin)
            seslSetDrawerMarginBottom(maxOf(imeInsetBottom, systemBarsInsets.bottom))
        } else {
            if (activity.fitsSystemWindows) {
                setPadding(0, 0, 0, imeInsetBottom)
            }else{
                setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right,
                    maxOf(imeInsetBottom, systemBarsInsets.bottom))
            }
            seslSetDrawerMarginTop(defaultDrawerTopMargin)
            seslSetDrawerMarginBottom(0)
        }
    }

    private inner class SlidingPaneSlideListener : PanelSlideListener {
        override fun onPanelClosed(panel: View) = dispatchDrawerStateChange(0f)
        override fun onPanelOpened(panel: View) = dispatchDrawerStateChange(1f)
        override fun onPanelSlide(panel: View, slideOffset: Float) =
            dispatchDrawerStateChange(slideOffset)
    }

    fun isDrawerEnabled() = drawerEnabled

    @JvmOverloads
    fun setDrawerEnabled(enabled: Boolean, hideOnCollapsed: Boolean = false, animate: Boolean = true) {
        if (this.drawerEnabled == enabled && this.hideDrawerOnCollapse == hideOnCollapsed) return
        this.drawerEnabled = enabled
        this.hideDrawerOnCollapse = hideOnCollapsed

        mSlideViewPane.clearAnimation()

        when {
            drawerEnabled && !hideOnCollapsed -> {
                val defaultMargin =
                    context.resources.getDimensionPixelSize(splR.dimen.navigation_rail_margin_start)
                if (animate) {
                    createDrawerModeAnimator(mSlideViewPane.marginStart, defaultMargin).apply {
                        addListener(
                            onStart = { mDrawerPane.isVisible = true },
                            onEnd = { updateNavButton() }
                        )
                        start()
                    }
                }else{
                    mDrawerPane.isVisible = true
                    updateSlideViewPaneWidth(defaultMargin)
                    updateNavButton()
                }
            }

            drawerEnabled && hideOnCollapsed -> {
                val startMargin = mSlideViewPane.marginStart
                if (startMargin == 0) {
                    seslSetResizeOff(seslGetResizeOff())
                    return
                }

                if (animate) {
                    createDrawerModeAnimator(startMargin, 0).apply {
                        doOnEnd { updateNavButton() }
                        start()
                    }
                }else{
                    mDrawerPane.isVisible = true
                    updateSlideViewPaneWidth(0)
                    updateNavButton()
                }
            }

            else -> { //drawerEnabled == false
                close(false)
                val startMargin = mSlideViewPane.marginStart
                if (startMargin == 0) return

                if (animate) {
                    createDrawerModeAnimator(startMargin, 0).apply {
                        doOnEnd { updateNavButton(); mDrawerPane.isInvisible = true }
                        start()
                    }
                }else{
                    mDrawerPane.isInvisible = true
                    updateSlideViewPaneWidth(0)
                    updateNavButton()
                }
            }
        }
    }

    private fun createDrawerModeAnimator(startValue: Int, endValue: Int): ValueAnimator {
        return ValueAnimator.ofInt(startValue, endValue).apply {
            duration = 350
            addUpdateListener { updateSlideViewPaneWidth(it.animatedValue as Int) }
        }
    }

    private fun updateSlideViewPaneWidth(width: Int){
        mSlideViewPane.updateLayoutParams<LayoutParams> {
            marginStart = width
            seslSetResizeOff(seslGetResizeOff())
        }
    }

    companion object {
        private const val TAG = "SemSlidingPaneLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        const val DEFAULT_OVERHANG_SIZE = 32 // dp
    }

}