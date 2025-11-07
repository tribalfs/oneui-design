@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowInsets
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_ALL
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_TOP_LEFT
import androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_TOP_RIGHT
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SemSearchView
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.applyActionModeSearchStyle
import androidx.appcompat.widget.applyThemeColors
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.model.AppBarModel
import com.google.android.material.appbar.model.view.AppBarView
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.fitsSystemWindows
import dev.oneuiproject.oneui.ktx.isDescendantOf
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.pxToDp
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneui.layout.ToolbarLayout.ActionModeListener
import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners.ALL
import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners.BOTTOM
import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners.NONE
import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners.TOP
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode.Concurrent
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode.Dismiss
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode.NoDismiss
import dev.oneuiproject.oneui.layout.internal.backapi.BackHandler
import dev.oneuiproject.oneui.layout.internal.backapi.OnBackCallbackDelegateCompat
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutBackHandler
import dev.oneuiproject.oneui.layout.internal.delegate.ToolbarLayoutButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.ImmersiveScrollHelper
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.hasShowingChild
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.navBarCanImmScroll
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.setVisibility
import dev.oneuiproject.oneui.utils.internal.BADGE_LIMIT_NUMBER
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.MenuSynchronizer
import dev.oneuiproject.oneui.utils.MenuSynchronizer.State
import dev.oneuiproject.oneui.utils.applyEdgeToEdge
import dev.oneuiproject.oneui.utils.internal.badgeCountToText
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.Companion.MARGIN_PROVIDER_ADP_DEFAULT
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MarginProvider
import dev.oneuiproject.oneui.widget.RoundedFrameLayout
import dev.oneuiproject.oneui.widget.RoundedLinearLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.abs
import androidx.appcompat.R as appcompatR

/**
 *  Custom collapsing Appbar like in any App from Samsung. Includes a [SearchView] and Samsung's ActionMode.
 *
 * **Important:**
 * - This view must be hosted within an [AppCompatActivity][androidx.appcompat.app.AppCompatActivity]
 * as it relies on AppCompat-specific features and theming. Otherwise, it will result to runtime exceptions.
 *
 * ## XML Attributes
 * The following XML attributes are supported:
 *
 * - `app:title`: Sets the toolbar's collapsed and expanded title.
 *    Can also be set programmatically via [setTitle].
 * - `app:subtitle`: Sets the toolbar's collapsed and expanded subtitle.
 *    Can also be set programmatically via [setSubtitle].
 * - `app:expandable`: Enables or disables the expanding toolbar.
 *    Also accessible via the [isExpandable] property. Default is `true`.
 * - `app:expanded`: Sets the initial expanded/collapsed state when `app:expandable` is `true`.
 *    Also accessible via the [isExpanded] property. Default is `true`.
 * - `app:navigationIcon`: Sets the toolbar's navigation icon when [showNavigationButtonAsBack] is `false` (default).
 *    Tooltip should be set programmatically using [setNavigationButtonTooltip].
 * - `app:showNavButtonAsBack`: Displays the navigation button as a "back/up" affordance.
 *    When `true`, the button shows a back icon, sets the tooltip to "Navigate up", and invokes [androidx.activity.OnBackPressedDispatcher.onBackPressed] when clicked.
 *    Also accessible via the [showNavigationButtonAsBack] property. Default is `false`.
 * - `app:showSwitchBar`: Shows a [SeslSwitchBar] below the AppBarLayout.
 *    Accessing the [switchBar] property automatically sets this to `true`. Default is `false`.
 * - `app:handleInsets`: Handles system and IME insets internally. Default is `true`.
 * - `app:edgeInsetHorizontal`: Sets the horizontal edge inset and rounded corner radius for the main content.
 *    Does not affect horizontal padding. Default is `10dp`.
 * - `app:toolbarGravity`: Sets the gravity for the toolbar(s). Default is `bottom`.
 *
 *  ## Setting the locations of the direct child views
 *  The locations of direct child views can be set by setting any of the following values to the
 *  `app:layout_location` attribute of the child view
 *
 * - `main_content` (default): The child will be added as the primary content of this layout.
 *    This view will have adaptive margins applied, which are managed by the layout.
 * - `appbar_header`" Sets this view as the custom title view for the expanded AppBarLayout.
 *     This is equivalent to invoking the `setCustomTitleView` method.
 * - `footer`: Adds the view to the bottom of the layout.
 * - `root`: Adds the view as a direct child of the CoordinatorLayout.
 *    Typically overlaps with the `main_content` view.
 */
