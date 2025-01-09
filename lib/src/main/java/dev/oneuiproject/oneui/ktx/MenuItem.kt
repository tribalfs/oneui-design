@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.ktx

import android.util.Log
import android.view.MenuItem
import androidx.appcompat.view.menu.SeslMenuItem
import dev.oneuiproject.oneui.layout.Badge

@JvmName("setMenuItemBadge")
inline fun MenuItem.setBadge(badge: Badge){
    (this as? SeslMenuItem)?.setBadge(badge)
        ?: Log.e(MenuItem::class.java.simpleName,
            "setBadge is invoked on a menu item that is not an implementation of SeslMenuItem")
}

@JvmName("clearMenuItemBadge")
inline fun MenuItem.clearBadge() = setBadge(Badge.NONE)

@JvmName("clearMenuItemBadge")
inline fun SeslMenuItem.clearBadge() = setBadge(Badge.NONE)

@JvmName("setMenuItemBadge")
inline fun SeslMenuItem.setBadge(badge: Badge){
    badgeText = badge.toBadgeText()
}
