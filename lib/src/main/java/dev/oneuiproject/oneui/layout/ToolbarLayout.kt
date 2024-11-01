@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.ContextWrapper
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
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.MenuRes
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.SeslMenuItem
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.use
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.delegates.OnBackPressedDelegate
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_CLOSE
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS
import dev.oneuiproject.oneui.utils.BADGE_LIMIT_NUMBER
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.MenuSynchronizer
import dev.oneuiproject.oneui.utils.MenuSynchronizer.State
import dev.oneuiproject.oneui.utils.badgeCountToText
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils
import dev.oneuiproject.oneui.view.internal.NavigationBadgeIcon
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
    attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    @Deprecated("Use the `ActionModeListener` parameter when calling StartActionMode() instead.")
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

    var activity: AppCompatActivity? = null
        get() {
            if (field == null) {
                var context = context
                while (context is ContextWrapper) {
                    if (context is AppCompatActivity) {
                        field = context
                        break
                    }
                    context = context.baseContext
                }
            }
            return field
        }
        private set

    private var mSelectedItemsCount = 0

    @Deprecated("Replaced with mActionModeCallBack")
    private var mActionModeCallback: ActionModeCallback? = null
    private var mActionModeListener: ActionModeListener? = null
    private var mActionModeMenuRes: Int = 0

    private val mObpDelegate: OnBackPressedDelegate by lazy {OnBackPressedDelegate(activity!!)}

    private fun updateObpCallbackState() {
        mObpDelegate.stopListening(this)

        val enable = when {
            isActionMode -> true
            isSearchMode -> {
                when (searchModeOBPBehavior) {
                    DISMISS, CLEAR_DISMISS -> true
                    CLEAR_CLOSE -> searchView.query.isNotEmpty() || searchView.isSoftKeyboardShowing
                }
            }
            else -> false
        }
        if (enable.not()) return

        mObpDelegate.startListening(this, {
            when {
                isActionMode -> endActionMode()
                isSearchMode -> {
                    when (searchModeOBPBehavior) {
                        DISMISS -> endSearchMode()
                        CLEAR_CLOSE -> {
                            if (searchView.isSoftKeyboardShowing) {
                                searchView.clearFocus()
                            } else {
                                searchView.setQuery("", true)
                            }
                            updateObpCallbackState()
                        }
                        CLEAR_DISMISS -> {
                            if (searchView.isSoftKeyboardShowing) {
                                searchView.clearFocus()
                            } else if (searchView.query.isNotEmpty()) {
                                searchView.setQuery("", true)
                            } else endSearchMode()

                        }
                    }
                }
            }
        })
    }

    private var mActionModeTitleFadeListener: AppBarOffsetListener? = null

    @JvmField
    protected var mLayout: Int = 0
    @JvmField
    protected var mExpandable: Boolean = false
    @JvmField
    protected var mExpanded: Boolean = false
    @JvmField
    protected var mTitleCollapsed: CharSequence? = null
    @JvmField
    protected var mTitleExpanded: CharSequence? = null
    @JvmField
    protected var mMainContainer: FrameLayout? = null
    @JvmField
    protected var mNavigationIcon: Drawable? = null
    @JvmField
    protected var mSubtitleCollapsed: CharSequence? = null
    @JvmField
    protected var mSubtitleExpanded: CharSequence? = null

    private var mNavigationBadgeIcon: LayerDrawable? = null

    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mCollapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var mMainToolbar: Toolbar

    private lateinit var mCoordinatorLayout: CoordinatorLayout

    private var mActionModeToolbar: Toolbar? = null
    private lateinit var mActionModeSelectAll: LinearLayout
    private lateinit var mActionModeCheckBox: AppCompatCheckBox
    private lateinit var mActionModeTitleTextView: TextView

    private var mFooterContainer: FrameLayout? = null
    private lateinit var mFooterParent: LinearLayout
    private lateinit var mBottomRoundedCorner: LinearLayout
    private lateinit var mBottomActionModeBar: BottomNavigationView

    private var mSearchToolbar: Toolbar? = null
    private lateinit var mSearchView: SearchView
    private var mSearchModeListener: SearchModeListener? = null
    @JvmField
    protected var searchModeOBPBehavior = CLEAR_DISMISS
    @Deprecated("Replaced with mActionModeCallBack")
    private var mOnSelectAllListener: CompoundButton.OnCheckedChangeListener? = null

    private var mMenuSynchronizer: MenuSynchronizer? = null

    /**
     * Check if SearchMode is enabled(=the [SearchView] in the Toolbar is visible).
     */
    var isSearchMode: Boolean = false
        private set

    /**
     * Checks if the ActionMode is enabled.
     */
    var isActionMode: Boolean = false
        private set

    protected var mHandleInsets: Boolean = false

    /**
     * Callback for the Toolbar's SearchMode.
     * Notification that the [SearchView]'s text has been edited or it's visibility changed.
     *
     * @see startSearchMode
     * @see endSearchMode
     */
    interface SearchModeListener {
        fun onQueryTextSubmit(query: String?): Boolean
        fun onQueryTextChange(newText: String?): Boolean
        fun onSearchModeToggle(searchView: SearchView, visible: Boolean)
    }

    protected open fun initLayoutAttrs(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ToolbarLayout, 0, 0).use {
            mLayout = it.getResourceId(R.styleable.ToolbarLayout_android_layout, R.layout.oui_layout_toolbarlayout_appbar)
            mExpandable = it.getBoolean(R.styleable.ToolbarLayout_expandable, true)
            mExpanded = it.getBoolean(R.styleable.ToolbarLayout_expanded, mExpandable)
            mNavigationIcon = it.getDrawable(R.styleable.ToolbarLayout_navigationIcon)
            mTitleCollapsed = it.getString(R.styleable.ToolbarLayout_title)
            mTitleExpanded = mTitleCollapsed
            mSubtitleExpanded = it.getString(R.styleable.ToolbarLayout_subtitle)
            if (VERSION.SDK_INT >= 30 && !fitsSystemWindows) {
                mHandleInsets = it.getBoolean(R.styleable.ToolbarLayout_handleInsets, true)
            }
        }
    }

    protected open fun inflateChildren() {
        if (mLayout != R.layout.oui_layout_toolbarlayout_appbar) {
            Log.w(TAG, "Inflating custom $TAG")
        }

        val inflater = LayoutInflater.from(context)
        inflater.inflate(mLayout, this, true)
        addView(
            inflater.inflate(
                R.layout.oui_layout_toolbarlayout_footer, this, false
            )
        )
    }

    private fun initAppBar() {
        mCoordinatorLayout = findViewById(R.id.toolbarlayout_coordinator_layout)
        mAppBarLayout = mCoordinatorLayout.findViewById(R.id.toolbarlayout_app_bar)
        mCollapsingToolbarLayout = mAppBarLayout.findViewById(R.id.toolbarlayout_collapsing_toolbar)
        mMainToolbar = mCollapsingToolbarLayout.findViewById(R.id.toolbarlayout_main_toolbar)

        mMainContainer = findViewById(R.id.toolbarlayout_main_container)
        mFooterContainer = findViewById(R.id.toolbarlayout_footer_container)
        mFooterParent = findViewById(R.id.toolbarlayout_footer_content)
        mBottomRoundedCorner = findViewById(R.id.toolbarlayout_bottom_corners)

        activity?.apply {
            setSupportActionBar(mMainToolbar)
            supportActionBar!!.apply {
                setDisplayHomeAsUpEnabled(false)
                setDisplayShowTitleEnabled(false)
            }
        }

        setNavigationButtonIcon(mNavigationIcon)
        setTitle(mTitleExpanded, mTitleCollapsed)
        setExpandedSubtitle(mSubtitleExpanded)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (mMainContainer == null || mFooterContainer == null) {
            super.addView(child, index, params)
        } else {
            when ((params as ToolbarLayoutParams).layoutLocation) {
                MAIN_CONTENT -> mMainContainer!!.addView(child, params)
                APPBAR_HEADER -> setCustomTitleView(child,
                    CollapsingToolbarLayout.LayoutParams(params))
                FOOTER -> mFooterContainer!!.addView(child, params)
                ROOT -> mCoordinatorLayout.addView(child, cllpWrapper(params as LayoutParams))
                else -> mMainContainer!!.addView(child, params)
            }
        }
    }

    public override fun generateDefaultLayoutParams(): LayoutParams {
        return ToolbarLayoutParams(context, null)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return ToolbarLayoutParams(context, attrs)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resetAppBar()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshLayout(newConfig)
        resetAppBar()
        syncActionModeMenu()
    }

    private val sideMarginUpdater = Runnable {
        val sideMarginParams = ToolbarLayoutUtils.getSideMarginParams(this.activity!!)
        ToolbarLayoutUtils.setSideMarginParams(mMainContainer!!, sideMarginParams)
        ToolbarLayoutUtils.setSideMarginParams(mBottomRoundedCorner, sideMarginParams)
        ToolbarLayoutUtils.setSideMarginParams(mFooterParent, sideMarginParams)
        requestLayout()
    }

    init {
        orientation = VERTICAL

        initLayoutAttrs(attrs)
        inflateChildren()
        initAppBar()
        this.activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        refreshLayout(resources.configuration)
    }

    private fun refreshLayout(newConfig: Configuration) {
        removeCallbacks(sideMarginUpdater)
        postDelayed(sideMarginUpdater, 40)

        if (!isInEditMode) ToolbarLayoutUtils
            .hideStatusBarForLandscape(this.activity!!, newConfig.orientation)

        val isLandscape = newConfig.orientation == ORIENTATION_LANDSCAPE

        isExpanded = !isLandscape and mExpanded
    }

    private fun resetAppBar() {
        mAppBarLayout.isEnabled = mExpandable
        if (mExpandable) {
            mAppBarLayout.seslSetCustomHeightProportion(false, 0f)
        } else {
            mAppBarLayout.seslSetCustomHeight(context.resources
                .getDimensionPixelSize(appcompatR.dimen.sesl_action_bar_height_with_padding))
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

    var isExpandable: Boolean
        /**
         * Returns if the expanding Toolbar functionality is enabled or not.
         *
         * @see .setExpandable
         */
        get() = mExpandable
        /**
         * Enable or disable the expanding Toolbar functionality.
         * If you simply want to programmatically expand or collapse the toolbar.
         *
         * @see .setExpanded
         */
        set(expandable) {
            if (mExpandable != expandable) {
                mExpandable = expandable
                resetAppBar()
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

    var isExpanded: Boolean
        /**
         * Get the current state of the toolbar.
         *
         * @see .setExpanded
         * @see .setExpanded
         */
        get() = mExpandable && !mAppBarLayout.seslIsCollapsed()
        /**
         * Programmatically expand or collapse the Toolbar.
         */
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
        (params?: CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)).apply {
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

    @Deprecated("Use setMenuItemBadge(Int, Badge) instead.",
        replaceWith = ReplaceWith("setMenuItemBadge(id, badge)"))
    fun setMenuItemBadge(@IdRes id: Int, text: String?) {
    }

    /**
     * Sets the badge of a Toolbar MenuItem.
     *
     * Important: Requires sesl.androidx.appcompat:1.7.0+1.0.34-sesl6+rev0 or higher
     *
     * @param menuItem The menuItem to set the badge
     * @param badge The [Badge] to be set.
     */
    fun setMenuItemBadge(menuItem: SeslMenuItem, badge: Badge) {
        when (badge) {
            is Badge.Numeric -> menuItem.badgeText = badge.count.toString()
            is Badge.Dot-> menuItem.badgeText = ""
            is Badge.None -> menuItem.badgeText = null
        }
    }

    /**
     * Sets the badge of a Toolbar MenuItem.
     * This should be invoked after the MenuItem is inflated.
     *
     * Important: Requires sesl.androidx.appcompat:1.7.0+1.0.34-sesl6+rev0 or higher
     *
     * @param id    The resource ID of the MenuItem
     * @param badge The [Badge] to be displayed.
     */
    fun setMenuItemBadge(@IdRes id: Int, badge: Badge) {
        val item = mMainToolbar.menu.findItem(id)
        if (item is SeslMenuItem) {
            setMenuItemBadge(item, badge)
        }else{
            Log.e(TAG, "setMenuItemBadge: MenuItem with id $id not found. " +
                    "Ensure that it's already instantiated.")
        }
    }

    //
    // Navigation Button methods
    //
    /**
     * Set the navigation icon of the Toolbar.
     * Don't forget to also set a Tooltip with [.setNavigationButtonTooltip].
     */
    fun setNavigationButtonIcon(icon: Drawable?) {
        mNavigationIcon = icon
        if (mNavigationBadgeIcon != null) {
            mNavigationBadgeIcon!!.setDrawable(0, mNavigationIcon)
            mNavigationBadgeIcon!!.invalidateSelf()
            mMainToolbar.navigationIcon = mNavigationBadgeIcon
        } else {
            mMainToolbar.navigationIcon = mNavigationIcon
        }
    }

    /**
     * Change the visibility of the navigation button.
     */
    fun setNavigationButtonVisible(visible: Boolean) {
        if (mNavigationBadgeIcon != null) {
            mMainToolbar.navigationIcon = if (visible) mNavigationBadgeIcon else null
        } else if (mNavigationIcon != null) {
            mMainToolbar.navigationIcon = if (visible) mNavigationIcon else null
        } else {
            this.activity!!.supportActionBar!!.setDisplayHomeAsUpEnabled(visible)
        }
    }

    /**
     * Add a badge to the navigation button.
     * The badge is small orange circle in the top right of the icon which contains text.
     *
     * @param badge The [Badge] to be displayed.
     */
    fun setNavigationButtonBadge(badge: Badge) {
        if (mNavigationIcon != null) {
            when(badge){
                is Badge.Dot, is Badge.Numeric -> {
                    val badgeIcon = NavigationBadgeIcon(context)
                    mNavigationBadgeIcon = LayerDrawable(arrayOf(mNavigationIcon!!, badgeIcon))
                    badgeIcon.setBadge(badge)
                    mMainToolbar.navigationIcon = mNavigationBadgeIcon
                }
                is Badge.None -> {
                    mNavigationBadgeIcon = null
                    mMainToolbar.navigationIcon = mNavigationIcon
                }
            }

        } else Log.d(
            TAG, "setNavigationButtonBadge: no navigation icon" +
                    " has been set"
        )
    }

    /**
     * Set the Tooltip of the navigation button.
     */
    fun setNavigationButtonTooltip(tooltipText: CharSequence?) {
        mMainToolbar.navigationContentDescription = tooltipText
    }

    /**
     * Callback for the navigation button click event.
     */
    fun setNavigationButtonOnClickListener(listener: OnClickListener?) {
        mMainToolbar.setNavigationOnClickListener(listener)
    }

    /**
     * Sets the icon the a back icon, the tooltip to 'Navigate up' and calls [OnBackPressedDispatcher.onBackPressed] when clicked.
     *
     * @see setNavigationButtonIcon
     * @see setNavigationButtonTooltip
     * @see android.app.ActionBar.setDisplayHomeAsUpEnabled
     */
    fun setNavigationButtonAsBack() {
        if (!isInEditMode) {
            this.activity!!.apply {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                setNavigationButtonOnClickListener { onBackPressedDispatcher.onBackPressed() }
            }
        }
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
    open fun startSearchMode(listener: SearchModeListener,
                             searchModeOnBackBehavior: SearchModeOnBackBehavior = CLEAR_DISMISS) {
        ensureSearchModeViews()
        isSearchMode = true
        mSearchModeListener = listener
        if (isActionMode) endActionMode()
        searchModeOBPBehavior = searchModeOnBackBehavior
        animatedVisibility(mMainToolbar, GONE)
        animatedVisibility(mSearchToolbar!!, VISIBLE)
        mFooterContainer!!.visibility = GONE
        setExpanded(expanded = false, animate = true)
        setupSearchModeListener()
        mSearchView.isIconified = false
        mCollapsingToolbarLayout.title = resources.getString(appcompatR.string.sesl_searchview_description_search)
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        updateObpCallbackState()
        mSearchModeListener!!.onSearchModeToggle(mSearchView, true)
    }

    private inline fun setupSearchModeListener() {
        mSearchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return mSearchModeListener?.onQueryTextSubmit(query) == true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    updateObpCallbackState()
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
        isSearchMode = false
        mSearchToolbar!!.visibility = GONE
        animatedVisibility(mMainToolbar, VISIBLE)
        setTitle(mTitleExpanded, mTitleCollapsed)
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
        mFooterContainer!!.visibility = VISIBLE
        mSearchView.setQuery("", false)
        updateObpCallbackState()
        mSearchView.apply {
            setQuery("", false)
            setOnQueryTextListener(null)
        }
        mSearchView.apply {
            setQuery("", false)
            setOnQueryTextListener(null)
        }
        mSearchModeListener!!.onSearchModeToggle(mSearchView, false)
        mSearchModeListener = null
    }

    /**
     * Show the [SearchView] in the Toolbar.
     * To enable the voice input icon in the SearchView, please refer to the project wiki.
     * TODO: link to the wiki on how to use the voice input feature.
     */
    @Deprecated("Replaced by startSearchMode().", ReplaceWith("startSearchMode(listener)"))
    open fun showSearchMode() {
        mSearchModeListener?.let{ startSearchMode(it)}
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

    private fun ensureSearchModeViews(){
        if (mSearchToolbar == null){
            mSearchToolbar = mCollapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_search).inflate() as Toolbar
            mSearchView = mSearchToolbar!!.findViewById(R.id.toolbarlayout_search_view)
            mSearchView.seslSetUpButtonVisibility(VISIBLE)
            mSearchView.seslSetOnUpButtonClickListener { dismissSearchMode() }
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
    @Deprecated("Set it as parameter when calling startSearchMode().",
        ReplaceWith("startSearchMode(listener)"))
    fun setSearchModeListener(listener: SearchModeListener?) {
        mSearchModeListener = listener
    }

    /**
     * Forward the voice input result to the Toolbar.
     * TODO: link to the wiki on how to use the voice input feature.
     */
    fun onSearchModeVoiceInputResult(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            mSearchView.setQuery(intent.getStringExtra(SearchManager.QUERY), true)
        }
    }

    @Deprecated("This is now a no op.")
    fun setActionModeToolbarShowAlwaysMax(max: Int) {
        //no op
    }

    @Deprecated("Use the `ActionModeListener` param when calling StartActionMode() instead.")
    fun setOnActionModeListener(callback: ActionModeCallback?) {
        mActionModeCallback = callback
    }

    //
    // Action Mode methods
    //
    private var updateAllSelectorJob: Job? = null

    /**
     * Starts an Action Mode session. This shows the Toolbar's ActionMode with a toggleable 'All' Checkbox
     * and a counter ('x selected') that temporarily replaces the Toolbar's title.
     *
     * @param listener The [ActionModeListener] to be invoke for this action mode.
     * @param keepSearchMode (Optional) Set to `true` to keep active search mode and
     * restore it's interface when this ActionMode is ended. This is set `false` by default.
     *
     * @see [endActionMode]
     */
    @JvmOverloads
    open fun startActionMode(listener: ActionModeListener,
                             keepSearchMode: Boolean = false,
                             allSelectorStateFlow: StateFlow<AllSelectorState>? = null){
        Log.d(TAG, "startActionMode")
        isActionMode = true
        updateAllSelectorJob?.cancel()
        ensureActionModeViews()
        mActionModeListener = listener
        if (isSearchMode) {
            if (keepSearchMode) {
                animatedVisibility(mSearchToolbar!!, GONE)
            }else{
                endSearchMode()
            }
        }
        animatedVisibility(mMainToolbar, GONE)
        animatedVisibility(mActionModeToolbar!!, VISIBLE)
        mFooterContainer!!.visibility = GONE
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
        updateObpCallbackState()
    }


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
            mCollapsingToolbarLayout.title = resources.getString(appcompatR.string.sesl_searchview_description_search)
            mCollapsingToolbarLayout.seslSetSubtitle(null)
        }else{
            animatedVisibility(mMainToolbar, VISIBLE)
            mFooterContainer!!.visibility = VISIBLE
            setTitle(mTitleExpanded, mTitleCollapsed)
            mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
            mMainToolbar.subtitle = mSubtitleCollapsed
        }
        mBottomActionModeBar.visibility = GONE
        mActionModeListener!!.onEndActionMode()
        mMenuSynchronizer!!.clear()
        mAppBarLayout.removeOnOffsetChangedListener(mActionModeTitleFadeListener)
        mActionModeSelectAll.setOnClickListener(null)
        setActionModeMenuListenerInternal(null)
        mActionModeTitleFadeListener = null
        mActionModeListener = null
        mMenuSynchronizer = null
        updateAllSelectorJob = null
        updateObpCallbackState()
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
        }else{
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
                mMenuSynchronizer!!.updateState(State.PORTRAIT)
            } else {
                mMenuSynchronizer!!.updateState(State.LANDSCAPE)
            }
        } else {
            mMenuSynchronizer!!.updateState(State.HIDDEN)
        }
    }

    private fun ensureActionModeViews(){
        if (mActionModeToolbar == null){
            mActionModeToolbar = (mCollapsingToolbarLayout.findViewById<ViewStub>(R.id.viewstub_oui_view_toolbar_action_mode)
                .inflate() as Toolbar).also {
                mActionModeSelectAll = it.findViewById(R.id.toolbarlayout_selectall)
                mActionModeTitleTextView = it.findViewById(R.id.toolbar_layout_action_mode_title)
            }
            mActionModeCheckBox = mActionModeSelectAll.findViewById(R.id.toolbarlayout_selectall_checkbox)
            mActionModeSelectAll.setOnClickListener { mActionModeCheckBox.setChecked(!mActionModeCheckBox.isChecked) }
            mBottomActionModeBar = findViewById<ViewStub>(R.id.viewstub_tbl_actionmode_bottom_menu).inflate() as BottomNavigationView
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
    @Deprecated("Use the ActionModeListener#onInflateActionMenu() callback when calling StartActionMode() instead.",
        ReplaceWith(""))
    fun setActionModeBottomMenu(@MenuRes menuRes: Int) {
        setActionModeMenu(menuRes)
    }

    /**
     * Set the menu resource for the ActionMode's [BottomNavigationView].
     * On landscape orientation where ActionMode's [BottomNavigationView] will be hidden,
     * the visible items from this menu resource we be shown to ActionMode's [Toolbar] [Menu]
     */
    @Deprecated("Use the ActionModeListener#onInflateActionMenu() callback when calling StartActionMode() instead.",
        ReplaceWith(""))
    fun setActionModeMenu(@MenuRes menuRes: Int) {
        mActionModeMenuRes = menuRes
    }

    @Deprecated("Access this with ActionModeListener#onInflateActionMenu() callback when calling StartActionMode() instead.",
        ReplaceWith(""))
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
    @Deprecated("Use the ActionModeListener#onMenuItemClicked() callback when calling StartActionMode() instead.",
        ReplaceWith(""))
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

    @Deprecated("Use the ActionModeListener#onInflateActionMenu() callback when calling StartActionMode() instead.")
    val actionModeToolbarMenu: Menu
        /**
         * Returns the [Menu] of the ActionMode's [Toolbar].
         *
         */
        get(){
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
        if (updateAllSelectorJob != null){
            Log.w(TAG, "'updateAllSelector' ignored because `startActionMode` " +
                    " is provided as param for  startActionMode().")
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
    @Deprecated("Use updateAllSelector() instead.", ReplaceWith("updateAllSelector(count, enabled, checked)"))
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
    @Deprecated("Use setActionModeAllSelector() instead.",
        ReplaceWith("setActionModeAllSelector(count, enabled, checked)"))
    fun setActionModeCount(count: Int, total: Int) {
        setActionModeAllSelector(count, true, count == total)
    }

    /**
     * Set the listener for the 'All' Checkbox of the ActionMode.
     */
    @Deprecated("Use ActionModeListener#onSelectAll() callback when calling StartActionMode() instead.",
        ReplaceWith(""))
    fun setActionModeCheckboxListener(listener: CompoundButton.OnCheckedChangeListener?) {
        mOnSelectAllListener = listener
    }

    //
    // others
    //
    protected fun handleInsets(): Boolean {
        return mHandleInsets
    }

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

    class ToolbarLayoutParams(context: Context, attrs: AttributeSet?) : LayoutParams(context, attrs) {
        val layoutLocation =  attrs?.let {at ->
            context.obtainStyledAttributes(at, R.styleable.ToolbarLayoutParams).use {
                it.getInteger(R.styleable.ToolbarLayoutParams_layout_location, 0)
            }
        } ?: 0
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
            .setInterpolator(PathInterpolator(0.33f, 0.0f, 0.1f, 1.0f))
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
                    mActionModeTitleTextView.alpha =  (150.0f / alphaRange
                            * (layoutPosition - toolbarTitleAlphaStart) / 255f).coerceIn(0f, 1f)
                }
            }
        }
    }

    /**
     * Select instance of either  [Dot][Badge.Dot], [Numeric][Badge.Numeric] or [None][Badge.None]
     */
    abstract class Badge private constructor() {
        class Dot : Badge()
        class Numeric(val count: Int) : Badge()
        class None : Badge()
    }

    companion object {
        private const val TAG = "ToolbarLayout"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal const val AMT_GROUP_MENU_ID: Int = 9999
        private const val MAIN_CONTENT = 0
        private const val APPBAR_HEADER = 1
        private const val FOOTER = 2
        private const val ROOT = 3

    }
}


////////////////////////////////////////////////////////////////
//         Kotlin consumables
////////////////////////////////////////////////////////////////

/**
 * Type-safe way to set badge. Select either [Badge.NUMERIC], [Badge.DOT] or [Badge.NONE]
 */
sealed class Badge{
    /**
     * @param count Set to any positive integer up to [BADGE_LIMIT_NUMBER].
     * Values <= 0 will be ignored.
     */
    data class NUMERIC(@IntRange(from = 1, to = BADGE_LIMIT_NUMBER.toLong())
                       @JvmField val count: Int): Badge()
    data object DOT: Badge()
    data object NONE: Badge()

    fun toBadgeText(): String? =
        when(this){
            is NUMERIC -> count.badgeCountToText()
            DOT -> ""
            NONE -> null
        }

}

inline fun <T:ToolbarLayout>T.setNavigationBadge(badge: Badge){
    setNavigationButtonBadge(
        when (badge) {
            is Badge.NUMERIC -> ToolbarLayout.Badge.Numeric(badge.count)
            is Badge.DOT -> ToolbarLayout.Badge.Dot()
            is Badge.NONE -> ToolbarLayout.Badge.None()
        }
    )
}

inline fun <T:ToolbarLayout>T.setMenuItemBadge(menuItem: SeslMenuItem, badge: Badge){
    setMenuItemBadge(
        menuItem,
        when (badge) {
            is Badge.NUMERIC -> ToolbarLayout.Badge.Numeric(badge.count)
            is Badge.DOT -> ToolbarLayout.Badge.Dot()
            is Badge.NONE -> ToolbarLayout.Badge.None()
        }
    )
}