open class ToolbarLayout @JvmOverloads constructor(
    @JvmField protected var context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    interface ActionModeListener {
        /** Called at the start of [startActionMode].
         * These object references passed in the parameter should be used
         * during the lifetime of this action mode.
         *
         * @param menu The [Menu] to be used for action menu items.
         * @param menuInflater The [MenuInflater] to use for inflating the menu.
         *
         * Inflate the menu items for this action mode session using this menu and menuInflater.
         *
         * @see onMenuItemClicked */
        fun onInflateActionMenu(menu: Menu, menuInflater: MenuInflater)

        /** Called when the current action mode session is ended.*/
        fun onEndActionMode()

        /** Called when an action mode menu item is clicked.
         * @see onInflateActionMenu*/
        fun onMenuItemClicked(item: MenuItem): Boolean

        /** Called when the 'All' selector is clicked. This will not be triggered with [updateAllSelector].*/
        fun onSelectAll(isChecked: Boolean)
    }

    /**
     * Options for the 'on back' behavior of when on search mode.
     * @property DISMISS
     * @property CLEAR_DISMISS]
     * @property CLEAR_CLOSE]
     *
     * @see startSearchMode
     * @see endSearchMode
     */
    enum class SearchModeOnBackBehavior {
        /**
         * Swipe gesture or back button press ends the search mode
         * and then unregisters it's on back callback. This is intended to be used
         * in a search fragment of a multi-fragment activity
         */
        DISMISS,
        /** Similar to [DISMISS] but checks and clears the non-empty input query first. */
        CLEAR_DISMISS,
        /** Similar to [CLEAR_DISMISS] but it unregisters it's onBackPressed callback
         * without ending the search mode. Intended to be  used on a dedicated search activity. */
        CLEAR_CLOSE
    }


    /**
     * The configuration for rounded corners that can be applied to the main content
     * located between the app bar and the footer.
     * @property ALL
     * @property TOP
     * @property BOTTOM
     * @property NONE
     */
    enum class MainRoundedCorners {
        /** Rounded corners on all corners */
        ALL,
        /** Rounded corners only on top corners */
        TOP,
        /** Rounded corners only on bottom corners */
        BOTTOM,
        /** No rounded corners on all corners */
        NONE
    }

    /**
     * Options for search mode behavior when [starting action mode][startActionMode]:
     *  Choose either [Dismiss], [NoDismiss] or [Concurrent].
     */
    sealed interface SearchOnActionMode {
        /**
         * Search mode will be dismissed if active. This is the default behavior if not specified.
         */
        data object Dismiss : SearchOnActionMode
        /**
         * Search mode will not be dismissed if active. It will return to search interface once action mode is ended.
         */
        data object NoDismiss : SearchOnActionMode
        /**
         * Same as [NoDismiss] but also show the search interface while in action mode.
         *
         * The [searchModeListener] is required if the search mode is inactive when initiating action mode.
         * If the search mode is active and no [SearchModeListener] is specified,
         * the existing listener for the current search mode will be used.
         * If specified, it overrides the current search mode listener.
         *
         * @property searchModeListener The listener for search mode events.
         * This is nullable, but a non-null listener is required if search mode is inactive.
         */
        data class Concurrent(internal val searchModeListener: SearchModeListener?) : SearchOnActionMode
    }

    internal val activity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity }

    private var allSelectorItemsCount = SELECTED_ITEMS_UNSET
    private var allSelectorEnabled = true
    private var allSelectorChecked: Boolean? = null

    private var actionModeListener: ActionModeListener? = null

    internal var isSofInputShowing: Boolean = isSoftKeyboardShowing
        private set

    internal open val backHandler: BackHandler by lazy(LazyThreadSafetyMode.NONE) {
        ToolbarLayoutBackHandler(this@ToolbarLayout)
    }

    private val onBackCallbackDelegate: OnBackCallbackDelegateCompat by lazy {
        OnBackCallbackDelegateCompat(activity!!, this, backHandler)
    }

    internal open fun updateOnBackCallbackState() {
        if (isInEditMode) return
        if (getBackCallbackStateUpdate()) {
            onBackCallbackDelegate.startListening(true)
        } else {
            onBackCallbackDelegate.stopListening()
        }
    }

    /**
     * return true if on back callback should be registered.
     */
    @CallSuper
    protected open fun getBackCallbackStateUpdate(): Boolean {
        return when {
            //Let the system handle the dismissing
            //of the keyboard
            isSofInputShowing -> false
            isActionMode -> true
            isSearchMode -> {
                when (searchModeOBPBehavior) {
                    DISMISS, CLEAR_DISMISS -> true
                    CLEAR_CLOSE -> _searchView!!.query.isNotEmpty()
                }
            }

            else -> false
        }
    }

    private var scrollDelta = 0
    private var navBarInsetBottom = 0
    private var footerHeight = 0
    private var bottomOffsetListeners: MutableList<WeakReference<(bottomOffset: Float) -> Unit>>? = null
    private var appBarOffsetListeners: MutableList<WeakReference<(bottomOffset: Float) -> Unit>>? = null
    private var appBarOffsetListener = AppBarOffsetListener()

    private var layoutRes: Int = 0

    private var _expandable: Boolean = false
    private var expandedPortrait: Boolean = false
    private var _collapsedTitle: CharSequence? = null
    private var _titleExpanded: CharSequence? = null
    private var _collapsedSubtitle: CharSequence? = null
    private var _subtitleExpanded: CharSequence? = null
    private var navigationIcon: Drawable? = null

    private var _showNavigationButtonAsBack = false
    private var _showNavigationButton = false

    private var _mainContainer: RoundedFrameLayout? = null
    internal val mainContainer: FrameLayout get() = _mainContainer!!

    private var _mainRoundedCorners: MainRoundedCorners = ALL

    private lateinit var mainContainerParent: LinearLayout
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout

    /** The [AppBarLayout] use by this view.*/
    lateinit var appBarLayout: AppBarLayout
        private set

    private lateinit var _mainToolbar: Toolbar

    /** The main [Toolbar] use by this view.*/
    val toolbar: Toolbar get() = _mainToolbar

    private lateinit var adpCoordinatorLayout: AdaptiveCoordinatorLayout
    private lateinit var bottomRoundedCorner: RoundedLinearLayout
    internal lateinit var footerParent: LinearLayout
        private set

    private var actionModeToolbar: Toolbar? = null
    private lateinit var actionModeSelectAll: LinearLayout
    private lateinit var actionModeCheckBox: CheckBox
    private lateinit var actionModeTitleTextView: TextView
    private var updateAllSelectorJob: Job? = null

    private var customFooterContainer: FrameLayout? = null
    private lateinit var bottomActionModeBar: BottomNavigationView

    private var searchToolbar: Toolbar? = null

    private var toolbarGravity = Gravity.BOTTOM

    private var _searchView: SemSearchView? = null

    /**
     * The [SearchView] for [search mode][startSearchMode] or [action mode search][SearchOnActionMode].
     *
     * Note: Apps should access this view using [SearchModeListener] callback.
     */
    internal val searchView: SearchView? get() = _searchView

    private var searchModeListener: SearchModeListener? = null

    @JvmField
    internal var searchModeOBPBehavior = CLEAR_DISMISS
    private var searchMenuItem: MenuItem? = null

    private var searchOnActionMode: SearchOnActionMode? = null
    private var showActionModeSearchPending = false

    private var menuSynchronizer: MenuSynchronizer? = null
    private var forcePortraitMenu: Boolean = false
    private var syncMenuPending = false
    private var edgeInsetHorizontal = 10f //dp

    private var _showSwitchBar = false

    /**
     * The [SeslSwitchbar][androidx.appcompat.widget.SeslSwitchBar] shown below the app bar.
     * Invoking this will automatically inflate and add the switch bar to the layout.
     */
    open val switchBar by lazy {
        mainContainerParent.findViewById<ViewStub>(R.id.viewstub_tbl_switchbar)
            .inflate() as SeslSwitchBar
    }

    /**
     * Check if SearchMode is currently active.
     * @see startSearchMode
     * @see endSearchMode
     */
    var isSearchMode: Boolean = false
        private set

    /**
     * Checks if the ActionMode is currently active.
     * @see startActionMode
     * @see endActionMode
     */
    var isActionMode: Boolean = false
        private set

    private var _handleInsets: Boolean = false

    private var _landscapeHeightForStatusBar: Int = 420

    /**
     * Listener for the search mode.
     *
     * @see startSearchMode
     * @see endSearchMode
     */
    interface SearchModeListener : SearchView.OnQueryTextListener {
        /**
         * Called when search mode is toggled active or inactive.
         */
        fun onSearchModeToggle(searchView: SearchView, isActive: Boolean)
    }

    protected open fun getDefaultLayoutResource(): Int = R.layout.oui_des_layout_tbl_main
    protected open fun getDefaultNavigationIconResource(): Int? = null


    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        when ((params as ToolbarLayoutParams).layoutLocation) {
            APPBAR_HEADER -> setCustomTitleView(
                child,
                CollapsingToolbarLayout.LayoutParams(params)
            )

            FOOTER -> customFooterContainer!!.addView(child, params)
            ROOT, ROOT_BOUNDED -> {
                if (params.layoutLocation == ROOT) {
                    child.setTag(R.id.tag_side_margin_excluded, true)
                }
                adpCoordinatorLayout.addView(child, cllpWrapper(params as LayoutParams))
            }
            else -> _mainContainer?.addView(child, params) ?: super.addView(child, index, params)
        }
    }

    override fun generateDefaultLayoutParams() = ToolbarLayoutParams(LayoutParams(MATCH_PARENT, WRAP_CONTENT))

    override fun generateLayoutParams(attrs: AttributeSet) = ToolbarLayoutParams(context, attrs)

    internal open val navButtonsHandler: NavButtonsHandler by lazy(LazyThreadSafetyMode.NONE) {
        ToolbarLayoutButtonsHandler(_mainToolbar)
    }

    init {
        setWillNotDraw(true)
        activity?.applyEdgeToEdge()
        orientation = VERTICAL
        context.withStyledAttributes(attrs, R.styleable.ToolbarLayout, 0, 0) {
            layoutRes = getResourceId(
                R.styleable.ToolbarLayout_android_layout,
                getDefaultLayoutResource()
            )
            getDimension(R.styleable.ToolbarLayout_edgeInsetHorizontal, 10f).let{px ->
                if (px != 10f) edgeInsetHorizontal = px.pxToDp(resources)
            }
            toolbarGravity = getInt(R.styleable.ToolbarLayout_toolbarGravity, Gravity.BOTTOM)

            inflateChildren()
            initViews()

            _expandable = getBoolean(R.styleable.ToolbarLayout_expandable, true)
            expandedPortrait = getBoolean(R.styleable.ToolbarLayout_expanded, _expandable)
            _showNavigationButtonAsBack =
                getBoolean(R.styleable.ToolbarLayout_showNavButtonAsBack, false)
            navigationIcon = getDrawable(R.styleable.ToolbarLayout_navigationIcon)
                ?: getDefaultNavigationIconResource()?.let { d ->
                    ContextCompat.getDrawable(
                        context,
                        d
                    )
                }
            _collapsedTitle = getString(R.styleable.ToolbarLayout_title)
            _titleExpanded = _collapsedTitle
            _subtitleExpanded = getString(R.styleable.ToolbarLayout_subtitle)
            _collapsedSubtitle = _subtitleExpanded
            _handleInsets = getBoolean(R.styleable.ToolbarLayout_handleInsets, true)
            _showSwitchBar = getBoolean(R.styleable.ToolbarLayout_showSwitchBar, false)
            _mainRoundedCorners = MainRoundedCorners.entries[getInteger(
                R.styleable.ToolbarLayout_mainRoundedCorners,
                0
            )]
            _landscapeHeightForStatusBar = getInt(R.styleable.ToolbarLayout_landscapeHeightForStatusBar, 420)
        }
    }

    private fun inflateChildren() {
        if (layoutRes != getDefaultLayoutResource()) {
            Log.w(TAG, "Inflating custom layout")
        }
        LayoutInflater.from(context).inflate(layoutRes, this, true)
    }

    private fun initViews() {
        adpCoordinatorLayout = findViewById<AdaptiveCoordinatorLayout>(R.id.toolbarlayout_coordinator_layout).apply {
            setLandscapeHeightForStatusBar(this@ToolbarLayout._landscapeHeightForStatusBar)
        }
        appBarLayout = adpCoordinatorLayout.findViewById<AppBarLayout>(R.id.toolbarlayout_app_bar)
            .apply { setTag(R.id.tag_side_margin_excluded, true) }
        collapsingToolbarLayout = appBarLayout.findViewById(R.id.toolbarlayout_collapsing_toolbar)
        _mainToolbar = collapsingToolbarLayout.findViewById<Toolbar>(R.id.toolbarlayout_main_toolbar).apply {
            if (toolbarGravity != Gravity.BOTTOM) {
                updateLayoutParams<CollapsingToolbarLayout.LayoutParams> { gravity = toolbarGravity }
            }
        }

        mainContainerParent = adpCoordinatorLayout.findViewById(R.id.tbl_main_content_parent)
        _mainContainer = mainContainerParent.findViewById(R.id.tbl_main_content)
        bottomRoundedCorner = adpCoordinatorLayout.findViewById(R.id.tbl_bottom_corners)

        footerParent = findViewById(R.id.tbl_footer_parent)
        customFooterContainer = footerParent.findViewById(R.id.tbl_custom_footer_container)

        activity?.apply {
            setSupportActionBar(_mainToolbar)
            supportActionBar!!.apply {
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowTitleEnabled(false)
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        adpCoordinatorLayout.configureAdaptiveMargin(marginProviderImpl, getAdaptiveChildViews())
        navButtonsHandler.apply {
            showNavigationButtonAsBack = _showNavigationButtonAsBack
            showNavigationButton = _showNavigationButton
        }
        if (_showSwitchBar) switchBar.visibility = VISIBLE
        setNavigationButtonIcon(navigationIcon)
        applyCachedTitles()
        updateAppbarHeight()
        if (_mainRoundedCorners != ALL) {
            applyMainRoundedCorners()
        }
        updateHorizontalEdgeInsets()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshLayout(resources.configuration)
        syncActionModeMenu()
        updateOnBackCallbackState()
        appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
        footerParent.addOnLayoutChangeListener(footerLayoutListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
        footerParent.removeOnLayoutChangeListener(footerLayoutListener)
        isTouching = false
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus && isTouching) {
            isTouching = false
            handlePendingActions()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isTouching = false
        if (!isAttachedToWindow) return
        refreshLayout(newConfig)
        updateAppbarHeight()
        syncActionModeMenu()
        updateOnBackCallbackState()
        updateHorizontalEdgeInsets()
    }

    private fun refreshLayout(newConfig: Configuration) {
        val isLandscape = newConfig.orientation == ORIENTATION_LANDSCAPE
        isExpanded = !isLandscape && expandedPortrait
    }

    private var marginProviderImpl: MarginProvider = MARGIN_PROVIDER_ADP_DEFAULT

    /**
     * Assigns a custom implementation of [MarginProvider] to be used
     * for determining the side margins of the main content of this [ToolbarLayout].
     *
     * This will override the default behavior provided by
     * [MARGIN_PROVIDER_ADP_DEFAULT][AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ADP_DEFAULT].
     *
     * @param provider The custom [MarginProvider] implementation to use.
     *
     * @see [AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ZERO].
     */
    @CallSuper
    open fun setAdaptiveMarginProvider(provider: MarginProvider) {
        // For functional interfaces (lambdas), equality is by reference, not by logic.
        if (marginProviderImpl === provider) return
        marginProviderImpl = provider
        adpCoordinatorLayout.configureAdaptiveMargin(provider, getAdaptiveChildViews())
    }

    internal open fun getAdaptiveChildViews(): Set<View>? =
        adpCoordinatorLayout.children.filterNot { it.getTag(R.id.tag_side_margin_excluded) == true }
            .toSet()


    private fun updateAppbarHeight() {
        appBarLayout.isEnabled = _expandable
        if (_expandable) {
            appBarLayout.seslSetCustomHeightProportion(false, 0f)
        } else {
            appBarLayout.seslSetCustomHeight(
                context.resources.getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_height_with_padding)
            )
        }
    }

    /**
     * Applies horizontal insets to the edges and rounded corners of the main content.
     * Note that this method does not add any horizontal padding or margin.
     * It is the responsibility of the content to set the appropriate padding or margin as needed.
     */
    fun setEdgeInsetHorizontal(@Px px: Float) {
        if (edgeInsetHorizontal == px) return
        edgeInsetHorizontal = px
        updateHorizontalEdgeInsets()
    }

    private fun updateHorizontalEdgeInsets() {
        edgeInsetHorizontal.dpToPx(resources).let {px ->
            Insets.of(px, 0, px, 0).let {
                _mainContainer!!.edgeInsets = it
                bottomRoundedCorner.edgeInsets = it
            }
        }
    }

    //
    // AppBar methods
    //

    /**
     * Set the title of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see setTitle[CharSequence?, CharSequence?] for setting titles independently.
     * @see setCustomTitleView
     */
    open fun setTitle(title: CharSequence?) = setTitle(title, title)

    /**
     * Set the title of the collapsed and expanded Toolbar independently.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see setCustomTitleView
     * @see expandedTitle
     * @see collapsedTitle
     */
    fun setTitle(expandedTitle: CharSequence?, collapsedTitle: CharSequence?) {
        this.collapsedTitle = collapsedTitle
        this.expandedTitle = expandedTitle
    }

    /**
     * The title for the expanded Toolbar.
     *
     * @see setCustomTitleView
     * @see collapsedTitle
     */
    var expandedTitle: CharSequence?
        get() = _titleExpanded
        set(value) {
            if (_titleExpanded == value) return
            collapsingToolbarLayout.title = value.also { _titleExpanded = it }
        }

    /**
     * The title for the collapsed Toolbar.
     *
     * @see expandedTitle
     */
    var collapsedTitle: CharSequence?
        get() = _collapsedTitle
        set(value) {
            if (_collapsedTitle == value) return
            _mainToolbar.title = value.also { _collapsedTitle = it }
        }

    /**
     * Set the subtitle of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see expandedSubtitle
     * @see collapsedSubtitle
     */
    fun setSubtitle(subtitle: CharSequence?) {
        this.expandedSubtitle = subtitle
        this.collapsedSubtitle = subtitle
    }

    /**
     * The subtitle of the expanded Toolbar.
     * This might not be visible in landscape or on devices with small dpi.
     *
     * @see collapsedSubtitle
     */
    var expandedSubtitle: CharSequence?
        get() = _subtitleExpanded
        set(value) {
            if (_subtitleExpanded == value) return
            collapsingToolbarLayout.seslSetSubtitle(value.also { _subtitleExpanded = it })
        }

    /**
     * The subtitle of the collapsed Toolbar.
     *
     * @see expandedSubtitle
     */
    var collapsedSubtitle: CharSequence?
        get() = _collapsedSubtitle
        set(value) {
            if (_collapsedSubtitle == value) return
            _mainToolbar.subtitle = value.also { _collapsedSubtitle = it }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setTitlesNoCache(
        expandedTitle: CharSequence?,
        collapsedTitle: CharSequence?,
        expandedSubTitle: CharSequence?,
        collapsedSubtitle: CharSequence?
    ) {
        _mainToolbar.title = collapsedTitle
        collapsingToolbarLayout.title = expandedTitle
        _mainToolbar.subtitle = collapsedSubtitle
        collapsingToolbarLayout.seslSetSubtitle(expandedSubTitle)
    }

    /** Restore the original toolbar titles*/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun applyCachedTitles() {
        _mainToolbar.title = _collapsedTitle
        collapsingToolbarLayout.title = _titleExpanded
        _mainToolbar.subtitle = _collapsedSubtitle
        collapsingToolbarLayout.seslSetSubtitle(_subtitleExpanded)
    }

    /**
     * Represents whether the Toolbar can be expanded or collapsed.
     *
     * @see setExpanded
     */
    var isExpandable: Boolean
        get() = _expandable
        set(expandable) {
            if (_expandable != expandable) {
                _expandable = expandable
                updateAppbarHeight()
            }
        }

    /**
     * Programmatically expand or collapse the Toolbar with an optional animation.
     *
     * @see isExpanded
     * @param animate whether or not to animate the expanding or collapsing.
     */
    fun setExpanded(expanded: Boolean, animate: Boolean) {
        if (_expandable) {
            expandedPortrait = expanded
            appBarLayout.setExpanded(expanded, animate)
        } else Log.d(TAG, "setExpanded: mExpandable is false")
    }

    /**
     * Represents the expanded state of the Toolbar.
     *
     * @see setExpanded
     */
    var isExpanded: Boolean
        get() = _expandable && !appBarLayout.seslIsCollapsed()
        set(expanded) {
            setExpanded(expanded, appBarLayout.isLaidOut)
        }

    /**
     * Replace the title of the expanded Toolbar with a custom View including LayoutParams.
     * This might not be visible in landscape or on devices with small dpi.
     * This is similar to setting [R.styleable.ToolbarLayout_Layout_layout_location]
     * to [R.attr.layout_location#appbar_header]
     *
     * @see customSubtitleView
     */
    @JvmOverloads
    fun setCustomTitleView(view: View, params: CollapsingToolbarLayout.LayoutParams? = null) {
        val lp = params ?: CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            .apply { seslSetIsTitleCustom(true) }
        collapsingToolbarLayout.seslSetCustomTitleView(view, lp)
    }

    /**
     * The custom View that replaces the title of the expanded Toolbar.
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun getCustomTitleView(): View? {
        return collapsingToolbarLayout.children.find {
            (it.layoutParams as? CollapsingToolbarLayout.LayoutParams)?.seslIsTitleCustom() == true
        }
    }

    /**
     * The custom View that replaces the subtitle of the expanded Toolbar.
     * This might not be visible in landscape or on devices with small dpi.
     */
    var customSubtitleView: View?
        get() = collapsingToolbarLayout.seslGetCustomSubtitle()
        set(value) {
            if (value == collapsingToolbarLayout.seslGetCustomSubtitle()) return
            collapsingToolbarLayout.seslSetCustomSubtitle(value)
        }

    private var immersiveScrollHelper: ImmersiveScrollHelper? = null

    /**
     * Activate or deactivate the immersive scroll behavior.
     * When this is activated, the AppBar and the layout footer will completely hide when scrolling up.
     * This is only available on api level 30 and above and when not in [desktop mode][DeviceLayoutUtil.isDeskTopMode].
     *
     * @param activate
     * @param footerAlpha The alpha value of the footer when on immersive scroll.
     */
    @JvmOverloads
    @CallSuper
    open fun activateImmersiveScroll(
        activate: Boolean, @FloatRange(0.0, 1.0)
        footerAlpha: Float = 1f
    ): Boolean {
        if (VERSION.SDK_INT < 30) {
            Log.w(
                TAG,
                "activateImmersiveScroll: immersive scroll is available only on api 30 and above"
            )
            return false
        }

        if (DeviceLayoutUtil.isDeskTopMode(context.resources)) {
            Log.w(
                TAG,
                "activateImmersiveScroll: immersive scroll is not available on desktop mode."
            )
            return false
        }

        if (activate) {
            if (immersiveScrollHelper == null) {
                immersiveScrollHelper =
                    ImmersiveScrollHelper(activity!!, appBarLayout, footerParent, footerAlpha)
            }
            immersiveScrollHelper!!.activateImmersiveScroll()
        } else {
            immersiveScrollHelper?.deactivateImmersiveScroll()
            immersiveScrollHelper = null
            requestApplyInsets()
        }
        return true
    }

    /**
     * Represents if immersive scroll is active or not.
     * This does not apply on API level 29 and below
     * and when on desktop mode.
     *
     * @see activateImmersiveScroll
     */
    @set:CallSuper
    @get:CallSuper
    open var isImmersiveScroll: Boolean
        /**
         * Returns if immersive scroll behavior is active or not.
         * This always returns false on API level 29 and below.
         */
        get() = VERSION.SDK_INT >= 30 && immersiveScrollHelper?.isImmersiveScrollActivated == true
        /**
         * Activate or deactivate the immersive scroll behavior.
         * When this is activated, the AppBar, the Navigation bar and the layout footer will completely hide when scrolling up.
         */
        set(activate) {
            activateImmersiveScroll(activate, if (VERSION.SDK_INT >= 35) 0.8f else 1f)
        }

    /**
     * Sets the suggestion view for the AppBarLayout.
     *
     * <p>The suggestion view is dedicated for displaying contextual suggestions or actions,
     * such as quick actions, recommendations, or dynamic content relevant to the current screen.
     * Unlike [setCustomTitleView] and [customSubtitleView], which permanently replace the title and
     * subtitle of the toolbar, the suggestion view can be made removable and is intended for transient
     * or auxiliary content, rather than for primary titles or subtitles.
     *
     * @param appBarModel The [AppBarModel] implementation representing the suggested view. If null, any existing
     * suggestion view will be removed.
     */
    fun setAppBarSuggestView(appBarModel: AppBarModel<out AppBarView>?){
        collapsingToolbarLayout.seslSetSuggestView(appBarModel)
    }

    /**
     * Sets whether to enable or disable the fade effect of the collapsed title.
     * This is set to `true` by default.
     */
    fun setEnableFadeToolbarTitle(enable: Boolean) =
        collapsingToolbarLayout.seslEnableFadeToolbarTitle(enable)

    //
    // Navigation Button methods
    //
    /**
     * Set the icon on the navigation button.
     * Don't forget to also set a tooltip description with [setNavigationButtonTooltip].
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     *
     * @see setNavigationButtonOnClickListener
     */
    fun setNavigationButtonIcon(icon: Drawable?) {
        navButtonsHandler.setNavigationButtonIcon(icon)
    }


    /**
     * Set a badge to the navigation button.
     *
     * @param badge The [Badge] to be displayed.
     */
    fun setNavigationButtonBadge(badge: Badge) {
        navButtonsHandler.setNavigationButtonBadge(badge)
    }

    /**
     * Set the tooltip description on the navigation button.
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     *
     * @see setNavigationButtonIcon
     * @see setNavigationButtonOnClickListener
     *
     */
    fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        navButtonsHandler.setNavigationButtonTooltip(tooltipText)
    }


    /**
     * Set the click listener for the navigation button click event.
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     *
     * @see setNavigationButtonTooltip
     * @see setNavigationButtonIcon
     */
    fun setNavigationButtonOnClickListener(listener: OnClickListener?) {
        navButtonsHandler.setNavigationButtonOnClickListener(listener)
    }


    /**
     * Indicates whether the toolbar navigation button should be displayed as a "back/up" affordance.
     * Set this to `true` if clicking the navigation button returns the user up by a single level in your UI;
     * the navigation button will display a back icon, set the tooltip to 'Navigate up', and
     * invoke OnBackPressedDispatcher#onBackPressed when clicked.
     *
     * This is `false` by default.
     *
     * @see setNavigationButtonOnClickListener
     * @see setNavigationButtonIcon
     * @see setNavigationButtonTooltip
     */
    var showNavigationButtonAsBack
        get() = navButtonsHandler.showNavigationButtonAsBack
        set(value) {
            if (_showNavigationButtonAsBack == value) return
            _showNavigationButtonAsBack = value
            navButtonsHandler.showNavigationButtonAsBack = value
        }

    /**
     * Represents the visibility of the toolbar navigation button.
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     * This is `false` by default.
     *
     * @see setNavigationButtonOnClickListener
     * @see setNavigationButtonIcon
     * @see setNavigationButtonTooltip
     */
    var showNavigationButton
        get() = navButtonsHandler.showNavigationButton
        set(value) {
            if (_showNavigationButton == value) return
            _showNavigationButton = value
            navButtonsHandler.showNavigationButton = value
        }

    //
    // Search Mode methods
    //
    /**
     * Shows the [SearchView] widget in the Toolbar.
     *
     * @param listener [SearchModeListener] to apply.
     * @param searchModeOnBackBehavior (optional) [SearchModeOnBackBehavior] to apply.
     * Defaults to [CLEAR_DISMISS] when not set.
     *
     * @see [endSearchMode]
     */
    @JvmOverloads
    open fun startSearchMode(
        listener: SearchModeListener,
        searchModeOnBackBehavior: SearchModeOnBackBehavior = CLEAR_DISMISS
    ) {
        searchModeListener = listener
        searchModeOBPBehavior = searchModeOnBackBehavior

        if (isActionMode) {
            if (searchOnActionMode is Concurrent) {
                if (!isTouching && !_searchView!!.isVisible) {
                    showActionModeSearchView()
                }
                return
            } else {
                endActionMode()
            }
        }

        isSearchMode = true
        ensureSearchModeToolbar()
        setupSearchView()
        animatedVisibility(_mainToolbar, GONE)
        animatedVisibility(searchToolbar!!, VISIBLE)
        customFooterContainer!!.setVisibility(GONE, false)
        setExpanded(expanded = false, animate = true)
        setupSearchModeListener()
        _searchView!!.isIconified = false//to focus
        collapsingToolbarLayout.title =
            resources.getString(appcompatR.string.sesl_searchview_description_search)
        collapsingToolbarLayout.seslSetSubtitle(null)
        updateOnBackCallbackState()
        searchModeListener!!.onSearchModeToggle(_searchView!!, true)
    }

    private fun setupSearchModeListener() {
        _searchView!!.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                private var backCallbackUpdaterJob: Job? = null

                override fun onQueryTextSubmit(query: String): Boolean {
                    return searchModeListener?.onQueryTextSubmit(query) == true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    if (!isActionMode && searchModeOBPBehavior == CLEAR_CLOSE) {
                        backCallbackUpdaterJob?.cancel()
                        backCallbackUpdaterJob = activity!!.lifecycleScope.launch {
                            delay(250)
                            updateOnBackCallbackState()
                        }
                    }
                    return searchModeListener?.onQueryTextChange(newText) == true
                }
            })
    }

    /**
     * Dismiss the [SearchView] in the Toolbar.
     *
     * @see startSearchMode
     */
    open fun endSearchMode() {
        if (!isSearchMode) return
        isSearchMode = false

        applyCachedTitles()

        //Restore views visibility
        searchToolbar!!.visibility = GONE
        animatedVisibility(_mainToolbar, VISIBLE)
        customFooterContainer!!.setVisibility(VISIBLE, true, 450)

        searchModeListener!!.onSearchModeToggle(_searchView!!, false)
        // We are clearing the listener first. We don't want to trigger
        // SearchModeListener.onQueryTextChange callback
        // when clearing the SearchView's input field
        searchModeListener = null
        _searchView!!.apply {
            setOnQueryTextListener(null)
            setQuery("", false)
        }
        updateOnBackCallbackState()
    }


    private fun ensureSearchModeToolbar() {
        if (searchToolbar == null) {
            searchToolbar =
                (collapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_search).inflate() as Toolbar).apply {
                    if (toolbarGravity != Gravity.BOTTOM) {
                        updateLayoutParams<CollapsingToolbarLayout.LayoutParams> { gravity = toolbarGravity }
                    }
                }
        }
    }

    private fun setupSearchView() {
        if (_searchView == null) {
            _searchView = SemSearchView(context).apply {
                setIconifiedByDefault(false)
                activity?.let { setSearchableInfoFrom(it) }
            }
        }

        when {
            isActionMode -> {
                _searchView!!.apply {
                    isVisible = false
                    if (tag == 0) return
                    tag = 0
                    applyActionModeSearchStyle()
                    seslSetUpButtonVisibility(GONE)
                    seslSetOnUpButtonClickListener(null)
                    onCloseClickListener = {
                        searchToolbar?.isVisible = false//not anymore needed
                        isSearchMode = false
                        iconifyActionModeSearchView(true)
                        setOnQueryTextListener(null)
                        searchModeListener?.onSearchModeToggle(this, false)
                        setQuery("", false)
                    }
                    if (!isDescendantOf(mainContainerParent)) {
                        (parent as? ViewGroup)?.removeView(this)
                        mainContainerParent.addView(
                            this,
                            0,
                            MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        )
                        updateLayoutParams<MarginLayoutParams> {
                            val dpToPx = context.dpToPxFactor
                            height = (48 * dpToPx).toInt()
                            bottomMargin = (12 * dpToPx).toInt()
                            marginStart = (10 * dpToPx).toInt()
                            marginEnd = (10 * dpToPx).toInt()
                        }
                    }
                }
            }

            isSearchMode -> {
                _searchView!!.apply {
                    if (tag == 1) {
                        isVisible = true; return
                    }
                    tag = 1
                    background = null
                    applyThemeColors()
                    seslSetUpButtonVisibility(VISIBLE)
                    seslSetOnUpButtonClickListener { endSearchMode() }
                    onCloseClickListener = null
                    if (!isDescendantOf(searchToolbar!!)) {
                        (parent as? ViewGroup)?.removeView(this)
                        searchToolbar!!.addView(
                            this,
                            0,
                            Toolbar.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        )
                    }
                    isVisible = true
                }
            }
        }
    }

    private fun iconifyActionModeSearchView(iconify: Boolean) {
        _searchView!!.apply {
            isVisible = !iconify
            if (!iconify) isIconified = false//To focus
        }
        searchMenuItem!!.isVisible = iconify
    }


    /**
     * Updates the query text of the [SearchView] with the search query extracted from the provided [Intent].
     * This method is specifically designed to handle search intents with the action [Intent.ACTION_SEARCH],
     * which are typically received by a search activity.
     *
     * @param intent The [Intent] containing the search query. It is expected to have the action [Intent.ACTION_SEARCH].
     *               The search query should be accessible via [SearchManager.QUERY] in the intent's extras.
     *
     * @throws IllegalArgumentException if the intent does not contain a valid search query.
     *
     * Usage example:
     * ```
     * val searchIntent = intent
     * if (Intent.ACTION_SEARCH == searchIntent.action) {
     *     setSearchQueryFromIntent(searchIntent)
     * }
     * ```
     */
    fun setSearchQueryFromIntent(intent: Intent) {
        require(Intent.ACTION_SEARCH == intent.action) {
            "setSearchQueryFromIntent: Intent action is not ACTION_SEARCH."
        }
        if (isSearchMode) {
            _searchView!!.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
        }
    }


    //
    // Action Mode methods
    //

    //We need to track the touch state for the action mode menu visibility
    private var isTouching = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> isTouching = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                handlePendingActions()
            }
        }
        return super.dispatchTouchEvent(event)
    }


    /**
     * Starts an Action Mode session. This shows the Toolbar's ActionMode with a toggleable 'All' Checkbox
     * and a counter ('x selected') that temporarily replaces the Toolbar's title.
     *
     * @param listener The [ActionModeListener] to be invoked for this action mode.
     * @param searchOnActionMode (optional) The [SearchOnActionMode] option to set for this action mode.
     * It is set to [Dismiss] by default.
     * @param allSelectorStateFlow (optional) StateFlow of [AllSelectorState] that will be used to update
     * "All" selector state and count. If not set, "All" selector state must be updated by calling [updateAllSelector].
     * @param showCancel (optional) Show a Cancel button in the toolbar menu. Setting this to true
     * disables adaptive action mode menu (i.e. menu will always be shown as a bottom action menu.
     * This is false by default.
     * @param maxActionItems (optional) The maximum number of items to show as action items in the Toolbar.
     * By default, it's 4.
     * @see [endActionMode]
     */
    @JvmOverloads
    open fun startActionMode(
        listener: ActionModeListener,
        searchOnActionMode: SearchOnActionMode = Dismiss,
        allSelectorStateFlow: StateFlow<AllSelectorState>? = null,
        showCancel: Boolean = false,
        maxActionItems: Int = 4
    ) {
        isActionMode = true
        updateAllSelectorJob?.cancel()
        ensureActionModeViews()
        actionModeListener = listener
        this.searchOnActionMode = searchOnActionMode

        when (searchOnActionMode) {
            Dismiss -> if (isSearchMode) endSearchMode()
            NoDismiss -> if (isSearchMode) animatedVisibility(searchToolbar!!, GONE)
            is Concurrent -> {
                setupActionModeSearchMenu()
                setupSearchView()
                searchOnActionMode.searchModeListener?.let {
                    searchModeListener = it
                }
                post {
                    if (isSearchMode || !_searchView!!.query.isNullOrEmpty()) {
                        if (isTouching) {
                            showActionModeSearchPending = true
                        } else {
                            showActionModeSearchView()
                        }
                    }
                }
            }
        }

        //Note: Using INVISIBLE, instead of GONE because
        //to workaround the incorrect top inset issue when
        //swapping Toolbars on landscape.
        animatedVisibility(_mainToolbar, INVISIBLE)
        showActionModeToolbarAnimate()
        setupActionModeMenu(showCancel, maxActionItems)

        allSelectorStateFlow?.let {ass ->
            allSelectorItemsCount = 0
            updateAllSelectorJob = activity!!.lifecycleScope.launch {
                ass.flowWithLifecycle(activity!!.lifecycle)
                    .collectLatest {
                        updateAllSelectorInternal(it.totalSelected, it.isEnabled, it.isChecked)
                    }
            }
        } ?: run {
            if (allSelectorItemsCount != SELECTED_ITEMS_UNSET) {
                applyAllSelectorCount(allSelectorItemsCount, true)
                applyAllSelectorState(allSelectorEnabled, allSelectorChecked)
            }
        }

        collapsingToolbarLayout.seslSetSubtitle(null)
        _mainToolbar.subtitle = null

        setupAllSelectorOnClickListener()
        updateOnBackCallbackState()

        if (isTouching) syncMenuPending = true else syncActionModeMenu()
    }

    private inline fun showActionModeToolbarAnimate() {
        animatedVisibility(actionModeToolbar!!, VISIBLE)

        if (!appBarLayout.seslIsCollapsed()) return

        val overshoot = CachedInterpolatorFactory.getOrCreate(Type.OVERSHOOT)

        actionModeCheckBox.apply {
            scaleX = 0f
            scaleY = 0f
            alpha = 0f
            animate()
                .withLayer()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(overshoot)
                .alpha(1f)
                .setDuration(260)
        }

        actionModeTitleTextView.apply {
            translationX = if (isRTLayout) 80f else -80f
            animate()
                .withLayer()
                .translationX(0f)
                .setInterpolator(overshoot)
                .setDuration(260)
        }
    }

    @SuppressLint("VisibleForTests")
    private inline fun showMainToolbarAnimate() {
        animatedVisibility(_mainToolbar, VISIBLE)

        if (!appBarLayout.seslIsCollapsed()) return

        val overshoot = CachedInterpolatorFactory.getOrCreate(Type.OVERSHOOT)
        val start = if (isRTLayout) -80f else 80f

        _mainToolbar.titleTextView?.apply {
            alpha = 0f
            translationX = start
            animate()
                .setStartDelay(20)
                .withLayer()
                .withStartAction { alpha = 1f }
                .translationX(0f)
                .setInterpolator(overshoot)
                .setDuration(260)
        }

        _mainToolbar.subtitleTextView?.apply {
            alpha = 0f
            translationX = start
            animate()
                .setStartDelay(20)
                .withLayer()
                .withStartAction { alpha = 1f }
                .translationX(0f)
                .setInterpolator(overshoot)
                .setDuration(260)
        }
    }

    private inline fun setupActionModeSearchMenu() {
        actionModeToolbar!!.apply {
            inflateMenu(R.menu.oui_des_tbl_am_common)
            searchMenuItem = menu.findItem(R.id.menu_item_am_search)!!.also {
                it.isVisible = true
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                it.setOnMenuItemClickListener {
                    showActionModeSearchView()
                    true
                }
            }
        }
    }

    private fun showActionModeSearchView() {
        iconifyActionModeSearchView(false)
        if (!isSearchMode) {
            setupSearchModeListener()
            searchModeListener!!.onSearchModeToggle(_searchView!!, true)
        }
        setExpanded(expanded = false, animate = true)
    }

    private val isRTLayout get() = layoutDirection == LAYOUT_DIRECTION_RTL

    /**
     * Ends the current ActionMode.
     *
     * @see startActionMode
     */
    open fun endActionMode() {
        if (!isActionMode) return
        updateAllSelectorJob?.cancel()
        isActionMode = false
        animatedVisibility(actionModeToolbar!!, GONE)
        if (isSearchMode) {
            //return to search mode interface
            ensureSearchModeToolbar()
            setupSearchView()
            animatedVisibility(searchToolbar!!, VISIBLE)
            collapsingToolbarLayout.title =
                resources.getString(appcompatR.string.sesl_searchview_description_search)
            collapsingToolbarLayout.seslSetSubtitle(null)
            _searchView!!.isIconified = false
        } else {
            clearActionModeSearch()
            showMainToolbarAnimate()
            applyCachedTitles()
            customFooterContainer!!.setVisibility(VISIBLE, true, 0)
        }
        bottomActionModeBar.setVisibility(GONE, true, 0)
        actionModeListener!!.onEndActionMode()
        //This clears menu including the common action mode menu
        //items - search and cancel
        menuSynchronizer!!.clear()
        actionModeSelectAll.setOnClickListener(null)
        actionModeCheckBox.isChecked = false
        allSelectorItemsCount = SELECTED_ITEMS_UNSET
        actionModeListener = null
        menuSynchronizer = null
        updateAllSelectorJob = null
        searchOnActionMode = null
        updateOnBackCallbackState()
    }

    private inline fun setupAllSelectorOnClickListener() {
        actionModeSelectAll.setOnClickListener {
            actionModeCheckBox.apply {
                isChecked = !isChecked
                actionModeListener!!.onSelectAll(isChecked)
            }
        }
    }

    private inline fun setupActionModeMenu(showCancel: Boolean, maxActionItems: Int) {
        forcePortraitMenu = showCancel

        actionModeToolbar!!.apply {
            var cancelMenuItem: MenuItem? = menu.findItem(R.id.menu_item_am_cancel)
            if (showCancel && cancelMenuItem == null) {
                inflateMenu(R.menu.oui_des_tbl_am_common)
                cancelMenuItem = menu.findItem(R.id.menu_item_am_cancel)!!
            }
            cancelMenuItem?.let {
                it.isVisible = showCancel
                if (showCancel) {
                    it.setOnMenuItemClickListener {
                        endActionMode()
                        true
                    }
                }
            }
        }

        menuSynchronizer = MenuSynchronizer(
            bottomActionModeBar,
            actionModeToolbar!!,
            onMenuItemClick = {
                actionModeListener!!.onMenuItemClicked(it)
            },
            null,
            maxActionItems = maxActionItems
        ).apply {
            actionModeListener!!.onInflateActionMenu(this.menu, activity!!.menuInflater)
        }
    }

    private fun handlePendingActions() {
        if (syncMenuPending) {
            syncMenuPending = false
            syncActionModeMenu()
        }
        if (showActionModeSearchPending) {
            showActionModeSearchPending = false
            showActionModeSearchView()
        }
    }

    private fun syncActionModeMenu() {
        if (!isActionMode) return
        if (isTouching) {
            syncMenuPending = true
            return
        }
        if (customFooterContainer!!.hasShowingChild()) {
            customFooterContainer!!.isVisible = false
            if (VERSION.SDK_INT >= 26) {
                postOnAnimationDelayed({ if (isActionMode) syncActionModeMenuInternal() }, 375)
                return
            }
        }
        syncActionModeMenuInternal()
    }

    private fun syncActionModeMenuInternal() {
        if (allSelectorItemsCount > 0) {
            if (!isTouching) {
                val isMenuModePortrait = forcePortraitMenu
                        || DeviceLayoutUtil.isPortrait(resources.configuration)
                        || DeviceLayoutUtil.isTabletLayoutOrDesktop(context)
                menuSynchronizer!!.state =
                    if (isMenuModePortrait) State.PORTRAIT else State.LANDSCAPE
                if (isImmersiveScroll) {
                    postOnAnimation { appBarLayout.setExpanded(false, true) }
                }
            }
        } else {
            menuSynchronizer!!.state = State.HIDDEN
        }
    }

    private fun ensureActionModeViews() {
        if (actionModeToolbar == null) {
            actionModeToolbar =
                (collapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_action_mode)
                    .inflate() as Toolbar).also {
                    actionModeSelectAll = it.findViewById(R.id.toolbarlayout_selectall)
                    actionModeTitleTextView =
                        it.findViewById(R.id.toolbar_layout_action_mode_title)
                }
            actionModeCheckBox =
                actionModeSelectAll.findViewById(R.id.toolbarlayout_selectall_checkbox)
            actionModeSelectAll.setOnClickListener { actionModeCheckBox.setChecked(!actionModeCheckBox.isChecked) }
            bottomActionModeBar =
                findViewById<ViewStub>(R.id.viewstub_tbl_actionmode_bottom_menu).inflate() as BottomNavigationView
        }
    }

    private inline fun clearActionModeSearch() {
        _searchView?.apply {
            isGone = true
            searchModeListener?.onSearchModeToggle(this, false)
            searchModeListener = null
            setQuery("", false)
        }
    }


    /**
     * Update action mode's 'All' selector state.
     *
     * **Note: Don't call this method if 'allSelectorStateFlow' is provided on [startActionMode].**
     *
     * @param count Number of selected items
     * @param enabled To enable/disable clicking the 'All' selector.
     * @param checked (Optional) check all selector toggle. When not provided or `null` is set,
     * it will keep the current checked state.
     */
    @JvmOverloads
    fun updateAllSelector(count: Int, enabled: Boolean, checked: Boolean? = null) {
        if (updateAllSelectorJob != null) {
            Log.w(
                TAG, "'updateAllSelector' ignored because param `allSelectorStateFlow` " +
                        " is already provided on startActionMode()."
            )
            return
        }
        updateAllSelectorInternal(count, enabled, checked)
    }

    /**
     * Updates the ActionMode's count and Select all checkBox's enabled state and check state
     *
     * @param count number of selected items in the list
     * @param enabled enable or disable click
     * @param checked
     */
    private fun updateAllSelectorInternal(
        @IntRange(from = 0) count: Int,
        enabled: Boolean,
        checked: Boolean?
    ) {
        if (!isActionMode) {
            //Cache for later
            allSelectorItemsCount = count
            allSelectorEnabled = enabled
            allSelectorChecked = checked
            Log.w(TAG, "'updateAllSelector' called with action mode not started.")
            return
        }

        ensureActionModeViews()
        if (allSelectorItemsCount != count) {
            val updateMenu = count == 0 || allSelectorItemsCount == 0 ||
                    allSelectorItemsCount == SELECTED_ITEMS_UNSET
            allSelectorItemsCount = count
            applyAllSelectorCount(count, updateMenu)
        }
        applyAllSelectorState(enabled, checked)
    }

    private inline fun applyAllSelectorCount(count: Int, updateMenu: Boolean) {
        if (updateMenu) syncActionModeMenu()
        val title = if (count > 0) {
            resources.getString(R.string.oui_des_action_mode_n_selected, count)
        } else {
            resources.getString(R.string.oui_des_action_mode_select_items)
        }
        collapsingToolbarLayout.title = title
        actionModeTitleTextView.text = title
    }

    private inline fun applyAllSelectorState(isEnabled: Boolean, isChecked: Boolean?) {
        if (isChecked != null) {
            actionModeCheckBox.isChecked = isChecked
        }
        actionModeSelectAll.isEnabled = isEnabled
    }

    //
    // others
    //

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
        insetsCompat.getInsets(navigationBars()).bottom.let {
            if (it != navBarInsetBottom) {
                navBarInsetBottom = it
                dispatchBottomOffsetChanged()
            }
        }

        //Note: insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
        //returns incorrect on api29-
        if (isSofInputShowing != isSoftKeyboardShowing) {
            isSofInputShowing = !isSofInputShowing
            updateOnBackCallbackState()
        }

        if (_handleInsets) {
            applyWindowInsets(insetsCompat)
            return insets
        } else {
            return super.onApplyWindowInsets(insets)
        }
    }

    open fun applyWindowInsets(insets: WindowInsetsCompat) {
        val activity = activity ?: return
        val imeInsetBottom = insets.getInsets(ime()).bottom
        val basePadding = insets.getInsets(systemBars() or displayCutout())

        if (isImmersiveScroll) {
            setPadding(basePadding.left, basePadding.top, basePadding.right, imeInsetBottom)
        } else {
            if (activity.fitsSystemWindows) {
                setPadding(0, 0, 0, imeInsetBottom)
            } else {
                setPadding(
                    basePadding.left, basePadding.top, basePadding.right,
                    maxOf(basePadding.bottom, imeInsetBottom)
                )
            }
        }
    }

    /**
     * Set [rounded corners][MainRoundedCorners] around the main content.
     * This is set to [ROUNDED_CORNER_ALL] by default.
     *
     */
    var mainRoundedCorners: MainRoundedCorners
        get() = _mainRoundedCorners
        set(value) {
            if (_mainRoundedCorners == value) return
            _mainRoundedCorners = value
            applyMainRoundedCorners()
        }

    private fun applyMainRoundedCorners() {
        when (mainRoundedCorners) {
            ALL -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_TOP
                bottomRoundedCorner.isVisible = true
            }

            TOP -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_TOP
                bottomRoundedCorner.isVisible = false
            }

            BOTTOM -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_NONE
                bottomRoundedCorner.isVisible = true
            }

            NONE -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_NONE
                bottomRoundedCorner.isVisible = false
            }
        }
    }

    class ToolbarLayoutParams : LinearLayout.LayoutParams {
        @JvmField
        var layoutLocation: Int = MAIN_CONTENT

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            context.obtainStyledAttributes(attrs, R.styleable.ToolbarLayout_Layout).use {
                layoutLocation = it.getInteger(
                    R.styleable.ToolbarLayout_Layout_layout_location,
                    MAIN_CONTENT
                )
            }
        }
        constructor(width: Int, height: Int) : super(width, height)
        constructor(width: Int, height: Int, weight: Float) : super(width, height)
        constructor(params: ViewGroup.LayoutParams?) : super(params)
        constructor(source: MarginLayoutParams?) : super(source)
        constructor(source: LayoutParams) : super(source)
    }

    private fun cllpWrapper(oldLp: LayoutParams): CoordinatorLayout.LayoutParams {
        return CoordinatorLayout.LayoutParams(oldLp).apply {
            width = oldLp.width
            height = oldLp.height
            leftMargin = oldLp.leftMargin
            topMargin = oldLp.topMargin
            rightMargin = oldLp.rightMargin
            bottomMargin = oldLp.bottomMargin
            gravity = oldLp.gravity
        }
    }

    private fun animatedVisibility(view: View, visibility: Int) {
        view.visibility = VISIBLE
        view.animate()
            .alphaBy(1.0f)
            .alpha(if (visibility == VISIBLE) 1.0f else 0.0f)
            .setDuration(200)
            .setInterpolator(CachedInterpolatorFactory.getOrCreate(Type.SINE_IN_OUT_90))
            .withEndAction { view.visibility = visibility }
            .start()
    }

    fun addOnAppbarOffsetListener(listener: (Float) -> Unit) {
        if (appBarOffsetListeners == null) {
            appBarOffsetListeners = mutableListOf(WeakReference(listener))
            return
        }
        appBarOffsetListeners!!.add(WeakReference(listener))
    }

    fun removeOnCollapsingToolbarOffsetListener(listener: (Float) -> Unit) {
        appBarOffsetListeners?.let { list ->
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val weakRef = iterator.next()
                if (weakRef.get() == listener || weakRef.get() == null) {
                    iterator.remove()
                }
            }
        }
    }

    internal fun addOnBottomOffsetChangedListener(listener: (Float) -> Unit) {
        if (bottomOffsetListeners == null) {
            bottomOffsetListeners = mutableListOf(WeakReference(listener))
            return
        }
        bottomOffsetListeners!!.apply {
            if (find { it.get() == listener } == null) {
                add(WeakReference(listener))
            }
        }
    }

    internal fun removeOnBottomOffsetChangedListener(listener: (Float) -> Unit) {
        bottomOffsetListeners?.let { list ->
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val weakRef = iterator.next()
                if (weakRef.get() == listener || weakRef.get() == null) {
                    iterator.remove()
                }
            }
        }
    }

    private val footerLayoutListener: OnLayoutChangeListener by lazy(LazyThreadSafetyMode.NONE) {
        OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            v.height.let {
                if (it != footerHeight) {
                    footerHeight = it; dispatchBottomOffsetChanged()
                }
            }
        }
    }

    private fun dispatchBottomOffsetChanged() {
        bottomOffsetListeners?.apply {
            val scrollDeltaBottom = scrollDelta - appBarLayout.seslGetCollapsedHeight()
            val maxBottomInset = navBarInsetBottom + footerHeight
            val adjBottomInset = if (isImmersiveScroll && maxBottomInset > 0) {
                val scrollRatio = (1f + scrollDeltaBottom / maxBottomInset).coerceIn(0f, 1f)
                (maxBottomInset * scrollRatio).applyMinForNonScrollingNavBar()
            } else 0f
            forEach {
                it.get()?.invoke(scrollDelta + adjBottomInset)
            }
        }
    }

    private var lastAppbarOffsetDispatched = -1f
    private fun dispatchAppBarOffsetChanged(offset: Float) {
        if (lastAppbarOffsetDispatched != offset) {
            lastAppbarOffsetDispatched = offset
            appBarOffsetListeners?.let {
                for (listener in it) {
                    listener.get()?.invoke(offset)
                }
            }
        }
    }

    private inline fun Float.applyMinForNonScrollingNavBar() =
        if (context.navBarCanImmScroll()) this else {
            this.coerceAtLeast(navBarInsetBottom.toFloat())
        }

    private inner class AppBarOffsetListener : AppBarLayout.OnOffsetChangedListener {
        override fun onOffsetChanged(layout: AppBarLayout, verticalOffset: Int) {
            scrollDelta = verticalOffset + layout.totalScrollRange
            dispatchBottomOffsetChanged()
            dispatchAppBarOffsetChanged( layout.totalScrollRange.toFloat().let{ if (it == 0f) 0f else 1f + verticalOffset / it} )

            if (isActionMode && actionModeToolbar!!.isVisible) {
                val layoutPosition = abs(appBarLayout.top)
                val collapsingTblHeight = collapsingToolbarLayout.height
                val alphaRange = collapsingTblHeight * 0.17999999f
                val toolbarTitleAlphaStart = collapsingTblHeight * 0.35f

                if (appBarLayout.seslIsCollapsed()) {
                    actionModeTitleTextView.alpha = 1.0f
                } else {
                    actionModeTitleTextView.alpha = (150.0f / alphaRange
                            * (layoutPosition - toolbarTitleAlphaStart) / 255f).coerceIn(0f, 1f)
                }
            }
        }
    }

    internal companion object {
        private const val TAG = "ToolbarLayout"

        /**
         * The child will be added as the primary content of this layout located
         * right below the app bar and will have the adaptive margins applied,
         * which are managed by the layout.
         */
        internal const val MAIN_CONTENT = 0
        /**
         * Sets this view as the custom title view for the expanded AppBarLayout.
         * This is equivalent to invoking the `setCustomTitleView` method.
         */
        private const val APPBAR_HEADER = 1
        /**
         * Adds the view to the bottom of the layout ensuring it does not overlap with other content.
         * This view will also have adaptive margins applied.
         */
        private const val FOOTER = 2
        /**
         * Adds the view as a direct child of the CoordinatorLayout.
         * Typically overlaps with the `main_content` view.
         */
        private const val ROOT = 3
        private const val ROOT_BOUNDED = 6

        private const val ROUNDED_CORNER_TOP = ROUNDED_CORNER_TOP_LEFT or ROUNDED_CORNER_TOP_RIGHT
        private const val SELECTED_ITEMS_UNSET = -1
    }
}


