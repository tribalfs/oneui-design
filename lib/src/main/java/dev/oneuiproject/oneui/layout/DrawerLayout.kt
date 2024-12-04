package dev.oneuiproject.oneui.layout

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.customview.widget.Openable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.getThemeAttributeValue
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.utils.ViewUtils
import dev.oneuiproject.oneui.utils.badgeCountToText
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import androidx.appcompat.R as appcompatR

/**
 * Custom DrawerLayout extending [ToolbarLayout]. Looks and behaves the same as the one in Apps from Samsung.
 */
open class DrawerLayout(context: Context, attrs: AttributeSet?) :
    ToolbarLayout(context, attrs), Openable {

    private lateinit var mDrawerListener: DrawerListener

    enum class DrawerState{
        OPEN,
        CLOSE,
        CLOSING,
        OPENING
    }
    private var mDrawerStateListener: ((state: DrawerState)-> Unit)? = null
    @Volatile
    private var mCurrentState: DrawerState = DrawerState.CLOSE
    private var mSlideOffset = 0f

    private lateinit var mDrawerLayout: DrawerLayout
    private var mSlideViewPane: LinearLayout? = null
    private lateinit var mDrawerPane: LinearLayout
    private lateinit var mHeaderView: View
    private var mDrawerHeaderButton: ImageButton? = null
    private var mDrawerHeaderBadge: TextView? = null
    private var mDrawerContainer: FrameLayout? = null
    private var scrimAlpha = 0f
    private var systemBarsColor = -1

    internal var enableDrawerBackAnimation: Boolean = false
        private set

    init {
        initViews()

        if (!isInEditMode) {
            ViewUtils.semSetRoundedCorners(
                activity!!.window.decorView,
                ROUNDED_CORNER_NONE
            )
        }
    }

    override val backHandler: BackHandler
        get() = DrawerLayoutBackHandler(this@DrawerLayout, DrawerBackAnimator(mDrawerPane, mSlideViewPane!!))

    override fun getDefaultLayoutResource() = R.layout.oui_layout_drawerlayout
    override fun getDefaultNavigationIconResource(): Int = R.drawable.oui_ic_ab_drawer

    override fun initLayoutAttrs(attrs: AttributeSet?) {
        super.initLayoutAttrs(attrs)
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.DrawerLayout, 0, 0).use {
            enableDrawerBackAnimation = it.getBoolean(R.styleable.DrawerLayout_drawerBackAnimation, false)
        }
    }

    override fun inflateChildren() {
        if (mLayout != getDefaultLayoutResource()) {
            Log.w(TAG, "Inflating custom $TAG")
        }
        LayoutInflater.from(context)
            .inflate(mLayout, this, true)
    }

    protected open fun initViews() {
        val scrimColor = context.getColor(R.color.oui_drawerlayout_drawer_dim_color)

        mDrawerLayout = findViewById<DrawerLayout?>(R.id.drawer_layout)
            .apply {
                setScrimColor(scrimColor)
                setDrawerElevation(0f)
            }.also {
                mDrawerPane =  it.findViewById(R.id.drawer_panel)
                mSlideViewPane = it.findViewById(R.id.slideable_view)
            }

        mHeaderView = mDrawerPane.findViewById(R.id.header_layout)
        mDrawerHeaderButton = mHeaderView.findViewById(R.id.drawer_header_button)
        mDrawerHeaderBadge = mHeaderView.findViewById(R.id.drawer_header_button_badge)

        mDrawerContainer = mDrawerPane.findViewById(R.id.drawer_items_container)

        scrimAlpha = ((scrimColor shr 24) and 0xFF) / 255f

        if (Build.VERSION.SDK_INT < 35) {
            systemBarsColor = context.getThemeAttributeValue(appcompatR.attr.roundedCornerColor)?.data
                ?: ContextCompat.getColor(context, R.color.oui_round_and_bgcolor)
        }

        updateDrawerWidth()
        setDrawerCornerRadius(DEFAULT_DRAWER_RADIUS)

        setNavigationButtonTooltip(resources.getText(R.string.oui_navigation_drawer))
        setNavigationButtonOnClickListener { mDrawerLayout.openDrawer(mDrawerPane) }

        if (!isInEditMode) {
            mDrawerListener = DrawerListener(findViewById(R.id.drawer_custom_translation))
            mDrawerLayout.addDrawerListener(mDrawerListener)
            if (sIsDrawerOpened) {
                mDrawerLayout.post {
                    mDrawerListener.onDrawerSlide(
                        mDrawerPane, 1f
                    )
                }
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (mSlideViewPane == null || mDrawerContainer == null) {
            super.addView(child, index, params)
        } else {
            when ((params as ToolbarLayoutParams).layoutLocation) {
                DRAWER_HEADER -> {
                    mDrawerPane.removeView(mHeaderView)
                    mDrawerHeaderButton = null
                    mDrawerHeaderBadge = null
                    mDrawerPane.addView(child, 0, params)
                    mHeaderView = mDrawerPane.getChildAt(0)
                }

                DRAWER_PANEL -> mDrawerContainer!!.addView(child, params)
                else -> super.addView(child, index, params)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        updateDrawerLayout()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateDrawerLayout()
    }

    private fun updateDrawerLayout(){
        doOnLayout {
            updateDrawerWidth()
            if (sIsDrawerOpened) {
                mDrawerLayout.post {
                    mDrawerListener.onDrawerSlide(
                        mDrawerPane, 1f
                    )
                }
            }
        }
    }

    internal open fun lockDrawerIfAvailable(lock: Boolean) {
        if (lock) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    open fun isDrawerLocked(): Boolean =
        mDrawerLayout.getDrawerLockMode(Gravity.LEFT) != DrawerLayout.LOCK_MODE_UNLOCKED

    internal open fun updateDrawerWidth() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        val displayWidth = size.x
        val density = resources.displayMetrics.density
        val dpi = displayWidth.toFloat() / density

        val widthRate = when {
            dpi >= 1920.0f -> 0.22
            dpi in 960.0f..1919.9f -> 0.2734
            dpi in 600.0f..959.9f -> 0.46
            dpi in 480.0f..599.9f -> 0.5983
            else -> 0.844
        }

        mDrawerPane.updateLayoutParams<MarginLayoutParams> {
            width = (displayWidth * widthRate).toInt()
        }
    }

    @Deprecated(
        "Use startActionMode() instead.",
        replaceWith = ReplaceWith("startActionMode(callback)")
    )
    override fun showActionMode() {
        lockDrawerIfAvailable(true)
        super.showActionMode()
    }

    @Deprecated("Use endActionMode() instead.", replaceWith = ReplaceWith("endActionMode()"))
    override fun dismissActionMode() {
        super.dismissActionMode()
        lockDrawerIfAvailable(false)
    }

    override fun startActionMode(
        listener: ActionModeListener,
        keepSearchMode: Boolean,
        allSelectorStateFlow: StateFlow<AllSelectorState>?
    ) {
        lockDrawerIfAvailable(true)
        super.startActionMode(listener, keepSearchMode, allSelectorStateFlow)
    }

    override fun endActionMode() {
        super.endActionMode()
        if (isSearchMode) return
        lockDrawerIfAvailable(false)
    }

    @Deprecated(
        "Replaced by startSearchMode().",
        replaceWith = ReplaceWith("startSearchMode(listener)")
    )
    override fun showSearchMode() {
        lockDrawerIfAvailable(true)
        super.showSearchMode()
    }

    @Deprecated("Replaced by endSearchMode().", replaceWith = ReplaceWith("endSearchMode()"))
    override fun dismissSearchMode() {
        super.dismissSearchMode()
        lockDrawerIfAvailable(false)
    }

    override fun startSearchMode(
        listener: SearchModeListener,
        searchModeOnBackBehavior: SearchModeOnBackBehavior
    ) {
        lockDrawerIfAvailable(true)
        super.startSearchMode(listener, searchModeOnBackBehavior)
    }

    override fun endSearchMode() {
        super.endSearchMode()
        if (isActionMode) return
        lockDrawerIfAvailable(false)
    }

    //
    // Drawer methods
    //
    /**
     * Show a margin at the top of the drawer panel. Some Apps from Samsung do have this.
     */
    @Deprecated("This is now no op.")
    fun showDrawerTopMargin(show: Boolean) {
    }

    /**
     * Set a custom radius for the drawer panel's edges.
     */
    fun setDrawerCornerRadius(@Dimension dp: Float) {
        setDrawerCornerRadius(dp.dpToPx(resources))
    }

    /**
     * Set a custom radius for the drawer panel's edges.
     */
    fun setDrawerCornerRadius(@Px px: Int) {
        mDrawerPane.outlineProvider = DrawerOutlineProvider(px)
        mDrawerPane.clipToOutline = true
    }

    /**
     * Sets the icon for the drawer header button, located in the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     */
    @Deprecated("Use setHeaderButtonIcon() instead.",
        replaceWith = ReplaceWith("setHeaderButtonIcon(icon)"))
    fun setDrawerButtonIcon(icon: Drawable?){
        setHeaderButtonIcon(icon)
    }

    /**
     * Sets the icon for the drawer header button, located in the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     * @param tint An optional tint to apply to the icon.
     */
    @JvmOverloads
    open fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int? = null) {
        if (mDrawerHeaderButton != null) {
            mDrawerHeaderButton!!.setImageDrawable(icon)
            mDrawerHeaderButton!!.imageTintList = ColorStateList.valueOf(
                context.getColor(R.color.oui_drawerlayout_header_icon_color)
            )
            mHeaderView.visibility =
                if (icon != null) VISIBLE else GONE
        } else {
            Log.e(TAG, "setHeaderButtonIcon: `drawer_header_button` id is not set in custom header view")
        }
    }

    /**
     * Set the tooltip of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     */
    @Deprecated("Use setHeaderButtonTooltip() instead.",
        replaceWith = ReplaceWith("setHeaderButtonTooltip(tooltipText)"))
    fun setDrawerButtonTooltip(tooltipText: CharSequence?) {
        setHeaderButtonTooltip(tooltipText)
    }

    /**
     * Set the tooltip of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     */
    open fun setHeaderButtonTooltip(tooltipText: CharSequence?) {
        if (mDrawerHeaderButton != null) {
            TooltipCompat.setTooltipText(mDrawerHeaderButton!!, tooltipText)
        } else {
            Log.e(
                TAG, "setDrawerButtonTooltip: this method can be used " +
                        "only with the default header view"
            )
        }
    }

    /**
     * Set the click listener of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    @Deprecated("Use setHeaderButtonOnClickListener(listener) instead",
        ReplaceWith("setHeaderButtonOnClickListener(listener)"))
    fun setDrawerButtonOnClickListener(listener: OnClickListener?) {
        setHeaderButtonOnClickListener(listener)
    }

    /**
     * Set the click listener of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    open fun setHeaderButtonOnClickListener(listener: OnClickListener?) {
        if (mDrawerHeaderButton != null) {
            mDrawerHeaderButton!!.setOnClickListener(listener)
        } else {
            Log.e(
                TAG, "setDrawerButtonOnClickListener: this method can be used " +
                        "only with the default header view"
            )
        }
    }

    /**
     * Set the badges of the navigation button and drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     * The badge is small orange circle in the top right of the icon which contains text.
     *
     * @param navigationBadge The [badge][dev.oneuiproject.oneui.layout.ToolbarLayout.Badge] to set for navigation button.
     * @param drawerBadge The [badge][dev.oneuiproject.oneui.layout.ToolbarLayout.Badge] to set for drawer button.
     * @see ToolbarLayout.setNavigationButtonBadge
     */
    fun setButtonBadges(navigationBadge: Badge, drawerBadge: Badge) {
        setNavigationButtonBadge(navigationBadge)
        setHeaderButtonBadge(drawerBadge)
    }

    /**
     * Set the badge of the drawer button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     *
     * @param badge The [badge][dev.oneuiproject.oneui.layout.Badge] to set.
     */
    @Deprecated("Use setHeaderButtonBadge()",
        ReplaceWith("setHeaderButtonBadge(badge)"))
    fun setDrawerButtonBadge(badge: Badge) {
        setHeaderButtonBadge(badge)
    }

    /**
     * Set the badge of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     *
     * @param badge The [badge][dev.oneuiproject.oneui.layout.Badge] to setn.
     */
    open fun setHeaderButtonBadge(badge: Badge) {
        if (mDrawerHeaderBadge != null) {
            updateBadgeView(mDrawerHeaderBadge!!, badge)
        } else {
            Log.e(
                TAG, "setDrawerButtonBadge: this method can be used " +
                        "only with the default header view"
            )
        }
    }

    private fun updateBadgeView(badgeView: TextView, badge: Badge){
        when(badge){
            Badge.DOT -> {
                val res = resources
                val badgeSize = res.getDimensionPixelSize(appcompatR.dimen.sesl_menu_item_badge_size)
                val badgeMargin = 8.dpToPx(res)
                badgeView.apply {
                    background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_dot_badge)
                    text = null
                    updateLayoutParams<MarginLayoutParams> {
                        width = badgeSize
                        height = badgeSize
                        marginEnd = badgeMargin
                        topMargin = badgeMargin
                    }
                    isVisible = true
                }
            }
            is Badge.NUMERIC -> {
                val badgeText = badge.count.badgeCountToText()!!
                val res = resources
                val defaultWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_default_width)
                val additionalWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_additional_width)
                val badgeMargin = 6.dpToPx(res)
                badgeView.apply {
                    background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_noti_badge)
                    text = badgeText
                    updateLayoutParams<MarginLayoutParams> {
                        width = defaultWidth + additionalWidth * badgeText.length
                        height = defaultWidth + additionalWidth
                        marginEnd = badgeMargin
                        topMargin = badgeMargin
                    }
                    isVisible = true
                }
            }
            Badge.NONE -> {
                badgeView.apply {
                    isGone = true
                    text = null
                }
            }

        }
    }

    override fun isOpen(): Boolean = mDrawerLayout.isOpen

    override fun open() = setDrawerOpen(true, animate = true)

    override fun close() = setDrawerOpen(false, animate = true)

    /**
     * Open or close the drawer panel with an optional animation.
     *
     * @param animate whether or not to animate the opening and closing
     */
    open fun setDrawerOpen(open: Boolean, animate: Boolean) {
        if (open) {
            mDrawerLayout.openDrawer(mDrawerPane, animate)
        } else {
            mDrawerLayout.closeDrawer(mDrawerPane, animate)
        }
    }

    /**
     * Set callback to be invoked when the drawer state changes.
     *
     * @param listener lambda to be invoked with the new [DrawerState]
     */
    open fun setDrawerStateListener(listener: ((state: DrawerState)-> Unit)?){
        mDrawerStateListener = listener
        dispatchDrawerStateChange()
    }

    private fun dispatchDrawerStateChange(){
        val newState =
            when (mSlideOffset) {
                1f -> DrawerState.OPEN
                0f -> DrawerState.CLOSE
                else -> {
                    when (mCurrentState) {
                        DrawerState.OPEN -> DrawerState.CLOSING
                        DrawerState.CLOSE -> DrawerState.OPENING
                        else -> mCurrentState
                    }
                }
            }

        if (newState != mCurrentState){
            mCurrentState = newState
            updateOnBackCallbackState()
            mDrawerStateListener?.invoke(newState)
        }
    }

    internal inline val isDrawerOpenOrOpening: Boolean
        get() = mCurrentState == DrawerState.OPEN || mCurrentState == DrawerState.OPENING

    override fun getBackCallbackStateUpdate(): Boolean = shouldCloseDrawer || super.getBackCallbackStateUpdate()

    internal inline val shouldAnimateDrawer: Boolean
        get() = enableDrawerBackAnimation && shouldCloseDrawer

    internal open val shouldCloseDrawer: Boolean get() = isDrawerOpenOrOpening && !isDrawerLocked()

    private inner class DrawerOutlineProvider(@param:Px private val mCornerRadius: Int) :
        ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val isRTL = isLayoutRTL
            outline.setRoundRect(
                if (isRTL) 0 else -mCornerRadius,
                0,
                if (isRTL) view.width + mCornerRadius else view.width, view.height,
                mCornerRadius.toFloat()
            )
        }
    }

    protected val isLayoutRTL get() = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

    @Suppress("NOTHING_TO_INLINE")
    private inner class DrawerListener(private val translationView: View?) : SimpleDrawerListener() {

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            super.onDrawerSlide(drawerView, slideOffset)
            mSlideOffset = slideOffset
            updateSlideViewTranslation(drawerView.width * slideOffset)
            updateSystemBarsScrim(slideOffset)
            dispatchDrawerStateChange()
        }

        private inline fun updateSlideViewTranslation(slideX: Float){
            (translationView?:mSlideViewPane)!!.translationX = if (isLayoutRTL) slideX * -1f else slideX
        }

        private inline fun updateSystemBarsScrim(slideOffset: Float){
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

        override fun onDrawerOpened(drawerView: View) {
            super.onDrawerOpened(drawerView)
            sIsDrawerOpened = true
            mSlideOffset = 1f
            dispatchDrawerStateChange()
        }

        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            sIsDrawerOpened = false
            mSlideOffset = 0f
            dispatchDrawerStateChange()
        }
    }

    @SuppressLint("NewApi")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (handleInsets) {
            return insets.also {
                val systemBarsInsets = it.getInsets(WindowInsetsCompat.Type.systemBars())
                val topInset = systemBarsInsets.top
                val bottomInset = max(systemBarsInsets.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)

                mSlideViewPane!!.setPadding(
                    systemBarsInsets.left,
                    topInset,
                    systemBarsInsets.right,
                    bottomInset
                )
                mDrawerPane.updateLayoutParams<MarginLayoutParams> {
                    topMargin = topInset + resources.getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_top_padding)
                    bottomMargin = bottomInset
                }
            }
        } else {
            return super.onApplyWindowInsets(insets)
        }
    }

    companion object {
        private const val TAG = "DrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        private const val DRAWER_HEADER = 4
        private const val DRAWER_PANEL = 5

        private var sIsDrawerOpened = false

    }
}
