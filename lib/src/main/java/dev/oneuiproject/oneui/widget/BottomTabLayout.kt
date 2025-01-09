@file:Suppress( "MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.PointerIcon
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.customview.view.AbsSavedState
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.dialog.GridMenuDialog
import dev.oneuiproject.oneui.ktx.addCustomTab
import dev.oneuiproject.oneui.ktx.addTab
import dev.oneuiproject.oneui.ktx.getTabView//import dev.oneuiproject.oneui.ktx.isNumericValue
import dev.oneuiproject.oneui.ktx.isNumericValue
import dev.oneuiproject.oneui.ktx.setListener
import dev.oneuiproject.oneui.dialog.internal.toGridDialogItem
import dev.oneuiproject.oneui.ktx.windowWidthNetOfInsets
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isDisplayTypeSub
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isLandscape
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_90
import com.google.android.material.R as materialR

@SuppressLint("NewApi", "RestrictedApi")
class BottomTabLayout(
    context: Context,
    attrs: AttributeSet?
) : MarginsTabLayout(context, attrs), TabLayout.OnTabSelectedListener {

    private var mSlideDownAnim: Animation? = null
    private var mSlideUpAnim: Animation? = null
    private lateinit var mTextViews: ArrayList<TextView?>

    private var gridMenuDialog: GridMenuDialog? = null
    private var onMenuItemClicked: ((menuItem: MenuItem) -> Unit)? = null

    private lateinit var mTabDimens: TabDimens
    private var mTabTextPadding: Float = resources.getDimension(R.dimen.oui_tab_layout_default_tab_padding)

    private var isPopulatingTabs = true
    private var tblMatchParent = true
    private var mOverFLowItems: ArrayList<MenuItemImpl>? = null

    init {
        isFocusable = false
        context.theme
            .obtainStyledAttributes(attrs, R.styleable.BottomTabLayout, 0, 0).use { a ->
                if (a.hasValue(R.styleable.BottomTabLayout_menu)) {
                    inflateMenu((a.getResourceId(R.styleable.BottomTabLayout_menu, 0)))
                }
            }

        addOnTabSelectedListener(this)
    }


    private inner class BottomTabLayoutMenu(context: Context): MenuBuilder(context){
        @JvmField
        var suspendUpdate = false

        override fun onItemsChanged(structureChanged: Boolean) {
            super.onItemsChanged(structureChanged)
            if (!suspendUpdate) {
                Log.d(TAG, "onItemsChanged")
                updateTabs()
            }
        }
    }

    private lateinit var bottomTabLayoutMenu: BottomTabLayoutMenu

    fun inflateMenu(
        @MenuRes menuResId: Int,
        onMenuItemClicked: ((menuItem: MenuItem) -> Unit)? = null
    ){
        Log.d(TAG, "inflateMenu")
        this.onMenuItemClicked = onMenuItemClicked

        bottomTabLayoutMenu = BottomTabLayoutMenu(context).also {
            it.suspendUpdate = true
            SupportMenuInflater(context).inflate(menuResId, it)
            it.suspendUpdate = false
        }
        updateTabs()
    }


    private fun updateTabs() {
        val tabItems = ArrayList<MenuItemImpl>()
        val overflowItems = ArrayList<MenuItemImpl>()

        var tabItemsAdded = 0
        for (i in 0 until bottomTabLayoutMenu.size()) {
            val menuItem = bottomTabLayoutMenu.getItem(i) as MenuItemImpl
            if (menuItem.isVisible) {
                if (tabItemsAdded < 3) {
                    tabItems.add(menuItem)
                    tabItemsAdded++
                }else{
                    overflowItems.add(menuItem)
                }
            }
        }
        this.mOverFLowItems = overflowItems
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
                tabTitleRes =/* if (showMoreText)materialR.string.sesl_more_item_label else*/ null,
                tabIconRes = R.drawable.oui_ic_ab_drawer,
                listener = { createAndShowGridDialog() }
            ).apply{
                tabIconTint = ContextCompat.getColorStateList(context, R.color.sesl_tablayout_selected_indicator_color)
                id = R.id.bottom_tab_menu_show_grid_dialog
            }
        }

        isPopulatingTabs = false
        mRemeasure = true

        if (isAttachedToWindow){
            invalidateTabLayout()
        }
    }

    private inline fun addTabForMenu(menuItem: MenuItemImpl) {
        addTab(
            tabTitle = menuItem.title,
            tabIcon = /*menuItem.icon*/null
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
        gridMenuDialog = createGridMenuDialog(mOverFLowItems!!)
        gridMenuDialog!!.show()
    }

    private fun hasOverflowItems(): Boolean = mOverFLowItems?.isNotEmpty() == true

    override fun invalidateTabLayout() {
        if (isPopulatingTabs) return
        doOnLayout {
            post {
                Log.d(TAG, "invalidateTabLayout remeasure: $mRemeasure")
                if (mRemeasure) {
                    remeasureDimens()
                    mRemeasure = false
                }
                updateSideInsetsAndTabMode()
                setScrollPosition(selectedTabPosition, 0.0f, true)
            }
        }
    }

    private fun remeasureDimens() {
        Log.d(TAG, "remeasureDimens")

        tblMatchParent = (layoutParams as? MarginLayoutParams)?.width == MATCH_PARENT
        val containerWidth = if (tblMatchParent){
            (parent as ViewGroup).width
        }else{
            context.windowWidthNetOfInsets
        }

        mTabDimens = TabDimens(mTabTextPadding, containerWidth)

        mTextViews = arrayListOf()
        val systemIcon = if (Build.VERSION.SDK_INT >= 24) {
            PointerIcon.getSystemIcon(context, 1000)
        } else null
        val tabCount = tabCount
        for (i in 0 until tabCount) {
            val tab = getTabAt(i)
            val viewGroup = getTabView(i) as? ViewGroup
            if (tab != null && viewGroup != null) {
                val textView = tab.seslGetTextView()
                mTextViews.add(textView)
                viewGroup.contentDescription = textView?.text
                systemIcon?.let {
                    viewGroup.pointerIcon = it
                }
            }
        }
    }


    private fun updateSideInsetsAndTabMode() {
        val tabTextWidthsList = arrayListOf<Float>()

        val tabCount = tabCount
        var tabTextWidthSum = 0.0f
        for (i in 0 until tabCount) {
            val tabTextView = mTextViews[i]
            val tabTextWidth = tabTextView?.paint?.measureText(tabTextView.getText().toString()) ?: 0f
            tabTextWidthsList.add(tabTextWidth)
            tabTextWidthSum += tabTextWidth
        }

        val calculatedTabLayoutPadding = mTabDimens.calculateTabLayoutPadding(
            context,
            tabTextWidthSum,
            if (applySideMargin) sideMargin.toFloat() else 0f
        ).toInt()

        val maxTotalTabPaddings = mTabDimens.screenWidthPixels - (calculatedTabLayoutPadding * 2f)

        val tabModeToSet: Int
        if (mTabDimens.mTabTextPaddingSum + tabTextWidthSum < maxTotalTabPaddings) {

            tabModeToSet = MODE_FIXED

            val tabPadding = ((maxTotalTabPaddings - tabTextWidthSum) / 2f + mTabTextPadding).coerceAtLeast(0f)
            val lastTabPaddingAdj = maxTotalTabPaddings - tabTextWidthSum - (8 * tabPadding)

            for (i in 0 until tabCount) {
                val tabView = getTabView(i)!!
                val tabWidth = if (lastTabPaddingAdj != 0.0f && i == 3/*last tab*/) {
                    (tabPadding * 2f + tabTextWidthsList[i] + lastTabPaddingAdj).toInt()
                } else {
                    (tabPadding * 2f + tabTextWidthsList[i]).toInt()
                }
                tabView.minimumWidth = tabWidth
                (tabView as? ViewGroup)?.getChildAt(0)?.minimumWidth = tabWidth
            }
        } else {
            //Total tab paddings exceeds maxTotalPaddings
            //so will make it scrollable
            tabModeToSet = MODE_SCROLLABLE

            for (i in 0 until tabCount) {
                val tabView = getTabView(i)!!
                val tabWidth = ((mTabTextPadding * 2f) + tabTextWidthsList[i]).toInt()
                tabView.minimumWidth = tabWidth
                (tabView as? ViewGroup)?.getChildAt(0)?.minimumWidth = tabWidth
            }
        }

        (layoutParams as? MarginLayoutParams)?.let {
            it.width = if (tblMatchParent) MATCH_PARENT else WRAP_CONTENT
            Log.d(TAG, "Update side insets to $calculatedTabLayoutPadding")
            it.setMargins(calculatedTabLayoutPadding, 0, calculatedTabLayoutPadding, 0)
            layoutParams = it
        } ?: Log.e(TAG, "layoutParams is null")

        Log.d(TAG, "Update tabMode to ${if (tabModeToSet == MODE_FIXED) "MODE_FIXED" else "MODE_SCROLLABLE"}")
        tabMode = tabModeToSet
    }

    private fun createGridMenuDialog(menu: ArrayList<MenuItemImpl>) =
        GridMenuDialog(context).apply {
            create()
            updateItems(menu.map { it.toGridDialogItem() })
            setOnItemClickListener{item: GridMenuDialog.GridItem ->
                onMenuItemClicked?.apply {
                    this@BottomTabLayout.bottomTabLayoutMenu.findItem(item.itemId)?.let {
                        invoke(it)
                    }
                }
                true
            }
        }

    private val reshowDialogRunnable = Runnable {
        if (isAttachedToWindow){
            createAndShowGridDialog()
        }
    }

    private fun reshowGridDialog(){
        removeCallbacks(reshowDialogRunnable)
        postDelayed(reshowDialogRunnable,50)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //Block touch on parent
        (parent as View).setOnTouchListener { _, _ -> true }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (parent as View).setOnTouchListener(null)
        //Dismiss to prevent activity window from leaking
        gridMenuDialog?.dismiss()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (gridMenuDialog?.isShowing == true){
            gridMenuDialog?.updateDialog()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (gridMenuDialog?.isShowing == true){
            gridMenuDialog?.updateDialog()
        }
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
        if (mSlideDownAnim == null) {
            mSlideDownAnim = AnimationUtils.loadAnimation(context, R.anim.oui_bottom_tab_slide_down).apply {
                interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_90)
                startOffset = 100L
                setListener(
                    onStart = { this@BottomTabLayout.visibility = INVISIBLE}
                )
            }
        }
        startAnimation(mSlideDownAnim)
    }

    private fun doSlideUpAnimation() {
        if (mSlideUpAnim == null) {
            mSlideUpAnim = AnimationUtils.loadAnimation(context, R.anim.oui_bottom_tab_slide_up).apply {
                interpolator = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_90)
                startOffset = 400L
            }
        }
        visibility = VISIBLE
        startAnimation(mSlideUpAnim)
    }

    private fun getTabCountWithoutOverflow() = if (hasOverflowItems()) tabCount - 1 else tabCount

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

    fun applyAnimation(show: Boolean) {
        if (show) {
            doSlideUpAnimation()
        } else {
            doSlideDownAnimation()
        }
    }


    fun blockFocus(block: Boolean) {
        setDescendantFocusability(if (block) FOCUS_BLOCK_DESCENDANTS else FOCUS_AFTER_DESCENDANTS)
    }

    override fun onInitializeAccessibilityNodeInfo(nodeInfo: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(nodeInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        Log.e("BottomTabLayout", "`setTabSelected`,  $position is an invalid position.")
    }

    fun setItemBadge(itemId: Int, badge: Badge) {
        findTab(itemId)?.position?.let {
            when(badge){
                Badge.NONE -> {
                    seslShowBadge(it, false, "")
                    seslShowDotBadge(it, false)
                }
                Badge.DOT -> seslShowDotBadge(it, true)
                is Badge.NUMERIC -> seslShowBadge(it, true, badge.toBadgeText())
            }
            return
        }
        gridMenuDialog?.apply {
            setBadge(itemId, badge)
            seslShowDotBadge(tabCount - 1, gridMenuDialog!!.isShowingBadge())
            return
        }
        Log.e("BottomTabLayout", "`showBadge`,  0x${Integer.toHexString(itemId)} item id is invalid.")
    }

    fun setEnableItem(itemId: Int, enabled: Boolean){
        findTab(itemId)?.position?.let {
            isEnabled = enabled
            return
        }
        gridMenuDialog?.apply {
            setEnableItem(itemId, enabled)
            return
        }
        Log.e("BottomTabLayout", "`setEnableItem`, 0x${Integer.toHexString(itemId)} item id is invalid.")
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
        onMenuItemClicked?.apply {
            bottomTabLayoutMenu.findItem(tab.id)?.let {
                invoke(it as MenuItemImpl)
            }
        }
    }

    override fun onTabUnselected(tab: Tab) = Unit

    override fun onTabReselected(tab: Tab) = Unit

    fun onClickMenuItem(onClick: (menuItem: MenuItem) -> Unit){
        this.onMenuItemClicked = onClick
    }

    fun setSelectedItem(itemId: Int) = findTab(itemId)?.position?.let { setTabSelected(it) }

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
                                                  loader: ClassLoader): SavedState = SavedState(parcel, null)
                    override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel, null)
                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(Build.VERSION_CODES.R)
    private class TabDimens(
        tabTextPadding: Float,
        var screenWidthPixels: Int
    ) {
        companion object {
            private const val PADDING_COUNT = 8
        }

        var mTabTextPaddingSum: Float = tabTextPadding * PADDING_COUNT

        fun calculateTabLayoutPadding(context: Context, tabTextWidthSum: Float, tabLayoutPaddingMin: Float): Float {
            val configuration = context.resources.configuration

            if (isLandscape(configuration) && isDisplayTypeSub(configuration)) {
                return tabLayoutPaddingMin / 2
            }

            if (configuration.screenWidthDp <= 480) {
                return tabLayoutPaddingMin
            }

            val tabLayoutPaddingMax: Float = screenWidthPixels * 0.125f

            return ((screenWidthPixels - tabTextWidthSum - mTabTextPaddingSum) / 2).coerceIn(tabLayoutPaddingMin, tabLayoutPaddingMax)
        }
    }

    companion object{
        private const val TAG = "BottomTabLayout"
    }
}