/**
 * Badge represents a notification to the user shown as an orange dot or
 * a small text with orange background on the top right side of an anchor view.
 *
 * Select either [Badge.NUMERIC], [Badge.DOT] or [Badge.NONE]
 */
sealed class Badge {
    /**
     * @param count Set to any positive integer up to [BADGE_LIMIT_NUMBER].
     * Values <= 0 will be ignored.
     */
    data class NUMERIC(
        @field:IntRange(from = 1, to = BADGE_LIMIT_NUMBER.toLong())
        @JvmField val count: Int
    ) : Badge()

    data object DOT : Badge()
    data object NONE : Badge()

    fun toBadgeText(): String? =
        when (this) {
            is NUMERIC -> count.badgeCountToText()
            DOT -> ""
            NONE -> null
        }
}


/**
 * Starts the search mode for this [ToolbarLayout].
 *
 * This method sets up the search mode with the specified behaviors and callbacks.
 * This handles dismissing the soft keyboard when the query is submitted.
 *
 * ## Example usage:
 * ```
 * toolbarLayout.startSearchMode(
 *     ToolbarLayout.SearchModeOnBackBehavior.CLEAR_SEARCH,
 *     { query, isSubmit ->
 *         // Handle query change or submission
 *         true // Return true to indicate the query is handled
 *     },
 *     onStart = { searchView ->
 *         // Perform actions when search mode starts
 *     },
 *     onEnd = { searchView ->
 *         // Perform actions when search mode ends
 *     }
 * )
 * ```
 * @param onBackBehavior Defines the [behavior][ToolbarLayout.SearchModeOnBackBehavior] when the back button is pressed during search mode.
 * @param onQuery Lambda function to be invoked when the query text changes or is submitted.
 *               Return true if the query has been handled, false otherwise.
 * @param onStart Lambda function to be invoked when search mode starts.
 * @param onEnd Lambda function to be invoked when search mode ends.
 *
 * @see ToolbarLayout.endSearchMode
 */
