package dev.oneuiproject.oneui.layout.internal.util

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.DrawerLayout.DrawerState
import dev.oneuiproject.oneui.layout.internal.delegate.DrawerLayoutBackHandler

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface DrawerLayoutInterface{
    fun setHandleInsets(handle: Boolean)
    fun open(animate: Boolean = true)
    fun close(animate: Boolean = true)
    fun setDrawerCornerRadius(@Dimension dp: Float)
    fun setDrawerCornerRadius(@Px px: Int)
    fun setCustomHeader(headerView: View, params: ViewGroup.LayoutParams)
    fun addDrawerContent(child: View, params: ViewGroup.LayoutParams)
    fun setOnDrawerStateChangedListener(listener: ((DrawerState)-> Unit)?)
    fun getDrawerPane(): View
    fun getContentPane(): View
    var isLocked: Boolean
    val isDrawerOpen: Boolean
    val isDrawerOpenOrIsOpening: Boolean
    fun getDrawerSlideOffset(): Float
    fun getOrCreateBackHandler(drawerLayout: DrawerLayout) : DrawerLayoutBackHandler<DrawerLayout>
}