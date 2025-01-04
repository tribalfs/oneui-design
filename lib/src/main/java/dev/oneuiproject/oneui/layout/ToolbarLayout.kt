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
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.MenuRes
import androidx.annotation.RestrictTo
import androidx.appcompat.view.menu.SeslMenuItem
import androidx.appcompat.widget.ActionModeSearchView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.appCompatActivity
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneui.layout.ToolbarLayout.ActionModeListener
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode
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
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.*
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.Companion.MARGIN_PROVIDER_ADP_DEFAULT
import kotlinx.coroutines.Job
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

    @Deprecated("Use the `ActionModeListener` parameter when calling startActionMode() instead.")
    interface ActionModeCallback {
        fun onShow(toolbarLayout: ToolbarLayout?)
        fun onDismiss(toolbarLayout: ToolbarLayout?)
    }

    interface ActionModeListener {
        /** Called at the start of [startActionMode].
         * @param menu The [Menu] to be used for action menu items.
         * Inflate the menu items for this action mode session using this menu.
         * @see onMenuItemClicked */
        fun onInflateActionMenu(menu: Menu)

        /** Called when the current action mode session is ended.*/
        fun onEndActionMode()

        /** Called when an action mode menu item is clicked.
         * @see onInflateActionMenu*/
        fun onMenuItemClicked(item: MenuItem): Boolean

        /** Called when the 'All' selector is clicked. This will not be triggered with [setActionModeAllSelector].*/
        fun onSelectAll(isChecked: Boolean)
    }

    /**
     * @see DISMISS
     * @see CLEAR_CLOSE
     * @see CLEAR_DISMISS
     */
    enum class SearchModeOnBackBehavior {
        /**
         * Search mode behavior where swipe gesture or back button press
         * ends search mode and then unregisters it's onBackPressed callback.
         * Intended to be used when implementing search mode in a fragment of a multi-fragment activity
         * @see startSearchMode
         * @see endSearchMode
         */
        DISMISS,

        /**
         * Similar to [DISMISS] but clears the non-empty input query first.
         * @see startSearchMode
         * @see endSearchMode
         */
        CLEAR_DISMISS,

        /**
         * Similar to [CLEAR_DISMISS] but it unregisters it's onBackPressed callback without ending the search mode.
         * Intended to be  used on a dedicated search activity.
         */
        CLEAR_CLOSE
    }

    /**
     * @see Dismiss
     * @see NoDismiss
     * @see Concurrent
     */
    sealed interface SearchOnActionMode{
        /**
         * Search mode will be dismissed if active.
         */
        data object Dismiss: SearchOnActionMode

        /**
         * Search mode will not be dismissed if active.
         * It will return to search interface once action mode is ended.
         */
        data object NoDismiss: SearchOnActionMode

        /**
         * Allow search while on action mode. This requires a separate [SearchModeListener] to be
         * set.
         * @param searchModeListener The [SearchModeListener] for this action mode search.
         */
        data class Concurrent(val searchModeListener: SearchModeListener): SearchOnActionMode
    }

    val activity by lazy(LazyThreadSafetyMode.NONE) { context.appCompatActivity }

    private var mSelectedItemsCount = 0

    @Deprecated("Replaced with mActionModeCallBack")
    private var mActionModeCallback: ActionModeCallback? = null
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
    @JvmField
    internal var mExpanded: Boolean = false
    @JvmField
    internal var mTitleCollapsed: CharSequence? = null
    @JvmField
    internal var mTitleExpanded: CharSequence? = null
    @JvmField
    internal var mMainContainer: FrameLayout? = null
    private lateinit var mMainContainerParent: LinearLayout
    private var mNavigationIcon: Drawable? = null
    private var mSubtitleCollapsed: CharSequence? = null
    private var mSubtitleExpanded: CharSequence? = null

    private var mNavigationBadgeIcon: LayerDrawable? = null

    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mCollapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var mMainToolbar: Toolbar

    private lateinit var mCoordinatorLayout: AdaptiveCoordinatorLayout
    private lateinit var mBottomRoundedCorner: LinearLayout
    private lateinit var mFooterParent: LinearLayout

    private var mActionModeToolbar: Toolbar? = null
    private lateinit var mActionModeSelectAll: LinearLayout
    private lateinit var mActionModeCheckBox: CheckBox
    private lateinit var mActionModeTitleTextView: TextView
    @Deprecated("Replaced with mActionModeCallBack")
    private var mOnSelectAllListener: CompoundButton.OnCheckedChangeListener? = null

    private var mCustomFooterContainer: FrameLayout? = null
    private lateinit var mBottomActionModeBar: BottomNavigationView

    private var mSearchToolbar: Toolbar? = null
    private lateinit var mSearchView: SearchView
    private var mSearchModeListener: SearchModeListener? = null
    @JvmField
    internal var searchModeOBPBehavior = CLEAR_DISMISS
    private var mActionModeSearchView: ActionModeSearchView? = null
    private var searchMenuItem: MenuItem? = null

    private var mMenuSynchronizer: MenuSynchronizer? = null
    @JvmField
    internal var mShowSwitchBar = false

    private var _switchBar: SeslSwitchBar? = null
    val switchBar
        get() = _switchBar
            ?: (mMainContainerParent.findViewById<ViewStub>(R.id.viewstub_tbl_switchbar)
                .inflate() as SeslSwitchBar).also { _switchBar = it }

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
    interface SearchModeListener {
        fun onQueryTextSubmit(query: String?): Boolean
        fun onQueryTextChange(newText: String?): Boolean
        fun onSearchModeToggle(searchView: SearchView, visible: Boolean)
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
            else -> mMainContainer?.addView(child, params) ?: super.addView(child, index, params)
        }
    }

    public override fun generateDefaultLayoutParams(): LayoutParams {
        return ToolbarLayoutParams(context, null)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return ToolbarLayoutParams(context, attrs)
    }

    open val navButtonsHandler: NavButtonsHandler by lazy(LazyThreadSafetyMode.NONE) {
        ToolbarLayoutButtonsHandler(mMainToolbar)
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
                ?: getDefaultNavigationIconResource()?.let { d ->
                    ContextCompat.getDrawable(context, d) }
            mTitleCollapsed = it.getString(R.styleable.ToolbarLayout_title)
            mTitleExpanded = mTitleCollapsed
            mSubtitleExpanded = it.getString(R.styleable.ToolbarLayout_subtitle)
            if (VERSION.SDK_INT >= 30 && !fitsSystemWindows) {
                mHandleInsets = it.getBoolean(R.styleable.ToolbarLayout_handleInsets, true)
            }
            mShowSwitchBar = it.getBoolean(R.styleable.ToolbarLayout_showSwitchBar, false)
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
        mAppBarLayout = mCoordinatorLayout.findViewById<AppBarLayout?>(R.id.toolbarlayout_app_bar)
            .apply { setTag(R.id.tag_side_margin_excluded, true) }
        mCollapsingToolbarLayout = mAppBarLayout.findViewById(R.id.toolbarlayout_collapsing_toolbar)
        mMainToolbar = mCollapsingToolbarLayout.findViewById(R.id.toolbarlayout_main_toolbar)

        mMainContainerParent = mCoordinatorLayout.findViewById(R.id.tbl_main_content_parent)
        mMainContainer = mMainContainerParent.findViewById(R.id.tbl_main_content)
        mBottomRoundedCorner = mCoordinatorLayout.findViewById(R.id.tbl_bottom_corners)

        mFooterParent = findViewById(R.id.tbl_footer_parent)
        mCustomFooterContainer = findViewById(R.id.tbl_custom_footer_container)

        activity?.apply {
            setSupportActionBar(mMainToolbar)
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
        navButtonsHandler.showNavigationButtonAsBack = _showNavAsBack
        if (mShowSwitchBar) switchBar.visibility = VISIBLE
        setNavigationButtonIcon(mNavigationIcon)
        setTitle(mTitleExpanded, mTitleCollapsed)
        setExpandedSubtitle(mSubtitleExpanded)
        updateAppbarHeight()
    }

    public override fun onAttachedToWindow() {
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
        mCoordinatorLayout.configureAdaptiveMargin(marginProviderImpl, getAdaptiveChildViews())
    }

    internal open fun getAdaptiveChildViews(): Set<View> =
         mCoordinatorLayout.children.filterNot { it.getTag(R.id.tag_side_margin_excluded) == true }.toSet()


    private fun updateAppbarHeight() {
        mAppBarLayout.isEnabled = mExpandable
        if (mExpandable) {
            mAppBarLayout.seslSetCustomHeightProportion(false, 0f)
        } else {
            mAppBarLayout.seslSetCustomHeight(
                context.resources.getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_height_with_padding))
        }
    }

    //
    // AppBar methods
    //
    val appBarLayout: AppBarLayout
        /**
         * Returns the [AppBarLayout].
         */
        get() = mAppBarLayout

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val toolbar: Toolbar
        /**
         * Returns the [Toolbar].
         */
        get() = mMainToolbar

    /**
     * Set the title of both the collapsed and expanded Toolbar.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     */
    open fun setTitle(title: CharSequence?) {
        setTitle(title, title)
    }

    /**
     * Set the title of the collapsed and expanded Toolbar independently.
     * The expanded title might not be visible in landscape or on devices with small dpi.
     */
    fun setTitle(
        expandedTitle: CharSequence?,
        collapsedTitle: CharSequence?
    ) {
        mMainToolbar.title = collapsedTitle.also { mTitleCollapsed = it }
        mCollapsingToolbarLayout.title = expandedTitle.also { mTitleExpanded = it }
    }

    /**
     * Set the subtitle of the [CollapsingToolbarLayout].
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun setExpandedSubtitle(expandedSubtitle: CharSequence?) {
        mCollapsingToolbarLayout.seslSetSubtitle(expandedSubtitle.also { mSubtitleExpanded = it })
    }

    /**
     * Set the subtitle of the collapsed Toolbar.
     */
    fun setCollapsedSubtitle(collapsedSubtitle: CharSequence?) {
        mMainToolbar.subtitle = collapsedSubtitle.also { mSubtitleCollapsed = it }
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
     * @param animate whether or not to animate the expanding or collapsing.
     */
    fun setExpanded(expanded: Boolean, animate: Boolean) {
        if (mExpandable) {
            mExpanded = expanded
            mAppBarLayout.setExpanded(expanded, animate)
        } else Log.d(TAG, "setExpanded: mExpandable is false")
    }

    /**
     * Represents the expanded state of the Toolbar.
     *
     * @see setExpanded
     */
    var isExpanded: Boolean
        get() = mExpandable && !mAppBarLayout.seslIsCollapsed()
        set(expanded) {
            setExpanded(expanded, mAppBarLayout.isLaidOut)
        }

    /**
     * Replace the title of the expanded Toolbar with a custom View.
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun setCustomTitleView(view: View) {
        setCustomTitleView(
            view,
            CollapsingToolbarLayout.LayoutParams(view.layoutParams)
        )
    }

    /**
     * Replace the title of the expanded Toolbar with a custom View including LayoutParams.
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun setCustomTitleView(
        view: View,
        params: CollapsingToolbarLayout.LayoutParams? = null
    ) {
        (params ?: CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)).apply {
            seslSetIsTitleCustom(true)
            mCollapsingToolbarLayout.seslSetCustomTitleView(view, this)
        }
    }

    /**
     * Replace the subtitle of the expanded Toolbar with a custom View.
     * This might not be visible in landscape or on devices with small dpi.
     */
    fun setCustomSubtitle(view: View) {
        mCollapsingToolbarLayout.seslSetCustomSubtitle(view)
    }

    var isImmersiveScroll: Boolean
        /**
         * Returns true if the immersive scroll is enabled.
         */
        get() = mAppBarLayout.seslGetImmersiveScroll()
        /**
         * Enable or disable the immersive scroll of the Toolbar.
         * When this is enabled the Toolbar will completely hide when scrolling up.
         */
        set(activate) {
            if (VERSION.SDK_INT >= 30) {
                mAppBarLayout.seslSetImmersiveScroll(activate)
            } else {
                Log.e(
                    TAG,
                    "setImmersiveScroll: immersive scroll is available only on api 30 and above"
                )
            }
        }

    @Deprecated("You can directly invoke setBadge on the MenuItem.",
        ReplaceWith("menuItem.setBadge(badge)", "dev.oneuiproject.oneui.ktx.setBadge")
    )
    fun setMenuItemBadge(@IdRes id: Int, text: String?) {
    }

    /**
     * Sets the badge of a Toolbar MenuItem.
     *
     * Important: Requires sesl.androidx.appcompat:1.7.0+1.0.34-sesl6+rev0 or higher
     *
     * @param menuItem The [menuItem][SeslMenuItem] to set the badge
     * @param badge The [Badge] to be set.
     */
    @Deprecated("You can directly invoke setBadge on the MenuItem.",
        ReplaceWith("menuItem.setBadge(badge)", "dev.oneuiproject.oneui.ktx.setBadge")
    )
    inline fun setMenuItemBadge(menuItem: SeslMenuItem, badge: Badge) = menuItem.setBadge(badge)

    /**
     * Sets the badge of a Toolbar MenuItem.
     * This should be invoked after the MenuItem is inflated.
     *
     * Important: Requires sesl.androidx.appcompat:1.7.0+1.0.34-sesl6+rev0 or higher
     *
     * @param id    The resource ID of the MenuItem
     * @param badge The [Badge] to be displayed.
     */
    @Deprecated("You can directly invoke setBadge on the MenuItem.",
        ReplaceWith("menu.findItem(id).setBadge(badge)", "dev.oneuiproject.oneui.ktx.setBadge")
    )
    fun setMenuItemBadge(@IdRes id: Int, badge: Badge) {
        val item = mMainToolbar.menu.findItem(id)
        if (item is SeslMenuItem) {
            setMenuItemBadge(item, badge)
        } else {
            Log.e(
                TAG, "setMenuItemBadge: MenuItem with id $id not found. " +
                        "Ensure that it's already instantiated."
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
     * Change the visibility of the navigation button.
     * This applies only when [showNavigationButtonAsBack] is `false` (default).
     * This is `false` by default.
     *
     * @see setNavigationButtonOnClickListener
     * @see setNavigationButtonIcon
     * @see setNavigationButtonTooltip
     */
    @Deprecated("Use `showNavigationButton` property instead.",
        replaceWith = ReplaceWith("showNavigationButton = visible"))
    fun setNavigationButtonVisible(visible: Boolean) {
        navButtonsHandler.showNavigationButton = visible
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
     * Sets the icon to a back icon, the tooltip to 'Navigate up' and calls [OnBackPressedDispatcher.onBackPressed] when clicked.
     *
     * @see showNavigationButtonAsBack
     */
    @Deprecated("Use `showNavigationButtonAsBack` property instead",
        replaceWith = ReplaceWith("showNavigationButtonAsBack = true"))
    fun setNavigationButtonAsBack() {
        if (!isInEditMode) {
            showNavigationButtonAsBack = true
        }
    }

    private var _showNavAsBack = false

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
        ensureSearchModeViews()
        isSearchMode = true
        mSearchModeListener = listener
        if (isActionMode) endActionMode()
        searchModeOBPBehavior = searchModeOnBackBehavior
        animatedVisibility(mMainToolbar, GONE)
        animatedVisibility(mSearchToolbar!!, VISIBLE)
        mCustomFooterContainer!!.visibility = GONE
        setExpanded(expanded = false, animate = true)
        setupSearchModeListener()
        mSearchView.isIconified = false
        mCollapsingToolbarLayout.title =
            resources.getString(appcompatR.string.sesl_searchview_description_search)
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        updateOnBackCallbackState()
        mSearchModeListener!!.onSearchModeToggle(mSearchView, true)
    }

    private inline fun setupSearchModeListener() {
        mSearchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return mSearchModeListener?.onQueryTextSubmit(query) == true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    updateOnBackCallbackState()
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
        mSearchToolbar!!.visibility = GONE
        animatedVisibility(mMainToolbar, VISIBLE)
        setTitle(mTitleExpanded, mTitleCollapsed)
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
        mCustomFooterContainer!!.visibility = VISIBLE
        mSearchModeListener!!.onSearchModeToggle(mSearchView, false)
        // We are clearing the listener first. We don't want to trigger
        // SearchModeListener.onQueryTextChange callback
        // when clearing the SearchView's input field
        mSearchModeListener = null
        mSearchView.apply {
            setOnQueryTextListener(null)
            setQuery("", false)
        }
        updateOnBackCallbackState()
    }

    /**
     * Show the [SearchView] in the Toolbar.
     * To enable the voice input icon in the SearchView, please refer to the project wiki.
     * TODO: link to the wiki on how to use the voice input feature.
     */
    @Deprecated("Replaced by startSearchMode().", ReplaceWith("startSearchMode(listener)"))
    open fun showSearchMode() {
        mSearchModeListener?.let { startSearchMode(it) }
            ?: Log.e(TAG, "Can't start search mode without setting the listener.")
    }

    /**
     * Dismiss the [SearchView] in the Toolbar.
     *
     * @see showSearchMode
     * @see startSearchMode
     * @see endSearchMode
     */
    @Deprecated("Replaced by endSearchMode().", ReplaceWith("endSearchMode()"))
    open fun dismissSearchMode() {
        endSearchMode()
    }

    private fun ensureSearchModeViews() {
        if (mSearchToolbar == null) {
            mSearchToolbar =
                mCollapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_search)
                    .inflate() as Toolbar
            mSearchView = mSearchToolbar!!.findViewById(R.id.toolbarlayout_search_view)
            mSearchView.seslSetUpButtonVisibility(VISIBLE)
            mSearchView.seslSetOnUpButtonClickListener { endSearchMode() }
            activity?.let { mSearchView.setSearchableInfoFrom(it) }
        }
    }

    val searchView: SearchView
        /**
         * Returns the [SearchView] of the Toolbar.
         */
        get() {
            ensureSearchModeViews()
            return mSearchView
        }

    /**
     * Set the [SearchModeListener] for the Toolbar's SearchMode.
     */
    @Deprecated(
        "Set it as parameter when calling startSearchMode().",
        ReplaceWith("startSearchMode(listener)")
    )
    fun setSearchModeListener(listener: SearchModeListener?) {
        mSearchModeListener = listener
    }

    /**
     * Forward the voice input result to the Toolbar.
     */
    @Deprecated("Use setSearchQueryFromIntent(intent) instead.",
        ReplaceWith("setSearchQueryFromIntent(intent)"))
    fun onSearchModeVoiceInputResult(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            mSearchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
        }
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
        require (Intent.ACTION_SEARCH == intent.action) {
           "setSearchQueryFromIntent: Intent action is not ACTION_SEARCH."
        }
        if (mActionModeSearchView?.isVisible == true) {
            mActionModeSearchView!!.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
        }else if (isSearchMode) {
            mSearchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
        }
    }

    @Deprecated("This is now a no op.")
    fun setActionModeToolbarShowAlwaysMax(max: Int) {
        //no op
    }

    @Deprecated("Use the `ActionModeListener` param when calling startActionMode() instead.")
    fun setOnActionModeListener(callback: ActionModeCallback?) {
        mActionModeCallback = callback
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
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private var updateAllSelectorJob: Job? = null
    private var mSearchOnActionMode: SearchOnActionMode? = null

    /**
     * Starts an Action Mode session. This shows the Toolbar's ActionMode with a toggleable 'All' Checkbox
     * and a counter ('x selected') that temporarily replaces the Toolbar's title.
     *
     * @param listener The [ActionModeListener] to be invoked for this action mode.
     * @param searchOnActionMode (optional) The [SearchOnActionMode] option to set for this action mode.
     * It is set to [SearchOnActionMode.Dismiss] by default.
     * @param allSelectorStateFlow StateFlow of [AllSelectorState] that will be used to update "All" selector state and count
     *
     * @see [endActionMode]
     */
    @JvmOverloads
    open fun startActionMode(
        listener: ActionModeListener,
        searchOnActionMode: SearchOnActionMode = SearchOnActionMode.Dismiss,
        allSelectorStateFlow: StateFlow<AllSelectorState>? = null
    ) {
        isActionMode = true
        updateAllSelectorJob?.cancel()
        ensureActionModeViews()
        mActionModeListener = listener
        mSearchOnActionMode = searchOnActionMode
        when (searchOnActionMode) {
            SearchOnActionMode.Dismiss -> {
                if (isSearchMode) endSearchMode()
            }

            SearchOnActionMode.NoDismiss -> {
                if (isSearchMode) {
                    animatedVisibility(mSearchToolbar!!, GONE)
                }
            }

            is SearchOnActionMode.Concurrent -> {
                setupActionModeSearch()
                if (isSearchMode) {
                    val query = mSearchView.query
                    //endSearchMode()
                    animatedVisibility(mSearchToolbar!!, GONE)
                    showActionModeSearchView()
                    mActionModeSearchView!!.setQuery(query, true)
                }
            }
        }
        animatedVisibility(mMainToolbar, GONE)
        showActionModeToolbarAnimate()
        mCustomFooterContainer!!.visibility = GONE
        setupActionModeMenu()
        allSelectorStateFlow?.let {
            updateAllSelectorJob = activity!!.lifecycleScope.launch {
                it.flowWithLifecycle(activity!!.lifecycle)
                    .collectLatest {
                        updateAllSelectorInternal(it.totalSelected, it.isEnabled, it.isChecked)
                    }
            }
        }
        mAppBarLayout.addOnOffsetChangedListener(
            AppBarOffsetListener().also { mActionModeTitleFadeListener = it })
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        mMainToolbar.setSubtitle(null)

        syncActionModeMenuInternal()

        setupAllSelectorOnClickListener()
        updateOnBackCallbackState()
    }

    /**
     * Starts an Action Mode session. This shows the Toolbar's ActionMode with a toggleable 'All' Checkbox
     * and a counter ('x selected') that temporarily replaces the Toolbar's title.
     *
     * @param listener The [ActionModeListener] to be invoke for this action mode.
     * @param keepSearchMode Set to `true` to keep active search mode and
     * restore it's interface when this ActionMode is ended. This is set `false` by default.
     * @param allSelectorStateFlow (Optional) StateFlow of [AllSelectorState] that will be used
     * to update "All" selector state and count
     *
     * @see [ToolbarLayout.endActionMode]
     */
    @Deprecated("Use startActionMode that accepts SearchOnActionMode as one of the params replacing the boolean `keepSearchMode` param.",
        ReplaceWith("startActionMode(listener, searchOnActionMode, allSelectorStateFlow)"))
    @JvmOverloads
    inline fun startActionMode(
        listener: ActionModeListener,
        keepSearchMode: Boolean,
        allSelectorStateFlow: StateFlow<AllSelectorState>? = null
    ){
        startActionMode(listener, if (keepSearchMode) SearchOnActionMode.NoDismiss else SearchOnActionMode.Dismiss , allSelectorStateFlow)
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
        animatedVisibility(mMainToolbar, VISIBLE)

        val overshoot = CachedInterpolatorFactory.getOrCreate(Type.OVERSHOOT)
        val start = if (isRTLayout) -80f else 80f

        mMainToolbar.titleTextView?.apply {
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

        mMainToolbar.subtitleTextView?.apply {
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

    private inline fun setupActionModeSearch() {
        ensureActionModeSearchViews()
        mActionModeToolbar!!.apply {
            inflateMenu(R.menu.tbl_am_search_menu)
            menu.findItem(R.id.menu_item_am_search)!!.let {
                searchMenuItem = it.apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    setOnMenuItemClickListener {
                        showActionModeSearchView()
                        true
                    }
                }
                mActionModeSearchView!!.onCloseClickListener = { v ->
                    mActionModeSearchView!!.query = ""
                    v.isVisible = false
                    it.isVisible = true
                    (mSearchOnActionMode as SearchOnActionMode.Concurrent).searchModeListener
                        .onSearchModeToggle(mActionModeSearchView!!, false)
                }
            }
        }
    }

    private inline fun ensureActionModeSearchViews() {
        if (mActionModeSearchView == null) {
            mActionModeSearchView =
                (mMainContainerParent.findViewById<ViewStub>(R.id.viewstub_oui_view_actionmode_searchview)
                    .inflate() as ActionModeSearchView)
                    .apply {
                        setSearchableInfoFrom(activity!!)
                    }
        }
    }


    private fun showActionModeSearchView() {
        val actionModeSearchListener =
            (mSearchOnActionMode as SearchOnActionMode.Concurrent).searchModeListener
        mActionModeSearchView!!.setOnQueryTextListener(
            object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = actionModeSearchListener.onQueryTextSubmit(query)
                override fun onQueryTextChange(newText: String) = actionModeSearchListener.onQueryTextChange(newText)
            })
        mActionModeSearchView!!.isVisible = true
        searchMenuItem!!.isVisible = false
        setExpanded(expanded = false, animate = true)
        actionModeSearchListener.onSearchModeToggle(mActionModeSearchView!!, true)
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
            animatedVisibility(mSearchToolbar!!, VISIBLE)
            mCollapsingToolbarLayout.title =
                resources.getString(appcompatR.string.sesl_searchview_description_search)
            mCollapsingToolbarLayout.seslSetSubtitle(null)
            mSearchView.requestFocus()
        } else {
            showMainToolbarAnimate()
            mCustomFooterContainer!!.visibility = VISIBLE
            setTitle(mTitleExpanded, mTitleCollapsed)
            mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
            mMainToolbar.subtitle = mSubtitleCollapsed
        }
        hideAndClearActionModeSearchView()
        mBottomActionModeBar.visibility = GONE
        mActionModeListener!!.onEndActionMode()
        mMenuSynchronizer!!.clear()
        mAppBarLayout.removeOnOffsetChangedListener(mActionModeTitleFadeListener)
        mActionModeSelectAll.setOnClickListener(null)
        mActionModeCheckBox.isChecked = false
        setActionModeMenuListenerInternal(null)
        mActionModeTitleFadeListener = null
        mActionModeListener = null
        mMenuSynchronizer = null
        updateAllSelectorJob = null
        mSearchOnActionMode = null
        updateOnBackCallbackState()
    }

    /**
     * Show the Toolbar's ActionMode. This will show a 'All' Checkbox instead of the navigation button,
     * temporarily replace the Toolbar's title with a counter ('x selected')
     * and show a [BottomNavigationView] in the footer.
     * The ActionMode is useful when the user can select items in a list.
     */
    @Deprecated("Use startActionMode() instead.", ReplaceWith("startActionMode(callback)"))
    open fun showActionMode() {
        startActionMode(
            object : ActionModeListener {
                override fun onInflateActionMenu(menu: Menu) {
                    if (mActionModeMenuRes != 0) {
                        activity!!.menuInflater.inflate(mActionModeMenuRes, menu)
                    }
                    mActionModeCallback?.onShow(this@ToolbarLayout)
                }

                override fun onEndActionMode() {
                    mActionModeCallback?.onDismiss(this@ToolbarLayout)
                }

                override fun onMenuItemClicked(item: MenuItem): Boolean = false
                override fun onSelectAll(isChecked: Boolean) {
                    mOnSelectAllListener?.onCheckedChanged(mActionModeCheckBox, isChecked)
                }
            }
        )
    }

    private inline fun setupAllSelectorOnClickListener() {
        mActionModeSelectAll.setOnClickListener {
            mActionModeCheckBox.apply {
                isChecked = !isChecked
                mActionModeListener!!.onSelectAll(isChecked)
            }
        }
    }

    private inline fun setupActionModeMenu() {
        mBottomActionModeBar.menu.apply {
            clear()
            mActionModeListener!!.onInflateActionMenu(this)
        }
        mMenuSynchronizer = MenuSynchronizer(
            mBottomActionModeBar,
            mActionModeToolbar!!,
            onMenuItemClick = {
                mActionModeListener!!.onMenuItemClicked(it)
            },
            null
        )
    }

    private fun setActionModeMenuListenerInternal(listener: NavigationBarView.OnItemSelectedListener?) {
        mBottomActionModeBar.setOnItemSelectedListener(listener)
        if (listener != null) {
            mActionModeToolbar!!.setOnMenuItemClickListener { item: MenuItem ->
                listener.onNavigationItemSelected(
                    mActionModeToolbar!!.menu.findItem(item.itemId)
                )
            }
        } else {
            mActionModeToolbar!!.setOnMenuItemClickListener(null)
        }
    }

    private inline fun syncActionModeMenu() {
        if (!isActionMode) return
        syncActionModeMenuInternal()
    }

    private fun syncActionModeMenuInternal() {
        if (mSelectedItemsCount > 0) {
            val isActionModePortrait = DeviceLayoutUtil.isPortrait(resources.configuration)
                    || DeviceLayoutUtil.isTabletLayoutOrDesktop(context)
            if (isActionModePortrait) {
                if (!isTouching) {
                    mMenuSynchronizer!!.updateState(State.PORTRAIT)
                } else {
                    mMenuSynchronizer!!.updateState(State.HIDDEN)
                }
            } else {
                if (!isTouching) {
                    mMenuSynchronizer!!.updateState(State.LANDSCAPE)
                } else {
                    mMenuSynchronizer!!.updateState(State.HIDDEN)
                }
            }
        } else {
            mMenuSynchronizer!!.updateState(State.HIDDEN)
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

    private inline fun hideAndClearActionModeSearchView() {
        mActionModeSearchView?.apply {
            visibility = GONE
            if (isSearchMode) {
                mSearchView.setQuery(mActionModeSearchView!!.query, false)
                setOnQueryTextListener(null)
                setQuery("", false)
            }else{
                setQuery("", false)
                setOnQueryTextListener(null)
            }
        }
        searchMenuItem?.let {
            if (!it.isVisible) it.isVisible = true
        }
    }

    /**
     * Dismiss the ActionMode.
     *
     * @see endActionMode
     */
    @Deprecated("Use endActionMode() instead.", ReplaceWith("endActionMode()"))
    open fun dismissActionMode() {
        endActionMode()
    }

    /**
     * Set the menu resource for the ActionMode's [BottomNavigationView]
     */
    @Deprecated(
        "Use the ActionModeListener#onInflateActionMenu() callback when calling startActionMode() instead.",
        ReplaceWith("")
    )
    fun setActionModeBottomMenu(@MenuRes menuRes: Int) {
        setActionModeMenu(menuRes)
    }

    /**
     * Set the menu resource for the ActionMode's [BottomNavigationView].
     * On landscape orientation where ActionMode's [BottomNavigationView] will be hidden,
     * the visible items from this menu resource we be shown to ActionMode's [Toolbar] [Menu]
     */
    @Deprecated(
        "Use the ActionModeListener#onInflateActionMenu() callback when calling startActionMode() instead.",
        ReplaceWith("")
    )
    fun setActionModeMenu(@MenuRes menuRes: Int) {
        mActionModeMenuRes = menuRes
    }

    @Deprecated(
        "Access this with ActionModeListener#onInflateActionMenu() callback when calling startActionMode() instead.",
        ReplaceWith("")
    )
    val actionModeBottomMenu: Menu
        /**
         * Returns the [Menu] of the ActionMode's [BottomNavigationView].
         */
        get() {
            ensureActionModeViews()
            return mBottomActionModeBar.menu
        }

    /**
     * Set the listener for the ActionMode's [BottomNavigationView].
     * On landscape orientation, the same listener will be invoke for ActionMode's [Toolbar] [MenuItem]s
     * which are copied from ActionMode's [BottomNavigationView]
     */
    @Deprecated(
        "Use the ActionModeListener#onMenuItemClicked() callback when calling startActionMode() instead.",
        ReplaceWith("")
    )
    fun setActionModeMenuListener(listener: NavigationBarView.OnItemSelectedListener) {
        ensureActionModeViews()
        setActionModeMenuListenerInternal(listener)
    }

    /**
     * Set the menu resource for the ActionMode's [Toolbar].
     */
    @Deprecated("This is now no op.")
    fun setActionModeToolbarMenu(@MenuRes menuRes: Int) {
        //no op
    }

    /**
     * Set the listener for the ActionMode's [Toolbar].
     */
    @Deprecated("This is now no op.")
    fun setActionModeToolbarMenuListener(listener: Toolbar.OnMenuItemClickListener?) {
        //no op
    }

    @Deprecated("Use the ActionModeListener#onInflateActionMenu() callback when calling startActionMode() instead.")
    val actionModeToolbarMenu: Menu
        /**
         * Returns the [Menu] of the ActionMode's [Toolbar].
         *
         */
        get() {
            ensureActionModeViews()
            return mActionModeToolbar!!.menu
        }

    /**
     * Set the ActionMode's count and  checkbox enabled state.
     * Check state will stay.
     *
     * @param count number of selected items in the list
     * @param enabled enabled click
     */
    @Deprecated(
        "Use updateAllSelector() instead.",
        ReplaceWith("updateAllSelector(count, enabled, checked)")
    )
    fun setActionModeAllSelector(count: Int, enabled: Boolean) {
        setActionModeAllSelector(count, enabled, null)
    }

    /**
     * Update action mode's 'All' selector state
     *
     * @param count Number of selected items
     * @param enabled To enable/disable clicking the 'All' selector.
     * @param checked (Optional) check all selector toggle. When not provided or `null` is set,
     * it will keep the current checked state
     */
    @JvmOverloads
    fun updateAllSelector(count: Int, enabled: Boolean, checked: Boolean? = null) {
        if (updateAllSelectorJob != null) {
            Log.w(
                TAG, "'updateAllSelector' ignored because `startActionMode` " +
                        " is provided as param for  startActionMode()."
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

    /**
     * Set the ActionMode's count and Select all checkBox's enabled state and check state
     *
     * @param count number of selected items in the list
     * @param enabled enable or disable click
     * @param checked
     */
    @Deprecated(
        "Use updateAllSelector() instead.",
        ReplaceWith("updateAllSelector(count, enabled, checked)")
    )
    fun setActionModeAllSelector(count: Int, enabled: Boolean, checked: Boolean?) {
        updateAllSelectorInternal(count, enabled, checked)
    }

    /**
     * Set the ActionMode's count. This will change the count in the Toolbar's title
     * and if count = total, the 'All' Checkbox will be checked.
     *
     * @param count number of selected items in the list
     * @param total number of total items in the list
     */
    @Deprecated(
        "Use setActionModeAllSelector() instead.",
        ReplaceWith("setActionModeAllSelector(count, enabled, checked)")
    )
    fun setActionModeCount(count: Int, total: Int) {
        setActionModeAllSelector(count, true, count == total)
    }

    /**
     * Set the listener for the 'All' Checkbox of the ActionMode.
     */
    @Deprecated(
        "Use ActionModeListener#onSelectAll() callback when calling startActionMode() instead.",
        ReplaceWith("")
    )
    fun setActionModeCheckboxListener(listener: CompoundButton.OnCheckedChangeListener?) {
        mOnSelectAllListener = listener
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
                val layoutPosition = abs(mAppBarLayout.top)
                val collapsingTblHeight = mCollapsingToolbarLayout.height
                val alphaRange = collapsingTblHeight * 0.17999999f
                val toolbarTitleAlphaStart = collapsingTblHeight * 0.35f

                if (mAppBarLayout.seslIsCollapsed()) {
                    mActionModeTitleTextView.alpha = 1.0f
                } else {
                    mActionModeTitleTextView.alpha = (150.0f / alphaRange
                            * (layoutPosition - toolbarTitleAlphaStart) / 255f).coerceIn(0f, 1f)
                }
            }
        }
    }

    companion object {
        protected const val TAG = "ToolbarLayout"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val AMT_GROUP_MENU_ID: Int = 9999
        internal const val MAIN_CONTENT = 0
        private const val APPBAR_HEADER = 1
        private const val FOOTER = 2
        private const val ROOT = 3
        private const val ROOT_BOUNDED = 6
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

@Deprecated(
    "Use setNavigationButtonBadge()",
    ReplaceWith("setNavigationButtonBadge(badge)")
)
inline fun <T : ToolbarLayout> T.setNavigationBadge(badge: Badge) {
    setNavigationButtonBadge(badge)
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
            override fun onQueryTextSubmit(query: String?) =
                onQuery(query ?: "", true).also { searchView.clearFocus() }

            override fun onQueryTextChange(newText: String?) = onQuery(newText ?: "", false)


            override fun onSearchModeToggle(searchView: SearchView, visible: Boolean) {
                if (visible) {
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
 *     onInflateMenu = { menu ->
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
    crossinline onInflateMenu: (menu: Menu) -> Unit,
    crossinline onEnd: () -> Unit,
    crossinline onSelectMenuItem: (item: MenuItem) -> Boolean,
    crossinline onSelectAll: (Boolean) -> Unit,
    searchOnActionMode: SearchOnActionMode = SearchOnActionMode.Dismiss,
    allSelectorStateFlow: StateFlow<AllSelectorState>? = null
) {
    startActionMode(
        object : ActionModeListener {
            override fun onInflateActionMenu(menu: Menu) = onInflateMenu(menu)
            override fun onEndActionMode() = onEnd()
            override fun onMenuItemClicked(item: MenuItem) = onSelectMenuItem(item)
            override fun onSelectAll(isChecked: Boolean) = onSelectAll.invoke(isChecked)
        },
        searchOnActionMode,
        allSelectorStateFlow
    )
}


@Deprecated("Use startActionMode that accepts SearchOnActionMode as one of the params replacing the boolean `keepSearchMode` param.",
    ReplaceWith("startActionMode(onInflateMenu, onEnd, onSelectMenuItem, onSelectAll, searchOnActionMode, allSelectorStateFlow)"))
inline fun <T : ToolbarLayout> T.startActionMode(
    crossinline onInflateMenu: (menu: Menu) -> Unit,
    crossinline onEnd: () -> Unit,
    crossinline onSelectMenuItem: (item: MenuItem) -> Boolean,
    crossinline onSelectAll: (Boolean) -> Unit,
    keepSearchMode: Boolean,
    allSelectorStateFlow: StateFlow<AllSelectorState>? = null
) {
    startActionMode(
        object : ActionModeListener {
            override fun onInflateActionMenu(menu: Menu) = onInflateMenu(menu)
            override fun onEndActionMode() = onEnd()
            override fun onMenuItemClicked(item: MenuItem) = onSelectMenuItem(item)
            override fun onSelectAll(isChecked: Boolean) = onSelectAll.invoke(isChecked)
        },
        if (keepSearchMode) SearchOnActionMode.NoDismiss else SearchOnActionMode.Dismiss,
        allSelectorStateFlow
    )
}

