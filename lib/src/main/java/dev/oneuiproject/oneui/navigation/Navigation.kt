
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DrawerNavigation")

package dev.oneuiproject.oneui.navigation

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.FloatRange
import androidx.navigation.ActivityNavigator
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
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.navigation.delegates.OnBackAppBarHandler
import dev.oneuiproject.oneui.navigation.widget.DrawerNavigationView
import dev.oneuiproject.oneui.widget.BottomTabLayout
import kotlin.reflect.KClass

/**
 * Sets up navigation for [NavDrawerLayout] with a [DrawerNavigationView] and [NavHostFragment].
 * Supports both XML and programmatically created navigation graphs (NavGraphBuilder DSL).
 *
 * @param drawerNavigationView The [DrawerNavigationView] containing the [navigation menu][dev.oneuiproject.oneui.navigation.widget.DrawerMenuView] to be linked with navigation actions.
 * @param navHostFragment The [NavHostFragment] that hosts the navigation graph and manages navigation transactions.
 * @param configuration Optional [AppBarConfiguration] to customize top-level destinations and drawer behavior.
 *                      By default, uses the menu from [drawerNavigationView] and this [NavDrawerLayout] as the drawer layout.
 * @param toolbarBackThreshold The threshold (from 0.0 to 1.0) at which the [NavDrawerLayout] toolbar switches its title, subtitle and navigation icon
 * to those of the back destination during predictive back animation progress. Defaults to 0.75f.
 *
 * By default, the back stack will be popped back to the navigation graph's start destination except
 * for Activity and FloatingWindow destinations and for menu items that have `android:menuCategory="secondary"`.
 *
 * Usage example:
 * ```
 * navDrawerLayout.setupNavigation(drawerNavigationView, navHostFragment)
 * ```
 *
 * @see DrawerNavigationView
 * @see NavHostFragment
 * @see AppBarConfiguration
 */
@JvmName("setup")
@JvmOverloads
fun NavDrawerLayout.setupNavigation(
    drawerNavigationView: DrawerNavigationView,
    navHostFragment: NavHostFragment,
    configuration: AppBarConfiguration = AppBarConfiguration(drawerNavigationView.getDrawerMenu(), this),
    @FloatRange(0.0, 1.0)
    toolbarBackThreshold: Float = 0.75f
) {
    val navController = navHostFragment.navController
    val startDestination = navController.graph.findStartDestination()

    val onBackAppBarHandler = OnBackAppBarHandler(this, navController, configuration, toolbarBackThreshold)
        .apply { this.startDestination = startDestination }

    navHostFragment.childFragmentManager.addOnBackStackChangedListener(onBackAppBarHandler)

    drawerNavigationView.setNavigationItemSelectedListener { item ->
        if (item.itemId == navController.currentDestination?.id) {
            setDrawerOpen(false, animate = true, ignoreOnNavRailMode = true)
            return@setNavigationItemSelectedListener false
        }

        onNavDestinationSelected(item, navController).also { handled ->
            if (handled) {
                val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
                if (destinationNode !is FloatingWindow
                    && destinationNode !is ActivityNavigator.Destination) {
                    if (isActionMode) endActionMode()
                    if (isSearchMode) endSearchMode()
                    setDrawerOpen(false, animate = true, ignoreOnNavRailMode = true)
                    closeNavRailOnBack = item.itemId == startDestination.id
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
fun NavDrawerLayout.setupNavigation(
    drawerNavigationView: DrawerNavigationView,
    navHostFragment: NavHostFragment,
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    configuration: AppBarConfiguration = AppBarConfiguration(drawerNavigationView.getDrawerMenu(), this),
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
    setupNavigation(drawerNavigationView, navHostFragment, configuration, toolbarBackThreshold)
}

private fun onNavDestinationSelected(
    item: MenuItem,
    navController: NavController
): Boolean {
    val builder = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setRestoreState(true)

    val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
    if (destinationNode !is ActivityNavigator.Destination
        && destinationNode !is FloatingWindow
        && item.order and Menu.CATEGORY_SECONDARY == 0) {
        builder.setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
    }

    return try {
        navController.navigate(item.itemId, null, builder.build())
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

        onNavDestinationSelected(item, navController).also { handled ->
            if (handled) {
                val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
                if (destinationNode !is FloatingWindow
                    && destinationNode !is ActivityNavigator.Destination) {
                    if (isActionMode) endActionMode()
                    if (isSearchMode) endSearchMode()
                    bottomTabLayout.setSelectedItem(item.itemId)
                }
            } else {
                navController.currentDestination?.let {
                    bottomTabLayout.setSelectedItem(it.id)
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