inline fun <T : ToolbarLayout> T.startSearchMode(
    onBackBehavior: ToolbarLayout.SearchModeOnBackBehavior,
    crossinline onQuery: (query: String, isSubmit: Boolean) -> Boolean,
    crossinline onStart: (searchView: SearchView) -> Unit = {},
    crossinline onEnd: (searchView: SearchView) -> Unit = {}
) {
    startSearchMode(
        object : ToolbarLayout.SearchModeListener {
            private var searchView: SearchView? = null

            override fun onQueryTextSubmit(query: String?) = onQuery(query ?: "", true)
                .also { if (!query.isNullOrEmpty()) searchView?.clearFocus() }

            override fun onQueryTextChange(newText: String?) = onQuery(newText ?: "", false)

            override fun onSearchModeToggle(searchView: SearchView, isActive: Boolean) {
                if (isActive) {
                    this.searchView = searchView
                    onStart.invoke(searchView)
                } else {
                    onEnd.invoke(searchView)
                }
            }
        },
        onBackBehavior
    )
}

/**
 * Starts the action mode for this [ToolbarLayout].
 * This method sets up the action mode with the specified callbacks and options.
 *
 * ## Example usage:
 * ```
 * toolbarLayout.startActionMode(
 *     onInflateMenu = { menu, menuInflater ->
 *         // Inflate menu items here
 *         menuInflater.inflate(R.menu.action_menu, menu)
 *     },
 *     onEnd = {
 *         // Perform actions when action mode ends
 *     },
 *     onSelectMenuItem = { item ->
 *         // Handle action item click
 *         true // Return true to indicate the item click is handled
 *     },
 *     onSelectAll = { isChecked ->
 *         // Handle "All" selector click
 *     },
 *     allSelectorStateFlow = viewModel.allSelectorStateFlow
 * )
 * ```
 * @param onInflateMenu Lambda function called at the start of [startActionMode].
 * Inflate the menu items for this action mode session using this menu.
 * @param onEnd Lambda function to be invoked when action mode ends.
 * @param onSelectMenuItem Lambda function to be invoked when an action menu item is selected.
 * Return true if the item click is handled, false otherwise.
 * @param onSelectAll Lambda function to be invoked when the `All` selector is clicked.
 * This will not be triggered with [ToolbarLayout.updateAllSelector].
 * @param searchOnActionMode (optional) The [SearchOnActionMode] option to set for this action mode.
 * Defaults to [SearchOnActionMode.Dismiss].
 * @param allSelectorStateFlow (Optional) StateFlow of [AllSelectorState] that updates the `All` selector state and count.
 * @param showCancel (optional) Show a Cancel button in the toolbar menu. Setting this to true
 * disables adaptive action mode menu (i.e. menu will always be shown as a bottom action menu.
 * This is false by default.
 * @param maxActionItems (optional) The maximum number of items to show as action items in the Toolbar.
 * By default, it's 4.
 * @see ToolbarLayout.endActionMode
 */
