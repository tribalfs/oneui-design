package dev.oneuiproject.oneui.utils

import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Interpolator
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.oneuiproject.oneui.utils.MenuSynchronizer.State


/**
 * Handles the synchronization of menu items between a BottomNavigationView and a Toolbar,
 * adapting to different orientation states.
 *
 * @param bottomNavView The BottomNavigationView to synchronize with the Toolbar.
 * @param toolbar The Toolbar to synchronize with the BottomNavigationView.
 * @param onMenuItemClick A callback to handle menu item click events.
 * @param initialState The initial state of the menu - either [State.PORTRAIT], [State.LANDSCAPE], or [State.HIDDEN].
 * @param maxActionItems The maximum number of items to show as action items in the Toolbar. By default, it's 2.
 * @param copyIcon Boolean flag to determine whether to copy icons from BottomNavigationView to Toolbar.
 * By default, it's true when there are more than one item in BottomNavigationView and `maxActionItems` is greater than 1..
 */
class MenuSynchronizer @JvmOverloads constructor(
    private val bottomNavView: BottomNavigationView,
    private val toolbar: Toolbar,
    onMenuItemClick: (menuItem: MenuItem) -> Boolean,
    initialState: State? = null,
    maxActionItems: Int = 2,
    private var copyIcon: Boolean? = null,
) {

    private var interpolator: Interpolator? = null

    init{
        val toolbarMenu = toolbar.menu
        toolbarMenu.removeGroup(AMT_GROUP_MENU_ID)
        val amBottomMenu = bottomNavView.menu
        val size = amBottomMenu.size()
        if (copyIcon == null){
            copyIcon = size > 1
        }
        var menuItemsAdded = 0
        for (a in 0 until size) {
            val ambMenuItem = amBottomMenu.getItem(a)
            if (ambMenuItem.isVisible) {
                toolbarMenu.add(AMT_GROUP_MENU_ID, ambMenuItem.itemId, Menu.NONE, ambMenuItem.title).apply {
                    if (copyIcon!!) {
                        icon = ambMenuItem.icon
                        MenuItemCompat.setIconTintList(this, MenuItemCompat.getIconTintList(ambMenuItem) )
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        ambMenuItem.tooltipText?.let { MenuItemCompat.setTooltipText(this, it) }
                    }
                    setOnMenuItemClickListener {
                        onMenuItemClick(it)
                    }
                    menuItemsAdded++
                    if (menuItemsAdded <= maxActionItems) {
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    }
                }
            }
        }

        bottomNavView.setOnItemSelectedListener {
            onMenuItemClick(it)
        }

        initialState?.let {
            updateState(it)
        }
    }

    private var currentState: State? = null

    fun updateState(state: State): Boolean {
        if (currentState == state) return false
        currentState = state
        when (state) {
            State.PORTRAIT -> {
                toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
                bottomNavView.isVisible = true
            }

            State.LANDSCAPE -> {
                toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, true)
                bottomNavView.isVisible = false
            }

            State.HIDDEN -> {
                toolbar.menu.setGroupVisible(AMT_GROUP_MENU_ID, false)
                bottomNavView.isVisible = false
            }
        }
        return true
    }


    fun setMenuItemEnabled(@IdRes menuItemId: Int, enabled: Boolean) {
        bottomNavView.menu.findItem(menuItemId)?.isEnabled = enabled
        toolbar.menu.findItem(menuItemId)?.isEnabled = enabled
    }

    fun setHideMenuItem(@IdRes menuItemId: Int, isHidden: Boolean) {
        bottomNavView.menu.findItem(menuItemId)?.isVisible = !isHidden
        toolbar.menu.findItem(menuItemId)?.isVisible = !isHidden
    }


    fun clear(){
        bottomNavView.setOnItemSelectedListener(null)
        toolbar.setOnMenuItemClickListener(null)
        toolbar.menu.clear()
        bottomNavView.menu.clear()
        currentState = null
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