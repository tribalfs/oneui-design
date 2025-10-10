@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.PointerIcon
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AnimationUtils
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.content.withStyledAttributes
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.customview.view.AbsSavedState
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.GridMenuDialog
import dev.oneuiproject.oneui.dialog.internal.toGridDialogItem
import dev.oneuiproject.oneui.ktx.addCustomTab
import dev.oneuiproject.oneui.ktx.addTab
import dev.oneuiproject.oneui.ktx.doOnEnd
import dev.oneuiproject.oneui.ktx.getTabView
import dev.oneuiproject.oneui.ktx.isNumericValue
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.ktx.setListener
import dev.oneuiproject.oneui.ktx.tabViewGroup
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isDisplayTypeSub
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isLandscape
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_90
import com.google.android.material.R as materialR

/**
 * A widget that displays a row of tabs with an optional overflow menu for additional items.
 *
 * It is designed to be used as a bottom navigation bar, providing quick access to different
 * sections or actions within an application.
 *
 * Features:
 * - Supports up to 4 visible tabs, with additional items accessible through an overflow menu.
 * - Customizable appearance with attributes for minimum side margin and menu resource.
 * - Integrates with [GridMenuDialog] for displaying overflow items in a grid layout.
 * - Supports visibility animations (slide up/down) .
 * - Supports navigation integration via [setupNavigation][dev.oneuiproject.oneui.navigation.setupNavigation]
 * for navigation handling when used in conjunction with a [ToolbarLayout][dev.oneuiproject.oneui.layout.ToolbarLayout].
 *
 * ## Example usage:
 * ```xml
 * <dev.oneuiproject.oneui.widget.BottomTabLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:layout_location="footer"
 *     app:menu="@menu/menu_bottom_tabs"/>
 * ```
 *
 * @param context The Context the view is running in, through which it can access the
 * current theme, resources, etc.
 * @param attrs (Optional) The attributes of the XML tag that is inflating the view.
 */