inline fun <T : ToolbarLayout> T.startActionMode(
    crossinline onInflateMenu: (menu: Menu, menuInflater: MenuInflater) -> Unit,
    crossinline onEnd: () -> Unit,
    crossinline onSelectMenuItem: (item: MenuItem) -> Boolean,
    crossinline onSelectAll: (Boolean) -> Unit,
    searchOnActionMode: SearchOnActionMode = Dismiss,
    allSelectorStateFlow: StateFlow<AllSelectorState>? = null,
    showCancel: Boolean = false,
    maxActionItems: Int = 4
) {
    startActionMode(
        object : ActionModeListener {
            override fun onInflateActionMenu(menu: Menu, menuInflater: MenuInflater) =
                onInflateMenu(menu, menuInflater)

            override fun onEndActionMode() = onEnd()
            override fun onMenuItemClicked(item: MenuItem) = onSelectMenuItem(item)
            override fun onSelectAll(isChecked: Boolean) = onSelectAll.invoke(isChecked)
        },
        searchOnActionMode,
        allSelectorStateFlow,
        showCancel,
        maxActionItems
    )
}

/**
 * Sets the title of both the collapsed and expanded Toolbar from a string resource.
 * The expanded title might not be visible in landscape or on devices with small DPI.
 *
 * @param titleRes The string resource ID for the title.
 * @see setTitle
 * @see ToolbarLayout.expandedTitle
 * @see ToolbarLayout.collapsedTitle
 */
