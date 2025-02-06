package dev.oneuiproject.oneui.navigation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuView
import androidx.core.content.res.use
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.android.material.R.styleable.NavigationView
import com.google.android.material.R.styleable.NavigationView_menu
import com.google.android.material.internal.NavigationMenu
import com.google.android.material.navigation.NavigationView
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils.getLayoutLocationInfo
import dev.oneuiproject.oneui.navigation.menu.DrawerMenuPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi")
class DrawerNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =  0
) : FrameLayout(context, attrs, defStyleAttr), MenuView {

    private var navigationItemSelectedListener: NavigationView.OnNavigationItemSelectedListener? = null
    private var mMenuPresenter: DrawerMenuPresenter
    private var mNavDrawerMenu: NavigationMenu
    @MenuRes
    private var mNavMenuRes: Int = 0
    private var lastTimeClicked = 0L

    private val drawerLayout by lazy(LazyThreadSafetyMode.NONE) {
        getLayoutLocationInfo().tblParent as DrawerLayout
    }

    init {
        context.theme
            .obtainStyledAttributes(attrs, NavigationView, 0, 0).use { a ->
                mNavMenuRes = a.getResourceId(NavigationView_menu, 0)
            }


        mNavDrawerMenu = NavigationMenu(context).apply {
            isGroupDividerEnabled = true
        }

        mMenuPresenter = DrawerMenuPresenter { (drawerLayout as? NavDrawerLayout)?.getNavRailSlideRange() ?: 1 }


        if (mNavMenuRes != 0) {
            inflateMenu(mNavMenuRes)
        }

        mNavDrawerMenu.setCallback(
            object : MenuBuilder.Callback {
                override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                    return isClickAllowed() && navigationItemSelectedListener?.onNavigationItemSelected(item) == true
                }

                override fun onMenuModeChange(menu: MenuBuilder) {}
            })

        mNavDrawerMenu.addMenuPresenter(mMenuPresenter)
        addView(mMenuPresenter.getMenuView(this))
    }

    //Workaround https://issuetracker.google.com/issues/340202276
    private fun isClickAllowed(): Boolean{
        System.currentTimeMillis().let {
            if (it - lastTimeClicked < 350)  return false
            lastTimeClicked = it
            return true
        }
    }

    private val drawerStateListener by lazy(LazyThreadSafetyMode.NONE) {
        DrawerLayout.DrawerStateListener {
            when (it){
                DrawerState.OPEN -> {
                    offsetUpdaterJob?.cancel()
                    mMenuPresenter.adapter!!.updateOffset(1f)
                }
                DrawerState.CLOSE-> {
                    offsetUpdaterJob?.cancel()
                    mMenuPresenter.adapter!!.updateOffset(0f)
                }

                DrawerState.CLOSING,
                DrawerState.OPENING -> {
                    startOffsetUpdater()
                }
            }
        }
    }

    private var offsetUpdaterJob: Job? = null
    private fun startOffsetUpdater(){
        if (offsetUpdaterJob?.isActive == true) return
        offsetUpdaterJob = CoroutineScope(Dispatchers.Main).launch {
            while(isActive) {
                mMenuPresenter.adapter!!.updateOffset(drawerLayout.drawerOffset)
                delay(20)
            }
        }
    }

    private fun getInitialOffset() =
        (drawerLayout as? NavDrawerLayout)?.isLargeScreenMode?.let {
            if (it) drawerLayout.drawerOffset else 1f
        } ?: 1f


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnLayout {
            (drawerLayout as? NavDrawerLayout)?.apply {
                if (isLargeScreenMode) setDrawerStateListener(drawerStateListener)
            }
            mMenuPresenter.adapter!!.updateOffset(getInitialOffset())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (drawerLayout as? NavDrawerLayout)?.setDrawerStateListener(null)
    }

    override fun initialize(menu: MenuBuilder) = Unit

    override fun getWindowAnimations(): Int = 0

    private fun inflateMenu(@MenuRes resId: Int) {
        mMenuPresenter.setUpdateSuspended(true)
        SupportMenuInflater(context).inflate(resId, mNavDrawerMenu)
        mMenuPresenter.setUpdateSuspended(false)
        mMenuPresenter.updateMenuView(false)

        if(isAttachedToWindow){
            doOnLayout { mMenuPresenter.adapter!!.updateOffset(getInitialOffset()) }
        }
    }

    /**
     * Set a listener that will be notified when a menu item is selected.
     *
     * @param listener The listener to notify
     */
    fun setNavigationItemSelectedListener(
        listener: NavigationView.OnNavigationItemSelectedListener?
    ) {
        this.navigationItemSelectedListener = listener
    }

    fun getDrawerMenu() = mNavDrawerMenu

    fun updateSelectedItem(destination: NavDestination){
        mNavDrawerMenu.forEach { item ->
            @Suppress("RestrictedApi")
            (item as MenuItemImpl).isChecked = destination.matchDestination(item.itemId)
        }
    }

    private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
        hierarchy.any { it.id == destId }

    companion object{
        private const val TAG = "DrawerNavigationView"
    }
}
