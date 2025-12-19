package dev.oneuiproject.oneui.layout.internal.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.fitsSystemWindows
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.ktx.ifNegativeOrZero
import dev.oneuiproject.oneui.ktx.semSetRoundedCorners
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.Companion.DEFAULT_DRAWER_WIDTH_PROVIDER
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerWidthProvider
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.getDrawerStateUpdate
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.updateBadgeView
import dev.oneuiproject.oneui.layout.internal.util.DrawerOutlineProvider
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import androidx.appcompat.R as appcompatR

internal typealias OneUIDrawerLayout = dev.oneuiproject.oneui.layout.DrawerLayout

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SemDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): DrawerLayout(context, attrs, defStyle), DrawerLayoutInterface, NavButtonsHandler {

    private val drawerListener = DrawerListener()
    private var scrimAlpha = 0f
    private var systemBarsColor = context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)?.data!!
    private val hsv = FloatArray(3)
    //This changes with size and orientation
    @Suppress("PrivateResource")
    private val defaultActionBarTopPadding get() =
        resources.getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_top_padding)

    private lateinit var drawerPane: LinearLayout
    private lateinit var slideViewPane: FrameLayout
    private lateinit var headerView: View
    private var drawerHeaderButton: ImageButton? = null
    private var drawerHeaderBadgeView: TextView? = null

    private var navDrawerButtonBadge: Badge = Badge.NONE
    private var headerButtonBadge: Badge = Badge.NONE

    private lateinit var drawerItemsContainer: FrameLayout

    private lateinit var translationView: View
    private var handleInsets = false
    private var _showNavButtonAsBack = false
    private var _showNavigationButton = true

    private lateinit var navButtonsHandlerDelegate: NavButtonsHandler

    private val activity by lazy(LazyThreadSafetyMode.NONE) {  context.appCompatActivity }

    private val updateHandler = Handler(Looper.getMainLooper())
    private var lastScreenWidthDp: Int = 0

    private var drawerWidthProvider: DrawerWidthProvider = DEFAULT_DRAWER_WIDTH_PROVIDER

    init {
        ContextCompat.getColor(context, R.color.oui_des_drawerlayout_drawer_dim_color).let {
            setScrimColor(it)
            scrimAlpha = ((it shr 24) and 0xFF) / 255f
        }

        drawerElevation = 0f
        setupViews()
        setDrawerCornerRadius(DEFAULT_DRAWER_RADIUS)

        navButtonsHandlerDelegate = ToolbarLayoutButtonsHandler(findViewById(R.id.toolbarlayout_main_toolbar))
        navButtonsHandlerDelegate.setNavigationButtonOnClickListener{ openDrawer(drawerPane, true) }
        addDrawerListener(drawerListener)

        if (!isInEditMode) {
            activity!!.window.decorView.semSetRoundedCorners(ROUNDED_CORNER_NONE)
        }
    }

    private fun setupViews() {
        slideViewPane = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        drawerPane = inflate(context,
            R.layout.oui_des_layout_drawer_panel,
            null
        ).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START
                topMargin = context.resources
                    .getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_top_padding)
            }
        } as LinearLayout

        addView(slideViewPane, 0)
        addView(drawerPane, 1)

        val details = inflate(context,
            R.layout.oui_des_layout_drawerlayout_details,
            null
        ).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        } as LinearLayout

        slideViewPane.addView(details, 0)

        headerView = drawerPane.findViewById(R.id.header_layout)
        drawerItemsContainer = drawerPane.findViewById(R.id.drawer_items_container)

        drawerHeaderButton = headerView.findViewById(R.id.oui_des_drawer_header_button)
        drawerHeaderBadgeView = headerView.findViewById(R.id.oui_des_drawer_header_button_badge)

        translationView = findViewById(R.id.drawer_custom_translation) ?: slideViewPane
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateNavBadgeVisibility()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDrawerWidthAndState()
        navButtonsHandlerDelegate.apply {
            showNavigationButton = _showNavigationButton
            showNavigationButtonAsBack = _showNavButtonAsBack
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        updateDrawerWidthAndState()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldw == w || !isAttachedToWindow) return
        updateDrawerWidthAndState()
    }

    private fun updateDrawerWidthAndState(){
        updateDrawerWidth()
        if (isOpen) {
            removeCallbacks(ensureDrawerOpenState)
            postDelayed(ensureDrawerOpenState, 50)
        }
    }

    private val ensureDrawerOpenState = Runnable {
        if (isOpen) {
            openDrawer(drawerPane, false)
            updateContentTranslation(drawerPane, 1f)
        }
    }

    private fun updateDrawerWidth() {
        val drawerWidth = drawerWidthProvider.getWidth(
            resources,
            isLargeScreenMode = false,
            isMultiWindow = false
        )

        if (drawerPane.layoutParams.width != drawerWidth) {
            drawerPane.updateLayoutParams { width = drawerWidth }
        }

        if (isInEditMode && isOpen){
            ensureOpenDrawerPreview()
        }
    }

    override fun setDrawerWidthProvider(drawerWidthProvider: DrawerWidthProvider) {
        // For functional interfaces (lambdas), equality is by reference, not by logic.
        if (this.drawerWidthProvider === drawerWidthProvider) return
        this.drawerWidthProvider = drawerWidthProvider
        updateDrawerWidth()
    }

    private fun ensureOpenDrawerPreview(){
        val width = drawerPane.width.ifNegativeOrZero { 356.dpToPx(resources) }
        drawerPane.updateLayoutParams { this.width = width }
        slideViewPane.translationX = width * if (isLayoutRTL) -1f else 1f
    }

    override fun open(animate: Boolean) = openDrawer(drawerPane, animate)

    override fun close(animate: Boolean) = closeDrawer(drawerPane, animate)

    override fun close() = closeDrawer(drawerPane, true)

    override fun setDrawerCornerRadius(@Dimension dp: Float) {
        setDrawerCornerRadius(dp.dpToPx(resources))
    }

    /**
     * Set a custom radius for the drawer panel's edges.
     * Set to -1 to reset the default.
     */
    override fun setDrawerCornerRadius(@Px px: Int) {
        val targetRadius = if (px == -1) DEFAULT_DRAWER_RADIUS.dpToPx(resources) else px
        (drawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            if (it.cornerRadius != targetRadius) {
                it.cornerRadius = targetRadius
                drawerPane.invalidateOutline()
            }
        } ?: run {
            drawerPane.outlineProvider = DrawerOutlineProvider(targetRadius)
            drawerPane.clipToOutline = true
        }
    }

    override fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams){
        drawerPane.removeView(this@SemDrawerLayout.headerView)
        drawerHeaderButton = null
        drawerHeaderBadgeView = null
        drawerPane.addView(headerView, 0, params)
        this@SemDrawerLayout.headerView = drawerPane.getChildAt(0)
    }

    override fun addDrawerContent(child: View, params: ViewGroup.LayoutParams){
        drawerItemsContainer.addView(child, params)
    }

    private var drawerStateListener: ((state: DrawerState)-> Unit)? = null

    override fun setOnDrawerStateChangedListener(listener: ((DrawerState) -> Unit)?) {
        drawerStateListener = listener
    }

    override fun getDrawerPane(): View = drawerPane

    override fun getContentPane(): View = slideViewPane

    private fun updateContentTranslation(drawerView: View, slideOffset: Float){
        val transX = drawerView.width * slideOffset
        translationView.translationX = transX * if (isLayoutRTL) -1f else 1f
    }

    private fun dispatchDrawerStateChange(newSlideOffset: Float) {
        if (sSlideOffset == newSlideOffset) return
        val newState = getDrawerStateUpdate(sSlideOffset, newSlideOffset)
        sSlideOffset = newSlideOffset

        if (!isInEditMode) {
            if (systemBarsColor != -1) {
                Color.colorToHSV(systemBarsColor, hsv)
                hsv[2] *= 1f - (newSlideOffset * scrimAlpha)
                (parent as ViewGroup).setBackgroundColor(Color.HSVToColor(hsv))
            }
        }

        if (newState != sCurrentState) {
            sCurrentState = newState
            drawerStateListener?.invoke(newState)

            updateNavBadgeVisibility()
        }
    }

    private fun updateNavBadgeVisibility() {
        if (sCurrentState != DrawerState.CLOSE){
            navButtonsHandlerDelegate.setNavigationButtonBadge(Badge.NONE)
        }else {
            if (navDrawerButtonBadge != Badge.NONE) {
                navButtonsHandlerDelegate.setNavigationButtonBadge(navDrawerButtonBadge)
            }else {
                navButtonsHandlerDelegate.setNavigationButtonBadge(headerButtonBadge)
            }
        }
    }

    private inner class DrawerListener : SimpleDrawerListener() {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            super.onDrawerSlide(drawerView, slideOffset)
            dispatchDrawerStateChange(slideOffset)
            updateContentTranslation(drawerView, slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {
            super.onDrawerOpened(drawerView)
            dispatchDrawerStateChange(1f)
        }

        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            dispatchDrawerStateChange(0f)
        }
    }

    private inline val isLayoutRTL get() = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

    override var isLocked: Boolean
        set(lock){
            if (lock){
                setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)
            }else{
                setDrawerLockMode(LOCK_MODE_UNLOCKED)
            }
        }
        get() = getDrawerLockMode(Gravity.LEFT) != LOCK_MODE_UNLOCKED

    override val isDrawerOpen: Boolean get() = sCurrentState == DrawerState.OPEN

    override val isDrawerOpenOrIsOpening: Boolean
        get() = sCurrentState == DrawerState.OPEN || sCurrentState == DrawerState.OPENING


    override var showNavigationButtonAsBack: Boolean
        get() = _showNavButtonAsBack
        set(value) {
            if (_showNavButtonAsBack == value) return
            _showNavButtonAsBack = value
            if (isAttachedToWindow) {
                navButtonsHandlerDelegate.showNavigationButtonAsBack = value
            }
        }

    override var showNavigationButton: Boolean
        get() = _showNavigationButton
        set(value) {
            if (_showNavigationButton == value) return
            _showNavigationButton = value
            if (isAttachedToWindow) {
                navButtonsHandlerDelegate.showNavigationButton = value
            }
        }

    override fun setNavigationButtonOnClickListener(listener: OnClickListener?) = Unit

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) =
        navButtonsHandlerDelegate.setNavigationButtonTooltip(tooltipText)

    override fun setNavigationButtonBadge(badge: Badge) {
        if (navDrawerButtonBadge == badge) return
        navDrawerButtonBadge = badge
        if (badge != Badge.NONE) {
            navButtonsHandlerDelegate.setNavigationButtonBadge(badge)
        } else if (!isOpen && headerButtonBadge != Badge.NONE){
            navButtonsHandlerDelegate.setNavigationButtonBadge(headerButtonBadge)
        }
        updateNavBadgeVisibility()
    }

    override fun setNavigationButtonIcon(icon: Drawable?) =
        navButtonsHandlerDelegate.setNavigationButtonIcon(icon)

    override fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int?) {
        drawerHeaderButton?.apply {
            setImageDrawable(icon)
            imageTintList = ColorStateList.valueOf(tint
                ?: ContextCompat.getColor(context, R.color.oui_des_drawerlayout_header_icon_tint))
            headerView.isVisible = icon != null
        } ?: Log.e(TAG, "setHeaderButtonIcon: this method can be used " +
                "only with the default header view")
    }

    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        drawerHeaderButton?.semSetToolTipText(tooltipText)
            ?: Log.e(TAG, "setDrawerButtonTooltip: this method can be used " +
                    "only with the default header view")
    }

    override fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        drawerHeaderButton?.setOnClickListener(listener)
            ?: Log.e(TAG, "setDrawerButtonOnClickListener: this method can be used " +
                    "only with the default header view")
    }

    override fun setHeaderButtonBadge(badge: Badge) {
        if (headerButtonBadge == badge) return
        headerButtonBadge = badge
        drawerHeaderBadgeView?.apply {
            updateBadgeView(badge)
            updateNavBadgeVisibility()
        } ?: Log.e(TAG, "setDrawerButtonBadge: this method can be used " +
                "only with the default header view")

    }

    override fun getDrawerSlideOffset(): Float = sSlideOffset

    private var _backHandler: DrawerLayoutBackHandler<OneUIDrawerLayout>? = null

    override fun getOrCreateBackHandler(drawerLayout: OneUIDrawerLayout) : DrawerLayoutBackHandler<OneUIDrawerLayout> {
        return _backHandler
            ?: DrawerLayoutBackHandler(drawerLayout, DrawerBackAnimator(this@SemDrawerLayout))
                .also { _backHandler = it }
    }

    override fun applyWindowInsets(
        insets: WindowInsetsCompat,
        isImmersiveActive: Boolean
    ) {
        val currentActivity = activity ?: return
        val systemAndCutoutInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val imeInsetBottom = insets.getInsets(ime()).bottom

        if (currentActivity.fitsSystemWindows) {
            applyInsetsForFitSystemWindows(imeInsetBottom)
        } else {
            applyInsetsForEdgeToEdge(systemAndCutoutInsets, imeInsetBottom, isImmersiveActive)
        }
    }

    private fun applyInsetsForFitSystemWindows(imeInsetBottom: Int) {

        val rootLp = layoutParams as MarginLayoutParams
        if (rootLp.leftMargin != 0 || rootLp.rightMargin != 0) {
            updateLayoutParams<MarginLayoutParams> { leftMargin = 0; rightMargin = 0 }
        }
        updateSlideViewPanePadding(top = 0, bottom = imeInsetBottom)
        val actionBarTopPadding = defaultActionBarTopPadding
        val drawerLp = drawerPane.layoutParams as MarginLayoutParams
        if (drawerLp.topMargin != actionBarTopPadding || drawerLp.bottomMargin != imeInsetBottom) {
            drawerPane.updateLayoutParams<MarginLayoutParams> {
                topMargin = actionBarTopPadding; bottomMargin = imeInsetBottom
            }
        }
    }

    private fun applyInsetsForEdgeToEdge(
        systemAndCutoutInsets: Insets,
        imeInsetBottom: Int,
        isImmersiveActive: Boolean
    ) {
        val finalBottomInset = maxOf(systemAndCutoutInsets.bottom, imeInsetBottom)
        val systemTopInset = systemAndCutoutInsets.top

        val rootLp = layoutParams as MarginLayoutParams
        if (rootLp.leftMargin != systemAndCutoutInsets.left || rootLp.rightMargin != systemAndCutoutInsets.right) {
            updateLayoutParams<MarginLayoutParams> {
                leftMargin = systemAndCutoutInsets.left
                rightMargin = systemAndCutoutInsets.right
            }
        }

        if (isImmersiveActive) {
            updateSlideViewPanePadding(top = 0, bottom = imeInsetBottom)
        } else {
            updateSlideViewPanePadding(top = systemTopInset, bottom = finalBottomInset)
        }

        val actionBarTopPadding = systemTopInset + defaultActionBarTopPadding
        val drawerLp = drawerPane.layoutParams as MarginLayoutParams
        if (drawerLp.topMargin != actionBarTopPadding || drawerLp.bottomMargin != finalBottomInset) {
            drawerPane.updateLayoutParams<MarginLayoutParams> {
                topMargin = actionBarTopPadding; bottomMargin = finalBottomInset
            }
        }
    }

    private fun updateSlideViewPanePadding(top: Int, bottom: Int) {
        if (slideViewPane.paddingTop != top || slideViewPane.paddingBottom != bottom) {
            slideViewPane.updatePadding(top = top, bottom = bottom)
        }
    }

    companion object{
        private const val TAG = "SemDrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        @Volatile
        private var sSlideOffset = 0f
        private var sCurrentState: DrawerState = DrawerState.CLOSE
    }
}

