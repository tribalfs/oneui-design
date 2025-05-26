@file:Suppress("NOTHING_TO_INLINE", "ClickableViewAccessibility")

package dev.oneuiproject.oneui.ktx

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.utils.badgeCountToText

/**
 * Selects the tab at the specified index.
 *
 * @param index The index of the tab to be selected.
 */
@JvmOverloads
inline fun <T: TabLayout>T.selectTabAt(index: Int, updateIndicator: Boolean = true){
    getTabAt(index)?.let { selectTab(it, updateIndicator) }
        ?: Log.e(this::class.simpleName, "selectTabAt($index): This tabLayout has no tab at index $index")
}

/**
 * Adds a tab to this TabLayout.
 *
 * @param tabTitleRes The resource id of the string to set as title for the tab.
 * @param tabIconRes The resource id of the drawable to display on the tab.
 * @param listener [View.OnClickListener] to be set to the tab.
 *
 * @return The added [TabLayout.Tab] instance for further configuration, if needed.
 *
 * Example usage:
 * ```kotlin
 * val tab = tabLayout.addTab(
 *             tabTitleRes = R.string.tab_title,
 *             tabIconRes = R.drawable.tab_icon) {view ->
 *               // Handle tab click
 *            }
 * ```
 */
@JvmOverloads
inline fun <T: TabLayout>T.addTab(
    @StringRes tabTitleRes: Int?,
    @DrawableRes tabIconRes: Int? = null,
    listener: View.OnClickListener? = null
): TabLayout.Tab {
    return addTab(
        tabTitle = tabTitleRes?.let {context!!.getString(it) },
        null,
        listener
    ).apply {
        tabIconRes?.let {
            icon = AppCompatResources.getDrawable(context, it)
        }
    }
}


/**
 * Adds a tab to this TabLayout.
 *
 * @param tabTitle The title text to display on the tab.
 * @param tabIcon The drawable to display as the tab's icon.
 * @param listener [View.OnClickListener] to be set to the tab.
 *
 * @return The added [TabLayout.Tab] instance for further configuration, if needed.
 *
 * Example usage:
 * ```kotlin
 * val tab = tabLayout.addTab("Tab Name", isSelected = true) { view ->
 *     // Handle tab click
 * }
 * ```
 */
@JvmOverloads
inline fun <T: TabLayout>T.addTab(
    tabTitle: CharSequence?,
    tabIcon: Drawable? = null,
    listener: View.OnClickListener? = null,
): TabLayout.Tab {
    return newTab().apply {
        text = tabTitle
        icon = tabIcon
        addTab(this)
        //Call after added
        getTabView(position)?.apply {
            setOnClickListener(listener)
        }
    }
}

/**
 * Adds a custom tab to this TabLayout. A custom tab has no selected state.
 *
 * @param tabTitleRes The resource id of the string to set as title for the tab.
 * @param tabIconRes The resource id of the drawable to display on the tab.
 * @param listener [View.OnClickListener] to be invoked when the tab is clicked.
 *
 * @return The added [TabLayout.Tab] instance for further configuration, if needed.
 *
 * Example usage:
 * ```kotlin
 * val tab = tabLayout.addCustomTab(
 *             tabTitleRes = R.string.tab_title,
 *             tabIconRes = R.drawable.tab_icon) {view ->
 *               // Handle tab click
 *            }
 * ```
 */
inline fun <T: TabLayout>T.addCustomTab(
    @StringRes tabTitleRes: Int?,
    @DrawableRes tabIconRes: Int?,
    listener: View.OnClickListener
): TabLayout.Tab = addCustomTab(
    tabTitle = tabTitleRes?.let { context!!.getString(it) },
    tabIcon = tabIconRes?.let { AppCompatResources.getDrawable(context, it) },
    listener = listener
)


/**
 * Adds a custom tab to this TabLayout. A custom tab has no selected state.
 *
 * @param tabTitle The title to display on the tab.
 * @param tabIcon (Optional) drawable object to display as the tab's icon.
 * @param listener [View.OnClickListener] to be invoked when the tab is clicked.
 *
 * @return The added [TabLayout.Tab] instance for further configuration, if needed.
 *
 * Example usage:
 * ```kotlin
 * val tab = tabLayout.addCustomTab(tabTitle = "Tab 1", tabIcon = null) {view ->
 *               // Handle tab click
 *           }
 * ```
 */
