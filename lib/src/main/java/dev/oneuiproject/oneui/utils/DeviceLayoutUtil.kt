@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.reflect.content.res.SeslConfigurationReflector
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.utils.internal.getSystemProp


object DeviceLayoutUtil {
    private var sIsDexMode: Boolean? = null
    private var sIsTabBuildOrCategory: Boolean? = null

    inline fun isPortrait(configuration: Configuration) = configuration.orientation == ORIENTATION_PORTRAIT

    inline fun isInMultiWindowModeCompat(context: Context) =  VERSION.SDK_INT >= 24 && (context.activity?.isInMultiWindowMode ?: false)

    @JvmStatic
    inline fun isLandscape(configuration: Configuration) = configuration.orientation == ORIENTATION_LANDSCAPE

    private inline fun isLandscape(context: Context): Boolean {
        return !isFlipCoverScreen(context) && isLandscape(context.resources.configuration)
    }

    fun isLandscapeView(context: Context): Boolean {
        val resources = context.resources
        return !isTabletLayout(resources) && isLandscape(resources.configuration) && !isInMultiWindowModeCompat(context)
    }

    fun isFlipCoverScreen(context: Context): Boolean {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.activity?.display?.displayId?.let { return  it == 1 }
        }
        return false
    }

    inline fun isPhoneLandscape(context: Context): Boolean {
        val resources = context.resources
        return !isTabletLayout(resources) && isLandscape(resources.configuration)
                && !isInMultiWindowModeCompat(context) && !isDeskTopMode(resources)
    }

    inline fun isTabletLayoutOrDesktop(context: Context): Boolean {
        val resources = context.resources
        return isDeskTopMode(resources) || isTabletLayout(resources)
    }

    @SuppressLint("RestrictedApi")
    fun isDeskTopMode(resources: Resources) =  sIsDexMode
        ?: SeslConfigurationReflector.isDexEnabled(resources.configuration).also { sIsDexMode = it }

    inline fun isTabletLayout(resources: Resources) = resources.configuration.smallestScreenWidthDp >= 600

    inline fun isTabletStyle(context: Context) = !isFlipCoverScreen(context) && isTabletLayoutOrDesktop(context)

    fun isDisplayTypeSub(config: Configuration): Boolean =
        getDisplayDeviceType(config) == 5/*Configuration.SEM_DISPLAY_DEVICE_TYPE_SUB*/

    private fun getDisplayDeviceType(config: Configuration): Int? {
        return try {
            val field =   @Suppress("PrivateApi") Configuration::class.java.getDeclaredField("semDisplayDeviceType")
            field.isAccessible = true
            val displayDeviceType = field[config]
            displayDeviceType as? Int?
        } catch (_: Exception) {
            null
        }
    }

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

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    fun getStatusBarHeight(resources: Resources): Int {
        val identifier: Int = resources.getIdentifier(
            "status_bar_height",
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

    fun isPhoneLandscapeOrTablet(context: Context): Boolean {
        val resources = context.resources
        return isTabletLayoutOrDesktop(context) || (isLandscape(resources.configuration) && !isInMultiWindowModeCompat(context))
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

    fun getWidthExcludingSystemInsets(context: Context): Int{
        val activity = context.activity
        activity?.let {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = it.windowManager.currentWindowMetrics
                val windowInsets = metrics.windowInsets
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars()
                            or WindowInsets.Type.displayCutout()
                )
                val insetsWidth = insets.right + insets.left
                return metrics.bounds.width() - insetsWidth
            }
        }

        return Point().apply {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(this) }.x
    }

    fun getWindowHeight(context: Context): Int {
        return Point().apply {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(this) }.y
    }
}