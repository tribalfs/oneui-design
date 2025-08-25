
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DrawerNavigation")

package dev.oneuiproject.oneui.navigation

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.FloatRange
import androidx.navigation.ActivityNavigator
import androidx.navigation.ActivityNavigator.Destination
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.reflect.DeviceInfo
import androidx.savedstate.SavedState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.ktx.semSetPopOverOptions
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.navigation.delegates.OnBackAppBarHandler
import dev.oneuiproject.oneui.navigation.widget.DrawerNavigationView
import dev.oneuiproject.oneui.popover.PopOverOptions
import dev.oneuiproject.oneui.widget.BottomTabLayout
import kotlin.reflect.KClass

private const val EXTRA_NAV_SOURCE = "android-support-navigation:ActivityNavigator:source"
private const val EXTRA_NAV_CURRENT = "android-support-navigation:ActivityNavigator:current"
private const val EXTRA_POP_ENTER_ANIM = "android-support-navigation:ActivityNavigator:popEnterAnim"
private const val EXTRA_POP_EXIT_ANIM = "android-support-navigation:ActivityNavigator:popExitAnim"

/**
 * Sets up navigation for [DrawerLayout] with a [DrawerNavigationView] and [NavHostFragment].
 * Supports both XML and programmatically created navigation graphs (NavGraphBuilder DSL).
 *
 * @param drawerNavigationView The [DrawerNavigationView] containing the [navigation menu][dev.oneuiproject.oneui.navigation.widget.DrawerMenuView] to be linked with navigation actions.
 * @param navHostFragment The [NavHostFragment] that hosts the navigation graph and manages navigation transactions.
 * @param configuration Optional [AppBarConfiguration] to customize top-level destinations and drawer behavior.
 *                      By default, uses the menu from [drawerNavigationView] and this [DrawerLayout] as the drawer layout.
 * @param toolbarBackThreshold The threshold (from 0.0 to 1.0) at which the [DrawerLayout] toolbar switches its title, subtitle and navigation icon
 * to those of the back destination during predictive back animation progress. Defaults to 0.75f.
 * @param lockWithDrawer Whether to update the lock state of the [DrawerNavigationView] when the drawer's lock state changes. Defaults to true.
 *
 * By default, the back stack will be popped back to the navigation graph's start destination except
 * for Activity and FloatingWindow destinations and for menu items that have `android:menuCategory="secondary"`.
 *
 * Usage example:
 * ```
 * drawerLayout.setupNavigation(drawerNavigationView, navHostFragment)
 * ```
 *
 * @see DrawerNavigationView
 * @see NavHostFragment
 * @see AppBarConfiguration
 */
@JvmName("setup")
@JvmOverloads
fun <T: DrawerLayout> T.setupNavigation(
    drawerNavigationView: DrawerNavigationView,
    navHostFragment: NavHostFragment,
    configuration: AppBarConfiguration = AppBarConfiguration(drawerNavigationView.getDrawerMenu(), this),
    @FloatRange(0.0, 1.0)
    toolbarBackThreshold: Float = 0.75f,
    lockWithDrawer: Boolean = true
) {

    if (lockWithDrawer) {
        setDrawerLockListener { isLocked ->
            drawerNavigationView.updateLock(isLocked)
        }
    }

    val navController = navHostFragment.navController
    val startDestination = navController.graph.findStartDestination()

    val onBackAppBarHandler = OnBackAppBarHandler(this, navController, configuration, toolbarBackThreshold)
        .apply { this.startDestination = startDestination }

    navHostFragment.childFragmentManager.addOnBackStackChangedListener(onBackAppBarHandler)

    drawerNavigationView.setNavigationItemSelectedListener { item ->
        if (item.itemId == navController.currentDestination?.id) {
            if (this is NavDrawerLayout) {
                setDrawerOpen(false, animate = true, ignoreOnNavRailMode = true)
            } else {
                setDrawerOpen(false, animate = true)
            }
            return@setNavigationItemSelectedListener false
        }

        onDestinationSelected(item, navController).also { handled ->
            if (handled) {
                val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
                if (destinationNode !is FloatingWindow
                    && destinationNode !is ActivityNavigator.Destination) {
                    if (isActionMode) endActionMode()
                    if (isSearchMode) endSearchMode()
                    if (this is NavDrawerLayout) {
                        setDrawerOpen(false, animate = true, ignoreOnNavRailMode = true)
                        closeNavRailOnBack = item.itemId == startDestination.id
                    } else {
                        setDrawerOpen(false, animate = true)
                    }
                }
            } else {
                navController.currentDestination?.let {
                    drawerNavigationView.updateSelectedItem(it)
                }
            }
        }
    }

    navController.addOnDestinationChangedListener(
        object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                if (destination is FloatingWindow) return
                drawerNavigationView.updateSelectedItem(destination)
                onBackAppBarHandler.onDestinationChanged(destination)
            }
        }
    )
}

