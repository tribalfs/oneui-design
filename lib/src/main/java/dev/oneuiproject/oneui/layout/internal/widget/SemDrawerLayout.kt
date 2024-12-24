package dev.oneuiproject.oneui.layout.internal.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.ktx.ifNegativeOrZero
import dev.oneuiproject.oneui.ktx.semSetRoundedCorners
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.ktx.widthExcludingSystemInsets
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.DrawerOutlineProvider
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.getDrawerStateUpdate
import dev.oneuiproject.oneui.layout.internal.util.updateBadge
import kotlin.math.max
typealias OneUIDrawerLayout = dev.oneuiproject.oneui.layout.DrawerLayout

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SemDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): DrawerLayout(context, attrs, defStyle), DrawerLayoutInterface, NavButtonsHandler {

    private val mDrawerListener = DrawerListener()
    private var scrimAlpha = 0f
    private var systemBarsColor = -1

    private lateinit var mDrawerPane: LinearLayout
    private lateinit var mSlideViewPane: FrameLayout
    private lateinit var mHeaderView: View
    private var mDrawerHeaderButton: ImageButton? = null
    private var mDrawerHeaderBadge: TextView? = null
    private lateinit var mDrawerItemsContainer: FrameLayout

    private lateinit var translationView: View
    private var handleInsets = false

    private lateinit var mNavButtonsHandlerDelegate: NavButtonsHandler

    private val activity by lazy(LazyThreadSafetyMode.NONE) {  context.appCompatActivity }

    init{
        context.getColor(R.color.oui_drawerlayout_drawer_dim_color).let {
            setScrimColor(it)
            scrimAlpha = ((it shr 24) and 0xFF) / 255f
        }

        if (Build.VERSION.SDK_INT < 35) {
            systemBarsColor = context.getThemeAttributeValue(androidx.appcompat.R.attr.roundedCornerColor)?.data!!
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

        mDrawerHeaderButton = mHeaderView.findViewById(R.id.drawer_header_button)
        mDrawerHeaderBadge = mHeaderView.findViewById(R.id.drawer_header_button_badge)

        translationView = findViewById(R.id.drawer_custom_translation) ?: mSlideViewPane

        setDrawerCornerRadius(DEFAULT_DRAWER_RADIUS)

        mNavButtonsHandlerDelegate = ToolbarLayoutButtonsHandler(findViewById(R.id.toolbarlayout_main_toolbar))
        mNavButtonsHandlerDelegate.setNavigationButtonOnClickListener{
            openDrawer(mDrawerPane, true)
        }
        mNavButtonsHandlerDelegate.showNavigationButton = true

        removeDrawerListener(mDrawerListener)
        addDrawerListener(mDrawerListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDrawerLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        updateDrawerLayout()
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isAttachedToWindow) return
        updateDrawerLayout()
    }

    private fun updateDrawerLayout(){
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
        val displayWidth = context.widthExcludingSystemInsets

        val density = resources.displayMetrics.density
        val dpi = displayWidth.toFloat() / density

        val widthRate = when {
            dpi >= 1920.0f -> 0.22
            dpi in 960.0f..1919.9f -> 0.2734
            dpi in 600.0f..959.9f -> 0.46
            dpi in 480.0f..599.9f -> 0.5983
            else -> 0.844
        }

        mDrawerPane.apply {
            updateLayoutParams{ width = (displayWidth * widthRate).toInt() }
            requestLayout()
        }

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
     */
    override fun setDrawerCornerRadius(@Px px: Int) {
        (mDrawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            it.cornerRadius = px
        } ?: run {
            mDrawerPane.outlineProvider = DrawerOutlineProvider(px)
            mDrawerPane.clipToOutline = true
        }
    }

    override fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams){
        mDrawerPane.removeView(mHeaderView)
        mDrawerHeaderButton = null
        mDrawerHeaderBadge = null
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

        if (!isInEditMode) {
            //Handle system bars dim for api35-
            if (systemBarsColor != -1) {
                val hsv = FloatArray(3)
                Color.colorToHSV(systemBarsColor, hsv)
                hsv[2] *= 1f - (slideOffset * scrimAlpha)
                activity!!.window.apply {
                    @Suppress("DEPRECATION")
                    statusBarColor = Color.HSVToColor(hsv)
                    @Suppress("DEPRECATION")
                    navigationBarColor = Color.HSVToColor(hsv)
                }
            }
        }
    }

    private fun dispatchDrawerStateChange(newSlideOffset: Float) {
        if (sSlideOffset == newSlideOffset) return
        val newState = getDrawerStateUpdate(sSlideOffset, newSlideOffset)
        sSlideOffset = newSlideOffset

        if (newState != mCurrentState) {
            mCurrentState = newState
            mDrawerStateListener?.invoke(newState)
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

    override fun setHandleInsets(handle: Boolean){
        handleInsets = handle
        requestApplyInsets()
    }

    override var isLocked: Boolean
        set(lock){
            if (lock){
                if (isOpen) close(false)
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
        get() = mNavButtonsHandlerDelegate.showNavigationButtonAsBack
        set(value) {
            mNavButtonsHandlerDelegate.showNavigationButtonAsBack = value
        }

    override var showNavigationButton: Boolean = true

    override fun setNavigationButtonOnClickListener(listener: OnClickListener?) = Unit

    override fun setNavigationButtonTooltip(tooltipText: CharSequence?) =
        mNavButtonsHandlerDelegate.setNavigationButtonTooltip(tooltipText)

    override fun setNavigationButtonBadge(badge: Badge) =
        mNavButtonsHandlerDelegate.setNavigationButtonBadge(badge)

    override fun setNavigationButtonIcon(icon: Drawable?) =
        mNavButtonsHandlerDelegate.setNavigationButtonIcon(icon)

    override fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int?) {
        if (mDrawerHeaderButton != null) {
            mDrawerHeaderButton!!.apply {
                setImageDrawable(icon)
                imageTintList = ColorStateList.valueOf(tint
                    ?: context.getColor(R.color.oui_drawerlayout_header_icon_tint))
            }
            mHeaderView.isVisible = icon != null
        } else {
            Log.e(TAG, "setHeaderButtonIcon: `drawer_header_button` id is not set in custom header view")
        }
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
        mDrawerHeaderBadge?.updateBadge(badge)
            ?: Log.e(TAG, "setDrawerButtonBadge: this method can be used " +
                    "only with the default header view")
    }

    override fun getDrawerSlideOffset(): Float = sSlideOffset

    @SuppressLint("NewApi")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (handleInsets) {
            insets.also {
                val systemBarsInsets = it.getInsets(WindowInsetsCompat.Type.systemBars())
                val topInset = systemBarsInsets.top
                val bottomInset = max(systemBarsInsets.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)

                mSlideViewPane.setPadding(
                    systemBarsInsets.left,
                    topInset,
                    systemBarsInsets.right,
                    bottomInset
                )
                mDrawerPane.updateLayoutParams<MarginLayoutParams> {
                    topMargin = topInset + resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_action_bar_top_padding)
                    bottomMargin = bottomInset
                }
            }
        }
        return super.onApplyWindowInsets(insets)
    }

    private var _backHandler: DrawerLayoutBackHandler<OneUIDrawerLayout>? = null

    override fun getOrCreateBackHandler(drawerLayout: OneUIDrawerLayout) : DrawerLayoutBackHandler<OneUIDrawerLayout> {
        return _backHandler
            ?: DrawerLayoutBackHandler(drawerLayout, DrawerBackAnimator(this@SemDrawerLayout))
                .also { _backHandler = it }
    }

    companion object{
        private const val TAG = "SemDrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        @Volatile
        private var sSlideOffset = 0f
        private var mCurrentState: DrawerState = DrawerState.CLOSE
    }
}

