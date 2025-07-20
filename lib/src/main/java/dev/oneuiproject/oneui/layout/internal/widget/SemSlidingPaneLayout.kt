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
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
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

    private lateinit var toolbar: Toolbar
    private lateinit var _drawerPane: LinearLayout
    private var drawerHeaderButton: ImageButton? = null
    private var navRailSlideViewContent: LinearLayout? = null
    private lateinit var mainDetailsPane: LinearLayout
    private var drawerHeaderLayout: View? = null
    private var drawerItemsContainer: FrameLayout? = null
    private var drawerHeaderButtonBadgeView: TextView? = null
    private var splitDetailsPane: LinearLayout? = null
    private var navDrawerButtonBadge: Badge = Badge.NONE
    private var headerButtonBadge: Badge = Badge.NONE
    private lateinit var slideViewPane: FrameLayout

    private val defaultDrawerTopMargin = resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_action_bar_top_padding)

    private val badgeIcon by lazy(LazyThreadSafetyMode.NONE) { NavigationBadgeIcon(context) }

    @JvmField
    internal var navRailDrawerButton: ImageButton? = null

    @JvmField
    internal var navRailDrawerButtonBadgeView: TextView? = null

    @Volatile
    private var currentDrawerState: DrawerState = DrawerState.CLOSE

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

        toolbar = findViewById(R.id.toolbarlayout_main_toolbar)
        slideViewPane = findViewById(R.id.slideable_view)
        _drawerPane = findViewById(R.id.drawer_panel)
        navRailSlideViewContent = findViewById(R.id.slide_contents)
        mainDetailsPane = navRailSlideViewContent!!.findViewById(R.id.tbl_main_content_root)

        addPanelSlideListener(SlidingPaneSlideListener())

        if (navRailDrawerButton == null) {
            navRailDrawerButton =
                _drawerPane.findViewById<ImageButton>(R.id.navRailDrawerButton).apply {
                    isVisible = true
                    setOnClickListener {
                        if (isDrawerOpen) seslClosePane(true) else seslOpenPane(true)
                    }
                }

            navRailDrawerButtonBadgeView = _drawerPane.findViewById(R.id.navRailDrawerButtonBadge)

            SeslViewReflector.semSetHoverPopupType(
                navRailDrawerButton!!,
                getField_TYPE_NONE()
            )
        }

        drawerHeaderLayout = _drawerPane.findViewById(R.id.header_layout)
        drawerItemsContainer = _drawerPane.findViewById(R.id.drawer_items_container)
        drawerHeaderButton = drawerHeaderLayout!!.findViewById(R.id.oui_des_drawer_header_button)
        drawerHeaderButtonBadgeView =
            drawerHeaderLayout!!.findViewById(R.id.oui_des_drawer_header_button_badge)

        setNavigationButtonTooltip(context.getText(R.string.oui_des_navigation_drawer))
    }

    override fun onAttachedToWindow() {
        applyDrawerCornerRadius(drawerCornerRadius)
        configDetailsPane(false)
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
            navRailSlideViewContent!!.updatePadding(
                right = _drawerPane.layoutParams.width
                        - ((DEFAULT_OVERHANG_SIZE + 40) * context.dpToPxFactor + 0.5f).toInt()
            )
        }
    }

    internal fun setDualDetailPane(enable: Boolean) {
        if (isDualDetails == enable) return
        isDualDetails = enable
        if (isAttachedToWindow) configDetailsPane(enable)
    }

    private fun configDetailsPane(isDualDetails: Boolean){
        if (isDualDetails) {
            if (splitDetailsPane == null) {
                splitDetailsPane =
                    slideViewPane.findViewById<ViewStub>(R.id.viewstub_split_details_container)
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
            splitDetailsPane?.isGone = true
        }
    }


    //Workaround for drawer pane not closing
    //on RTL and when swiped from the left
    //unless this view or any of its child is touched.
    private fun simulateTouch() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = downTime + 10
        val location = intArrayOf(0, 0)

        _drawerPane.getLocationInWindow(location)
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
        if (isAttachedToWindow) applyDrawerCornerRadius(px)
    }

    private fun applyDrawerCornerRadius(@Px px: Int){
        val cornerRadius = if (px == -1) DEFAULT_DRAWER_RADIUS.dpToPx(resources) else px
        (_drawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            it.cornerRadius = cornerRadius
        } ?: run {
            _drawerPane.outlineProvider = DrawerOutlineProvider(cornerRadius)
            _drawerPane.clipToOutline = true
        }
        seslSetRoundedCornerOn(cornerRadius)
    }

    override fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams) {
        (drawerHeaderLayout as ViewGroup).apply {
            removeView(drawerHeaderButton)
            removeView(drawerHeaderButtonBadgeView)
        }
        _drawerPane.addView(headerView, 1, params)
        drawerHeaderButton = headerView.findViewById(R.id.oui_des_drawer_header_button)
        drawerHeaderButtonBadgeView = headerView.findViewById(R.id.oui_des_drawer_header_button_badge)
        navRailDrawerButton = headerView.findViewById<ImageButton>(R.id.navRailDrawerButton).apply {
            isVisible = true
        }
        navRailDrawerButtonBadgeView = headerView.findViewById(R.id.navRailDrawerButtonBadge)

        if (drawerHeaderButton == null) {
            Log.e(TAG, "`drawer_header_button` id is missing or is not an ImageButton")
        }
        if (drawerHeaderButtonBadgeView == null) {
            Log.e(TAG, "`drawer_header_button_badge` id is missing or is not a TextView")
        }
    }

    override fun addDrawerContent(child: View, params: ViewGroup.LayoutParams) =
        drawerItemsContainer!!.addView(child, params)

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        navRailDrawerButton!!.apply {
            contentDescription = tooltipText
            semSetToolTipText(tooltipText)
        }
    }

    override fun getDrawerPane(): View = _drawerPane

    override fun getContentPane(): View = slideViewPane

    override var isLocked: Boolean
        get() = lockMode == LOCK_MODE_LOCKED
        set(value) {
            lockMode = if (value || !drawerEnabled) LOCK_MODE_LOCKED else LOCK_MODE_UNLOCKED
            navRailDrawerButton!!.isEnabled = !value
        }

    override val isDrawerOpen: Boolean get() = currentDrawerState == DrawerState.OPEN

    override val isDrawerOpenOrIsOpening: Boolean
        get() = currentDrawerState == DrawerState.OPEN || currentDrawerState == DrawerState.OPENING

    private var drawerStateListener: ((state: DrawerState) -> Unit)? = null

    override fun setOnDrawerStateChangedListener(listener: ((DrawerState) -> Unit)?) {
        drawerStateListener = listener
    }

    override fun getDrawerSlideOffset(): Float = sSlideOffset

    private fun dispatchDrawerStateChange(newSlideOffset: Float) {
        if (sSlideOffset == newSlideOffset) return
        val newState = getDrawerStateUpdate(sSlideOffset, newSlideOffset)
        sSlideOffset = newSlideOffset

        if (newState != currentDrawerState) {
            currentDrawerState = newState
            drawerStateListener?.invoke(newState)
        }

        if (navDrawerButtonBadge == Badge.NONE && headerButtonBadge != Badge.NONE) {
            updateNavBadgeScale()
        }

        if (drawerEnabled && hideDrawerOnCollapse) {
            toolbar.apply {
                if (navigationIcon == null){
                    navigationIcon = createBadgeNavIcon()
                }
                navigationIcon?.apply {
                    @SuppressLint("VisibleForTests")
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
        slideViewPane.updatePadding(left = padding, right = padding)

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
                toolbar.apply {
                    setNavigationOnClickListener {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            } else {
                toolbar.apply {
                    titleTextView?.translationX = 0f
                    subtitleTextView?.translationX = 0f
                }
                if (drawerEnabled && hideDrawerOnCollapse) {
                    toolbar.apply {
                        navigationContentDescription = resources.getText(R.string.oui_des_navigation_drawer)
                        setNavigationOnClickListener{
                            navigationIcon = null
                            open(true)
                        }
                        if (currentDrawerState == DrawerState.OPEN) {
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
                    toolbar.apply {
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
        drawerHeaderButton?.apply {
            setImageDrawable(icon)
            imageTintList = ColorStateList.valueOf(
                tint ?: ContextCompat.getColor(context, R.color.oui_des_drawerlayout_header_icon_tint))
        } ?: Log.e(TAG, "setHeaderButtonIcon: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        drawerHeaderButton?.semSetToolTipText(tooltipText)
            ?: Log.e(TAG, "setHeaderButtonTooltip: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        drawerHeaderButton?.setOnClickListener(listener)
            ?: Log.e(TAG, "setHeaderButtonOnClickListener: `drawer_header_button` id is not set in custom header view")
    }

    override fun setHeaderButtonBadge(badge: Badge) {
        if (drawerHeaderButtonBadgeView != null) {
            if (headerButtonBadge == badge) return
            headerButtonBadge = badge
            drawerHeaderButtonBadgeView!!.updateBadgeView(badge)
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
        val basePadding = insets.getInsets(systemBars() or displayCutout())

        if (isImmersiveActive) {
            updateLayoutParams<MarginLayoutParams> {
                leftMargin = basePadding.left
                rightMargin = basePadding.right
                bottomMargin = 0
                topMargin = 0
            }
            seslSetDrawerMarginTop(basePadding.top + defaultDrawerTopMargin)
            seslSetDrawerMarginBottom(basePadding.bottom)
        } else {
            if (activity.fitsSystemWindows) {
                updateLayoutParams<MarginLayoutParams> {
                    leftMargin = 0
                    rightMargin = 0
                    bottomMargin = imeInsetBottom
                    topMargin = 0
                }
            }else{
                updateLayoutParams<MarginLayoutParams> {
                    leftMargin = basePadding.left
                    rightMargin = basePadding.right
                    bottomMargin = maxOf(imeInsetBottom, basePadding.bottom)
                    topMargin = basePadding.top
                }
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

    /**
     *
     * @return true if the drawer is enabled, false otherwise.
     */
    fun isDrawerEnabled() = drawerEnabled

    /**
     * Enable or disable the drawer.
     * When the drawer is disabled, the navigation button will be hidden
     * and the drawer pane will be hidden and will not be accessible.
     *
     * @param drawerEnabled True to enable the drawer, false to disable it.
     * @param hideOnCollapsed True to fully hide the the drawer pane when it is collapsed, false otherwise.
     */
    @JvmOverloads
    fun setDrawerEnabled(enabled: Boolean, hideOnCollapsed: Boolean = false, animate: Boolean = true) {
        if (this.drawerEnabled == enabled && this.hideDrawerOnCollapse == hideOnCollapsed) return
        this.drawerEnabled = enabled
        this.hideDrawerOnCollapse = hideOnCollapsed

        slideViewPane.clearAnimation()

        when {
            drawerEnabled && !hideOnCollapsed -> {
                val defaultMargin =
                    context.resources.getDimensionPixelSize(splR.dimen.navigation_rail_margin_start)
                if (animate) {
                    createDrawerModeAnimator(slideViewPane.marginStart, defaultMargin).apply {
                        addListener(
                            onStart = { _drawerPane.isVisible = true },
                            onEnd = { updateNavButton() }
                        )
                        start()
                    }
                }else{
                    _drawerPane.isVisible = true
                    updateSlideViewPaneWidth(defaultMargin)
                    updateNavButton()
                }
            }

            drawerEnabled && hideOnCollapsed -> {
                val startMargin = slideViewPane.marginStart
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
                    _drawerPane.isVisible = true
                    updateSlideViewPaneWidth(0)
                    updateNavButton()
                }
            }

            else -> { //drawerEnabled == false
                close(false)
                val startMargin = slideViewPane.marginStart
                if (startMargin == 0) return

                if (animate) {
                    createDrawerModeAnimator(startMargin, 0).apply {
                        doOnEnd { updateNavButton(); _drawerPane.isInvisible = true }
                        start()
                    }
                }else{
                    _drawerPane.isInvisible = true
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
        slideViewPane.updateLayoutParams<LayoutParams> {
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