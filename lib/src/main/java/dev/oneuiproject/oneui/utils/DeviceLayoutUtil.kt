@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import androidx.reflect.content.res.SeslConfigurationReflector


object DeviceLayoutUtil {
    private var sIsDexMode: Boolean? = null

    inline fun isPortrait(configuration: Configuration) = configuration.orientation == ORIENTATION_PORTRAIT

    inline fun isTabletLayoutOrDesktop(context: Context): Boolean {
        val resources = context.resources
        return isDeskTopMode(resources) || isTabletLayout(resources)
    }

    @SuppressLint("RestrictedApi")
    fun isDeskTopMode(resources: Resources) =  sIsDexMode
        ?: SeslConfigurationReflector.isDexEnabled(resources.configuration).also { sIsDexMode = it }

    inline fun isTabletLayout(resources: Resources) = resources.configuration.smallestScreenWidthDp >= 600

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    fun getNavigationBarHeight(resources: Resources): Int {
        val identifier: Int = resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android"
        )
        if (identifier > 0){
            return resources.getDimensionPixelSize(identifier)
        }
        return 0
    }
}