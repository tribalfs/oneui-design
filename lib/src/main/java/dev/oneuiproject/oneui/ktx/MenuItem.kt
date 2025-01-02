@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.util.Log
import android.view.MenuItem
import androidx.appcompat.view.menu.SeslMenuItem
import dev.oneuiproject.oneui.layout.Badge

inline fun MenuItem.setBadge(badge: Badge){
    (this as? SeslMenuItem)?.setBadge(badge)
        ?: Log.e("MenuItem", "setBadge is invoked on a menu item that is not an implementation of SeslMenuItem")
}

inline fun MenuItem.clearBadge() = setBadge(Badge.NONE)

inline fun SeslMenuItem.clearBadge() = setBadge(Badge.NONE)

inline fun SeslMenuItem.setBadge(badge: Badge){
    badgeText = badge.toBadgeText()
}
