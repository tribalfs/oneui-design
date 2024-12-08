package dev.oneuiproject.oneui.layout.internal.util

import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.ColorInt
import dev.oneuiproject.oneui.layout.Badge

interface NavButtonsHandler {
    var showNavigationButtonAsBack: Boolean
    var showNavigationButton: Boolean
    fun setNavigationButtonOnClickListener(listener: OnClickListener?)
    fun setNavigationButtonTooltip(tooltipText: CharSequence?)
    fun setNavigationButtonBadge(badge: Badge)
    fun setNavigationButtonIcon(icon: Drawable?)
    fun setHeaderButtonIcon(icon: Drawable?, @ColorInt tint: Int? = null)
    fun setHeaderButtonTooltip(tooltipText: CharSequence?)
    fun setHeaderButtonOnClickListener(listener: View.OnClickListener?)
    fun setHeaderButtonBadge(badge: Badge)
}
