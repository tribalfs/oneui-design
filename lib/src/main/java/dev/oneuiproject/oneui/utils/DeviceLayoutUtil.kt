@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.content.res.SemConfiguration
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.reflect.DeviceInfo
import androidx.reflect.content.res.SeslConfigurationReflector
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.utils.internal.getSystemProp
import dev.rikka.tools.refine.Refine


object DeviceLayoutUtil {
    private var sIsTabBuildOrCategory: Boolean? = null

    inline fun isPortrait(config: Configuration) = config.orientation == ORIENTATION_PORTRAIT

    inline fun isInMultiWindowModeCompat(context: Context) =
        VERSION.SDK_INT >= 24 && (context.activity?.isInMultiWindowMode ?: false)

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
    fun isDeskTopMode(resources: Resources): Boolean{
        if (DeviceInfo.isOneUI()) {
            try {
                if (VERSION.SDK_INT >= 34 &&
                    Refine.unsafeCast<SemConfiguration>(resources.configuration).isNewDexMode
                ) return true
            }catch (_: Throwable){}
            try {
                if (VERSION.SDK_INT >= 31) {
                    return Refine.unsafeCast<SemConfiguration>(resources.configuration).isDexMode
                }
            }catch (_: Throwable){}
            return SeslConfigurationReflector.isDexEnabled(resources.configuration)//private method
        } else {
            return false
        }
    }

    inline fun isTabletLayout(resources: Resources) = resources.configuration.smallestScreenWidthDp >= 600

    inline fun isTabletStyle(context: Context) = !isFlipCoverScreen(context) && isTabletLayoutOrDesktop(context)

    /**
     * Sub-display like on samsung flip phone cover screens
     */
    fun isDisplayTypeSub(config: Configuration): Boolean =
        getDisplayDeviceType(config) == 5/*Configuration.SEM_DISPLAY_DEVICE_TYPE_SUB*/

    private fun getDisplayDeviceType(config: Configuration): Int? {
        return try {
            val field = @Suppress("PrivateApi")
            Configuration::class.java.getDeclaredField("semDisplayDeviceType")
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

    /**
     * On API 30 and above, it retrieves the width of the `activity window` when called with an activity context,
     * excluding insets for navigation bars and display cutout on.
     *
     * On API 29 and below or if called with a non-activity context, it returns the width of the `entire display`,
     * minus the height of system decorations on API 29 and below.
     *
     * @param context The context from which to retrieve the window width.
     * @return The width in pixels, adjusted for insets if applicable.
     */
    fun getWindowWidthNet(context: Context): Int {
        val activity = context.activity
        activity?.let {
            if (VERSION.SDK_INT >= 30) {
                val metrics = it.windowManager.currentWindowMetrics
                val windowInsets = metrics.windowInsets
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
                )
                val insetsWidth = insets.right + insets.left
                return metrics.bounds.width() - insetsWidth
            }
        }
        
        return Point().apply {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(this)
        }.x
    }

    /**
     * Retrieves the height of the `app window' when called with an activity context.
     *
     * If called with a non-activity context, it returns the height of the `entire display`
     * (minus system decoration height on api29-)
     *
     * @param context The context from which to retrieve the window height.
     * @return The height in pixels.
     */
    fun getWindowHeight(context: Context): Int {
        return Point().apply {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(this)
        }.y
    }
}