/**
 * Overload for programmatic navigation graph creation using NavGraphBuilder DSL.
 *
 * Example usage:
 * ```
 * navDrawerLayout.setupNavigation(
 *     drawerNavigationView,
 *     navHostFragment,
 *     startDestination = "widgets_dest"
 * ) {
 *     mainNavGraph()
 * }
 * ```
 */
@JvmName("setupDsl")
@JvmOverloads
fun <T: DrawerLayout> T.setupNavigation(
    drawerNavigationView: DrawerNavigationView,
    navHostFragment: NavHostFragment,
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    configuration: AppBarConfiguration = AppBarConfiguration(drawerNavigationView.getDrawerMenu(), this),
    @FloatRange(0.0, 1.0)
    toolbarBackThreshold: Float = 0.75f,
    lockWithDrawer: Boolean = true,
    builder: NavGraphBuilder.() -> Unit
) {
    val navController = navHostFragment.navController
    val graph: NavGraph = navController.createGraph(
        startDestination = startDestination,
        route = route,
        builder = builder
    )
    navController.graph = graph
    setupNavigation(drawerNavigationView, navHostFragment, configuration, toolbarBackThreshold)
}

private fun onDestinationSelected(
    item: MenuItem,
    navController: NavController
): Boolean {
    val builder = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setRestoreState(true)

    val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
    val isDestinationActivity = destinationNode is ActivityNavigator.Destination
    if (!isDestinationActivity
        && destinationNode !is FloatingWindow
        && item.order and Menu.CATEGORY_SECONDARY == 0) {
        builder.setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
    }

    return try {
        if (isDestinationActivity) {
            navController.navigateToActivity(destinationNode, null, builder.build())
        } else {
            navController.navigate(item.itemId, null, builder.build())
        }
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}

/**
 * Sets up navigation for [ToolbarLayout] with a [BottomTabLayout] and [NavHostFragment].
 * Supports both XML and programmatically created navigation graphs (NavGraphBuilder DSL).
 *
 * @param bottomTabLayout The [BottomTabLayout] to be linked with navigation actions.
 * @param navHostFragment The [NavHostFragment] that hosts the navigation graph and manages navigation transactions.
 * @param configuration Optional [AppBarConfiguration] to customize top-level destinations and drawer behavior.
 *                      By default, uses the menu from the [bottomTabLayout].
 * @param toolbarBackThreshold The threshold (from 0.0 to 1.0) at which the [ToolbarLayout] toolbar switches its title, subtitle and navigation icon
 * to those of the back destination during predictive back animation progress. Defaults to 0.75f.
 *
 * By default, the back stack will be popped back to the navigation graph's start destination except
 * for Activity and FloatingWindow destinations and for menu items that have `android:menuCategory="secondary"`.
 *
 * Usage example:
 * ```
 * toolbarLayout.setupNavigation(bottomTabLayout, navHostFragment)
 * ```
 *
 * @see NavHostFragment
 * @see AppBarConfiguration
 */
@JvmName("setup")
@JvmOverloads
fun <T: ToolbarLayout>T.setupNavigation(
    bottomTabLayout: BottomTabLayout,
    navHostFragment: NavHostFragment,
    configuration: AppBarConfiguration = AppBarConfiguration(bottomTabLayout.menu, null),
    @FloatRange(0.0, 1.0)
    toolbarBackThreshold: Float = 0.75f
) {
    val navController = navHostFragment.navController
    val startDestination = navController.graph.findStartDestination()

    val onBackAppBarHandler = OnBackAppBarHandler(this, navController, configuration, toolbarBackThreshold)
        .apply { this.startDestination = startDestination }

    navHostFragment.childFragmentManager.addOnBackStackChangedListener(onBackAppBarHandler)

    bottomTabLayout.setOnMenuItemClickListener { item ->
        if (item.itemId == navController.currentDestination?.id) {
            return@setOnMenuItemClickListener true
        }

        onDestinationSelected(item, navController).also { handled ->
            if (handled) {
                val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
                if (destinationNode !is FloatingWindow
                    && destinationNode !is ActivityNavigator.Destination) {
                    if (isActionMode) endActionMode()
                    if (isSearchMode) endSearchMode()
                    bottomTabLayout.setSelectedItem(item.itemId)
                    return@also
                }
            }
            navController.currentDestination?.let {
                bottomTabLayout.setSelectedItem(it.id)
            }
        }
    }

    navController.addOnDestinationChangedListener(
        object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                if (destination is FloatingWindow) return
                bottomTabLayout.setSelectedItem(destination.id)
                onBackAppBarHandler.onDestinationChanged(destination)
            }
        }
    )
}

/**
 * Overload for programmatic navigation graph creation using NavGraphBuilder DSL.
 *
 * Example usage:
 * ```
 * toolbarLayout.setupNavigation(
 *     bottomTabLayout,
 *     navHostFragment,
 *     startDestination = "widgets_dest"
 * ) {
 *     mainNavGraph()
 * }
 * ```
 */
