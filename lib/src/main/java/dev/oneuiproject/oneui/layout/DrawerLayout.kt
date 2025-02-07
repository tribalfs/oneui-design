package dev.oneuiproject.oneui.layout

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.core.content.res.use
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.customview.view.AbsSavedState
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
    
    fun interface DrawerStateListener{
        fun onStateChanged(state: DrawerState)
    }

    internal var enableDrawerBackAnimation: Boolean = false
        private set

    protected var drawerEnabled = true
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
        containerLayout.setOnDrawerStateChangedListener { updateOnBackCallbackState() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode && mDrawerPreviewOpen) {
            containerLayout.open(false)
        }
        updateDrawerState()
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
        containerLayout.isLocked = isActionMode || isSearchMode || !drawerEnabled
    }

    fun isDrawerLocked(): Boolean = containerLayout.isLocked


    //
    // Drawer methods
    //

    /**Set a custom radius for the drawer panel's edges.*/
    fun setDrawerCornerRadius(@Dimension dp: Float) = containerLayout.setDrawerCornerRadius(dp)

    /**Set a custom radius for the drawer panel's edges.
     * @param px The radius in pixels. Set to -1 to restore the default radius.
     */
    fun setDrawerCornerRadius(@Px px: Int) = containerLayout.setDrawerCornerRadius(px)


    /**
     * Sets the icon for the drawer header button,
     * located at the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     * @param tint An optional tint to apply to the icon.
     */
    @JvmOverloads
    open fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int? = null) =
        navButtonsHandler.setHeaderButtonIcon(icon, tint)


    /**
     * Set the tooltip of the drawer header button,
     * located at the top right corner of the drawer panel.
     */
    open fun setHeaderButtonTooltip(tooltipText: CharSequence?) =
        navButtonsHandler.setHeaderButtonTooltip(tooltipText)


    /**
     * Set the click listener of the drawer header button,
     * located at the top right corner of the drawer panel.
     */
    open fun setHeaderButtonOnClickListener(listener: OnClickListener?) =
        navButtonsHandler.setHeaderButtonOnClickListener(listener)

    /**
     * Set badge to each of the navigation button and drawer header button (i.e.
     * the button at the top right corner of the drawer panel).
     *
     * @param navigationButtonBadge The [Badge] to set for navigation button.
     * @param headerButtonBadge The [Badge] to set for drawer header button.
     * @see setNavigationButtonBadge
     * @see setHeaderButtonBadge
     */
    fun setButtonBadges(navigationButtonBadge: Badge, headerButtonBadge: Badge) {
        setNavigationButtonBadge(navigationButtonBadge)
        setHeaderButtonBadge(headerButtonBadge)
    }

    /**
     * Set a badge of the drawer header button,
     * located at the top right corner of the drawer panel.
     *
     * @param badge The [Badge] to set.
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
    fun setDrawerStateListener(listener: DrawerStateListener?) {
        containerLayout.setOnDrawerStateChangedListener {
            updateOnBackCallbackState()
            listener?.onStateChanged(it)
        }
    }

    /**The current slide offset of the drawer pane.*/
    val drawerOffset get() = containerLayout.getDrawerSlideOffset()

    override fun getBackCallbackStateUpdate() =
        (shouldCloseDrawer || super.getBackCallbackStateUpdate())

    internal open val shouldCloseDrawer get() =
        (containerLayout.isDrawerOpenOrIsOpening && !containerLayout.isLocked)

    internal inline val shouldAnimateDrawer get() =
        enableDrawerBackAnimation && containerLayout.isDrawerOpen && !containerLayout.isLocked

    override fun applyWindowInsets(insets: WindowInsetsCompat) =
        containerLayout.applyWindowInsets(insets, isImmersiveScroll)
    
    open fun setDrawerEnabled(drawerEnabled: Boolean){
        if (this.drawerEnabled == drawerEnabled) return
        this.drawerEnabled = drawerEnabled
        if (isAttachedToWindow) updateDrawerState()
    }

    internal open fun updateDrawerState(animate: Boolean = true){
        updateDrawerLock()
        if (drawerEnabled) {
            showNavigationButton = true
            setNavigationButtonTooltip(resources.getText(R.string.oui_navigation_drawer))
          } else {
            showNavigationButton = false
            setNavigationButtonTooltip(null)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.drawerEnabled = drawerEnabled
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        if (this.drawerEnabled != state.drawerEnabled) {
            this.drawerEnabled = state.drawerEnabled
            updateDrawerState(false)
        }
        super.onRestoreInstanceState(state.superState)
    }

    private class SavedState : AbsSavedState {
        var drawerEnabled = true

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            drawerEnabled = parcel.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (drawerEnabled) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object :
                    ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(parcel: Parcel,
                                                  loader: ClassLoader): SavedState = SavedState(parcel, null)
                    override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel, null)
                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }

    companion object {
        private const val TAG = "DrawerLayout"
        private const val DRAWER_HEADER = 4
        const val DRAWER_PANEL = 5

    }
}

