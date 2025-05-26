
@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("DrawerNavigation")

package dev.oneuiproject.oneui.navigation

import android.os.Bundle
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
import dev.oneuiproject.oneui.navigation.delegates.OnBackAppBarHandler
import dev.oneuiproject.oneui.navigation.widget.DrawerNavigationView
import kotlin.reflect.KClass

/**
 * Sets up navigation for [NavDrawerLayout] with a [DrawerNavigationView] and [NavHostFragment].
 * Supports both XML and programmatically created navigation graphs (NavGraphBuilder DSL).
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

    val onBackAppBarHandler = OnBackAppBarHandler(this, navController, configuration, toolbarBackThreshold).apply {
        this.startDestination = startDestination }

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

private inline fun onNavDestinationSelected(
    item: MenuItem,
    navController: NavController
): Boolean {
    val builder = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setRestoreState(true)

    val destinationNode = navController.currentDestination!!.parent!!.findNode(item.itemId)
    if (destinationNode !is ActivityNavigator.Destination
        && destinationNode !is FloatingWindow) {
        builder.setPopUpTo(navController.graph.findStartDestination().id, inclusive = false)
    }

    return try {
        navController.navigate(item.itemId, null, builder.build())
        true
    } catch (_: IllegalArgumentException) {
        false
    }
}
