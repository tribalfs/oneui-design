@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.R as appcompatR
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState

object DrawerLayoutUtils {

    internal fun TextView.updateBadgeView(badge: Badge, marginOffset: Int = 0){

        val badgeText = badge.toBadgeText()
        if (badgeText == getTag(R.id.tag_badge_text)) return
        setTag(R.id.tag_badge_text, badgeText)
        text = badgeText

        val res = resources
        val badgeHeight: Int
        val badgeWidth: Int
        val badgeMargin: Int

        when(badge){
            Badge.NONE -> {
                isGone = true
                return
            }
            Badge.DOT  -> {
                background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_dot_badge)
                badgeHeight = res.getDimensionPixelSize(appcompatR.dimen.sesl_menu_item_badge_size)
                badgeWidth = badgeHeight
                badgeMargin = (11 + marginOffset).dpToPx(res)
            }
            is Badge.NUMERIC -> {
                background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_noti_badge)
                val defaultWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_default_width)
                val additionalWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_additional_width)
                badgeHeight = defaultWidth + additionalWidth
                badgeWidth = defaultWidth + additionalWidth * text.length
                badgeMargin = (9 + marginOffset).dpToPx(res)
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