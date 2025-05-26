package dev.oneuiproject.oneui.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import dev.oneuiproject.oneui.ktx.setTabsEnabled
import dev.oneuiproject.oneui.utils.TabPagerMediator.Companion.NO_POSITION

/**
 * A utility class that extends the functionality of [TabLayoutMediator]
 * that synchronizes the interaction between a [TabLayout] and a [ViewPager2].
 * - Customize the scrolling behavior when selecting tabs versus swiping
 * between pages
 * - Enable or disable user interaction with both the [TabLayout] and [ViewPager2].
 * - Automatically attached and detached the [[TabLayoutMediator]] as needed based on the [LifecycleOwner]'s state.
 *
 * @param lifecycleOwner The [LifecycleOwner] whose lifecycle will be observed to manage the
 * attachment and detachment of the mediator.
 * @param tabLayout The [TabLayout] instance to be synchronized with the [ViewPager2].
 * @param viewPager2 The [ViewPager2] instance to be synchronized with the [TabLayout].
 * @param autoRefresh Indicates whether the mediator should automatically refresh when the
 * [ViewPager2]'s adapter changes. Defaults to `true`.
 * @param smoothScrollSwipe Determines if the [ViewPager2] should use smooth scrolling when
 * swiping between pages. Defaults to `true`.
 * @param smoothScrollSelect Determines if the [ViewPager2] should use smooth scrolling when
 * a tab is selected. Defaults to `false`, disabling smooth scrolling for tab selection.
 * @param tabConfigurationCallback The [TabConfigurationStrategy] in which you set the text of the tab,
 * and/or perform any styling of the tabs that you require.
 *
 * Sample usage:
 * ```
 * class TabPagerFragment: Fragment() {
 *     fun initTabbedViewPager(){
 *          val titles = listOf("Icons", "Contacts", "Apps")
 *          val tabPagerMediator = TabPagerMediator(
 *              lifecycleOwner = this,
 *              tabLayout = binding.tabs,
 *              viewPager2 = binding.vp2
 *          ) { tab, pos ->  tab.text = titles[pos] }
 *      }
 *  }
 * ```
 */
class TabPagerMediator(
    lifecycleOwner: LifecycleOwner,
    private var tabLayout: TabLayout,
    private var viewPager2: ViewPager2,
    autoRefresh: Boolean = true,
    smoothScrollSwipe: Boolean = true,
    private var smoothScrollSelect: Boolean = false,
    tabConfigurationCallback: TabConfigurationStrategy
) {
    companion object {
        const val NO_POSITION = -1
    }

    private var tabLayoutMediator = TabLayoutMediator(
        tabLayout, viewPager2, autoRefresh,
        smoothScrollSwipe, tabConfigurationCallback
    )

    private var isCustomTabSelectedListenerAdded = false

    init {
        val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!smoothScrollSelect) {
                    viewPager2.setCurrentItem(tab.position, false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        }

        val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (smoothScrollSelect) return
                when (state) {
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        if (!isCustomTabSelectedListenerAdded) {
                            tabLayout.addOnTabSelectedListener(onTabSelectedListener)
                            isCustomTabSelectedListenerAdded = true
                        }
                    }

                    else -> { // SCROLL_STATE_DRAGGING or SCROLL_STATE_SETTLING
                        if (isCustomTabSelectedListenerAdded) {
                            tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
                            isCustomTabSelectedListenerAdded = false
                        }
                    }
                }
            }
        }

        val lifeCycleObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                if (isCustomTabSelectedListenerAdded) {
                    tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
                    isCustomTabSelectedListenerAdded = false
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                if (!tabLayoutMediator.isAttached && viewPager2.adapter != null) {
                    tabLayoutMediator.attach()
                }

                if (!smoothScrollSelect) {
                    viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
                    if (viewPager2.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                        tabLayout.addOnTabSelectedListener(onTabSelectedListener)
                        isCustomTabSelectedListenerAdded = true
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                if (tabLayoutMediator.isAttached) tabLayoutMediator.detach()

                if (!smoothScrollSelect) {
                    if (isCustomTabSelectedListenerAdded) {
                        tabLayout.removeOnTabSelectedListener(onTabSelectedListener)
                        isCustomTabSelectedListenerAdded = false
                    }
                    viewPager2.unregisterOnPageChangeCallback(onPageChangeCallback)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifeCycleObserver)
    }

    /**
     * @return The position of the current selected tab or [NO_POSITION] when nothing is selected
     *
     * @see TabLayout.getSelectedTabPosition
     */
    val selectedPosition
        get() = tabLayout.selectedTabPosition

    fun replaceAdapter(vpAdapter: RecyclerView.Adapter<*>) {
        if (viewPager2.adapter == vpAdapter && tabLayoutMediator.isAttached) {
            return
        }
        val wasAttached = tabLayoutMediator.isAttached
        if (wasAttached) tabLayoutMediator.detach()
        viewPager2.adapter = vpAdapter
        if (wasAttached) tabLayoutMediator.attach()
    }

    /**
     * Enables or disables the interaction with the [TabLayout] and [ViewPager2].
     *
     * @param enabled If `true`, enables interaction; if `false`, disables interaction.
     */
    fun setInteractionEnabled(enabled: Boolean) {
        tabLayout.setTabsEnabled(enabled)
        viewPager2.isUserInputEnabled = enabled
    }
}
