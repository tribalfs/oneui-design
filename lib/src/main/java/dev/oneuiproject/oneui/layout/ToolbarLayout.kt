@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION.SDK_INT
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.appcompat.util.SeslRoundedCorner
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.dpToPxFactor
import dev.oneuiproject.oneui.ktx.isDescendantOf
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
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
import dev.oneuiproject.oneui.layout.internal.util.NavButtonsHandler
import dev.oneuiproject.oneui.utils.BADGE_LIMIT_NUMBER
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.MenuSynchronizer
import dev.oneuiproject.oneui.utils.MenuSynchronizer.State
import dev.oneuiproject.oneui.utils.badgeCountToText
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.Companion.MARGIN_PROVIDER_ADP_DEFAULT
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MarginProvider
import dev.oneuiproject.oneui.widget.RoundedFrameLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import androidx.appcompat.R as appcompatR

/**
 * Custom collapsing Appbar like in any App from Samsung. Includes a [SearchView] and Samsung's ActionMode.
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
     * Options for the 'on back' behavior of search mode:
     *
     * - [DISMISS]: Swipe gesture or back button press ends the search mode
     * and then unregisters it's on back callback. This is intended to be used
     * in a search fragment of a multi-fragment activity.
     * - [CLEAR_DISMISS]: Similar to [DISMISS] but checks and clears the non-empty input query first.
     * - [CLEAR_CLOSE]: Similar to [CLEAR_DISMISS] but it unregisters it's onBackPressed callback
     * without ending the search mode. Intended to be  used on a dedicated search activity.
     *
     * @see startSearchMode
     * @see endSearchMode
     */
    enum class SearchModeOnBackBehavior {
        DISMISS,
        CLEAR_DISMISS,
        CLEAR_CLOSE
    }


    /**
     * The configuration for rounded corners that can be applied to the main content
     * located between the app bar and the footer.
     * - [ALL]
     * - [TOP]
     * - [BOTTOM]
     * - [NONE]
     */
    enum class MainRoundedCorners {
        ALL,
        TOP,
        BOTTOM,
        NONE
    }

    /**
     * Options for search mode behavior when [starting action mode][startActionMode]:
     * - [Dismiss]: Search mode will be dismissed if active.
     * - [NoDismiss]: Search mode will not be dismissed if active.
     * It will return to search interface once action mode is ended.
     * - [Concurrent]: Same as [NoDismiss] but also show the search interface while on action mode.
     * The [searchModeListener][SearchModeListener] is required if the search mode is inactive when initiating action mode.
     * If the search mode is active and no [SearchModeListener] is specified,
     * the existing listener for the current search mode will be used.
     * If specified, it overrides the current search mode listener.
     */
    sealed interface SearchOnActionMode{
        data object Dismiss: SearchOnActionMode
        data object NoDismiss: SearchOnActionMode
        data class Concurrent(val searchModeListener: SearchModeListener?): SearchOnActionMode
    }

    val activity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity }

    private var mSelectedItemsCount = 0

    private var mActionModeListener: ActionModeListener? = null
    private var mActionModeMenuRes: Int = 0

    open val backHandler: BackHandler
        get() = ToolbarLayoutBackHandler(this@ToolbarLayout)

    private val onBackCallbackDelegate: OnBackCallbackDelegateCompat by lazy {
        OnBackCallbackDelegateCompat(activity!!, this, backHandler)
    }

    internal open fun updateOnBackCallbackState() {
        if (isInEditMode) return
        if (getBackCallbackStateUpdate()) {
            onBackCallbackDelegate.startListening(true)
        }else{
            onBackCallbackDelegate.stopListening()
        }
    }

    /**
     * return true if on back callback should be registered.
     */
    @CallSuper
    protected open fun getBackCallbackStateUpdate(): Boolean {
        return when {
            isActionMode -> true
            isSearchMode -> {
                when (searchModeOBPBehavior) {
                    DISMISS, CLEAR_DISMISS -> true
                    CLEAR_CLOSE -> searchView.query.isNotEmpty() || searchView.isSoftKeyboardShowing
                }
            }

            else -> false
        }
    }

    private var mActionModeTitleFadeListener: AppBarOffsetListener? = null

    private var mLayout: Int = 0

    private var mExpandable: Boolean = false
    private var mExpanded: Boolean = false
    private var mTitleCollapsed: CharSequence? = null
    private var mTitleExpanded: CharSequence? = null
    private var mSubtitleCollapsed: CharSequence? = null
    private var mSubtitleExpanded: CharSequence? = null
    private var mNavigationIcon: Drawable? = null

    private var _showNavAsBack = false
    private var _showNavigationButton = false
    private var mNavigationBadgeIcon: LayerDrawable? = null

    private var _mainContainer: RoundedFrameLayout? = null
    internal val mainContainer: FrameLayout get() = _mainContainer!!
    private var _mainRoundedCorners: MainRoundedCorners = ALL

    private lateinit var mMainContainerParent: LinearLayout

    private lateinit var _appBarLayout: AppBarLayout
    private lateinit var mCollapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var _mainToolbar: Toolbar

    private lateinit var mCoordinatorLayout: AdaptiveCoordinatorLayout
    private lateinit var mBottomRoundedCorner: LinearLayout
    internal lateinit var footerParent: LinearLayout
        private set

    private var mActionModeToolbar: Toolbar? = null
    private lateinit var mActionModeSelectAll: LinearLayout
    private lateinit var mActionModeCheckBox: CheckBox
    private lateinit var mActionModeTitleTextView: TextView

    private var mCustomFooterContainer: FrameLayout? = null
    private lateinit var mBottomActionModeBar: BottomNavigationView

    private var mSearchToolbar: Toolbar? = null
    private var _searchView: SemSearchView? = null
    private var mSearchModeListener: SearchModeListener? = null
    @JvmField
    internal var searchModeOBPBehavior = CLEAR_DISMISS
    private var searchMenuItem: MenuItem? = null

    private var mMenuSynchronizer: MenuSynchronizer? = null
    private var forcePortraitMenu: Boolean = false
    private var mShowSwitchBar = false

    open val switchBar by lazy{
        mMainContainerParent.findViewById<ViewStub>(R.id.viewstub_tbl_switchbar).inflate() as SeslSwitchBar
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

    private var mHandleInsets: Boolean = false

    /**
     * Listener for the search mode.
     *
     * @see startSearchMode
     * @see endSearchMode
     */
    interface SearchModeListener: SearchView.OnQueryTextListener{
        /**
         * Called when search mode is toggled active or inactive.
         */
        fun onSearchModeToggle(searchView: SearchView, isActive: Boolean)
    }

    protected open fun getDefaultLayoutResource(): Int = R.layout.oui_layout_tbl_main
    protected open fun getDefaultNavigationIconResource(): Int? = null


    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        when ((params as ToolbarLayoutParams).layoutLocation) {
            APPBAR_HEADER -> setCustomTitleView(
                child,
                CollapsingToolbarLayout.LayoutParams(params)
            )
            FOOTER -> mCustomFooterContainer!!.addView(child, params)
            ROOT, ROOT_BOUNDED -> {
                if (params.layoutLocation == ROOT) {
                    child.setTag(R.id.tag_side_margin_excluded, true)
                }
                mCoordinatorLayout.addView(child, cllpWrapper(params as LayoutParams))
            }
            else ->  _mainContainer?.addView(child, params) ?: super.addView(child, index, params)
        }
    }

    public override fun generateDefaultLayoutParams(): LayoutParams {
        return ToolbarLayoutParams(context, null)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return ToolbarLayoutParams(context, attrs)
    }

    open val navButtonsHandler: NavButtonsHandler by lazy(LazyThreadSafetyMode.NONE) {
        ToolbarLayoutButtonsHandler(_mainToolbar)
    }

    init {
        orientation = VERTICAL
        context.theme.obtainStyledAttributes(attrs, R.styleable.ToolbarLayout, 0, 0).use {
            mLayout = it.getResourceId(
                R.styleable.ToolbarLayout_android_layout,
                getDefaultLayoutResource()
            )

            inflateChildren()
            initViews()

            mExpandable = it.getBoolean(R.styleable.ToolbarLayout_expandable, true)
            mExpanded = it.getBoolean(R.styleable.ToolbarLayout_expanded, mExpandable)
            _showNavAsBack = it.getBoolean(R.styleable.ToolbarLayout_showNavButtonAsBack, false)
            mNavigationIcon = it.getDrawable(R.styleable.ToolbarLayout_navigationIcon)
                ?: getDefaultNavigationIconResource()?.let { d -> ContextCompat.getDrawable(context, d) }
            mTitleCollapsed = it.getString(R.styleable.ToolbarLayout_title)
            mTitleExpanded = mTitleCollapsed
            mSubtitleExpanded = it.getString(R.styleable.ToolbarLayout_subtitle)
            if (VERSION.SDK_INT >= 30 && !fitsSystemWindows) {
                mHandleInsets = it.getBoolean(R.styleable.ToolbarLayout_handleInsets, true)
            }
            mShowSwitchBar = it.getBoolean(R.styleable.ToolbarLayout_showSwitchBar, false)
            _mainRoundedCorners = MainRoundedCorners.entries[it.getInteger(R.styleable.ToolbarLayout_mainRoundedCorners, 0)]
        }
    }

    private fun inflateChildren() {
        if (mLayout != getDefaultLayoutResource()) {
            Log.w(TAG, "Inflating custom layout")
        }
        LayoutInflater.from(context).inflate(mLayout, this, true)
    }

    private fun initViews() {
        mCoordinatorLayout = findViewById(R.id.toolbarlayout_coordinator_layout)
        _appBarLayout = mCoordinatorLayout.findViewById<AppBarLayout?>(R.id.toolbarlayout_app_bar)
            .apply { setTag(R.id.tag_side_margin_excluded, true) }
        mCollapsingToolbarLayout = _appBarLayout.findViewById(R.id.toolbarlayout_collapsing_toolbar)
        _mainToolbar = mCollapsingToolbarLayout.findViewById(R.id.toolbarlayout_main_toolbar)

        mMainContainerParent = mCoordinatorLayout.findViewById(R.id.tbl_main_content_parent)
        _mainContainer = mMainContainerParent.findViewById(R.id.tbl_main_content)
        mBottomRoundedCorner = mCoordinatorLayout.findViewById(R.id.tbl_bottom_corners)

        footerParent = findViewById(R.id.tbl_footer_parent)
        mCustomFooterContainer = footerParent.findViewById(R.id.tbl_custom_footer_container)

        activity?.apply {
            setSupportActionBar(_mainToolbar)
            supportActionBar!!.apply {
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowTitleEnabled(false)
            }
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mCoordinatorLayout.configureAdaptiveMargin(marginProviderImpl, getAdaptiveChildViews())
        navButtonsHandler.apply {
            showNavigationButtonAsBack = _showNavAsBack
            showNavigationButton = _showNavigationButton
        }
        if (mShowSwitchBar) switchBar.visibility = VISIBLE
        setNavigationButtonIcon(mNavigationIcon)
        applyCachedTitles()
        updateAppbarHeight()
        if (_mainRoundedCorners != ALL){
            applyMainRoundedCorners()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshLayout(resources.configuration)
        syncActionModeMenu()
        updateOnBackCallbackState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isAttachedToWindow) return
        refreshLayout(newConfig)
        updateAppbarHeight()
        syncActionModeMenu()
        updateOnBackCallbackState()
    }

    private fun refreshLayout(newConfig: Configuration) {
        val isLandscape = newConfig.orientation == ORIENTATION_LANDSCAPE
        isExpanded = !isLandscape and mExpanded
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
    open fun setAdaptiveMarginProvider(provider: MarginProvider){
        if (marginProviderImpl == provider) return
        marginProviderImpl = provider
        mCoordinatorLayout.configureAdaptiveMargin(provider, getAdaptiveChildViews())
    }

    internal open fun getAdaptiveChildViews(): Set<View>? =
        mCoordinatorLayout.children.filterNot { it.getTag(R.id.tag_side_margin_excluded) == true }.toSet()


    private fun updateAppbarHeight() {
        _appBarLayout.isEnabled = mExpandable
        if (mExpandable) {
            _appBarLayout.seslSetCustomHeightProportion(false, 0f)
        } else {
            _appBarLayout.seslSetCustomHeight(
                context.resources.getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_height_with_padding))
        }
    }

    //
    // AppBar methods
    //
    /**@return the [AppBarLayout].*/
    val appBarLayout: AppBarLayout get() = _appBarLayout

    /**@return the main [Toolbar].*/
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val toolbar: Toolbar get() = _mainToolbar

    /**
     * Set the title of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see setTitle[CharSequence?, CharSequence?] for setting titles independently.
     * @see setCustomTitleView
     */
    open fun setTitle(title: CharSequence?) {
        setTitle(title, title)
    }

    /**
     * Set the title of the collapsed and expanded Toolbar independently.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see setCustomTitleView
     * @see expandedTitle
     * @see collapsedTitle
     */
    fun setTitle(
        expandedTitle: CharSequence?,
        collapsedTitle: CharSequence?
    ) {
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
        get() = mTitleExpanded
        set(value) {
            if (mTitleExpanded == value) return
            mCollapsingToolbarLayout.title = value.also { mTitleExpanded = it }
        }

    /**
     * The title for the collapsed Toolbar.
     *
     * @see expandedTitle
     */
    var collapsedTitle: CharSequence?
        get() = mTitleCollapsed
        set(value) {
            if (mTitleCollapsed == value) return
            _mainToolbar.title = value.also { mTitleCollapsed = it }
        }

    /**
     * Set the subtitle of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     *
     * @see expandedSubtitle
     * @see collapsedSubtitle
     */
    fun setSubtitle(subtitle: CharSequence?){
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
        get() = mSubtitleExpanded
        set(value) {
            if (mSubtitleExpanded == value) return
            mCollapsingToolbarLayout.seslSetSubtitle(value.also { mSubtitleExpanded = it })
        }

    /**
     * The subtitle of the collapsed Toolbar.
     *
     * @see expandedSubtitle
     */
    var collapsedSubtitle: CharSequence?
        get() = mSubtitleCollapsed
        set(value) {
            if (mSubtitleCollapsed == value) return
            _mainToolbar.subtitle = value.also { mSubtitleCollapsed = it }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setTitlesNoCache(expandedTitle: CharSequence?,
                         collapsedTitle: CharSequence?,
                         expandedSubTitle: CharSequence?,
                         collapsedSubtitle: CharSequence?){
        _mainToolbar.title = collapsedTitle
        mCollapsingToolbarLayout.title = expandedTitle
        _mainToolbar.subtitle = collapsedSubtitle
        mCollapsingToolbarLayout.seslSetSubtitle(expandedSubTitle)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun applyCachedTitles(){
        _mainToolbar.title = mTitleCollapsed
        mCollapsingToolbarLayout.title = mTitleExpanded
        _mainToolbar.subtitle = mSubtitleCollapsed
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
    }

    /**
     * Represents whether the Toolbar can be expanded or collapsed.
     *
     * @see setExpanded
     */
    var isExpandable: Boolean
        get() = mExpandable
        set(expandable) {
            if (mExpandable != expandable) {
                mExpandable = expandable
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
        if (mExpandable) {
            mExpanded = expanded
            _appBarLayout.setExpanded(expanded, animate)
        } else Log.d(TAG, "setExpanded: mExpandable is false")
    }

    /**
     * Represents the expanded state of the Toolbar.
     *
     * @see setExpanded
     */
    var isExpanded: Boolean
        get() = mExpandable && !_appBarLayout.seslIsCollapsed()
        set(expanded) {
            setExpanded(expanded, _appBarLayout.isLaidOut)
        }


    /**
     * Replace the title of the expanded Toolbar with a custom View including LayoutParams.
     * This might not be visible in landscape or on devices with small dpi.
     * This is similar to setting [R.styleable.ToolbarLayoutParams_layout_location]
     * to [R.attr.layout_location#appbar_header]
     *
     * @see customSubtitleView
     */
    @JvmOverloads
    fun setCustomTitleView(view: View, params: CollapsingToolbarLayout.LayoutParams? = null) {
        (params ?: CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)).apply {
            seslSetIsTitleCustom(true)
            mCollapsingToolbarLayout.seslSetCustomTitleView(view, this)
        }
    }

    /**
     * The custom View that replaces the title of the expanded Toolbar.
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun getCustomTitleView(): View? {
        return mCollapsingToolbarLayout.children.find {
            (it.layoutParams as? CollapsingToolbarLayout.LayoutParams)?.seslIsTitleCustom() == true
        }
    }

    /**
     * The custom View that replaces the subtitle of the expanded Toolbar.
     * This might not be visible in landscape or on devices with small dpi.
     */
    var customSubtitleView: View?
        get() = mCollapsingToolbarLayout.seslGetCustomSubtitle()
        set(value) {
            if (value == mCollapsingToolbarLayout.seslGetCustomSubtitle()) return
            mCollapsingToolbarLayout.seslSetCustomSubtitle(value)
        }

    var isImmersiveScroll: Boolean
        /**
         * Returns true if the immersive scroll is enabled.
         */
        get() = _appBarLayout.seslGetImmersiveScroll()
        /**
         * Enable or disable the immersive scroll of the Toolbar.
         * When this is enabled the Toolbar will completely hide when scrolling up.
         */
        set(activate) {
            if (VERSION.SDK_INT >= 30) {
                _appBarLayout.seslSetImmersiveScroll(activate)
            } else {
                Log.e(
                    TAG,
                    "setImmersiveScroll: immersive scroll is available only on api 30 and above"
                )
            }
        }

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
     * invoke [OnBackPressedDispatcher.onBackPressed] when clicked.
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
            if (_showNavAsBack == value) return
            _showNavAsBack = value
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
        mSearchModeListener = listener
        searchModeOBPBehavior = searchModeOnBackBehavior

        if (isActionMode) {
            if (searchOnActionMode is Concurrent) {
                if (!isTouching && !_searchView!!.isVisible) {
                    showActionModeSearchView()
                }
                return
            }else{
                endActionMode()
            }
        }

        isSearchMode = true
        ensureSearchModeToolbar()
        setupSearchView()
        animatedVisibility(_mainToolbar, GONE)
        animatedVisibility(mSearchToolbar!!, VISIBLE)
        mCustomFooterContainer!!.visibility = GONE
        setExpanded(expanded = false, animate = true)
        setupSearchModeListener()
        _searchView!!.isIconified = false//to focus
        mCollapsingToolbarLayout.title =
            resources.getString(appcompatR.string.sesl_searchview_description_search)
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        updateOnBackCallbackState()
        mSearchModeListener!!.onSearchModeToggle(_searchView!!, true)
    }

    private fun setupSearchModeListener() {
        _searchView!!.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                private var backCallbackUpdaterJob: Job? = null

                override fun onQueryTextSubmit(query: String): Boolean {
                    return mSearchModeListener?.onQueryTextSubmit(query) == true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    if (searchModeOBPBehavior == CLEAR_CLOSE) {
                        backCallbackUpdaterJob?.cancel()
                        backCallbackUpdaterJob = activity!!.lifecycleScope.launch {
                            delay(250)
                            updateOnBackCallbackState()
                        }
                    }
                    return mSearchModeListener?.onQueryTextChange(newText) == true
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

        //Restore toolbar titles
        applyCachedTitles()

        //Restore views visibility
        mSearchToolbar!!.visibility = GONE
        animatedVisibility(_mainToolbar, VISIBLE)
        mCustomFooterContainer!!.visibility = VISIBLE

        mSearchModeListener!!.onSearchModeToggle(_searchView!!, false)
        // We are clearing the listener first. We don't want to trigger
        // SearchModeListener.onQueryTextChange callback
        // when clearing the SearchView's input field
        mSearchModeListener = null
        _searchView!!.apply {
            setOnQueryTextListener(null)
            setQuery("", false)
        }
        updateOnBackCallbackState()
    }


    private fun ensureSearchModeToolbar() {
        if (mSearchToolbar == null) {
            mSearchToolbar =
                mCollapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_search)
                    .inflate() as Toolbar
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
                if (_searchView!!.tag == 0) return
                _searchView!!.apply {
                    tag = 0
                    isVisible = false
                    applyActionModeSearchStyle()
                    seslSetUpButtonVisibility(GONE)
                    seslSetOnUpButtonClickListener(null)
                    onCloseClickListener = {
                        mSearchToolbar?.isVisible = false//not anymore needed
                        isSearchMode = false
                        iconifyActionModeSearchView(true)
                        setOnQueryTextListener(null)
                        mSearchModeListener?.onSearchModeToggle(this, false)
                        setQuery("", false)
                    }
                    if (!isDescendantOf(mMainContainerParent)) {
                        (parent as? ViewGroup)?.removeView(this)
                        val dpToPx = context.dpToPxFactor
                        val lp = MarginLayoutParams(MATCH_PARENT, (48 * dpToPx).toInt())
                            .apply { bottomMargin = (12 * dpToPx).toInt() }
                        mMainContainerParent.addView(this, 0, lp)
                    }
                }
            }

            isSearchMode -> {
                if (_searchView!!.tag == 1) return
                _searchView!!.apply {
                    tag = 1
                    background = null
                    applyThemeColors()
                    seslSetUpButtonVisibility(VISIBLE)
                    seslSetOnUpButtonClickListener { endSearchMode() }
                    onCloseClickListener = null
                    if (!isDescendantOf(mSearchToolbar!!)) {
                        (parent as? ViewGroup)?.removeView(this)
                        mSearchToolbar!!.addView(
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

    private fun iconifyActionModeSearchView(iconify: Boolean){
        _searchView!!.apply {
            isVisible = !iconify
            if (!iconify) isIconified = false//To focus
        }
        searchMenuItem!!.isVisible = iconify
    }

    /**
     * The [SearchView] for [search mode][startSearchMode] or [action mode search][SearchOnActionMode].
     *
     * Note: Apps should access this view using [SearchModeListener] callback
     * to prevent premature inflation.
     */
    internal val searchView: SearchView
        get() { setupSearchView(); return _searchView!! }


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
        require (Intent.ACTION_SEARCH == intent.action) {
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
                syncActionModeMenu()
                if (showActionModeSearchPending) {
                    showActionModeSearchPending = false
                    showActionModeSearchView()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private var updateAllSelectorJob: Job? = null
    private var searchOnActionMode: SearchOnActionMode? = null
    private var showActionModeSearchPending = false

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
     *
     * @see [endActionMode]
     */
    @JvmOverloads
    open fun startActionMode(
        listener: ActionModeListener,
        searchOnActionMode: SearchOnActionMode = Dismiss,
        allSelectorStateFlow: StateFlow<AllSelectorState>? = null,
        showCancel: Boolean = false
    ) {
        isActionMode = true
        updateAllSelectorJob?.cancel()
        ensureActionModeViews()
        mActionModeListener = listener
        this.searchOnActionMode = searchOnActionMode

        when (searchOnActionMode) {
            Dismiss -> if (isSearchMode) endSearchMode()
            NoDismiss -> if (isSearchMode) animatedVisibility(mSearchToolbar!!, GONE)
            is Concurrent -> {
                setupActionModeSearchMenu()
                setupSearchView()
                searchOnActionMode.searchModeListener?.let {
                    mSearchModeListener = it
                }
                post{
                    if (isSearchMode || !_searchView!!.query.isNullOrEmpty()) {
                        if (isTouching) {
                            showActionModeSearchPending = true
                        } else{
                            showActionModeSearchView()
                        }
                    }
                }
            }
        }

        animatedVisibility(_mainToolbar, GONE)
        showActionModeToolbarAnimate()
        setupActionModeMenu(showCancel)
        allSelectorStateFlow?.let {
            updateAllSelectorJob = activity!!.lifecycleScope.launch {
                it.flowWithLifecycle(activity!!.lifecycle)
                    .collectLatest {
                        updateAllSelectorInternal(it.totalSelected, it.isEnabled, it.isChecked)
                    }
            }
        }

        _appBarLayout.addOnOffsetChangedListener(
            AppBarOffsetListener().also { mActionModeTitleFadeListener = it })

        mCollapsingToolbarLayout.seslSetSubtitle(null)
        _mainToolbar.subtitle = null

        setupAllSelectorOnClickListener()
        updateOnBackCallbackState()
    }

    private inline fun showActionModeToolbarAnimate() {
        animatedVisibility(mActionModeToolbar!!, VISIBLE)

        val overshoot = CachedInterpolatorFactory.getOrCreate(Type.OVERSHOOT)

        mActionModeCheckBox.apply {
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

        mActionModeTitleTextView.apply {
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
        mActionModeToolbar!!.apply {
            inflateMenu(R.menu.tbl_am_common)
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

    private fun showActionModeSearchView(){
        iconifyActionModeSearchView(false)
        if (!isSearchMode) {
            setupSearchModeListener()
            mSearchModeListener!!.onSearchModeToggle(_searchView!!, true)
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
        animatedVisibility(mActionModeToolbar!!, GONE)
        if (isSearchMode) {
            //return to search mode interface
            ensureSearchModeToolbar()
            setupSearchView()
            animatedVisibility(mSearchToolbar!!, VISIBLE)
            mCollapsingToolbarLayout.title =
                resources.getString(appcompatR.string.sesl_searchview_description_search)
            mCollapsingToolbarLayout.seslSetSubtitle(null)
            _searchView!!.isIconified = false
        } else {
            clearActionModeSearch()
            showMainToolbarAnimate()
            mCustomFooterContainer!!.visibility = VISIBLE
            applyCachedTitles()
        }
        mBottomActionModeBar.visibility = GONE
        mActionModeListener!!.onEndActionMode()
        //This clears menu including the common action mode menu
        //items - search and cancel
        mMenuSynchronizer!!.clear()
        _appBarLayout.removeOnOffsetChangedListener(mActionModeTitleFadeListener)
        mActionModeSelectAll.setOnClickListener(null)
        mActionModeCheckBox.isChecked = false
        mActionModeTitleFadeListener = null
        mActionModeListener = null
        mMenuSynchronizer = null
        updateAllSelectorJob = null
        searchOnActionMode = null
        updateOnBackCallbackState()
    }

    private inline fun setupAllSelectorOnClickListener() {
        mActionModeSelectAll.setOnClickListener {
            mActionModeCheckBox.apply {
                isChecked = !isChecked
                mActionModeListener!!.onSelectAll(isChecked)
            }
        }
    }

    private inline fun setupActionModeMenu(showCancel: Boolean) {
        forcePortraitMenu = showCancel

        mActionModeToolbar!!.apply {
            var cancelMenuItem: MenuItem? = menu.findItem(R.id.menu_item_am_cancel)
            if (showCancel && cancelMenuItem == null) {
                inflateMenu(R.menu.tbl_am_common)
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

        mMenuSynchronizer = MenuSynchronizer(
            mBottomActionModeBar,
            mActionModeToolbar!!,
            onMenuItemClick = {
                mActionModeListener!!.onMenuItemClicked(it)
            },
            null
        ).apply {
          mActionModeListener!!.onInflateActionMenu(this.menu, activity!!.menuInflater)
        }
    }

    private inline fun syncActionModeMenu() {
        if (!isActionMode) return
        if (!isTouching && mCustomFooterContainer!!.isVisible){
            mCustomFooterContainer!!.isVisible = false
            if (SDK_INT >= 26) {
                postOnAnimationDelayed({ if (isActionMode) syncActionModeMenuInternal() }, 300)
                return
            }
        }
        syncActionModeMenuInternal()
    }

    private fun syncActionModeMenuInternal() {
        if (mSelectedItemsCount > 0) {
            if (!isTouching) {
                val isMenuModePortrait = forcePortraitMenu
                        || DeviceLayoutUtil.isPortrait(resources.configuration)
                        || DeviceLayoutUtil.isTabletLayoutOrDesktop(context)
                mMenuSynchronizer!!.state =
                    if (isMenuModePortrait) State.PORTRAIT else State.LANDSCAPE
            }
        } else {
            mMenuSynchronizer!!.state = State.HIDDEN
        }
    }

    private fun ensureActionModeViews() {
        if (mActionModeToolbar == null) {
            mActionModeToolbar =
                (mCollapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_action_mode)
                    .inflate() as Toolbar).also {
                    mActionModeSelectAll = it.findViewById(R.id.toolbarlayout_selectall)
                    mActionModeTitleTextView =
                        it.findViewById(R.id.toolbar_layout_action_mode_title)
                }
            mActionModeCheckBox =
                mActionModeSelectAll.findViewById(R.id.toolbarlayout_selectall_checkbox)
            mActionModeSelectAll.setOnClickListener { mActionModeCheckBox.setChecked(!mActionModeCheckBox.isChecked) }
            mBottomActionModeBar =
                findViewById<ViewStub>(R.id.viewstub_tbl_actionmode_bottom_menu).inflate() as BottomNavigationView
        }
    }

    private inline fun clearActionModeSearch() {
        _searchView?.apply {
            isGone = true
            mSearchModeListener?.onSearchModeToggle(this, false)
            mSearchModeListener = null
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
    private fun updateAllSelectorInternal(count: Int, enabled: Boolean, checked: Boolean?) {
        if (!isActionMode) {
            Log.w(TAG, "'updateAllSelector' called with action mode not started.")
            return
        }
        ensureActionModeViews()
        if (checked != null) {
            mActionModeCheckBox.isChecked = checked
        }
        if (mSelectedItemsCount != count) {
            mSelectedItemsCount = count
            syncActionModeMenu()
            val title = if (count > 0) {
                resources.getString(R.string.oui_action_mode_n_selected, count)
            } else {
                resources.getString(R.string.oui_action_mode_select_items)
            }
            mCollapsingToolbarLayout.title = title
            mActionModeTitleTextView.text = title
        }

        mActionModeSelectAll.isEnabled = enabled
    }


    //
    // others
    //
    protected open val handleInsets get() = mHandleInsets

    @SuppressLint("NewApi")
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (mHandleInsets) {
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                max(
                    systemBarsInsets.bottom.toDouble(),
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toDouble()
                ).toInt()
            )
            return insets
        } else {
            return super.onApplyWindowInsets(insets)
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

    private fun applyMainRoundedCorners(){
        when (mainRoundedCorners) {
            ALL -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_TOP
                mBottomRoundedCorner.isVisible = true
            }

            TOP -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_TOP
                mBottomRoundedCorner.isVisible = false
            }

            BOTTOM -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_NONE
                mBottomRoundedCorner.isVisible = true
            }

            NONE -> {
                _mainContainer!!.roundedCorners = ROUNDED_CORNER_NONE
                mBottomRoundedCorner.isVisible = false
            }
        }
    }

    class ToolbarLayoutParams(context: Context, attrs: AttributeSet?) :
        LayoutParams(context, attrs) {
        val layoutLocation = attrs?.let { at ->
            context.obtainStyledAttributes(at, R.styleable.ToolbarLayoutParams).use {
                it.getInteger(R.styleable.ToolbarLayoutParams_layout_location, MAIN_CONTENT)
            }} ?: MAIN_CONTENT
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

    private inner class AppBarOffsetListener : AppBarLayout.OnOffsetChangedListener {
        override fun onOffsetChanged(layout: AppBarLayout, verticalOffset: Int) {
            if (mActionModeToolbar!!.isVisible) {
                val layoutPosition = abs(_appBarLayout.top)
                val collapsingTblHeight = mCollapsingToolbarLayout.height
                val alphaRange = collapsingTblHeight * 0.17999999f
                val toolbarTitleAlphaStart = collapsingTblHeight * 0.35f

                if (_appBarLayout.seslIsCollapsed()) {
                    mActionModeTitleTextView.alpha = 1.0f
                } else {
                    mActionModeTitleTextView.alpha = (150.0f / alphaRange
                            * (layoutPosition - toolbarTitleAlphaStart) / 255f).coerceIn(0f, 1f)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ToolbarLayout"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val AMT_GROUP_MENU_ID: Int = 9999
        internal const val MAIN_CONTENT = 0
        private const val APPBAR_HEADER = 1
        private const val FOOTER = 2
        private const val ROOT = 3
        private const val ROOT_BOUNDED = 6

        private const val ROUNDED_CORNER_TOP = ROUNDED_CORNER_TOP_LEFT or ROUNDED_CORNER_TOP_RIGHT
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
        @IntRange(from = 1, to = BADGE_LIMIT_NUMBER.toLong())
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
 * It handles dismissing the soft keyboard when the query is submitted.
 *
 * @param onBackBehavior Defines the [behavior][ToolbarLayout.SearchModeOnBackBehavior] when the back button is pressed during search mode.
 * @param onQuery Lambda function to be invoked when the query text changes or is submitted.
 *               Return true if the query has been handled, false otherwise.
 * @param onStart Lambda function to be invoked when search mode starts.
 * @param onEnd Lambda function to be invoked when search mode ends.
 *
 * Example usage:
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
 *
 * This method sets up the action mode with the specified callbacks and options.
 **
 * @param onInflateMenu Lambda function called at the start of [startActionMode].
 * Inflate the menu items for this action mode session using this menu.
 *
 * @param onEnd Lambda function to be invoked when action mode ends.
 *
 * @param onSelectMenuItem Lambda function to be invoked when an action menu item is selected.
 * Return true if the item click is handled, false otherwise.
 *
 * @param onSelectAll Lambda function to be invoked when the `All` selector is clicked.
 * This will not be triggered with [ToolbarLayout.updateAllSelector].
 *
 * @param searchOnActionMode (optional) The [SearchOnActionMode] option to set for this action mode.
 * Defaults to [SearchOnActionMode.Dismiss].
 *
 * @param allSelectorStateFlow (Optional) StateFlow of [AllSelectorState] that updates the `All` selector state and count.
 *
 * Example usage:
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
 *
 * @see ToolbarLayout.endActionMode
 *
 */
inline fun <T : ToolbarLayout> T.startActionMode(
    crossinline onInflateMenu: (menu: Menu, menuInflater: MenuInflater) -> Unit,
    crossinline onEnd: () -> Unit,
    crossinline onSelectMenuItem: (item: MenuItem) -> Boolean,
    crossinline onSelectAll: (Boolean) -> Unit,
    searchOnActionMode: SearchOnActionMode = Dismiss,
    allSelectorStateFlow: StateFlow<AllSelectorState>? = null,
    showCancel: Boolean = false
) {
    startActionMode(
        object : ActionModeListener {
            override fun onInflateActionMenu(menu: Menu, menuInflater: MenuInflater) = onInflateMenu(menu, menuInflater)
            override fun onEndActionMode() = onEnd()
            override fun onMenuItemClicked(item: MenuItem) = onSelectMenuItem(item)
            override fun onSelectAll(isChecked: Boolean) = onSelectAll.invoke(isChecked)
        },
        searchOnActionMode,
        allSelectorStateFlow,
        showCancel
    )
}

