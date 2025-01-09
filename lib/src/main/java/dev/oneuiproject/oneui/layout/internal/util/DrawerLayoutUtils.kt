@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.R
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.utils.badgeCountToText

object DrawerLayoutUtils {

    internal fun TextView.updateBadgeView(badge: Badge, marginOffset: Int = 0){
        val res = resources
        val badgeHeight: Int
        val badgeWidth: Int
        val badgeMargin: Int

        when(badge){
            Badge.NONE -> {
                isGone = true
                text = null
                return
            }
            Badge.DOT  -> {
                text = null
                background = AppCompatResources.getDrawable(context, R.drawable.sesl_dot_badge)
                badgeHeight = res.getDimensionPixelSize(R.dimen.sesl_menu_item_badge_size)
                badgeWidth = badgeHeight
                badgeMargin = (8 + marginOffset).dpToPx(res)
            }
            is Badge.NUMERIC -> {
                text = badge.count.badgeCountToText()!!
                background = AppCompatResources.getDrawable(context, R.drawable.sesl_noti_badge)
                val defaultWidth = res.getDimensionPixelSize(R.dimen.sesl_badge_default_width)
                val additionalWidth = res.getDimensionPixelSize(R.dimen.sesl_badge_additional_width)
                badgeHeight = defaultWidth + additionalWidth
                badgeWidth = defaultWidth + additionalWidth * text.length
                badgeMargin = (6 + marginOffset).dpToPx(res)
            }
        }
        updateLayoutParams<MarginLayoutParams> {
            width = badgeWidth
            height = badgeHeight
            topMargin = badgeMargin
            marginEnd = badgeMargin
        }
        isVisible = true
    }

    inline fun getDrawerStateUpdate(previousOffset: Float, currentOffset: Float) =
        when (currentOffset) {
            1f -> DrawerState.OPEN
            0f -> DrawerState.CLOSE
            else -> {
                when {
                    currentOffset > previousOffset -> DrawerState.OPENING
                    else -> DrawerState.CLOSING
                }
            }
        }

}