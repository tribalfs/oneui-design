@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.Pools
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import androidx.appcompat.R as appcompatR

/**
 * A custom TabLayout that applies a bordered background and custom styling for tabs.
 *
 * This class extends [TabLayout] to provide a specific visual appearance introduced in
 * in One UI 8, featuring a bordered background and custom tab item layouts.
 *
 * Key features:
 * - Sets a predefined background drawable (`R.drawable.tab_layout_background`).
 * - Configures tab gravity to `GRAVITY_FILL` for even distribution of tabs.
 * - Automatically applies margins when attached to the window.
 * - Uses a pre-defined custom layout for tab items if a custom view
 *   is not already set for a tab.
 *
 * @param context The Context the view is running in, through which it can
 *                access the current theme, resources, etc.
 * @param attrs The attributes of the XML tag that is inflating the view.
 */
open class RoundedTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.tabStyle
) : TabLayout(context, attrs, defStyleAttr) {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var prevSelectedTabPosition: Int = -1
    private var customTabViewPool: Pools.Pool<View> = Pools.SimplePool(12)

    init {
        background =
            ContextCompat.getDrawable(context, R.drawable.oui_des_rounded_tab_layout_background)
        tabGravity = GRAVITY_FILL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateLayoutParams<MarginLayoutParams> {
            height = resources.getDimensionPixelSize(R.dimen.oui_des_rounded_tab_min_height)
        }
    }

    override fun newTab(): Tab {
        val tab = super.newTab()
        tab.parent = this
        tab.customView = getOrCreateCustomTabView(tab)
        if (tab.id != NO_ID) {
            tab.view.setId(tab.id)
        }
        return tab
    }

    override fun selectTab(tab: Tab?, updateIndicator: Boolean) {
        val selectedTab = tab ?: return
        val previouslySelectedTab = getTabAt(prevSelectedTabPosition)
        if (selectedTab == previouslySelectedTab) return

        (previouslySelectedTab?.tag as? TextView)?.let { updateTextStyle(it, false) }
        (selectedTab.tag as? TextView)?.let { updateTextStyle(it, true) }
        super.selectTab(selectedTab, updateIndicator)
    }

    override fun removeTabAt(position: Int) {
        val tabToRemove = getTabAt(position)
        super.removeTabAt(position)
        tabToRemove?.customView?.let { customView ->
            (tabToRemove.tag as? TextView)?.let { updateTextStyle(it, false) }
            customTabViewPool.release(customView)
        }
    }

    private fun updateTextStyle(textView: TextView, isSelected: Boolean) {
        @SuppressLint("PrivateResource")
        val styleResId =
            if (isSelected) appcompatR.style.RobotoMedium else appcompatR.style.RobotoRegular
        TextViewCompat.setTextAppearance(textView, styleResId)
    }

    @Deprecated("No effect in the view")
    override fun seslSetSubTabStyle() {
        //no op
    }

    /**
     * Helper to get or create the custom tab view.
     */
    private fun getOrCreateCustomTabView(tab: Tab): View {
        var tabView: View? = customTabViewPool.acquire()
        if (tabView == null) {
            tabView = layoutInflater.inflate(R.layout.oui_des_rounded_tab_item_layout, this, false)
        }
        val textView = tabView.findViewById<TextView>(android.R.id.text1)
        tab.tag = textView
        tabView!!.setFocusable(true)

        textView.text = tab.text ?: ""
        if (tab.contentDescription.isNullOrEmpty()) {
            tabView.setContentDescription(tab.text)
        } else {
            tabView.setContentDescription(tab.contentDescription)
        }
        return tabView
    }

}