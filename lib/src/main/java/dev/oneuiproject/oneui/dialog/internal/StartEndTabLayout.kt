@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.oneuiproject.oneui.dialog.internal

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import com.google.android.material.tabs.TabLayout
import dev.oneuiproject.oneui.design.R
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY)
class StartEndTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.style.OneUI_StartEndTimePickerTab
) : TabLayout(
    context, attrs, defStyleAttr
) {
    private var tabIndex = 0
    private var timeFormatter: ((Int) -> String)? = null
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
                tabIndex = tab.position
                onTabSelectedListener.onTabSelected(tabIndex, times[tabIndex])
            }
        })
        times[0] = startTime
        times[1] = endTime
        this@StartEndTabLayout.timeFormatter = timeFormatter
        reload()
        val tab = getTabAt(tabIndex)
        if (tab!!.isSelected) {
            onTabSelectedListener.onTabSelected(tabIndex, times[tabIndex])
            return
        }
        tab.select()
    }

    fun updateTime(time: Int) {
        updateTime(tabIndex, time)
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

    var showSubText: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value){
                val startTime = times[0]
                val endTime = times[1]
                getTabAt(0)!!.seslSetSubText(timeFormatter?.invoke(startTime) ?: defaultFormatter(startTime))
                getTabAt(1)!!.seslSetSubText(timeFormatter?.invoke(endTime) ?: defaultFormatter(endTime))
            }else{
                getTabAt(0)!!.seslSetSubText("")
                getTabAt(1)!!.seslSetSubText("")
            }
        }


    private fun updateTime(index: Int, time: Int) {
        times[index] = time
        if (!showSubText) return
        val tabAt = getTabAt(index)
        tabAt!!.seslSetSubText(timeFormatter?.invoke(time) ?: defaultFormatter(time))
    }

    private fun defaultFormatter(time: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60)
    }
}