package dev.oneuiproject.oneui.layout.internal.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
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
import androidx.appcompat.R.dimen.sesl_action_bar_top_padding
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
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
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.getDrawerStateUpdate
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.updateBadgeView
import dev.oneuiproject.oneui.layout.internal.util.DrawerOutlineProvider
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler

typealias OneUIDrawerLayout = dev.oneuiproject.oneui.layout.DrawerLayout

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SemDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): DrawerLayout(context, attrs, defStyle), DrawerLayoutInterface, NavButtonsHandler {

    private val mDrawerListener = DrawerListener()
    private var scrimAlpha = 0f
    private var systemBarsColor = context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)?.data!!
    private val hsv = FloatArray(3)

    private lateinit var mDrawerPane: LinearLayout
    private lateinit var mSlideViewPane: FrameLayout
    private lateinit var mHeaderView: View
    private var mDrawerHeaderButton: ImageButton? = null
    private var mDrawerHeaderBadgeView: TextView? = null

    private var navDrawerButtonBadge: Badge = Badge.NONE
    private var headerButtonBadge: Badge = Badge.NONE

    private lateinit var mDrawerItemsContainer: FrameLayout

    private lateinit var translationView: View
    private var handleInsets = false
    private var _showNavButtonAsBack = false
    private var _showNavigationButton = true

    private lateinit var mNavButtonsHandlerDelegate: NavButtonsHandler

    private val activity by lazy(LazyThreadSafetyMode.NONE) {  context.appCompatActivity }

    init{
        ContextCompat.getColor(context, R.color.oui_des_drawerlayout_drawer_dim_color).let {
            setScrimColor(it)
            scrimAlpha = ((it shr 24) and 0xFF) / 255f
        }

        drawerElevation = 0f

        if (!isInEditMode) {
            activity!!.window.decorView.semSetRoundedCorners(ROUNDED_CORNER_NONE)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        mDrawerPane = findViewById(R.id.drawer_panel)
        mSlideViewPane = findViewById(R.id.slideable_view)

        mHeaderView = mDrawerPane.findViewById(R.id.header_layout)
        mDrawerItemsContainer = mDrawerPane.findViewById(R.id.drawer_items_container)

        mDrawerHeaderButton = mHeaderView.findViewById(R.id.oui_des_drawer_header_button)
        mDrawerHeaderBadgeView = mHeaderView.findViewById(R.id.oui_des_drawer_header_button_badge)

        translationView = findViewById(R.id.drawer_custom_translation) ?: mSlideViewPane

        setDrawerCornerRadius(DEFAULT_DRAWER_RADIUS)

        mNavButtonsHandlerDelegate = ToolbarLayoutButtonsHandler(findViewById(R.id.toolbarlayout_main_toolbar))
        mNavButtonsHandlerDelegate.setNavigationButtonOnClickListener{
            openDrawer(mDrawerPane, true)
        }

        removeDrawerListener(mDrawerListener)
        addDrawerListener(mDrawerListener)
        updateNavBadgeVisibility()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDrawerWidthAndState()
        mNavButtonsHandlerDelegate.apply {
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
        if (!isAttachedToWindow) return
        updateDrawerWidthAndState()
    }

    private fun updateDrawerWidthAndState(){
        updateDrawerWidth()
        if (isOpen) {
            removeCallbacks(ensureDrawerOpenState)
            postDelayed(ensureDrawerOpenState, 50)
        }
    }

    private val ensureDrawerOpenState = Runnable{
        if (isOpen) {
            openDrawer(mDrawerPane, false)
            updateContentTranslation(mDrawerPane, 1f)
        }
    }

    private fun updateDrawerWidth() {
        val drawerWidth = resources.configuration.screenWidthDp.let {
            when {
                it >= 960 -> 310f
                it in 600..959 -> it * 0.40f
                it in 480..599 -> it * 0.60f
                else -> it * 0.86f
            }.dpToPx(resources)
        }

        mDrawerPane.updateLayoutParams{ width = drawerWidth }

        if (isInEditMode && isOpen){
            ensureOpenDrawerPreview()
        }
    }

    private fun ensureOpenDrawerPreview(){
        val width = mDrawerPane.width.ifNegativeOrZero { 356.dpToPx(resources) }
        mDrawerPane.updateLayoutParams { this.width = width }
        mSlideViewPane.translationX = width * if (isLayoutRTL) -1f else 1f
    }

    override fun open(animate: Boolean) = openDrawer(mDrawerPane, animate)

    override fun close(animate: Boolean) = closeDrawer(mDrawerPane, animate)

    override fun close() = closeDrawer(mDrawerPane, true)

    override fun setDrawerCornerRadius(@Dimension dp: Float) {
        setDrawerCornerRadius(dp.dpToPx(resources))
    }

    /**
     * Set a custom radius for the drawer panel's edges.
     * Set to -1 to reset the default.
     */
    override fun setDrawerCornerRadius(@Px px: Int) {
        (mDrawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            it.cornerRadius = if (px == -1) DEFAULT_DRAWER_RADIUS.dpToPx(resources) else px
        } ?: run {
            mDrawerPane.outlineProvider = DrawerOutlineProvider(px)
            mDrawerPane.clipToOutline = true
        }
    }

    override fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams){
        mDrawerPane.removeView(mHeaderView)
        mDrawerHeaderButton = null
        mDrawerHeaderBadgeView = null
        mDrawerPane.addView(headerView, 0, params)
        mHeaderView = mDrawerPane.getChildAt(0)
    }

    override fun addDrawerContent(child: View, params: ViewGroup.LayoutParams){
        mDrawerItemsContainer.addView(child, params)
    }

    private var mDrawerStateListener: ((state: DrawerState)-> Unit)? = null

    override fun setOnDrawerStateChangedListener(listener: ((DrawerState) -> Unit)?) {
        mDrawerStateListener = listener
    }

    override fun getDrawerPane(): View = mDrawerPane

    override fun getContentPane(): View = mSlideViewPane

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

        if (newState != mCurrentState) {
            mCurrentState = newState
            mDrawerStateListener?.invoke(newState)

            updateNavBadgeVisibility()
        }
    }

    private fun updateNavBadgeVisibility() {
        if (mCurrentState != DrawerState.CLOSE){
            mNavButtonsHandlerDelegate.setNavigationButtonBadge(Badge.NONE)
        }else {
            if (navDrawerButtonBadge != Badge.NONE) {
                mNavButtonsHandlerDelegate.setNavigationButtonBadge(navDrawerButtonBadge)
            }else {
                mNavButtonsHandlerDelegate.setNavigationButtonBadge(headerButtonBadge)
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

    override val isDrawerOpen: Boolean get() = mCurrentState == DrawerState.OPEN

    override val isDrawerOpenOrIsOpening: Boolean
        get() = mCurrentState == DrawerState.OPEN || mCurrentState == DrawerState.OPENING


    override var showNavigationButtonAsBack: Boolean
        get() = _showNavButtonAsBack
        set(value) {
            if (_showNavButtonAsBack == value) return
            _showNavButtonAsBack = value
            if (isAttachedToWindow) {
                mNavButtonsHandlerDelegate.showNavigationButtonAsBack = value
            }
        }

    override var showNavigationButton: Boolean
        get() = _showNavigationButton
        set(value) {
            if (_showNavigationButton == value) return
            _showNavigationButton = value
            if (isAttachedToWindow) {
                mNavButtonsHandlerDelegate.showNavigationButton = value
            }
        }

    override fun setNavigationButtonOnClickListener(listener: OnClickListener?) = Unit

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) =
        mNavButtonsHandlerDelegate.setNavigationButtonTooltip(tooltipText)

    override fun setNavigationButtonBadge(badge: Badge) {
        if (navDrawerButtonBadge == badge) return
        navDrawerButtonBadge = badge
        if (badge != Badge.NONE) {
            mNavButtonsHandlerDelegate.setNavigationButtonBadge(badge)
        } else if (!isOpen && headerButtonBadge != Badge.NONE){
            mNavButtonsHandlerDelegate.setNavigationButtonBadge(headerButtonBadge)
        }
        updateNavBadgeVisibility()
    }

    override fun setNavigationButtonIcon(icon: Drawable?) =
        mNavButtonsHandlerDelegate.setNavigationButtonIcon(icon)

    override fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int?) {
        mDrawerHeaderButton?.apply {
            setImageDrawable(icon)
            imageTintList = ColorStateList.valueOf(tint
                ?: ContextCompat.getColor(context, R.color.oui_des_drawerlayout_header_icon_tint))
            mHeaderView.isVisible = icon != null
        } ?: Log.e(TAG, "setHeaderButtonIcon: this method can be used " +
                "only with the default header view")
    }

    override fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        mDrawerHeaderButton?.semSetToolTipText(tooltipText)
            ?: Log.e(TAG, "setDrawerButtonTooltip: this method can be used " +
                    "only with the default header view")
    }

    override fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        mDrawerHeaderButton?.setOnClickListener(listener)
            ?: Log.e(TAG, "setDrawerButtonOnClickListener: this method can be used " +
                    "only with the default header view")
    }

    override fun setHeaderButtonBadge(badge: Badge) {
        if (headerButtonBadge == badge) return
        headerButtonBadge = badge
        mDrawerHeaderBadgeView?.apply {
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

    override fun applyWindowInsets(insets: WindowInsetsCompat, isImmersiveActive: Boolean){
        val activity = activity ?: return
        val baseInsets = insets.getInsets(systemBars() or displayCutout())
        val imeInsetBottom = insets.getInsets(ime()).bottom
        val baseInsetTop = baseInsets.top
        val defaultTopPadding = resources.getDimensionPixelSize(sesl_action_bar_top_padding)

        if (activity.fitsSystemWindows) {
            updateLayoutParams<MarginLayoutParams> { leftMargin = 0; rightMargin = 0 }
            mSlideViewPane.updatePadding(top = 0, bottom = imeInsetBottom)
            mDrawerPane.updateLayoutParams<MarginLayoutParams> {
                topMargin = defaultTopPadding; bottomMargin = imeInsetBottom }

        }else{
            val bottomInset = maxOf(baseInsets.bottom, imeInsetBottom)
            updateLayoutParams<MarginLayoutParams> {
                leftMargin = baseInsets.left; rightMargin = baseInsets.right }
            if (isImmersiveActive){
                mSlideViewPane.updatePadding(top = 0, bottom = imeInsetBottom)
            }else{
                mSlideViewPane.updatePadding(top = baseInsetTop, bottom = bottomInset )
            }
            mDrawerPane.updateLayoutParams<MarginLayoutParams> {
                topMargin = baseInsetTop + defaultTopPadding; bottomMargin = bottomInset
            }
        }
    }

    companion object{
        private const val TAG = "SemDrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        @Volatile
        private var sSlideOffset = 0f
        private var mCurrentState: DrawerState = DrawerState.CLOSE
    }
}

