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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.content.res.use
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
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_90
import com.google.android.material.R as materialR

@SuppressLint("NewApi", "RestrictedApi")
class BottomTabLayout(
    context: Context,
    attrs: AttributeSet?
) : MarginsTabLayout(context, attrs), TabLayout.OnTabSelectedListener {

    private var minSideMargin = 0
    private var slideDownAnim: Animation? = null
    private var slideUpAnim: Animation? = null
    private var transientState = TRANSIENT_NONE
    private var previousContainerWidth = -1

    private var gridMenuDialog: GridMenuDialog? = null
    private var itemClickedListener: MenuItem.OnMenuItemClickListener? = null

    private var isPopulatingTabs = false
    private var overFLowItems: ArrayList<MenuItemImpl>? = null

    val menu by lazy { BottomTabLayoutMenu(context){ updateTabs() } }

    init {
        isFocusable = false
        tabMode = if (isInEditMode) MODE_FIXED else MODE_SCROLLABLE
        tabGravity = GRAVITY_FILL
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.BottomTabLayout, 0, 0).use { a ->
                minSideMargin = a.getDimensionPixelSize(R.styleable.BottomTabLayout_minSideMargin, 0)
                if (a.hasValue(R.styleable.BottomTabLayout_menu)) {
                    inflateMenu((a.getResourceId(R.styleable.BottomTabLayout_menu, 0)))
                }
            }
        addOnTabSelectedListener(this)
    }

    class BottomTabLayoutMenu(
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

        @Suppress("RedundantOverride")
        //To suppress restricted api lint warning on caller
        override fun findItem(id: Int): MenuItem? = super.findItem(id)

        fun setBadge(itemId: Int, badge: Badge){
            findItem(itemId)?.setBadge(badge)
                ?: Log.e(TAG, "setBadge:  ${context.resources.getResourceEntryName(itemId)} is invalid.")
        }
    }

    override fun calculateMargins(){
        if (isPopulatingTabs) return
        if (mRecalculateTextWidths) {
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
        val sideMarginFinal = sideMargin.coerceAtLeast(minSideMargin)
        if (sideMarginChanged) {
            sideMarginChanged = false
            layoutParams = (layoutParams as MarginLayoutParams).apply {
                marginStart = sideMarginFinal
                marginEnd = sideMarginFinal
            }
        }

        if (containerWidth == null || previousContainerWidth == containerWidth) return
        previousContainerWidth = containerWidth!!
        val availableForTabPaddings = containerWidth!! - tabTextWidthsList.sum() - (sideMarginFinal * 2)
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

    fun inflateMenu(
        @MenuRes menuResId: Int,
        onMenuItemClicked: MenuItem.OnMenuItemClickListener? = null
    ){
        this.itemClickedListener = onMenuItemClicked

        menu.apply {
            suspendUpdate = true
            SupportMenuInflater(context).inflate(menuResId, this)
            suspendUpdate = false
        }
        removeAllTabs()
        isPopulatingTabs = true
        updateTabs()
    }


    private fun updateTabs() {
        val tabItems = ArrayList<MenuItemImpl>()
        val overflowItems = ArrayList<MenuItemImpl>()

        var tabItemsAdded = 0
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i) as MenuItemImpl
            if (menuItem.isVisible) {
                if (tabItemsAdded < 3) {
                    tabItems.add(menuItem)
                    tabItemsAdded++
                }else{
                    overflowItems.add(menuItem)
                }
            }
        }
        this.overFLowItems = overflowItems
        populateTabs(tabItems)
    }

    @SuppressLint("PrivateResource")
    private fun populateTabs(
        mBottomMenuItems: ArrayList<MenuItemImpl>
    ) {
        isPopulatingTabs = true
        removeAllTabs()

        var showMoreText = true
        for (menuItem in mBottomMenuItems) {
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
                setBadge(if (hasBadgeOnOverFlow()) Badge.DOT else Badge.NONE)
            }

            gridMenuDialog?.apply {
                if (isShowing) {
                    updateItems(overFLowItems!!.map { it.toGridDialogItem()})
                }
            }
        }

        isPopulatingTabs = false
        mRecalculateTextWidths = true
    }

    private fun addTabForMenu(menuItem: MenuItemImpl) {
        addTab(
            tabTitle = menuItem.title,
            tabIcon = menuItem.icon
        ).apply {
            isEnabled = menuItem.isEnabled
            menuItem.badgeText?.let {
                if (it.isNumericValue()) {
                    seslShowBadge(position, true, it)
                } else {
                    seslShowDotBadge(position, true)
                }
            }
        }
    }

    private fun createAndShowGridDialog(){
        gridMenuDialog = createGridMenuDialog(overFLowItems!!)
        gridMenuDialog!!.show()
    }

    private fun hasOverflowItems(): Boolean = overFLowItems?.isNotEmpty() == true

    private fun createGridMenuDialog(menu: ArrayList<MenuItemImpl>) =
        GridMenuDialog(context).apply {
            create()
            updateItems(menu.map { it.toGridDialogItem() })
            setOnItemClickListener{item: GridMenuDialog.GridItem ->
                itemClickedListener?.apply {
                    this@BottomTabLayout.menu.findItem(item.itemId)?.let {
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
        super.onConfigurationChanged(newConfig)
        updateGridDialogSizeAndHeight()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.isGridDialogShowing = gridMenuDialog?.isShowing == true
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.isGridDialogShowing) {
            createAndShowGridDialog()
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
        if (slideDownAnim == null) {
            slideDownAnim = AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_down).apply {
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
        startAnimation(slideDownAnim)
    }

    private fun doSlideUpAnimation() {
        if (isVisible) return
        transientState = ABOUT_TO_SHOW
        if (slideUpAnim == null) {
            slideUpAnim = AnimationUtils.loadAnimation(context, R.anim.oui_des_bottom_tab_slide_up).apply {
                startOffset = 240
                interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_90)
                doOnEnd { transientState = TRANSIENT_NONE }
            }
        }
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

    inline fun applyAnimation(show: Boolean) = if (show) show() else hide()

    @JvmOverloads
    fun show(animate: Boolean = true) {
        if (animate) {
            doSlideUpAnimation()
        }else{
            isVisible = true
        }
    }

    @JvmOverloads
    fun hide(animate: Boolean = true) {
        if (animate) {
            doSlideDownAnimation()
        }else{
            isVisible = false
        }
    }


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

    fun refresh(show: Boolean) {
        isVisible = show
        clearBadge()
        invalidate()
    }

    fun setTabSelected(position: Int) {
        val tabCount = tabCount
        for (i in 0 until tabCount) {
            getTabAt(i)?.let {
                if (position == it.position) {
                    if (it.isSelected()) return
                    it.select()
                    //Update last tab description
                    setTabContentDescription(getTabAt(tabCount - 1)/*last Tab*/, getTabView(i))
                    //Update current tab description
                    setTabContentDescription(it, getTabView(i))
                    return
                }
            }
        }
        Log.w(TAG, "setTabSelected:  $position is an invalid position.")
    }

    fun setItemBadge(itemId: Int, badge: Badge) = menu.setBadge(itemId, badge)

    fun setItemEnabled(itemId: Int, enabled: Boolean){
        menu.findItem(itemId)?.setEnabled(enabled)
            ?: Log.w(TAG, "setItemEnabled: ${context.resources.getResourceEntryName(itemId)} item id is invalid.")
    }

    fun setItemVisible(itemId: Int, visible: Boolean){
        menu.findItem(itemId)?.setVisible(visible)
            ?: Log.w(TAG, "`setItemVisible`, ${context.resources.getResourceEntryName(itemId)} item id is invalid.")
    }

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
        itemClickedListener?.apply {
            menu.findItem(tab.id)?.let {
                onMenuItemClick(it)
            }
        }
    }

    override fun onTabUnselected(tab: Tab) = Unit

    override fun onTabReselected(tab: Tab) = Unit

    fun setOnMenuItemClickListener(onMenuItemClickedListener: MenuItem.OnMenuItemClickListener){
        this.itemClickedListener = onMenuItemClickedListener
    }

    private class SavedState : AbsSavedState {
        var isGridDialogShowing = false

        constructor(superState: Parcelable?) : super(superState!!)
        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            isGridDialogShowing = parcel.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (isGridDialogShowing) 1 else 0)
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

    companion object{
        private const val TAG = "BottomTabLayout"
        private const val ABOUT_TO_HIDE = 1
        private const val ABOUT_TO_SHOW = 2
        private const val TRANSIENT_NONE = 0
    }
}