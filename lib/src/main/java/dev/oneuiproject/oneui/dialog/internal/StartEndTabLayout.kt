@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog.internal

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import java.util.Locale

class StartEndTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.style.OneUI_StartEndTimePickerTab
) : TabLayout(
    context, attrs, defStyleAttr
) {
    private var mTabIndex = 0
    private var mTimeFormatter: ((Int) -> String)? = null
    @JvmField
    val times: IntArray = IntArray(2)

    interface OnTabSelectedListener {
        fun onPreTabSelected()
        fun onTabSelected(index: Int, time: Int)
    }

    init {
        seslSetSubTabStyle()
    }

    fun init(
        startTime: Int,
        endTime: Int,
        onTabSelectedListener: OnTabSelectedListener,
        timeFormatter: ((Int) -> String)?
    ) {
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: Tab) {}
            override fun onTabUnselected(tab: Tab) {}
            override fun onTabSelected(tab: Tab) {
                onTabSelectedListener.onPreTabSelected()
                mTabIndex = tab.position
                onTabSelectedListener.onTabSelected(mTabIndex, times[mTabIndex])
            }
        })
        times[0] = startTime
        times[1] = endTime
        mTimeFormatter = timeFormatter
        reload()
        val tab = getTabAt(mTabIndex)
        if (tab!!.isSelected) {
            onTabSelectedListener.onTabSelected(mTabIndex, times[mTabIndex])
            return
        }
        tab.select()
    }

    fun updateTime(time: Int) {
        updateTime(mTabIndex, time)
    }

    fun select(index: Int) {
        val tabAt = getTabAt(index)
        tabAt!!.select()
    }

    fun reload() {
        for (i in times.indices) {
            updateTime(i, times[i])
        }
    }

    private fun updateTime(index: Int, time: Int) {
        times[index] = time
        val tabAt = getTabAt(index)
        tabAt!!.seslSetSubText(mTimeFormatter?.invoke(time) ?: defaultFormatter(time))
    }

    private fun defaultFormatter(time: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60)
    }
}