@SuppressLint("NewApi", "RestrictedApi")
class BottomTabLayout(
    context: Context,
    attrs: AttributeSet?
) : MarginsTabLayout(context, attrs), TabLayout.OnTabSelectedListener {

    private var minSideMargin = 0f
    private val slideDownAnim by lazy(LazyThreadSafetyMode.NONE) {
        AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_down).apply {
            startOffset = 100L
            interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_90)
            setListener(
                onStart = {this@BottomTabLayout.visibility = VISIBLE},
                onEnd = {
                    this@BottomTabLayout.visibility = GONE
                    transientState = TRANSIENT_NONE
                },
            )
        }
    }

    private val slideUpAnim by lazy(LazyThreadSafetyMode.NONE) {
        AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_up).apply {
            startOffset = 240
            interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_90)
            doOnEnd { transientState = TRANSIENT_NONE }
        }
    }

    private var transientState = TRANSIENT_NONE
    private var previousContainerWidth = -1

    private var gridMenuDialog: GridMenuDialog? = null
    private var itemClickedListener: MenuItem.OnMenuItemClickListener? = null

    private var isPopulatingTabs = false
    private var overFLowItems: ArrayList<MenuItemImpl>? = null
    private var selectedItemId: Int = -1
    private var defaultTabPadding = context.resources.getDimension(R.dimen.oui_des_tab_layout_default_tab_padding)

    //Keep internal for consistency and prevent leaks
    internal val bottomTabLayoutMenu by lazy { BottomTabLayoutMenu(context){ populateItems() } }

    /**
     * A custom [MenuBuilder] for the [BottomTabLayout].
     *
     * This class extends [MenuBuilder] to provide specific functionality for managing menu items
     * within the [BottomTabLayout].
     *
     * @param context The context in which the menu is operating.
     * @param onItemsChanged A callback function to be invoked when the menu items change.
     * The `structureChanged` parameter indicates whether the structure
     * of the menu (e.g., number of items, order) has changed.
     *
     * @see setBadge
     */
    internal class BottomTabLayoutMenu(
        context: Context,
        private var onItemsChanged: ((structureChanged: Boolean) -> Unit)? = null
    ): MenuBuilder(context){

        @JvmField
        internal var suspendUpdate = false

        override fun onItemsChanged(structureChanged: Boolean) {
            super.onItemsChanged(structureChanged)
            if (!suspendUpdate) {
                onItemsChanged?.invoke(structureChanged)
            }
        }
    }

    init {
        isFocusable = false
        tabMode = MODE_SCROLLABLE
        tabGravity = GRAVITY_START
        context.withStyledAttributes(attrs, R.styleable.BottomTabLayout, 0, 0) {
            minSideMargin = getDimension(R.styleable.BottomTabLayout_minSideMargin, 0f)
            configureTabDimen()
            if (hasValue(R.styleable.BottomTabLayout_menu)) {
                inflateMenu((getResourceId(R.styleable.BottomTabLayout_menu, 0)))
            }
        }
        addOnTabSelectedListener(this)
    }

    private fun configureTabDimen() {
        super.setCustomTabDimen {tabLayout, containerWidthPixels, totalTabTextsWidth ->
            val res = tabLayout.resources
            val configuration = res.configuration

            when {
                isLandscape(configuration) && isDisplayTypeSub(configuration) -> minSideMargin / 2f
                configuration.smallestScreenWidthDp <= 480 -> minSideMargin
                else -> {
                    val tabLayoutPaddingMax = maxOf(containerWidthPixels * 0.125f, minSideMargin)
                    val totalTabPaddings = defaultTabPadding * tabLayout.tabCount * 2
                    ((containerWidthPixels - totalTabTextsWidth - totalTabPaddings) / 2f)
                        .coerceIn(minSideMargin, tabLayoutPaddingMax)
                }
            }
        }
    }

    override fun calculateMargins(){
        if (isPopulatingTabs) return
        if (recalculateTextWidths) {
            updatePointerAndDescription()
        }
        super.calculateMargins()
    }

    private fun updatePointerAndDescription() {
        val systemIcon = if (SDK_INT >= 24) {
            PointerIcon.getSystemIcon(context, 1000)
        } else null

        val tabCount = tabCount
        for (i in 0 until tabCount) {
            val tab = getTabAt(i)
            val viewGroup = getTabView(i) as? ViewGroup
            if (tab != null && viewGroup != null) {
                val textView = tab.seslGetTextView()
                viewGroup.contentDescription = textView?.text
                systemIcon?.let {
                    @SuppressLint("NewApi")
                    viewGroup.pointerIcon = it
                }
            }
        }
    }

    override fun updateLayoutParams() {
        if (sideMarginChanged) {
            sideMarginChanged = false
            layoutParams = (layoutParams as MarginLayoutParams).apply {
                marginStart = sideMargin
                marginEnd = sideMargin
            }
        }

        if (containerWidth == 0 || previousContainerWidth == containerWidth) return
        previousContainerWidth = containerWidth
        val availableForTabPaddings = containerWidth - tabTextWidthsList.sum() - (sideMargin * 2)
        val tabPadding = (availableForTabPaddings/(tabCount * 2)).coerceAtLeast(defaultTabPadding)

        tabViewGroup?.let {
            for (i in 0 until tabCount) {
                val tabWidth =  (tabPadding * 2f + tabTextWidthsList[i]).toInt()
                it.getChildAt(i)?.apply {
                    minimumWidth = tabWidth
                    (this as? ViewGroup)?.getChildAt(0)?.minimumWidth = tabWidth
                }
            }
        }
        post(requestLayoutRunnable)
    }

    /**
     * Inflates a menu resource into this BottomTabLayout.
     *
     * This method clears any existing tabs and populates the layout with items from the specified menu resource.
     * It also sets up an optional listener for menu item clicks.
     *
     * @param menuResId The resource ID of the menu to inflate.
     * @param onMenuItemClicked An optional listener to be notified when a menu item is clicked.
     */
    fun inflateMenu(
        @MenuRes menuResId: Int,
        onMenuItemClicked: MenuItem.OnMenuItemClickListener? = null
    ){
        this.itemClickedListener = onMenuItemClicked

        bottomTabLayoutMenu.apply {
            suspendUpdate = true
            SupportMenuInflater(context).inflate(menuResId, this)
            suspendUpdate = false
        }
        populateItems()
    }

    private fun populateItems() {
        isPopulatingTabs = true
        removeAllTabs()

        val tabItems = ArrayList<MenuItemImpl>()
        val overflowItems = ArrayList<MenuItemImpl>()

        var tabItemsAdded = 0
        val menuSize = bottomTabLayoutMenu.size()
        for (i in 0 until menuSize) {
            val menuItem = bottomTabLayoutMenu.getItem(i) as MenuItemImpl
            if (menuItem.isVisible) {
                if (tabItemsAdded < 3 || menuSize == 4) {
                    tabItems.add(menuItem)
                    tabItemsAdded++
                }else{
                    overflowItems.add(menuItem)
                }
            }
        }
        this.overFLowItems = overflowItems

        gridMenuDialog?.setAnchor(null)

        populateTabs(tabItems)

        isPopulatingTabs = false

        if (selectedItemId != -1) {
            applySelectedItem()
        }

        if (isAttachedToWindow) {
            sideMargin = 0
            previousContainerWidth = -1
            calculateMargins()
        }

        gridMenuDialog?.apply {
            if (!isShowing)  return
            if (overflowItems.isNotEmpty()) {
                updateItems(overflowItems.map { it.toGridDialogItem() })
                setAnchor(getTabView(3))
            } else {
                dismiss()
            }
        }
    }

    @SuppressLint("PrivateResource")
    private fun populateTabs(
        bottomMenuItems: ArrayList<MenuItemImpl>
    ) {
        var showMoreText = true
        for (menuItem in bottomMenuItems) {
            addTabForMenu(menuItem)
            if (menuItem.icon == null){
                showMoreText = false
            }
        }

        if (hasOverflowItems()) {
            addCustomTab(
                tabTitleRes = if (showMoreText) materialR.string.sesl_more_item_label else null,
                tabIconRes = R.drawable.oui_des_ic_ab_drawer,
                listener = { createAndShowGridDialog() }
            ).apply{
                tabIconTint = getTabTextColors()
                id = R.id.bottom_tab_menu_show_grid_dialog
                setBadge(3, if (hasBadgeOnOverFlow()) "" else null)
                setTabContentDescription(this, view)
            }

            gridMenuDialog?.apply {
                if (isShowing) {
                    updateItems(overFLowItems!!.map { it.toGridDialogItem()})
                }
            }
        }
        recalculateTextWidths = true
    }

    private fun addTabForMenu(menuItem: MenuItemImpl) {
        addTab(
            tabTitle = menuItem.title,
            tabIcon = menuItem.icon
        ).apply {
            id = menuItem.itemId
            isEnabled = menuItem.isEnabled
            setBadge(position, menuItem.badgeText)
            setTabContentDescription(this, view)
        }
    }

    private fun TabLayout.setBadge(position: Int, badgeText: String?) {
        badgeText?.let {
            if (it.isNumericValue()) {
                seslShowBadge(position, true, it)
            } else {
                seslShowDotBadge(position, true)
            }
        }
    }

    private fun createAndShowGridDialog(){
        gridMenuDialog = createGridMenuDialog(overFLowItems!!).apply {
            if (findTab(selectedItemId) == null) {
                setSelectedItem(selectedItemId)
            }
            show()
        }
    }

    private fun hasOverflowItems(): Boolean = overFLowItems?.isNotEmpty() == true

    private fun createGridMenuDialog(menu: ArrayList<MenuItemImpl>) =
        GridMenuDialog(context).apply {
            create()
            updateItems(menu.map { it.toGridDialogItem() })
            setOnItemClickListener{item: GridMenuDialog.GridItem ->
                itemClickedListener?.apply {
                    bottomTabLayoutMenu.findItem(item.itemId)?.let {
                        onMenuItemClick(it)
                    }
                }
                true
            }
            setAnchor(getTabView(3)!!)
        }

    private val reshowDialogRunnable = Runnable { if (isAttachedToWindow) createAndShowGridDialog() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        when (transientState){
            ABOUT_TO_HIDE -> isGone = true
            ABOUT_TO_SHOW -> {
                isVisible = false
                doSlideUpAnimation()
            }
        }
        transientState = TRANSIENT_NONE
        //Block touch on parent
        @Suppress("ClickableViewAccessibility")
        (parent as View).setOnTouchListener { _, _ -> true }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (parent as View).setOnTouchListener(null)
        //Dismiss to prevent activity window from leaking
        gridMenuDialog?.dismiss()
    }

    private val dialogUpdateRunnable = Runnable {
        gridMenuDialog?.apply {
            updateDialog()
            window!!.decorView.alpha = 1f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGridDialogSizeAndHeight()
    }

    private fun updateGridDialogSizeAndHeight(){
        gridMenuDialog?.apply {
            if (isShowing) updateDialog()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        defaultTabPadding = context.resources.getDimension(R.dimen.oui_des_tab_layout_default_tab_padding)
        super.onConfigurationChanged(newConfig)
        updateGridDialogSizeAndHeight()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.isGridDialogShowing = gridMenuDialog?.isShowing == true
        state.selectedItemId = if (selectedItemId != -1) {
            selectedItemId
        } else getTabAt(selectedTabPosition)?.id ?: -1
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.selectedItemId != -1 && selectedItemId == -1) {
            setSelectedItem(state.selectedItemId)
        }
        if (state.isGridDialogShowing) {
            doOnLayout { createAndShowGridDialog() }
        }
    }

    private fun clearBadge() {
        val tabCount = tabCount
        for (i in 0 until tabCount) {
            seslShowDotBadge(i, false)
            seslShowBadge(i, false, "")
        }
    }

    private fun doSlideDownAnimation() {
        if (isGone) return
        transientState = ABOUT_TO_HIDE
        startAnimation(slideDownAnim)
    }

    private fun doSlideUpAnimation() {
        if (isVisible) return
        transientState = ABOUT_TO_SHOW
        visibility = VISIBLE
        startAnimation(slideUpAnim)
    }

    private fun getTabCountWithoutOverflow() = if (hasOverflowItems()) tabCount - 1 else tabCount
    private fun hasBadgeOnOverFlow() = overFLowItems?.any { it.badgeText != null } == true
    private fun isOverflowMenuTab(tab: Tab) = tab.id == R.id.bottom_tab_menu_show_grid_dialog

    private fun setTabContentDescription(tab: Tab?, view: View?) {
        if (tab == null || view == null) return

        if (isOverflowMenuTab(tab)) {
            view.setContentDescription(
                @Suppress("PrivateResource")
                context.getString(materialR.string.sesl_more_item_label))
        }else{
            view.apply {
                setContentDescription(if (this is TabView) null else tab.contentDescription)
                accessibilityDelegate = object : AccessibilityDelegate() {
                    override fun onInitializeAccessibilityEvent(
                        view: View,
                        accessibilityEvent: AccessibilityEvent
                    ) {
                        when(accessibilityEvent.eventType){
                            TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
                                this@BottomTabLayout.isFocusable = true
                            TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED ->
                                this@BottomTabLayout.isFocusable = false
                            else -> Unit
                        }
                        super.onInitializeAccessibilityEvent(view, accessibilityEvent)
                    }
                }
            }
        }
    }

    /**
     * Applies a show or hide animation to the BottomTabLayout.
     *
     * @param show `true` to show the BottomTabLayout, `false` to hide it.
     * @see show
     * @see hide
     */
    inline fun applyAnimation(show: Boolean) = if (show) show() else hide()

    /**
     * Shows the BottomTabLayout.
     *
     * @param animate `true` to animate the appearance (slide up), `false` to show it immediately.
     * Defaults to `true`.
     */
    @JvmOverloads
    fun show(animate: Boolean = true) {
        if (animate) {
            doSlideUpAnimation()
        }else{
            isVisible = true
        }
    }

    /**
     * Hides the BottomTabLayout.
     *
     * @param animate True to animate the transition, false to hide immediately.
     * Defaults to true.
     */
    @JvmOverloads
    fun hide(animate: Boolean = true) {
        if (animate) {
            doSlideDownAnimation()
        }else{
            isVisible = false
        }
    }


    /**
     * Sets whether this [BottomTabLayout] should block focus for its descendants.
     *
     * When focus is blocked, this view group will prevent its descendant views from receiving focus.
     * This can be useful in scenarios where you want to temporarily disable interaction with the
     * tabs.
     *
     * @param block `true` to block descendant focus, `false` to allow descendant focus.
     */
    fun blockFocus(block: Boolean) {
        setDescendantFocusability(if (block) FOCUS_BLOCK_DESCENDANTS else FOCUS_AFTER_DESCENDANTS)
    }

    override fun onInitializeAccessibilityNodeInfo(nodeInfo: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(nodeInfo)
        if (SDK_INT >= Build.VERSION_CODES.R) {
            AccessibilityNodeInfo.CollectionInfo(
                1,
                getTabCountWithoutOverflow(),
                false
            )
        }else{
            @Suppress("DEPRECATION")
            AccessibilityNodeInfo.CollectionInfo.obtain(
                1,
                getTabCountWithoutOverflow(),
                false
            )
        }.let {
            nodeInfo.setCollectionInfo(it)
        }
    }

    /**
     * Refreshes the BottomTabLayout's visibility and clears any badges.
     *
     * @param show `true` to make the BottomTabLayout visible, `false` to hide it.
     */
    fun refresh(show: Boolean) {
        isVisible = show
        clearBadge()
        invalidate()
    }

    /**
     * Selects the item with the specified id
     *
     * @param itemId The id of the item to select.
     */
    fun setSelectedItem(itemId: Int) {
        if (itemId == selectedItemId) return
        selectedItemId = itemId
        if (isPopulatingTabs) return
        applySelectedItem()
    }

    private fun applySelectedItem() {
        findTab(selectedItemId)?.apply {
            select()
            gridMenuDialog?.setSelectedItem(-1)
        } ?:
        run {
            if (bottomTabLayoutMenu.findItem(selectedItemId) != null) {
                findTab(R.id.bottom_tab_menu_show_grid_dialog)?.select()
                gridMenuDialog?.setSelectedItem(selectedItemId)
            }
        }
    }

    /**
     * Sets a badge on a specific menu item.
     *
     * @param itemId The ID of the menu item to set the badge on.
     * @param badge The [Badge] to display on the menu item.
     */
    fun setItemBadge(itemId: Int, badge: Badge) = findMenuItem(itemId)?.setBadge(badge)

    /**
     * Sets the enabled state of a menu item.
     *
     * @param itemId The ID of the menu item to update.
     * @param enabled `true` to enable the item, `false` to disable it.
     */
    fun setItemEnabled(itemId: Int, enabled: Boolean){
        bottomTabLayoutMenu.findItem(itemId)?.setEnabled(enabled)
            ?: Log.w(TAG, "setItemEnabled: ${context.resources.getResourceEntryName(itemId)} item id is invalid.")
    }

    /**
     * Sets the visibility of a menu item in the BottomTabLayout.
     *
     * @param itemId The resource ID of the menu item to modify.
     * @param visible `true` to make the item visible, `false` to make it invisible.
     */
    fun setItemVisible(itemId: Int, visible: Boolean){
        bottomTabLayoutMenu.findItem(itemId)?.setVisible(visible)
            ?: Log.w(TAG, "`setItemVisible`, ${context.resources.getResourceEntryName(itemId)} item id is invalid.")
    }

    /**
     * Finds a tab by its ID.
     *
     * @param tabId The ID of the tab to find.
     * @return The [Tab] with the specified ID, or `null` if not found.
     */
    fun findTab(tabId: Int): Tab? {
        for (i in 0 until tabCount) {
            val tab = getTabAt(i)
            if (tab?.id == tabId) {
                return tab
            }
        }
        return null
    }

    override fun onTabSelected(tab: Tab) {
        if (tab.id != -1) selectedItemId = tab.id
        itemClickedListener?.apply {
            bottomTabLayoutMenu.findItem(tab.id)?.let {
                onMenuItemClick(it)
            }
        }
    }

    override fun onTabUnselected(tab: Tab) = Unit

    override fun onTabReselected(tab: Tab) = Unit

    /**
     * Sets a listener to be notified when a menu item is clicked.
     *
     * This listener will be invoked when a tab representing a menu item is selected
     * or when an item in the overflow [GridMenuDialog] is clicked.
     *
     * @param onMenuItemClickedListener The listener to set.
     */
    fun setOnMenuItemClickListener(onMenuItemClickedListener: MenuItem.OnMenuItemClickListener?){
        this.itemClickedListener = onMenuItemClickedListener
    }

    /**
     * Finds a menu item by its ID.
     *
     * @param itemId The ID of the menu item to find.
     * @return The [MenuItem] with the specified ID, or `null` if not found.
     */
    fun findMenuItem(itemId: Int): MenuItem? = bottomTabLayoutMenu.findItem(itemId)

    /**
     * Adds a new menu item to the BottomTabLayout.
     *
     * This method programmatically adds a tab to the layout with the given title.
     *
     * @param itemTitle The title of the menu item to add.
     * @return The newly created [MenuItem].
     */
    fun addMenuItem(itemTitle: String): MenuItem =  bottomTabLayoutMenu.add(itemTitle)

    /**
     * Removes a menu item from the BottomTabLayout.
     *
     * This method removes the menu item with the specified ID and then re-populates the tabs
     * to reflect the change.
     *
     * @param id The ID of the menu item to remove.
     */
    fun removeMenuItem(id: Int) { bottomTabLayoutMenu.removeItem(id) }

    private class SavedState : AbsSavedState {
        var isGridDialogShowing = false
        var selectedItemId = -1

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            isGridDialogShowing = parcel.readInt() != 0
            selectedItemId = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (isGridDialogShowing) 1 else 0)
            out.writeInt(selectedItemId)
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

    @Deprecated("This does nothing in BottomTabLayout",
        level = DeprecationLevel.HIDDEN)
    override fun setCustomTabDimen(tabDimenImpl: TabDimen) {
        //No op
    }


    @Deprecated("This does nothing in BottomTabLayout",
        level = DeprecationLevel.HIDDEN)
    override fun seslSetSubTabStyle() {
        //No op
    }

    private companion object{
        const val TAG = "BottomTabLayout"
        const val ABOUT_TO_HIDE = 1
        const val ABOUT_TO_SHOW = 2
        const val TRANSIENT_NONE = 0
    }
}