inline fun <T: TabLayout>T.addCustomTab(
    tabTitle: CharSequence?,
    tabIcon: Drawable?,
    listener: View.OnClickListener,
): TabLayout.Tab {
    val newTab = newTab().apply {
        text = tabTitle
        icon = tabIcon
        addTab(this)
    }

    getTabView(newTab.position)!!.apply {
        background = AppCompatResources.getDrawable(context, R.drawable.oui_tab_layout_custom_button_background)
        setOnTouchListener { v, event ->
            val selectedTabPos = selectedTabPosition
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    listener.onClick(v)
                    if (selectedTabPosition != selectedTabPos) {
                        this@addCustomTab.selectTabAt(selectedTabPos)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    if (selectedTabPosition != selectedTabPos) {
                        this@addCustomTab.selectTabAt(selectedTabPos)
                    }
                }
            }
            true
        }
    }
    return newTab
}

/**
 * Retrieves the ViewGroup containing the tab views within this TabLayout.
 *
 * @return The ViewGroup containing the tab views, or null if not found or empty.
 */
inline val <T: TabLayout>T.tabViewGroup: ViewGroup?
    get() = if (childCount <= 0) null else getChildAt(0) as? ViewGroup

/**
 * Retrieves the view for the tab at the specified position.
 *
 * @param position The position of the tab whose view is to be retrieved.
 * @return The View corresponding to the tab at the specified position, or null if not found.
 */
inline fun <T: TabLayout>T.getTabView(position: Int): View? {
    val viewGroup = tabViewGroup
    return if (viewGroup != null && viewGroup.childCount > position) {
        viewGroup.getChildAt(position)
    } else null
}


/**
 * Enables or disables all or specified tabs.
 *
 * @param enabled `true` to enable the tabs, `false` to disable them.
 * @param tabIndex Optional. Array of zero-based indices representing the tabs to enable/disable.
 * If not provided, all tabs are affected.
 *
 * Example Usage:
 * ```kotlin
 * // Disable all tabs:
 * myTabLayout.setTabsEnabled(false)
 *
 * // Disable the tab at index 1:
 * myTabLayout.setTabsEnabled(false, 1)
 *
 * // Enable tabs at index 0 and 2:
 * myTabLayout.setTabsEnabled(true, 0, 2)
 * ```
 */
inline fun <T: TabLayout>T.setTabsEnabled(enabled: Boolean, vararg tabIndex: Int) {
    tabViewGroup?.apply {
        for (i in 0 until tabCount) {
            if (tabIndex.isEmpty() || i in tabIndex) {
                getChildAt(i)?.apply {
                    isEnabled = enabled
                    alpha = if (enabled) 1.0f else 0.4f
                }
            }
        }
    }
}


@JvmName("setTabBadge")
inline fun <T: TabLayout>T.setBadge(tabIndex: Int, badge: Badge){
    if (getTabAt(tabIndex) == null) {
        Log.e(this::class.simpleName, "setBadge($tabIndex, $badge): This tabLayout currently has no tab at index $tabIndex")
        return
    }
    when (badge){
        Badge.DOT -> seslShowDotBadge(tabIndex, true)
        Badge.NONE -> {
            seslShowDotBadge(tabIndex, false)
            seslShowBadge(tabIndex, false, null)
        }
        is Badge.NUMERIC -> seslShowBadge(tabIndex, true, badge.count.badgeCountToText())
    }
}

@JvmName("clearTabBadge")
inline fun <T: TabLayout>T.clearBadge(tabIndex: Int) = setBadge(tabIndex, Badge.NONE)


@JvmName("setTabBadge")
inline fun <T: TabLayout.Tab>T.setBadge(badge: Badge) =parent?.setBadge(position, badge)

@JvmName("clearTabBadge")
inline fun <T: TabLayout.Tab>T.clearBadge() = parent?.clearBadge(position)


