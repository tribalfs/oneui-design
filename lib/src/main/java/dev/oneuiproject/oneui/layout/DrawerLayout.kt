@file:Suppress("NOTHING_TO_INLINE")

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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.utils.ViewUtils
import dev.oneuiproject.oneui.utils.badgeCountToText
import kotlin.math.max
import androidx.appcompat.R as appcompatR
import dev.oneuiproject.oneui.layout.DrawerLayout as OuiDrawerLayout

/**
 * Custom DrawerLayout extending [ToolbarLayout]. Looks and behaves the same as the one in Apps from Samsung.
 */
class DrawerLayout(context: Context, attrs: AttributeSet?) :
    ToolbarLayout(context, attrs) {

    private val mDrawerListener: DrawerListener = DrawerListener()

    private lateinit var mDrawer: DrawerLayout
    private var mToolbarContent: LinearLayout? = null
    private lateinit var mDrawerContent: LinearLayout
    private lateinit var mHeaderView: View
    private var mHeaderButton: AppCompatImageButton? = null
    private var mHeaderBadge: TextView? = null
    private var mDrawerContainer: FrameLayout? = null
    private var scrimAlpha = 0f
    private var systemBarsColor = -1

    init {
        initDrawer()

        if (!isInEditMode) {
            ViewUtils.semSetRoundedCorners(
                activity!!.window.decorView,
                ViewUtils.SEM_ROUNDED_CORNER_NONE
            )
        }
    }

    override fun initLayoutAttrs(attrs: AttributeSet?) {
        super.initLayoutAttrs(attrs)
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.ToolbarLayout, 0, 0).use {
            mLayout = it.getResourceId(
                R.styleable.ToolbarLayout_android_layout,
                R.layout.oui_layout_drawerlayout
            )
        }
    }

    override fun inflateChildren() {
        if (mLayout != R.layout.oui_layout_drawerlayout) {
            Log.w(TAG, "Inflating custom $TAG")
        }
        LayoutInflater.from(context)
            .inflate(mLayout, this, true)
    }

    private fun initDrawer() {
        setNavigationButtonIcon(ContextCompat.getDrawable(context, R.drawable.oui_ic_ab_drawer))
        setNavigationButtonTooltip(resources.getText(R.string.oui_navigation_drawer))

        val scrimColor = context.getColor(R.color.oui_drawerlayout_drawer_dim_color)

        mDrawer = findViewById<DrawerLayout?>(R.id.drawerlayout_drawer).apply {
            setScrimColor(scrimColor)
            setDrawerElevation(0f)
        }
        mToolbarContent = findViewById(R.id.drawerlayout_toolbar_content)
        mDrawerContent = findViewById(R.id.drawerlayout_drawer_content)

        mHeaderView = mDrawerContent.findViewById(R.id.drawerlayout_default_header)
        mHeaderButton = mHeaderView.findViewById(R.id.drawerlayout_header_button)
        mHeaderBadge = mHeaderView.findViewById(R.id.drawerlayout_header_badge)

        mDrawerContainer = mDrawerContent.findViewById(R.id.drawerlayout_drawer_container)

        scrimAlpha = ((scrimColor shr 24) and 0xFF) / 255f

        if (Build.VERSION.SDK_INT < 35) {
            val sbTypedValue = TypedValue()
            systemBarsColor = if (context.theme.resolveAttribute(
                    appcompatR.attr.roundedCornerColor,
                    sbTypedValue,
                    true
                )
            ) {
                sbTypedValue.data
            } else {
                ContextCompat.getColor(context, R.color.oui_round_and_bgcolor)
            }
        }

        setDrawerWidth()
        setDrawerCornerRadius(DEFAULT_DRAWER_RADIUS)

        setNavigationButtonOnClickListener {
            mDrawer.openDrawer(
                mDrawerContent
            )
        }

        if (!isInEditMode) {
            mDrawer.addDrawerListener(mDrawerListener)
            if (sIsDrawerOpened) {
                mDrawer.post {
                    mDrawerListener.onDrawerSlide(
                        mDrawerContent, 1f
                    )
                }
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (mToolbarContent == null || mDrawerContainer == null) {
            super.addView(child, index, params)
        } else {
            when ((params as ToolbarLayoutParams).layoutLocation) {
                DRAWER_HEADER -> {
                    mDrawerContent.removeView(mHeaderView)
                    mHeaderButton = null
                    mHeaderBadge = null
                    mDrawerContent.addView(child, 0, params)
                    mHeaderView = mDrawerContent.getChildAt(0)
                }

                DRAWER_PANEL -> mDrawerContainer!!.addView(child, params)
                else -> super.addView(child, index, params)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setDrawerWidth()
        if (sIsDrawerOpened) {
            mDrawer.post {
                mDrawerListener.onDrawerSlide(
                    mDrawerContent, 1f
                )
            }
        }
    }

    private fun lockDrawerIfAvailable(lock: Boolean) {
        if (lock) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    private fun setDrawerWidth() {
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

        mDrawerContent.updateLayoutParams<MarginLayoutParams> {
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

    override fun startActionMode(listener: ActionModeListener){
        lockDrawerIfAvailable(true)
        super.startActionMode(listener)
    }

    override fun endActionMode() {
        super.endActionMode()
        lockDrawerIfAvailable(false)
    }

    override fun showSearchMode() {
        lockDrawerIfAvailable(true)
        super.showSearchMode()
    }

    override fun dismissSearchMode() {
        super.dismissSearchMode()
        lockDrawerIfAvailable(false)
    }

    //
    // Drawer methods
    //
    /**
     * Show a margin at the top of the drawer panel. Some Apps from Samsung do have this.
     */
    @Deprecated("")
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
        mDrawerContent.outlineProvider = DrawerOutlineProvider(px)
        mDrawerContent.clipToOutline = true
    }

    /**
     * Set the icon of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    fun setDrawerButtonIcon(icon: Drawable?) {
        if (mHeaderButton != null) {
            mHeaderButton!!.setImageDrawable(icon)
            mHeaderButton!!.imageTintList = ColorStateList.valueOf(
                context.getColor(R.color.oui_drawerlayout_header_icon_color)
            )
            mHeaderView.visibility =
                if (icon != null) VISIBLE else GONE
        } else {
            Log.e(
                TAG, "setDrawerButtonIcon: this method can be used " +
                        "only with the default header view"
            )
        }
    }

    /**
     * Set the tooltip of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    fun setDrawerButtonTooltip(tooltipText: CharSequence?) {
        if (mHeaderButton != null) {
            TooltipCompat.setTooltipText(mHeaderButton!!, tooltipText)
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
    fun setDrawerButtonOnClickListener(listener: OnClickListener?) {
        if (mHeaderButton != null) {
            mHeaderButton!!.setOnClickListener(listener)
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
        setDrawerButtonBadge(drawerBadge)
    }

    /**
     * Set the badge of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     * The badge is small orange circle in the top right of the icon.
     *
     * @param badge The [badge][dev.oneuiproject.oneui.layout.ToolbarLayout.Badge] to set for drawer button.
     */
    fun setDrawerButtonBadge(badge: Badge) {
        if (mHeaderBadge != null) {
            when (badge) {
                is Badge.Dot -> {
                    mHeaderBadge!!.text = ""
                    val res = resources
                    mHeaderBadge!!.updateLayoutParams<MarginLayoutParams> {
                        res.getDimension(appcompatR.dimen.sesl_menu_item_badge_size).toInt().let {
                            width = it
                            height = it
                        }
                        8.dpToPx(res).let {
                            marginEnd = it
                            topMargin = it
                        }
                    }
                    mHeaderBadge!!.visibility = VISIBLE
                }

                is Badge.None -> {
                    mHeaderBadge!!.visibility = GONE
                }

                is Badge.Numeric -> {
                    val badgeText = badge.count.badgeCountToText()!!
                    mHeaderBadge!!.text = badgeText
                    mHeaderBadge!!.updateLayoutParams<MarginLayoutParams> {
                        val res = resources
                        val defaultWidth: Float =
                            res.getDimension(appcompatR.dimen.sesl_badge_default_width)
                        val additionalWidth: Float =
                            res.getDimension(appcompatR.dimen.sesl_badge_additional_width)
                        width = (defaultWidth + badgeText.length * additionalWidth).toInt()
                        height = (defaultWidth + additionalWidth).toInt()
                        6.dpToPx(res).let {
                            marginEnd = it
                            topMargin = it
                        }
                    }
                    mHeaderBadge!!.visibility = VISIBLE
                }
            }
        } else {
            Log.e(
                TAG, "setDrawerButtonBadge: this method can be used " +
                        "only with the default header view"
            )
        }
    }

    /**
     * Open or close the drawer panel with an optional animation.
     *
     * @param animate whether or not to animate the opening and closing
     */
    fun setDrawerOpen(open: Boolean, animate: Boolean) {
        if (open) {
            mDrawer.openDrawer(mDrawerContent, animate)
        } else {
            mDrawer.closeDrawer(mDrawerContent, animate)
        }
    }

    private inner class DrawerOutlineProvider(@param:Px private val mCornerRadius: Int) :
        ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val isRTL = isRtl()
            outline.setRoundRect(
                if (isRTL) 0 else -mCornerRadius,
                0,
                if (isRTL) view.width + mCornerRadius else view.width, view.height,
                mCornerRadius.toFloat()
            )
        }
    }

    private fun isRtl() = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

    private inner class DrawerListener : SimpleDrawerListener() {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            super.onDrawerSlide(drawerView, slideOffset)

            ignoreBackGesture(slideOffset > 0 && slideOffset < 1)

            val translationView = findViewById<View>(R.id.drawer_custom_translation)

            val slideX = drawerView.width * slideOffset * if (isRtl()) -1f else 1f
            if (translationView != null) translationView.translationX = slideX
            else mToolbarContent!!.translationX = slideX

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
            ignoreBackGesture(false)
        }

        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            sIsDrawerOpened = false
            ignoreBackGesture(false)
        }

        @RequiresApi(33)
        private var dummyCallback: OnBackInvokedCallback? = null

        fun ignoreBackGesture(ignore: Boolean) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ignore) {
                    if (dummyCallback == null) {
                        dummyCallback = DummyCallback()
                        findOnBackInvokedDispatcher()!!.registerOnBackInvokedCallback(
                            OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                            dummyCallback!!
                        )
                    }
                } else {
                    if (dummyCallback != null) {
                        findOnBackInvokedDispatcher()!!.unregisterOnBackInvokedCallback(dummyCallback!!)
                        dummyCallback = null
                    }
                }
            }
        }

        @RequiresApi(33)
        private inner class DummyCallback : OnBackInvokedCallback {
            override fun onBackInvoked() = Unit
        }
    }

    @SuppressLint("NewApi")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (handleInsets()) {
            return insets.also {
                val systemBarsInsets = it.getInsets(WindowInsetsCompat.Type.systemBars())
                val topInset = systemBarsInsets.top
                val bottomInset = max(systemBarsInsets.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)

                mToolbarContent!!.setPadding(
                    systemBarsInsets.left,
                    topInset,
                    systemBarsInsets.right,
                    bottomInset
                )
                mDrawerContent.updateLayoutParams<MarginLayoutParams> {
                    topMargin = topInset + resources.getDimensionPixelSize(R.dimen.oui_drawerlayout_drawer_top_margin)
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

/**
 * Set the badge of the drawer button.
 * The drawer button is the button in the top right corner of the drawer panel.
 * The badge is small orange circle in the top right of the icon.
 *
 * @param badge
 */
inline fun OuiDrawerLayout.setDrawerButtonBadge(badge: Badge) {
    setDrawerButtonBadge(
        when (badge) {
            is Badge.NUMERIC -> ToolbarLayout.Badge.Numeric(badge.count)
            is Badge.DOT -> ToolbarLayout.Badge.Dot()
            is Badge.NONE -> ToolbarLayout.Badge.None()
        }
    )
}


/**
 * Set the badges of the navigation button and drawer button.
 * The drawer button is the button in the top right corner of the drawer panel.
 * The badge is small orange circle in the top right of the icon which contains text.
 *
 * @param navigationBadge
 * @param drawerBadge
 */
inline fun OuiDrawerLayout.setButtonBadges(navigationBadge: Badge, drawerBadge: Badge) {
    setNavigationBadge(navigationBadge)
    setDrawerButtonBadge(drawerBadge)

}
