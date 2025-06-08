package dev.oneuiproject.oneui.navigation.menu

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuPresenter
import androidx.appcompat.view.menu.SubMenuBuilder
import androidx.core.os.BundleCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.contains
import androidx.core.view.forEach
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate
import com.google.android.material.internal.ParcelableSparseArray
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.navigation.widget.DrawerCategoryItemView
import dev.oneuiproject.oneui.navigation.widget.DrawerDividerItemView
import dev.oneuiproject.oneui.navigation.widget.DrawerMenuItemView
import dev.oneuiproject.oneui.navigation.widget.DrawerMenuView

/**
 * @hide
 */
@SuppressLint("RestrictedApi")
internal class DrawerMenuPresenter(
    private var getNavRailSlideRange: () -> Int
) : MenuPresenter {

    private var drawerMenuView: DrawerMenuView? = null
    private var lastOffsetApplied = -1f

    sealed interface NavigationMenuItem{
        fun getItemId():Long
    }

    private data class NavigationMenuCategoryItem(val menuItem: MenuItemImpl) : NavigationMenuItem {
        override fun getItemId(): Long = menuItem.itemId.toLong()
        override fun equals(other: Any?): Boolean {
            return ((other as? NavigationMenuCategoryItem)?.menuItem == menuItem)
        }

        override fun hashCode(): Int {
            return menuItem.hashCode()
        }
    }

    /** Normal or subheader items.  */
    private data class NavigationMenuTextItem(val menuItem: MenuItemImpl,
                                              val needsEmptyIcon: Boolean = false,
                                              val isSubMenu: Boolean = false,
                                              val isExpanded: Boolean = false) :
        NavigationMenuItem {
        override fun getItemId(): Long  = menuItem.itemId.toLong()
        override fun equals(other: Any?): Boolean {
            if (other !is NavigationMenuTextItem) return false
            return (other.menuItem == menuItem
                    && other.needsEmptyIcon == needsEmptyIcon
                    && other.isSubMenu == isSubMenu
                    && other.isExpanded == isExpanded)
        }
        override fun hashCode(): Int {
            return super.hashCode()
        }
    }
    /** Divider items.  */
    private data class NavigationMenuSeparatorItem(val id: Long) : NavigationMenuItem {
        override fun getItemId(): Long = id
        override fun equals(other: Any?): Boolean {
            if (other !is NavigationMenuSeparatorItem) return false
            return (other.id == id)
        }
        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    private var callback: MenuPresenter.Callback? = null
    var menu: MenuBuilder? = null
    private var id = 1

    var adapter: NavigationMenuAdapter? = null
    private var layoutInflater: LayoutInflater? = null

    override fun initForMenu(context: Context, menu: MenuBuilder) {
        layoutInflater = LayoutInflater.from(context)
        this.menu = menu
    }

    override fun getMenuView(root: ViewGroup): DrawerMenuView {
        if (drawerMenuView == null) {
            drawerMenuView = (layoutInflater!!.inflate(R.layout.oui_des_drawer_menu_view, root, false) as DrawerMenuView)
                .apply {
                    setAccessibilityDelegateCompat( NavigationMenuViewAccessibilityDelegate(this))
                }

            adapter = NavigationMenuAdapter().apply {
                // Prevent recreating all the Views when notifyDataSetChanged()
                setHasStableIds(true)
            }
            drawerMenuView!!.adapter = adapter

        }
        return drawerMenuView!!
    }

    override fun updateMenuView(cleared: Boolean) {
        adapter?.update()
    }

    override fun setCallback(cb: MenuPresenter.Callback) {
        callback = cb
    }

    override fun onSubMenuSelected(subMenu: SubMenuBuilder) = false

    override fun onCloseMenu(menu: MenuBuilder, allMenusAreClosing: Boolean) {
        callback?.onCloseMenu(menu, allMenusAreClosing)
    }

    override fun flagActionItems() = false

    override fun expandItemActionView(menu: MenuBuilder, item: MenuItemImpl) = false

    override fun collapseItemActionView(menu: MenuBuilder, item: MenuItemImpl)= false

    override fun getId() = id

    override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        drawerMenuView?.apply {
            val hierarchy = SparseArray<Parcelable>()
            saveHierarchyState(hierarchy)
            state.putSparseParcelableArray(STATE_HIERARCHY, hierarchy)
        }
        adapter?.apply {
            state.putBundle(STATE_ADAPTER, createInstanceState())
        }
        return state
    }

    override fun onRestoreInstanceState(parcelable: Parcelable) {
        if (parcelable is Bundle) {
            BundleCompat.getSparseParcelableArray(
                parcelable,
                STATE_HIERARCHY,
                Parcelable::class.java
            )?.let {
                drawerMenuView!!.restoreHierarchyState(it)
            }
            parcelable.getBundle(STATE_ADAPTER)?.let {
                adapter!!.restoreInstanceState(it)
            }
        }
    }

    var checkedItem: MenuItemImpl?
        get() = adapter!!.getCheckedItem()
        set(item) {
            adapter!!.setCheckedItem(item!!)
        }

    fun setUpdateSuspended(updateSuspended: Boolean) {
        adapter?.setUpdateSuspended(updateSuspended)
    }

    /**
     * Handles click events for the [DrawerLayout][dev.oneuiproject.oneui.layout.DrawerLayout]
     * or [NavDrawerLayout][dev.oneuiproject.oneui.layout.NavDrawerLayout] navigation menu items.
     * The items has to be [DrawerMenuItemView].
     */
    val onClickListener: View.OnClickListener = View.OnClickListener { view ->
        val itemView = view as DrawerMenuItemView
        setUpdateSuspended(true)

        val item = itemView.itemData
        if (adapter!!.toggleIfHasSubMenu(item)) {
            setUpdateSuspended(false)
            return@OnClickListener
        }

        var checkStateChanged = false
        val result = menu!!.performItemAction(item, this@DrawerMenuPresenter, 0)
        if (item.isCheckable && result) {
            adapter!!.setCheckedItem(item)
            checkStateChanged = true
        }

        setUpdateSuspended(false)
        if (checkStateChanged) {
            updateMenuView(false)
        }
    }

    sealed class Payload{
        data class OFFSET(val slideOffset: Float): Payload()
        data object LOCK: Payload()
    }

    inner class NavigationMenuAdapter internal constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var checkedItem: MenuItemImpl? = null
        private var updateSuspended = false
        private var isLocked = false

        private val items: MutableList<NavigationMenuItem> = mutableListOf()

        init { prepareMenuItems() }

        fun updateLock(isLocked: Boolean) {
            if (this@NavigationMenuAdapter.isLocked == isLocked) return
            this@NavigationMenuAdapter.isLocked = isLocked
            notifyItemRangeChanged(0, itemCount, Payload.LOCK)
        }

        fun updateOffset(slideOffset: Float) {
            if (slideOffset == lastOffsetApplied) return
            lastOffsetApplied = slideOffset
            notifyItemRangeChanged(0, itemCount, Payload.OFFSET(slideOffset))
        }

        override fun getItemId(position: Int) = items[position].getItemId()

        override fun getItemCount()  = items.size

        override fun getItemViewType(position: Int): Int {
            return when(val item = items[position]){
                is NavigationMenuTextItem -> {
                    if (item.menuItem.hasSubMenu()) {
                        VIEW_TYPE_SUBHEADER
                    } else {
                        VIEW_TYPE_NORMAL
                    }
                }
                is NavigationMenuSeparatorItem -> {
                    return VIEW_TYPE_SEPARATOR
                }
                is NavigationMenuCategoryItem -> {
                    return VIEW_TYPE_CATEGORY
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_SEPARATOR -> SeparatorViewHolder(DrawerDividerItemView(parent.context))

                VIEW_TYPE_NORMAL -> NormalViewHolder(
                    layoutInflater!!.inflate(R.layout.oui_des_drawer_menu_item, parent, false) as DrawerMenuItemView
                ).apply {
                    itemView.setOnClickListener(onClickListener)
                }

                VIEW_TYPE_SUBHEADER -> SubheaderViewHolder(
                    layoutInflater!!.inflate(R.layout.oui_des_drawer_menu_item, parent, false) as DrawerMenuItemView
                ).apply {
                    itemView.setOnClickListener(onClickListener)
                }

                VIEW_TYPE_CATEGORY -> CategoryItemViewHolder(
                    layoutInflater!!.inflate(R.layout.oui_des_drawer_menu_category, parent, false) as DrawerMenuItemView
                ).apply {
                    itemView.setOnClickListener(onClickListener)
                }
                else -> throw IllegalArgumentException()
            }

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
            }else {
                for (payload in payloads.toSet()) {
                    when (payload) {
                        is Payload.OFFSET -> {
                            when (getItemViewType(position)) {
                                VIEW_TYPE_NORMAL ->  (holder as NormalViewHolder).updateOffset(payload.slideOffset)
                                VIEW_TYPE_SEPARATOR -> (holder as SeparatorViewHolder).updateOffset(payload.slideOffset)
                                VIEW_TYPE_CATEGORY -> (holder as CategoryItemViewHolder).updateOffset(payload.slideOffset)
                                VIEW_TYPE_SUBHEADER -> (holder as SubheaderViewHolder).updateOffset(payload.slideOffset)
                            }
                        }

                        Payload.LOCK -> {
                            when (getItemViewType(position)) {
                                VIEW_TYPE_NORMAL ->  {
                                    val item = items[position] as NavigationMenuTextItem
                                    (holder as NormalViewHolder).updateLock(item.menuItem)
                                }
                                VIEW_TYPE_CATEGORY -> {
                                    val item = items[position] as NavigationMenuCategoryItem
                                    (holder as CategoryItemViewHolder).updateLock(item.menuItem)
                                }
                                VIEW_TYPE_SUBHEADER -> {
                                    val item = items[position] as NavigationMenuTextItem
                                    (holder as SubheaderViewHolder).updateLock(item.menuItem)
                                }
                                VIEW_TYPE_SEPARATOR -> Unit
                            }

                        }
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                VIEW_TYPE_NORMAL -> {
                    val normalViewHolder = holder as NormalViewHolder
                    val item = items[position] as NavigationMenuTextItem
                    normalViewHolder.bind(item)
                    setAccessibilityDelegate(normalViewHolder.itemView, position, false)
                }

                VIEW_TYPE_SUBHEADER -> {
                    val subheaderViewHolder = holder as SubheaderViewHolder
                    val item = items[position] as NavigationMenuTextItem
                    subheaderViewHolder.bind(item)
                    setAccessibilityDelegate(subheaderViewHolder.itemView, position, true)

                }

                VIEW_TYPE_CATEGORY -> {
                    val categoryItemViewHolder = holder as CategoryItemViewHolder
                    val item = items[position] as NavigationMenuCategoryItem
                    categoryItemViewHolder.bind(item)
                    setAccessibilityDelegate(categoryItemViewHolder.itemView, position, false)
                }

                VIEW_TYPE_SEPARATOR -> Unit
            }
        }

        private inner class NormalViewHolder(
            private val menuItemView: DrawerMenuItemView
        ) : RecyclerView.ViewHolder(menuItemView) {

            fun bind(navItem: NavigationMenuTextItem) {
                val menuItem = navItem.menuItem
                menuItemView.initialize(menuItem, if (navItem.isSubMenu) DrawerMenuItemView.MENU_TYPE_SUBMENU else DrawerMenuItemView.MENU_TYPE_NORMAL)
                updateLock(menuItem)
                if (lastOffsetApplied != -1f){
                    menuItemView.applyOffset(lastOffsetApplied)
                }
            }

            fun updateOffset(alpha: Float) {
                menuItemView.applyOffset(alpha)
            }

            fun updateLock(menuItem: MenuItemImpl){
                menuItemView.isEnabled = !isLocked && menuItem.isEnabled
            }
        }

        private inner class CategoryItemViewHolder(private val menuItemView: DrawerMenuItemView) : RecyclerView.ViewHolder(menuItemView) {

            fun bind(categoryItem: NavigationMenuCategoryItem) {
                val menuItem = categoryItem.menuItem
                menuItemView.initialize(menuItem, DrawerMenuItemView.MENU_TYPE_CATEGORY)
                updateLock(menuItem)
                if (lastOffsetApplied != -1f){
                    menuItemView.applyOffset(lastOffsetApplied)
                }
            }

            fun updateOffset(offset: Float) {
                menuItemView.applyOffset(offset)
            }

            fun updateLock(menuItem: MenuItemImpl){
                menuItemView.isEnabled = !isLocked && menuItem.isEnabled
            }
        }

        private inner class SubheaderViewHolder(
            private val menuItemView: DrawerMenuItemView
        ) : RecyclerView.ViewHolder(menuItemView){

            fun bind(navItem: NavigationMenuTextItem) {
                val menuItem = navItem.menuItem
                menuItemView.initialize(menuItem, DrawerMenuItemView.MENU_TYPE_SUBHEADER)
                updateLock(menuItem)
                menuItemView.animateToggle(navItem.isExpanded)
                if (lastOffsetApplied != -1f){
                    menuItemView.applyOffset(lastOffsetApplied)
                }
            }

            fun updateOffset(alpha: Float) {
                menuItemView.applyOffset(alpha)
                if (alpha == 0f) {
                    drawerMenuView?.post {
                        toggleIfHasSubMenu(
                            (items[layoutPosition] as NavigationMenuTextItem).menuItem,
                            true
                        )
                    }
                }
            }

            fun updateLock(menuItem: MenuItemImpl){
                menuItemView.isEnabled = !isLocked && menuItem.isEnabled
            }
        }


        private inner class SeparatorViewHolder(dividerItemView: DrawerDividerItemView) : RecyclerView.ViewHolder(dividerItemView){
            init {
                (itemView as DrawerDividerItemView).setNavRailSlideRangeProvider(getNavRailSlideRange)
            }

            fun updateOffset(offset: Float) {
                (itemView as DrawerDividerItemView).applyOffset(offset)
            }
        }

        private fun setAccessibilityDelegate(view: View, position: Int, isHeader: Boolean) {
            ViewCompat.setAccessibilityDelegate(
                view,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View, info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.setCollectionItemInfo(
                            AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain( /* rowIndex= */
                                adjustItemPositionForA11yDelegate(position),  /* rowSpan= */
                                1,  /* columnIndex= */
                                1,  /* columnSpan= */
                                1,  /* heading= */
                                isHeader,  /* selected= */
                                host.isSelected
                            )
                        )
                    }
                })
        }

        /** Adjusts position based on the presence of separators and header.  */
        private fun adjustItemPositionForA11yDelegate(position: Int): Int {
            var adjustedPosition = position
            for (i in 0 until position) {
                if (adapter!!.getItemViewType(i) == VIEW_TYPE_SEPARATOR) {
                    adjustedPosition--
                }
            }
            return adjustedPosition
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            if (holder is NormalViewHolder) {
                (holder.itemView as DrawerMenuItemView).recycle()
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun update() {
            prepareMenuItems()
            notifyDataSetChanged()
        }

        /**
         * Flattens the visible menu items of [.menu] into [.items], while inserting
         * separators between items when necessary.
         */
        private fun prepareMenuItems() {
            if (updateSuspended) {
                return
            }
            updateSuspended = true

            items.clear()

            var currentGroupId = -1
            var currentGroupStart = 0
            var currentGroupHasIcon = false

            val itemsSize: Int = menu!!.size
            var item: MenuItemImpl
            for (i in 0 until itemsSize) {
                item = menu!!.getItem(i) as MenuItemImpl

                if (!item.isVisible) continue

                if (item.isChecked) setCheckedItem(item)

                if (item.isCheckable) item.isExclusiveCheckable = false

                if (item.actionView is DrawerCategoryItemView){
                    items.add(NavigationMenuCategoryItem(item))
                }else {
                    if (item.hasSubMenu()) {
                        val subMenu = item.subMenu
                        if (subMenu!!.hasVisibleItems()) {
                            if (i != 0) {
                                items.add(NavigationMenuSeparatorItem(i.toLong()))
                            }
                            items.add(NavigationMenuTextItem(item))
                        }
                    } else {
                        val groupId = item.groupId
                        if (groupId != currentGroupId) { // first item in group
                            currentGroupStart = items.size
                            currentGroupHasIcon = item.icon != null
                            if (i != 0 && menu!!.isGroupDividerEnabled) {
                                currentGroupStart++
                                items.add(NavigationMenuSeparatorItem(i.toLong()))
                            }
                        } else if (!currentGroupHasIcon && item.icon != null) {
                            currentGroupHasIcon = true
                            appendTransparentIconIfMissing(items, currentGroupStart, items.size)
                        }
                        val textItem = NavigationMenuTextItem(item, currentGroupHasIcon)
                        items.add(textItem)
                        currentGroupId = groupId
                    }
                }
            }
            updateSuspended = false
        }

        internal fun toggleIfHasSubMenu(item: MenuItemImpl, forceCollapsed: Boolean = false): Boolean {
            if (!item.hasSubMenu()) return false

            val index = items.indexOfFirst {  (it as? NavigationMenuTextItem)?.menuItem == item }
            val navMenuItem = items[index] as NavigationMenuTextItem

            if (forceCollapsed && !navMenuItem.isExpanded) return true

            val newValue =  !navMenuItem.isExpanded
            if (newValue && lastOffsetApplied< 0.05) return false//todo
            items[index] = navMenuItem.copy (
                isExpanded = newValue
            )

            if (newValue) {
                // Expand submenu
                var addedSubMenuCount = 0
                var subMenuHasIcon = false
                item.subMenu!!.forEach {sm ->
                    val subMenuItem = sm as MenuItemImpl
                    if (subMenuItem.isVisible) {
                        subMenuHasIcon = subMenuHasIcon || (subMenuItem.icon != null)
                        if (subMenuItem.isCheckable) {
                            subMenuItem.isExclusiveCheckable = false
                        }
                        addedSubMenuCount++
                        items.add(index + addedSubMenuCount, NavigationMenuTextItem(subMenuItem, isSubMenu = true))
                    }
                }
                if (addedSubMenuCount > 0) {
                    if (subMenuHasIcon) {
                        appendTransparentIconIfMissing(items, index + 1, index + addedSubMenuCount)
                    }
                    @Suppress("NotifyDataSetChanged")
                    notifyDataSetChanged()
                }
            } else {
                // Collapse submenu
                items.removeAll {
                    (it as? NavigationMenuTextItem)?.menuItem?.let { m -> item.subMenu!!.contains(m) } == true
                }
                drawerMenuView?.apply {
                    getRecycledViewPool().clear()
                    @Suppress("NotifyDataSetChanged")
                    notifyDataSetChanged()
                }
            }
            return true

        }

        private fun appendTransparentIconIfMissing(items: MutableList<NavigationMenuItem>, startIndex: Int, endIndex: Int) {
            for (i in startIndex until endIndex) {
                val textItem = items[i] as NavigationMenuTextItem
                items[i] = textItem.copy(needsEmptyIcon = true)
            }
        }

        fun setCheckedItem(checkedItem: MenuItemImpl) {
            if (this.checkedItem == checkedItem || !checkedItem.isCheckable) {
                return
            }
            if (this.checkedItem != null) {
                this.checkedItem!!.setChecked(false)
            }
            this.checkedItem = checkedItem
            checkedItem.setChecked(true)
        }

        fun getCheckedItem(): MenuItemImpl? {
            return checkedItem
        }

        fun createInstanceState(): Bundle {
            val state = Bundle()
            if (checkedItem != null) {
                state.putInt(STATE_CHECKED_ITEM, checkedItem!!.itemId)
            }
            // Store the states of the action views.
            val actionViewStates = SparseArray<ParcelableSparseArray>()
            var i = 0
            val size = items.size
            while (i < size) {
                val navigationMenuItem = items[i]
                if (navigationMenuItem is NavigationMenuTextItem) {
                    val item: MenuItemImpl = navigationMenuItem.menuItem
                    val actionView = item.actionView
                    if (actionView != null) {
                        val container = ParcelableSparseArray()
                        actionView.saveHierarchyState(container)
                        actionViewStates.put(item.itemId, container)
                    }
                }
                i++
            }
            state.putSparseParcelableArray(STATE_ACTION_VIEWS, actionViewStates)
            return state
        }

        fun restoreInstanceState(state: Bundle) {
            val checkedItem = state.getInt(STATE_CHECKED_ITEM, 0)
            if (checkedItem != 0) {
                updateSuspended = true
                var i = 0
                val size = items.size
                while (i < size) {
                    val item = items[i]
                    if (item is NavigationMenuTextItem) {
                        val menuItem: MenuItemImpl = item.menuItem
                        if (menuItem.itemId == checkedItem) {
                            setCheckedItem(menuItem)
                            break
                        }
                    }
                    i++
                }
                updateSuspended = false
                prepareMenuItems()
            }
            // Restore the states of the action views.
            val actionViewStates = BundleCompat.getSparseParcelableArray(
                state,
                STATE_ACTION_VIEWS,
                ParcelableSparseArray::class.java
            )
            if (actionViewStates != null) {
                var i = 0
                val size = items.size
                while (i < size) {
                    val navigationMenuItem = items[i]
                    if (navigationMenuItem !is NavigationMenuTextItem) {
                        i++
                        continue
                    }
                    val item: MenuItemImpl = navigationMenuItem.menuItem
                    val actionView = item.actionView
                    if (actionView == null) {
                        i++
                        continue
                    }
                    val container = actionViewStates[item.itemId]
                    if (container == null) {
                        i++
                        continue
                    }
                    actionView.restoreHierarchyState(container)
                    i++
                }
            }
        }

        fun setUpdateSuspended(updateSuspended: Boolean) {
            this.updateSuspended = updateSuspended
        }

        val rowCount: Int
            /** Returns the number of rows that will be used for accessibility.  */
            get() {
                var rowCount = 0
                val itemCount = adapter!!.itemCount
                for (i in 0 until itemCount) {
                    val type = adapter!!.getItemViewType(i)
                    if (type != VIEW_TYPE_SEPARATOR) {
                        rowCount++
                    }
                }
                return rowCount
            }

    }


    private inner class NavigationMenuViewAccessibilityDelegate(recyclerView: RecyclerView) :
        RecyclerViewAccessibilityDelegate(recyclerView) {
        override fun onInitializeAccessibilityNodeInfo(
            host: View, info: AccessibilityNodeInfoCompat
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.setCollectionInfo(
                AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                    adapter!!.rowCount,  /* columnCount= */1,  /* hierarchical= */false
                )
            )
        }
    }

    companion object {

        private const val TAG = "NavDrawerMenuPresenter"

        private const val STATE_HIERARCHY = "android:menu:list"
        private const val STATE_ADAPTER = "android:menu:adapter"
        private const val STATE_HEADER = "android:menu:header"

        private const val STATE_CHECKED_ITEM = "android:menu:checked"

        private const val STATE_ACTION_VIEWS = "android:menu:action_views"

        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_SUBHEADER = 1
        private const val VIEW_TYPE_SEPARATOR = 2
        private const val VIEW_TYPE_CATEGORY = 3
    }
}