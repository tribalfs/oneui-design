@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.dialog.internal

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.MenuItemCompat
import dev.oneuiproject.oneui.dialog.GridMenuDialog
import dev.oneuiproject.oneui.utils.toBadge

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
inline fun MenuItemImpl.toGridDialogItem(): GridMenuDialog.GridItem{
   return GridMenuDialog.GridItem(
       itemId = this.itemId,
       title = this.title,
       icon = this.icon,
       tooltipText = MenuItemCompat.getTooltipText(this),
       isEnabled = this.isEnabled,
       isVisible = this.isVisible,
       badge = this.badgeText.toBadge()
   )
}