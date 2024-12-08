package dev.oneuiproject.oneui.layout.internal.util

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.Px
import androidx.drawerlayout.widget.DrawerLayout.LAYOUT_DIRECTION_RTL

internal class DrawerOutlineProvider(@param:Px var cornerRadius: Int) :
        ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            val isRTL = view.layoutDirection == LAYOUT_DIRECTION_RTL
            outline.setRoundRect(
                if (isRTL) 0 else -cornerRadius,
                0,
                if (isRTL) view.width + cornerRadius else view.width, view.height,
                cornerRadius.toFloat()
            )
        }
    }