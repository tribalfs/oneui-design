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
import androidx.activity.OnBackPressedCallback
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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneui.utils.BADGE_LIMIT_NUMBER
import dev.oneuiproject.oneui.utils.badgeCountToText
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils
import dev.oneuiproject.oneui.view.internal.NavigationBadgeIcon
import kotlin.math.abs
import kotlin.math.max
import androidx.appcompat.R as appcompatR

/**
 * Custom collapsing Appbar like in any App from Samsung. Includes a [SearchView] and Samsung's ActionMode.
 */
open class ToolbarLayout @JvmOverloads constructor(
    @JvmField protected var context: Context,
    attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface ActionModeCallback {
        fun onShow(toolbarLayout: ToolbarLayout?)
        fun onDismiss(toolbarLayout: ToolbarLayout?)
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


    private var mAMTMenuShowAlwaysMax = 2
    private var switchActionModeMenu = false
    private var mSelectedItemsCount = 0

    private var mActionModeCallback: ActionModeCallback? = null

    private val mOnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (isSearchMode) dismissSearchMode()
            if (isActionMode) dismissActionMode()
        }
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
     * @see .showSearchMode
     * @see .dismissSearchMode
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
        updateActionModeMenuVisibility(newConfig)
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

        if (!isInEditMode) {
            this.activity!!.apply {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                onBackPressedDispatcher.addCallback(mOnBackPressedCallback)
            }
        }

        refreshLayout(resources.configuration)
    }

    private fun refreshLayout(newConfig: Configuration) {
        removeCallbacks(sideMarginUpdater)
        postDelayed(sideMarginUpdater, 40)

        if (!isInEditMode) ToolbarLayoutUtils
            .hideStatusBarForLandscape(this.activity!!, newConfig.orientation)

        val isLandscape = newConfig.orientation == ORIENTATION_LANDSCAPE

        isExpanded = !isLandscape and mExpanded

        if (mNavigationBadgeIcon != null) {
            val badgeIcon = mNavigationBadgeIcon!!
                .getDrawable(1) as NavigationBadgeIcon
        }
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
     * Show the [SearchView] in the Toolbar.
     * To enable the voice input icon in the SearchView, please refer to the project wiki.
     * TODO: link to the wiki on how to use the voice input feature.
     */
    open fun showSearchMode() {
        isSearchMode = true
        if (isActionMode) dismissActionMode()
        ensureSearchModeViews()
        mOnBackPressedCallback.isEnabled = true
        animatedVisibility(mMainToolbar, GONE)
        animatedVisibility(mSearchToolbar!!, VISIBLE)
        mFooterContainer!!.visibility = GONE

        mCollapsingToolbarLayout.title = resources.getString(appcompatR.string.sesl_searchview_description_search)
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        setExpanded(expanded = false, animate = true)

        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return if (mSearchModeListener != null){
                    mSearchModeListener!!.onQueryTextSubmit(query)
                } else false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return if (mSearchModeListener != null){
                    mSearchModeListener!!.onQueryTextSubmit(newText)
                } else false
            }
        })
        mSearchView.isIconified = false

        if (mSearchModeListener != null) mSearchModeListener!!.onSearchModeToggle(mSearchView, true)
    }

    /**
     * Dismiss the [SearchView] in the Toolbar.
     *
     * @see .showSearchMode
     */
    open fun dismissSearchMode() {
        if (mSearchModeListener != null) mSearchModeListener!!.onSearchModeToggle(
            mSearchView,
            false
        )
        isSearchMode = false
        mOnBackPressedCallback.isEnabled = false
        mSearchView.setQuery("", false)
        animatedVisibility(mSearchToolbar!!, GONE)
        animatedVisibility(mMainToolbar, VISIBLE)
        mFooterContainer!!.visibility = VISIBLE

        setTitle(mTitleExpanded, mTitleCollapsed)
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
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


    fun setActionModeToolbarShowAlwaysMax(max: Int) {
        mAMTMenuShowAlwaysMax = max
    }


    fun setOnActionModeListener(callback: ActionModeCallback?) {
        mActionModeCallback = callback
    }


    //
    // Action Mode methods
    //
    /**
     * Show the Toolbar's ActionMode. This will show a 'All' Checkbox instead of the navigation button,
     * temporarily replace the Toolbar's title with a counter ('x selected')
     * and show a [BottomNavigationView] in the footer.
     * The ActionMode is useful when the user can select items in a list.
     *
     * @see .setActionModeCount
     * @see .setActionModeCheckboxListener
     * @see .setActionModeMenu
     * @see .setActionModeMenuListener
     * @see .setActionModeToolbarMenu
     * @see .setActionModeToolbarMenuListener
     * @see .setActionModeBottomMenu
     * @see .setActionModeBottomMenuListener
     */
    open fun showActionMode() {
        ensureActionModeViews()
        isActionMode = true
        if (isSearchMode) dismissSearchMode()
        mOnBackPressedCallback.isEnabled = true
        animatedVisibility(mMainToolbar, GONE)
        animatedVisibility(mActionModeToolbar!!, VISIBLE)
        mFooterContainer!!.visibility = GONE
        mBottomActionModeBar!!.visibility = VISIBLE

        // setActionModeCount(0, -1);
        mAppBarLayout.addOnOffsetChangedListener(
            AppBarOffsetListener().also {
                mActionModeTitleFadeListener = it
            }
        )
        mCollapsingToolbarLayout.seslSetSubtitle(null)
        mMainToolbar.setSubtitle(null)

        updateActionModeMenuVisibility(context.resources.configuration)

        if (mActionModeCallback != null) {
            mActionModeCallback!!.onShow(this)
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

    private fun updateActionModeMenuVisibility(config: Configuration) {
        if (isActionMode) {
            if (mSelectedItemsCount > 0) {
                if (switchActionModeMenu && config.orientation == ORIENTATION_LANDSCAPE) {
                    mBottomActionModeBar.visibility = GONE
                    mActionModeToolbar!!.menu.setGroupVisible(AMT_GROUP_MENU_ID, true)
                } else {
                    mBottomActionModeBar.visibility = VISIBLE
                    mActionModeToolbar!!.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
                }
            } else {
                mBottomActionModeBar.visibility = GONE
                mActionModeToolbar!!.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
            }
        }
    }


    /**
     * Dismiss the ActionMode.
     *
     * @see .showActionMode
     */
    open fun dismissActionMode() {
        if (!isActionMode) return
        mOnBackPressedCallback.isEnabled = false
        animatedVisibility(mActionModeToolbar!!, GONE)
        animatedVisibility(mMainToolbar, VISIBLE)
        mFooterContainer!!.visibility = VISIBLE
        mBottomActionModeBar!!.visibility = GONE
        setTitle(mTitleExpanded, mTitleCollapsed)
        mAppBarLayout.removeOnOffsetChangedListener(mActionModeTitleFadeListener)
        mCollapsingToolbarLayout.seslSetSubtitle(mSubtitleExpanded)
        mMainToolbar.subtitle = mSubtitleCollapsed
        isActionMode = false
        setActionModeAllSelector(0, enabled = true, checked = false)
        mActionModeTitleFadeListener = null
        mActionModeCallback?.onDismiss(this)
    }

    /**
     * Set the menu resource for the ActionMode's [BottomNavigationView]
     */
    @Deprecated("Use setActionModeMenu() instead.",
        ReplaceWith("setActionModeMenu(menuRes)"))
    fun setActionModeBottomMenu(@MenuRes menuRes: Int) {
        mBottomActionModeBar!!.inflateMenu(menuRes)
    }

    /**
     * Set the menu resource for the ActionMode's [BottomNavigationView].
     * On landscape orientation where ActionMode's [BottomNavigationView] will be hidden,
     * the visible items from this menu resource we be shown to ActionMode's [Toolbar] [Menu]
     */
    fun setActionModeMenu(@MenuRes menuRes: Int) {
        ensureActionModeViews()
        actionModeBottomMenu.clear()
        actionModeToolbarMenu.removeGroup(AMT_GROUP_MENU_ID)
        mBottomActionModeBar!!.inflateMenu(menuRes)
        val amToolbarMenu = mActionModeToolbar!!.menu.apply { removeGroup(AMT_GROUP_MENU_ID) }
        val amBottomMenu = mBottomActionModeBar!!.menu
        val size = amBottomMenu.size()
        var menuItemsAdded = 0
        for (a in 0 until size) {
            val ambMenuItem = amBottomMenu.getItem(a)
            if (ambMenuItem.isVisible) {
                menuItemsAdded++
                val amtMenuItem = amToolbarMenu.add(
                    AMT_GROUP_MENU_ID,
                    ambMenuItem.itemId,
                    Menu.NONE,
                    ambMenuItem.title
                )
                if (menuItemsAdded <= mAMTMenuShowAlwaysMax) {
                    amtMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }
        }
        switchActionModeMenu = true
    }

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
    fun setActionModeMenuListener(listener: NavigationBarView.OnItemSelectedListener) {
        mBottomActionModeBar!!.setOnItemSelectedListener(listener)
        mActionModeToolbar!!.setOnMenuItemClickListener { item: MenuItem ->
            listener.onNavigationItemSelected(
                mActionModeToolbar!!.menu.findItem(item.itemId)
            )
        }
    }

    /**
     * Set the menu resource for the ActionMode's [Toolbar].
     */
    fun setActionModeToolbarMenu(@MenuRes menuRes: Int) {
        ensureActionModeViews()
        mActionModeToolbar!!.inflateMenu(menuRes)
    }

    /**
     * Set the listener for the ActionMode's [Toolbar].
     */
    fun setActionModeToolbarMenuListener(listener: Toolbar.OnMenuItemClickListener?) {
        ensureActionModeViews()
        mActionModeToolbar!!.setOnMenuItemClickListener(listener)
    }

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
     * Set the ActionMode's count and Select all checkBox's enabled state and check state
     *
     * @param count number of selected items in the list
     * @param enabled enable or disable click
     * @param checked
     */
    fun setActionModeAllSelector(count: Int, enabled: Boolean, checked: Boolean?) {
        if (mSelectedItemsCount != count) {
            mSelectedItemsCount = count
            val title = if (count > 0) {
                resources.getString(R.string.oui_action_mode_n_selected, count)
            } else {
                resources.getString(R.string.oui_action_mode_select_items)
            }
            mCollapsingToolbarLayout.title = title
            mActionModeTitleTextView.text = title
            updateActionModeMenuVisibility(context.resources.configuration)
        }
        if (checked != null && checked != mActionModeCheckBox.isChecked) {
            mActionModeCheckBox.isChecked = checked
        }
        if (enabled != mActionModeSelectAll.isEnabled) {
            mActionModeSelectAll.isEnabled = enabled
        }
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
        mSelectedItemsCount = count
        val title = if (count > 0)
            resources.getString(R.string.oui_action_mode_n_selected, count)
        else
            resources.getString(R.string.oui_action_mode_select_items)

        mCollapsingToolbarLayout.title = title
        mActionModeTitleTextView!!.text = title
        updateActionModeMenuVisibility(context.resources.configuration)
        mActionModeCheckBox.isChecked = count == total
    }

    /**
     * Set the listener for the 'All' Checkbox of the ActionMode.
     */
    fun setActionModeCheckboxListener(listener: CompoundButton.OnCheckedChangeListener?) {
        mActionModeCheckBox.setOnCheckedChangeListener(listener)
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