package dev.oneuiproject.oneui.layout.internal.util

import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.utils.badgeCountToText
import androidx.appcompat.R as appcompatR

internal fun TextView.updateBadge(badge: Badge){
    val res = resources
    when(badge){
        Badge.DOT -> {
            val badgeSize = res.getDimensionPixelSize(appcompatR.dimen.sesl_menu_item_badge_size)
            val badgeMargin = 8.dpToPx(res)
            background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_dot_badge)
            text = null
            updateLayoutParams<MarginLayoutParams> {
                width = badgeSize
                height = badgeSize
                marginEnd = badgeMargin
                topMargin = badgeMargin
            }
            isVisible = true
        }
        is Badge.NUMERIC -> {
            val badgeText = badge.count.badgeCountToText()!!
            val defaultWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_default_width)
            val additionalWidth = res.getDimensionPixelSize(appcompatR.dimen.sesl_badge_additional_width)
            val badgeMargin = 6.dpToPx(res)
            background = AppCompatResources.getDrawable(context, appcompatR.drawable.sesl_noti_badge)
            text = badgeText
            updateLayoutParams<MarginLayoutParams> {
                width = defaultWidth + additionalWidth * badgeText.length
                height = defaultWidth + additionalWidth
                marginEnd = badgeMargin
                topMargin = badgeMargin
            }
            isVisible = true
        }
        Badge.NONE -> {
            isGone = true
            text = null
        }

    }
}