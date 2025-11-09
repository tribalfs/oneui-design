package dev.oneuiproject.oneui.layout

import android.content.Context
import android.content.res.Resources
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
import androidx.core.content.withStyledAttributes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.Openable
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.util.DrawerLayoutInterface
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.layout.internal.widget.SemDrawerLayout
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isPortrait


/**
 * Custom DrawerLayout extending [ToolbarLayout]. Looks and behaves the same as the one in Apps from Samsung.
 *
 * **Important:**
 * - This view must be hosted within an [AppCompatActivity][androidx.appcompat.app.AppCompatActivity]
 * as it relies on AppCompat-specific features and theming. Otherwise, it will result to runtime exceptions.
 *
 * ## XML Attributes
 * The following XML attributes are supported in addition to attributes of [ToolbarLayout]:
 *
 * - `app:drawerBackAnimation`: Enable or disable drawer's predictive back animation.
 *       This applies only to api34+. This is set to false by default.
 *
 *  ## Setting the locations of the direct child views
 *  The locations of direct child views can be set by setting any of the following values to the
 *  `app:layout_location` attribute of the child view in addition to the location values allowed in [ToolbarLayout]:
 *
 * - `drawer_header` - Sets the view as a custom header row within the drawer panel.
 *    Serves as the topmost element in the drawer.
 * - `drawer_panel` - Adds the view to inside the drawer panel below the `drawer_header`.
 */
