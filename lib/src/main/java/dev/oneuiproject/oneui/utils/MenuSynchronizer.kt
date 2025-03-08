package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.oneuiproject.oneui.utils.MenuSynchronizer.State

/**
 * Handles the synchronization of menu items between a BottomNavigationView and a Toolbar,
 * which can be used to adapt menu to different device configurations and orientation states.
 *
 * @param bottomNavView The BottomNavigationView to synchronize the menu items with.
 * @param toolbar The Toolbar to synchronize the menu items with.
 * @param onMenuItemClick A callback to handle menu item click events.
 * @param initialState (Optional) The initial state of the menu - either [State.PORTRAIT], [State.LANDSCAPE], or [State.HIDDEN].
 * @param maxActionItems (Optional) The maximum number of items to show as action items in the Toolbar. By default, it's 2.
 * @param copyIcon (Optional) Boolean flag to determine whether to show icons on the Toolbar menu items.
 * By default, it's true when there are more than one item in the menu and `maxActionItems` is greater than 1.
 */
@SuppressLint("RestrictedApi")
class MenuSynchronizer @JvmOverloads constructor(
    bottomNavView: BottomNavigationView,
    toolbar: Toolbar,
    onMenuItemClick: (menuItem: MenuItem) -> Boolean,
    initialState: State? = null,
    maxActionItems: Int = 2,
    private var copyIcon: Boolean? = null,
) {

    val menu = SynchronizedMenuBuilder(toolbar, bottomNavView, maxActionItems)

    init{
        toolbar.setOnMenuItemClickListener {
            onMenuItemClick(it)
        }

        bottomNavView.setOnItemSelectedListener {
            onMenuItemClick(it)
        }

        initialState?.let {
            menu.setState(it)
        }
    }

    var state: State
        get() = menu.getState()
        set(value) {
            if (menu.getState() == value) return
            menu.setState(value)
        }

    fun setMenuItemEnabled(@IdRes menuItemId: Int, enabled: Boolean) =
        menu.setMenuItemEnabled(menuItemId, enabled)

    fun setMenuItemHidden(@IdRes menuItemId: Int, isHidden: Boolean) =
        menu.setMenuItemHidden(menuItemId, isHidden)

    fun clear()= menu.clearAll()

    class SynchronizedMenuBuilder(private val toolbar: Toolbar,
                                        private val bottomNavView: BottomNavigationView,
                                        private val maxActionItems: Int): MenuBuilder(toolbar.context){

        private var currentState: State = State.HIDDEN
        private var refreshToolbarMenu = false
        private var refreshBottomMenu = false

        override fun onItemsChanged(structureChanged: Boolean) {
            when (currentState){
                State.PORTRAIT -> {
                    toolbar.menu.removeGroup(AMT_GROUP_MENU_ID)
                    refreshBottomMenu = false
                    refreshToolbarMenu = true
                    refreshBottomMenu()
                }

                State.LANDSCAPE -> {
                    bottomNavView.menu.clear()
                    refreshBottomMenu = true
                    refreshToolbarMenu = false
                    refreshToolbarMenu()
                }

                State.HIDDEN -> {
                    bottomNavView.menu.clear()
                    toolbar.menu.removeGroup(AMT_GROUP_MENU_ID)
                    refreshBottomMenu = true
                    refreshToolbarMenu = true
                }
            }
        }

        private fun refreshToolbarMenu(){
            var menuItemsAdded = 0
            val size = size()
            forEach {
                if (!it.isVisible) return@forEach
                menuItemsAdded++
                addMenuItem(toolbar.menu, it, size > 1, AMT_GROUP_MENU_ID).apply {
                    if (menuItemsAdded <= maxActionItems) {
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    }
                }
            }
        }

        private fun refreshBottomMenu(){
            var menuItemsAdded = 0
            forEach {
                if (!it.isVisible) return@forEach
                menuItemsAdded++
                addMenuItem(bottomNavView.menu, it, true).apply {
                    if ((it as MenuItemImpl).requiresActionButton()){
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    }
                }
            }
        }

        @Suppress("RedundantOverride")
        //To suppress restricted api lint warning on caller
        override fun findItem(id: Int): MenuItem? = super.findItem(id)

        internal fun getState(): State = currentState

        internal fun setState(state: State) {
            currentState = state

            when (state) {
                State.PORTRAIT -> {
                    if (refreshBottomMenu){
                        refreshBottomMenu = false
                        refreshBottomMenu()
                    }
                    bottomNavView.isVisible = true
                    toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
                }

                State.LANDSCAPE -> {
                    if (refreshToolbarMenu){
                        refreshToolbarMenu = false
                        refreshToolbarMenu()
                    }
                    toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, true)
                    bottomNavView.isVisible = false
                }

                State.HIDDEN -> {
                    toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
                    bottomNavView.isVisible = false
                }
            }
        }


        internal fun setMenuItemEnabled(@IdRes menuItemId: Int, enabled: Boolean) {
            findItem(menuItemId)?.isEnabled = enabled
        }

        internal fun setMenuItemHidden(@IdRes menuItemId: Int, isHidden: Boolean) {
            findItem(menuItemId)?.isVisible = !isHidden
        }

        override fun clearAll(){
            super.clearAll()
            bottomNavView.setOnItemSelectedListener(null)
            toolbar.setOnMenuItemClickListener(null)
            toolbar.menu.clear()
            bottomNavView.menu.clear()
            currentState = State.HIDDEN
        }


        private fun addMenuItem(
            menu: Menu,
            item: MenuItem,
            addIcon: Boolean,
            groupId: Int? = null
        ): MenuItem {
            return menu.add(groupId ?: item.groupId, item.itemId, item.order, item.title).apply {
                isEnabled = item.isEnabled
                if (addIcon) {
                    icon = item.icon
                    MenuItemCompat.setIconTintList(this, MenuItemCompat.getIconTintList(item))
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    item.tooltipText?.let { t -> MenuItemCompat.setTooltipText(this, t) }
                }
            }
        }

    }

    enum class State{
        PORTRAIT,
        LANDSCAPE,
        HIDDEN
    }

    companion object{
        private const val AMT_GROUP_MENU_ID = 999
    }
}

