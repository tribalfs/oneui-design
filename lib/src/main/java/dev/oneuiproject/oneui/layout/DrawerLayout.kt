package dev.oneuiproject.oneui.layout

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.core.content.res.use
import androidx.core.view.doOnLayout
import androidx.customview.widget.Openable
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.layout.internal.widget.SemDrawerLayout


/**
 * Custom DrawerLayout extending [ToolbarLayout]. Looks and behaves the same as the one in Apps from Samsung.
 */
open class DrawerLayout(context: Context, attrs: AttributeSet?) :
    ToolbarLayout(context, attrs), Openable {

    enum class DrawerState {
        OPEN,
        CLOSE,
        CLOSING,
        OPENING
    }

    internal var enableDrawerBackAnimation: Boolean = false
        private set

    init { initLayoutAttrs(attrs) }

    override val backHandler: BackHandler get() = containerLayout.getOrCreateBackHandler(this@DrawerLayout)

    override fun getDefaultLayoutResource() = R.layout.oui_layout_drawerlayout_main
    override fun getDefaultNavigationIconResource(): Int = R.drawable.oui_ic_ab_drawer

    private var mDrawerPreviewOpen = false

    private fun initLayoutAttrs(attrs: AttributeSet?) {
        setWillNotDraw(background == null)

        context.theme.obtainStyledAttributes(
            attrs, R.styleable.DrawerLayout, 0, 0
        ).use {
            enableDrawerBackAnimation = it.getBoolean(R.styleable.DrawerLayout_drawerBackAnimation, false)
            if (isInEditMode) {
                mDrawerPreviewOpen = it.getBoolean(R.styleable.DrawerLayout_isOpen, false)
            }
        }
    }

    private val mSemDrawerLayout by lazy<SemDrawerLayout>(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.drawer_layout)
    }

    open val containerLayout: DrawerLayoutInterface get() = mSemDrawerLayout

    override val navButtonsHandler: NavButtonsHandler get() = mSemDrawerLayout

    override fun onFinishInflate() {
        super.onFinishInflate()
        containerLayout.apply {
            setHandleInsets(handleInsets)
            setOnDrawerStateChangedListener { updateOnBackCallbackState() }
            setNavigationButtonTooltip(resources.getText(R.string.oui_navigation_drawer))
            if (isInEditMode && mDrawerPreviewOpen) {
                containerLayout.open(false)
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        when ((params as ToolbarLayoutParams).layoutLocation) {
            DRAWER_HEADER -> containerLayout.setCustomHeader(child, params)
            DRAWER_PANEL -> containerLayout.addDrawerContent(child, params)
            else -> super.addView(child, index, params)
        }
    }

    override fun updateOnBackCallbackState() {
        updateDrawerLock()
        super.updateOnBackCallbackState()
    }

    internal open fun updateDrawerLock() {
        containerLayout.isLocked = isActionMode || isSearchMode
    }

    fun isDrawerLocked(): Boolean = containerLayout.isLocked


    //
    // Drawer methods
    //
    /**Show a margin at the top of the drawer panel. Some Apps from Samsung do have this.*/
    @Deprecated("This is now no op.")
    fun showDrawerTopMargin(show: Boolean) {}

    /**Set a custom radius for the drawer panel's edges.*/
    fun setDrawerCornerRadius(@Dimension dp: Float) = containerLayout.setDrawerCornerRadius(dp)

    /**Set a custom radius for the drawer panel's edges.*/
    fun setDrawerCornerRadius(@Px px: Int) = containerLayout.setDrawerCornerRadius(px)

    /**
     * Sets the icon for the drawer header button, located in the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     */
    @Deprecated("Use setHeaderButtonIcon() instead.",
        replaceWith = ReplaceWith("setHeaderButtonIcon(icon)"))
    fun setDrawerButtonIcon(icon: Drawable?) = setHeaderButtonIcon(icon)

    /**
     * Sets the icon for the drawer header button, located in the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     * @param tint An optional tint to apply to the icon.
     */
    @JvmOverloads
    open fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int? = null) =
        navButtonsHandler.setHeaderButtonIcon(icon, tint)

    /**
     * Set the tooltip of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     */
    @Deprecated(
        "Use setHeaderButtonTooltip() instead.",
        replaceWith = ReplaceWith("setHeaderButtonTooltip(tooltipText)")
    )
    fun setDrawerButtonTooltip(tooltipText: CharSequence?) =
        navButtonsHandler.setHeaderButtonTooltip(tooltipText)

    /**
     * Set the tooltip of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     */
    open fun setHeaderButtonTooltip(tooltipText: CharSequence?) =
        navButtonsHandler.setHeaderButtonTooltip(tooltipText)

    /**
     * Set the click listener of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    @Deprecated(
        "Use setHeaderButtonOnClickListener(listener) instead",
        ReplaceWith("setHeaderButtonOnClickListener(listener)")
    )
    fun setDrawerButtonOnClickListener(listener: OnClickListener?) =
        setHeaderButtonOnClickListener(listener)

    /**
     * Set the click listener of the drawer button.
     * The drawer button is the button in the top right corner of the drawer panel.
     */
    open fun setHeaderButtonOnClickListener(listener: OnClickListener?) =
        navButtonsHandler.setHeaderButtonOnClickListener(listener)

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
    fun setDrawerButtonBadge(badge: Badge) = setHeaderButtonBadge(badge)

    /**
     * Set the badge of the drawer header button.
     * The drawer header button is the button in the top right corner of the drawer panel.
     *
     * @param badge The [badge][dev.oneuiproject.oneui.layout.Badge] to setn.
     */
    open fun setHeaderButtonBadge(badge: Badge) = navButtonsHandler.setHeaderButtonBadge(badge)

    override fun isOpen(): Boolean = containerLayout.isDrawerOpen

    override fun open() = setDrawerOpen(true, animate = true)

    override fun close() = setDrawerOpen(false, animate = true)

    /**
     * Open or close the drawer panel with an optional animation.
     *
     * @param animate whether or not to animate the opening and closing
     */
    open fun setDrawerOpen(open: Boolean, animate: Boolean)  = doOnLayout{
        if (open) {
            containerLayout.open(animate)
        } else {
            containerLayout.close(animate)
        }
    }

    /**
     * Set callback to be invoked when the drawer state changes.
     *
     * @param listener lambda to be invoked with the new [DrawerState]
     */
    fun setDrawerStateListener(listener: ((state: DrawerState) -> Unit)?) {
        containerLayout.setOnDrawerStateChangedListener {
            updateOnBackCallbackState()
            listener?.invoke(it)
        }
    }

    override fun getBackCallbackStateUpdate() =
        (shouldCloseDrawer || super.getBackCallbackStateUpdate())

    internal open val shouldCloseDrawer get() =
        (containerLayout.isDrawerOpenOrIsOpening && !containerLayout.isLocked)

    internal inline val shouldAnimateDrawer get() =
        enableDrawerBackAnimation && containerLayout.isDrawerOpen && !containerLayout.isLocked

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets =
        if (handleInsets) insets else super.onApplyWindowInsets(insets)

    companion object {
        private const val TAG = "DrawerLayout"
        private const val DEFAULT_DRAWER_RADIUS = 15f
        private const val DRAWER_HEADER = 4
        const val DRAWER_PANEL = 5

    }
}