open class DrawerLayout(context: Context, attrs: AttributeSet?) :
    ToolbarLayout(context, attrs), Openable {

    enum class DrawerState {
        OPEN,
        CLOSE,
        CLOSING,
        OPENING
    }

    enum class FeatureState {
        UNSET, ENABLED, DISABLED
    }
    
    fun interface DrawerStateListener{
        fun onStateChanged(state: DrawerState)
    }

    fun interface DrawerLockListener{
        fun onLockChanged(isLocked: Boolean)
    }

    fun interface DrawerWidthProvider {
        fun getWidth(resources: Resources, isLargeScreenMode: Boolean, isMultiWindow: Boolean): Int
    }

    protected var drawerLockListener: DrawerLockListener? = null
        private set

    protected var enableDrawerBackAnimation: Boolean = false
        private set

    protected var drawerEnabledState: FeatureState = FeatureState.UNSET
        private set

    init { initLayoutAttrs(attrs) }

    override val backHandler: DrawerLayoutBackHandler<*> get() = containerLayout.getOrCreateBackHandler(this@DrawerLayout)

    override fun getDefaultLayoutResource() = R.layout.oui_des_layout_drawerlayout_main
    override fun getDefaultNavigationIconResource(): Int = R.drawable.oui_des_ic_ab_drawer

    private var drawerPreviewOpen = false

    private fun initLayoutAttrs(attrs: AttributeSet?) {
        setWillNotDraw(background == null)

        context.withStyledAttributes(attrs, R.styleable.DrawerLayout, 0, 0) {
            enableDrawerBackAnimation = getBoolean(R.styleable.DrawerLayout_drawerBackAnimation, false)
            if (isInEditMode) {
                drawerPreviewOpen = getBoolean(R.styleable.DrawerLayout_isOpen, false)
            }
        }
    }

    private val semDrawerLayout by lazy<SemDrawerLayout>(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.drawer_layout)
    }

    internal open val containerLayout: DrawerLayoutInterface get() = semDrawerLayout

    override val navButtonsHandler: NavButtonsHandler get() = semDrawerLayout

    override fun onFinishInflate() {
        super.onFinishInflate()
        containerLayout.setOnDrawerStateChangedListener { updateOnBackCallbackState() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode && drawerPreviewOpen) {
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

    protected open fun updateDrawerLock() {
        val newLockState = isActionMode || isSearchMode || drawerEnabledState == FeatureState.DISABLED
        if (containerLayout.isLocked != newLockState) {
            containerLayout.isLocked = newLockState
            drawerLockListener?.onLockChanged(newLockState)
        }
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
     * Setup the drawer header button,
     * located at the top right corner of the drawer panel.
     *
     * @param icon The drawable to use as the icon.
     * @param tint An optional tint to apply to the icon.
     * @param tooltipText The tooltip text to set for the button.
     * @param listener The click listener to set for the button.
     */
     fun setupHeaderButton(
        icon: Drawable,
        @ColorInt tint: Int? = null,
        tooltipText: CharSequence,
        listener: OnClickListener
    ){
        navButtonsHandler.apply {
            setHeaderButtonIcon(icon, tint)
            setHeaderButtonTooltip(tooltipText)
            setHeaderButtonOnClickListener(listener)
        }
    }

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

    /**
     * Sets the listener for changes in the drawer's lock state.
     *
     * @param listener An instance of [DrawerLockListener] to receive lock state change events.
     */
    fun setDrawerLockListener(listener: DrawerLockListener?) {
        drawerLockListener = listener
        listener?.onLockChanged(containerLayout.isLocked)
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
    
    /**
     * Enable or disable the drawer.
     * When the drawer is disabled, the navigation button will be hidden
     * and the drawer pane will not be accessible.
     *
     * @param enabled True to enable the drawer, false to disable it.
     */
    open fun setDrawerEnabled(enabled: Boolean){
        val newState = if (enabled) FeatureState.ENABLED else FeatureState.DISABLED
        if (drawerEnabledState == newState) return
        drawerEnabledState = newState
        if (isAttachedToWindow) {
            updateDrawerState()
        }
    }

    protected open fun updateDrawerState(animate: Boolean = true){
        updateDrawerLock()
        if (drawerEnabledState != FeatureState.DISABLED) {
            showNavigationButton = true
            setNavigationButtonTooltip(resources.getText(R.string.oui_des_navigation_drawer))
          } else {
            showNavigationButton = false
            setNavigationButtonTooltip(null)
        }
    }

    /**
     * Sets a custom [DrawerWidthProvider] to determine the drawer's width.
     *
     * @param drawerWidthProvider The provider to use for calculating drawer width.
     * By default this is set to [DEFAULT_DRAWER_WIDTH_PROVIDER].
     */
    open fun setDrawerWidthProvider (drawerWidthProvider: DrawerWidthProvider) =
        containerLayout.setDrawerWidthProvider(drawerWidthProvider)

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.drawerEnabledState = drawerEnabledState.ordinal
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        /** Only restore if drawerEnabled has not been explicitly set with [setDrawerEnabled] */
        if (drawerEnabledState == FeatureState.UNSET && drawerEnabledState.ordinal != state.drawerEnabledState) {
            drawerEnabledState = FeatureState.entries[state.drawerEnabledState]
            updateDrawerState(false)
        }
        super.onRestoreInstanceState(state.superState)
    }

    private class SavedState : AbsSavedState {
        var drawerEnabledState: Int = 0

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            drawerEnabledState = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(drawerEnabledState)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object :
                    ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(parcel: Parcel,
                                                  loader: ClassLoader): SavedState = SavedState(parcel, loader)
                    override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel, SavedState::class.java.classLoader)
                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }

    companion object {
        private const val TAG = "DrawerLayout"
        private const val DRAWER_HEADER = 4
        const val DRAWER_PANEL = 5

        /**
         * Default provider for calculating the drawer width.
         *
         *  **Large Screen Mode:**
         *   - Width is 40% of the screen width.
         *   - Maximum width is capped at 318dp. In landscape mode, except in multi-window mode,
         *     the maximum width is 378dp.
         *
         *  **Normal Screen Mode (based on screen width in dp):**
         *   - `>= 960dp`: 318dp
         *   - `600dp - 959dp`: 40% of screen width
         *   - `480dp - 599dp`: 60% of screen width
         *   - `< 480dp`: 86% of screen width
         */
        @JvmField
        val DEFAULT_DRAWER_WIDTH_PROVIDER = DrawerWidthProvider { resources, isLargeScreenMode, isMultiWindow, ->
            if (isLargeScreenMode) {
                val displayMetrics = resources.displayMetrics
                val isPortraitOrMultiWindow = isPortrait(resources.configuration) || isMultiWindow
                val maxWidth =  (if (isPortraitOrMultiWindow) 318 else 378).dpToPx(displayMetrics)
                (displayMetrics.widthPixels * 0.4).toInt().coerceAtMost(maxWidth)
            } else {
                resources.configuration.screenWidthDp.let {
                    when {
                        it >= 960 -> 318f
                        it in 600..959 -> it * 0.40f
                        it in 480..599 -> it * 0.60f
                        else -> it * 0.86f
                    }.dpToPx(resources)
                }
            }
        }
    }
}

