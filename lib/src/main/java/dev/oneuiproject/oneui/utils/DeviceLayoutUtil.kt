@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.reflect.content.res.SeslConfigurationReflector
import dev.oneuiproject.oneui.utils.internal.getSystemProp


object DeviceLayoutUtil {
    private var sIsDexMode: Boolean? = null
    private var sIsTabBuildOrCategory: Boolean? = null

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

    @JvmStatic
    fun isTabletCategoryOrBuild(context: Context): Boolean {
        if (sIsTabBuildOrCategory == null) {
            sIsTabBuildOrCategory = if (context.packageManager?.hasSystemFeature("com.samsung.feature.device_category_tablet") == true) {
                true
            } else {
                getSystemProp("ro.build.characteristics") == "tablet"
            }
        }
        return sIsTabBuildOrCategory!!
    }

    @JvmStatic
    inline fun isTabletBuildOrIsDeskTopMode(context: Context): Boolean {
        return isTabletCategoryOrBuild(context) || isDeskTopMode(context.resources)
    }

    @JvmStatic
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isScreenWidthLarge(
        context: Context,
        thresholdWidthPx: Int = 420
    ): Boolean {
        val smallestScreenWidthDp = context.resources
            .configuration.smallestScreenWidthDp
        val scaledScreenWidthPx =
            (smallestScreenWidthDp * getDensityScaleFactor(context)).toInt()
        return scaledScreenWidthPx > thresholdWidthPx
    }

    private fun getDensityScaleFactor(context: Context): Float {
        val metrics = context.resources.displayMetrics
        val config = context.resources.configuration
        return config.densityDpi / metrics.densityDpi.toFloat()
    }
}