inline fun <T : ToolbarLayout> T.setTitle(@StringRes titleRes: Int) =
    context.getString(titleRes).let {
        expandedTitle = it
        collapsedTitle = it
    }

/**
 * Set the title of the collapsed and expanded Toolbar independently.
 * The expanded title might not be visible in landscape or on devices with small dpi.
 *
 * @param expandedTitleRes The string resource for the expanded title.
 * @param collapsedTitleRes The string resource for the collapsed title.
 *
 * @see ToolbarLayout.setTitle
 * @see ToolbarLayout.expandedTitle
 * @see ToolbarLayout.collapsedTitle
 */
inline fun <T : ToolbarLayout> T.setTitle(
    @StringRes expandedTitleRes: Int,
    @StringRes collapsedTitleRes: Int
) =
    with(context) {
        expandedTitle = getString(expandedTitleRes)
        collapsedTitle = getString(collapsedTitleRes)
    }

/**
 * Sets the subtitle of both the collapsed and expanded toolbar using a string resource.
 * The expanded subtitle may not be visible in landscape mode or on devices with small screens.
 *
 * @param titleRes The string resource ID for the subtitle.
 * @see ToolbarLayout.setSubTitle
 * @see ToolbarLayout.expandedSubtitle
 * @see ToolbarLayout.collapsedSubtitle
 */
inline fun <T : ToolbarLayout> T.setSubTitle(@StringRes titleRes: Int) =
    context.getString(titleRes).let {
        expandedSubtitle = it
        collapsedSubtitle = it
    }

/**
 * Sets the subtitle of both the collapsed and expanded toolbar using string resources.
 *
 * @param expandedTitleRes The string resource ID for the expanded subtitle.
 * @param collapsedTitleRes The string resource ID for the collapsed subtitle.
 *
 * @see ToolbarLayout.setSubtitle
 * @see ToolbarLayout.expandedSubtitle
 * @see ToolbarLayout.collapsedSubtitle
 */
inline fun <T : ToolbarLayout> T.setSubTitle(
    @StringRes expandedTitleRes: Int,
    @StringRes collapsedTitleRes: Int
) =
    with(context) {
        expandedSubtitle = getString(expandedTitleRes)
        collapsedSubtitle = getString(collapsedTitleRes)
    }

