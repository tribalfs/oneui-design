package dev.oneuiproject.oneui.layout.internal.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewStub
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.reflect.view.SeslViewReflector
import androidx.reflect.widget.SeslHoverPopupWindowReflector.getField_TYPE_NONE
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.semSetToolTipText
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.NavDrawerBackAnimator
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.getDrawerStateUpdate
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutUtils.updateBadgeView
import dev.oneuiproject.oneui.layout.internal.util.DrawerOutlineProvider
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import kotlin.math.max

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SemSlidingPaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SlidingPaneLayout(context, attrs, defStyle), DrawerLayoutInterface, NavButtonsHandler {

    private var handleInsets = false
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
    private var mSplitDetailsPane: FrameLayout? = null
    private var navDrawerButtonBadge: Badge = Badge.NONE
    private var headerButtonBadge: Badge = Badge.NONE
    private lateinit var mSlideViewPane: FrameLayout

    @JvmField
    internal var navRailDrawerButton: ImageButton? = null

    @JvmField
    internal var navRailDrawerButtonBadgeView: TextView? = null

    @Volatile
    private var mCurrentState: DrawerState = DrawerState.CLOSE

    @Volatile
    private var sSlideOffset = 0f

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

        val cornerRadius = DEFAULT_DRAWER_RADIUS.dpToPx(resources)
        seslSetRoundedCornerOn(cornerRadius)
        mDrawerPane.apply {
            outlineProvider = DrawerOutlineProvider(cornerRadius)
            clipToOutline = true
        }

        setNavigationButtonTooltip(context.getText(R.string.oui_navigation_drawer))
        setDualDetailPane(false)

    }

    override fun setHandleInsets(handle: Boolean) {
        handleInsets = handle
        requestApplyInsets()
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
        if (enable) {
            if (mSplitDetailsPane == null) {
                mSplitDetailsPane =
                    mSlideViewPane.findViewById<ViewStub>(R.id.viewstub_split_details_container)
                        .inflate() as FrameLayout
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
        (mDrawerPane.outlineProvider as? DrawerOutlineProvider)?.let {
            it.cornerRadius = px
        } ?: run {
            mDrawerPane.outlineProvider = DrawerOutlineProvider(px)
            mDrawerPane.clipToOutline = true
        }
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

    override var isLocked: Boolean
        get() = seslGetLock()
        set(value) {
            if (seslGetLock() == value) return
            seslSetLock(value)
            navRailDrawerButton!!.isEnabled = !value
            postDelayed({
                mDrawerItemsContainer?.let { container ->
                    fun disableViewGroup(viewGroup: ViewGroup) {
                        for (i in 0 until viewGroup.childCount) {
                            val child = viewGroup.getChildAt(i)
                            child.isEnabled = !value
                            if (child is ViewGroup) {
                                disableViewGroup(child)
                            }
                        }
                    }
                    disableViewGroup(container)
                }
            }, 50)
        }

    override val isDrawerOpen: Boolean get() = mCurrentState == DrawerState.OPEN

    override val isDrawerOpenOrIsOpening: Boolean
        get() = mCurrentState == DrawerState.OPEN || mCurrentState == DrawerState.OPENING

    private var mDrawerStateListener: ((state: DrawerState) -> Unit)? = null

    override fun setOnDrawerStateChangedListener(action: ((DrawerState) -> Unit)?) {
        mDrawerStateListener = action
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
    }

    private fun updateNavBadgeScale() {
        navRailDrawerButtonBadgeView!!.apply {
            val scale = 1f - sSlideOffset
            scaleX = scale
            scaleY = scale
            alpha = scale
        }
    }

    internal fun updateContentMinSidePadding(@Px padding: Int) =
        mSlideViewPane.updatePadding(left = padding, right = padding)

    override var showNavigationButtonAsBack = false
        set(value) {
            if (value == field) return
            field = value
            updateNavButton()
        }

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
                    navigationIcon = null
                    navigationContentDescription = null
                    setNavigationOnClickListener(null)
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
            }
        } else {
            Log.e(TAG, "setHeaderButtonBadge: `drawer_header_button_badge` id is not set in custom header view")
        }
    }

    @SuppressLint("NewApi")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (handleInsets) {
            insets.also {
                val systemBarsInsets = it.getInsets(WindowInsetsCompat.Type.systemBars())
                val topInset = systemBarsInsets.top
                val bottomInset = max(
                    systemBarsInsets.bottom,
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                )

                mSlideViewPane.setPadding(
                    systemBarsInsets.left,
                    topInset,
                    systemBarsInsets.right,
                    bottomInset
                )
                mDrawerPane.updateLayoutParams<MarginLayoutParams> {
                    topMargin =
                        topInset + resources.getDimensionPixelSize(androidx.appcompat.R.dimen.sesl_action_bar_top_padding)
                    bottomMargin = bottomInset
                }
            }
        }
        return super.onApplyWindowInsets(insets)
    }


    private var _backHandler: DrawerLayoutBackHandler<OneUIDrawerLayout>? = null

    override fun getOrCreateBackHandler(drawerLayout: OneUIDrawerLayout) : DrawerLayoutBackHandler<OneUIDrawerLayout> {
        return _backHandler
            ?: DrawerLayoutBackHandler(drawerLayout, NavDrawerBackAnimator(this@SemSlidingPaneLayout))
                .also { _backHandler = it }
    }

    private inner class SlidingPaneSlideListener : PanelSlideListener {
        override fun onPanelClosed(panel: View) = dispatchDrawerStateChange(0f)
        override fun onPanelOpened(panel: View) = dispatchDrawerStateChange(1f)
        override fun onPanelSlide(panel: View, slideOffset: Float) =
            dispatchDrawerStateChange(slideOffset)
    }

    companion object {
        private const val TAG = "SemSlidingPaneLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        const val DEFAULT_OVERHANG_SIZE = 32 // dp
    }

}