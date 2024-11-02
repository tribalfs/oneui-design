@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.view.MenuItem
import androidx.appcompat.view.menu.SeslMenuItem
import dev.oneuiproject.oneui.layout.Badge

inline fun MenuItem.setBadge(badge: Badge){
    (this as? SeslMenuItem)?.badgeText = badge.toBadgeText()
}

inline fun MenuItem.clearBadge() = setBadge(Badge.NONE)