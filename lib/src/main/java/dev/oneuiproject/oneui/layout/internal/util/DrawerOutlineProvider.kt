package dev.oneuiproject.oneui.layout.internal.util

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.drawerlayout.widget.DrawerLayout.LAYOUT_DIRECTION_RTL

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DrawerOutlineProvider(@param:Px var cornerRadius: Int) :
        ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            val isRTL = view.layoutDirection == LAYOUT_DIRECTION_RTL
            val width = view.width
            val height = view.height
            // Ensure width and height are positive
            if (width > 0 && height > 0) {
                outline.setRoundRect(
                    if (isRTL) 0 else -cornerRadius,
                    0,
                    if (isRTL) view.width + cornerRadius else view.width,
                    view.height,
                    cornerRadius.toFloat()
                )
            } else {
                outline.setEmpty()
            }
        }
    }