@JvmName("setupDsl")
@JvmOverloads
fun <T: ToolbarLayout>T.setupNavigation(
    bottomTabLayout: BottomTabLayout,
    navHostFragment: NavHostFragment,
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    configuration: AppBarConfiguration = AppBarConfiguration(bottomTabLayout.menu, null),
    @FloatRange(0.0, 1.0)
    toolbarBackThreshold: Float = 0.75f,
    builder: NavGraphBuilder.() -> Unit
) {
    val navController = navHostFragment.navController
    val graph: NavGraph = navController.createGraph(
        startDestination = startDestination,
        route = route,
        builder = builder
    )
    navController.graph = graph
    setupNavigation(bottomTabLayout, navHostFragment, configuration, toolbarBackThreshold)
}


private fun NavController.navigateToActivity(
    destination: Destination,
    args: SavedState?,
    navOptions: NavOptions?
): NavDestination? {
    checkNotNull(destination.intent) {
        ("Destination ${destination.id} does not have an Intent set.")
    }
    val intent = Intent(destination.intent)

    val hostActivity = context.activity

    if (hostActivity == null) {
        // If we're not launching from an Activity context we have to launch in a new task.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (navOptions != null && navOptions.shouldLaunchSingleTop()) {
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    if (hostActivity != null) {
        val hostIntent = hostActivity.intent
        if (hostIntent != null) {
            val hostCurrentId = hostIntent.getIntExtra(EXTRA_NAV_CURRENT, 0)
            if (hostCurrentId != 0) {
                intent.putExtra(EXTRA_NAV_SOURCE, hostCurrentId)
            }
        }
    }
    val destId = destination.id
    intent.putExtra(EXTRA_NAV_CURRENT, destId)
    val resources = context.resources
    if (navOptions != null) {
        val popEnterAnim = navOptions.popEnterAnim
        val popExitAnim = navOptions.popExitAnim
        if (
            popEnterAnim > 0 && resources.getResourceTypeName(popEnterAnim) == "animator" ||
            popExitAnim > 0 && resources.getResourceTypeName(popExitAnim) == "animator"
        ) {
            Log.w(
                "Navigation",
                "Activity destinations do not support Animator resource. Ignoring " +
                        "popEnter resource ${resources.getResourceName(popEnterAnim)} and " +
                        "popExit resource ${resources.getResourceName(popExitAnim)} when " +
                        "launching $destination"
            )
        } else {
            // For use in applyPopAnimationsToPendingTransition()
            intent.putExtra(EXTRA_POP_ENTER_ANIM, popEnterAnim)
            intent.putExtra(EXTRA_POP_EXIT_ANIM, popExitAnim)
        }
    }
    if (hostActivity != null && DeviceInfo.isOneUI()) {
        // We're not allowed to use ActivityOptions for non-activity context
        val navArgIsPopOver = context.getString(R.string.navArg_isPopOver)
        val isPopOverActivity = destination.arguments[navArgIsPopOver]?.defaultValue == true
        if (isPopOverActivity) {
            val popoverOptions = PopOverOptions.centerAnchored(context)
            @SuppressLint("NewApi")//OneUI starts with API 28+
            val activityOptions = ActivityOptions.makeBasic().apply {
                semSetPopOverOptions(
                    popoverOptions.popOverSize.getWidthArray(),
                    popoverOptions.popOverSize.getHeightArray(),
                    popoverOptions.anchor.getPointArray(),
                    popoverOptions.anchorPositions.getFlagArray()
                )
            }
            context.startActivity(intent, activityOptions.toBundle())
        } else {
            context.startActivity(intent)
        }
    }else {
        context.startActivity(intent)
    }

    if (navOptions != null && hostActivity != null) {
        var enterAnim = navOptions.enterAnim
        var exitAnim = navOptions.exitAnim
        if (
            enterAnim > 0 && (resources.getResourceTypeName(enterAnim) == "animator") ||
            exitAnim > 0 && (resources.getResourceTypeName(exitAnim) == "animator")
        ) {
            Log.w(
                "Navigation",
                "Activity destinations do not support Animator resource. " +
                        "Ignoring " +
                        "enter resource " +
                        resources.getResourceName(enterAnim) +
                        " and exit resource " +
                        resources.getResourceName(exitAnim) +
                        "when " +
                        "launching " +
                        destination
            )
        } else if (enterAnim >= 0 || exitAnim >= 0) {
            enterAnim = enterAnim.coerceAtLeast(0)
            exitAnim = exitAnim.coerceAtLeast(0)
            @Suppress("DEPRECATION")
            hostActivity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    // You can't pop the back stack from the caller of a new Activity,
    // so we don't add this navigator to the controller's back stack
    return null
}
