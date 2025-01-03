@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.core.view.updateLayoutParams
import androidx.reflect.DeviceInfo
import dev.oneuiproject.oneui.ktx.activity
import dev.oneuiproject.oneui.ktx.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.ktx.widthExcludingSystemInsets
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isScreenWidthLarge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletBuildOrIsDeskTopMode
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.SideMarginParams

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object ToolbarLayoutUtils {

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated("Use Activity.updateStatusBarVisibility() instead",
        ReplaceWith("activity.updateStatusBarVisibility()"))
    fun hideStatusBarForLandscape(activity: Activity, orientation: Int) =
        activity.updateStatusBarVisibility()

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun Activity.updateStatusBarVisibility() {
        if (isTabletBuildOrIsDeskTopMode(this)) return

        val lp = window.attributes
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isInMultiWindowModeCompat || isScreenWidthLarge(this)) {
                lp.flags = lp.flags and -(WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            } else {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            }
            if (DeviceInfo.isOneUI()) {
                ReflectUtils.genericInvokeMethod(
                    WindowManager.LayoutParams::class.java,
                    lp,
                    "semAddExtensionFlags",
                    1 /* WindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT */
                )
            }
        } else {
            lp.flags = lp.flags and -(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

            if (DeviceInfo.isOneUI()) {
                ReflectUtils.genericInvokeMethod(
                    WindowManager.LayoutParams::class.java,
                    lp,
                    "semClearExtensionFlags",
                    1 /* WindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT */
                )
            }
        }
        window.attributes = lp
    }

    /**
     * @hide
     */
    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated("Use View.updateAdaptiveSideMargins() instead",
        ReplaceWith("View.updateAdaptiveSideMargins()"))
    inline fun updateListBothSideMargin(activity: Activity, layout: ViewGroup) {
        layout.updateAdaptiveSideMargins(activity)
    }

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmOverloads
    inline fun View.updateAdaptiveSideMargins(activity: Activity? = null) {
        (activity ?: context.activity)?.apply {
            findViewById<View>(android.R.id.content).post {
                val sideMarginParams = getAdaptiveSideMarginParams()
                setSideMarginParams(sideMarginParams, 0, 0)
                requestLayout()
            }
        }
    }


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun Context.getAdaptiveSideMarginParams(minSideMargin: Int = 0): SideMarginParams {
        val config = resources.configuration
        val screenWidthDp = config.screenWidthDp
        val screenHeightDp = config.screenHeightDp
        val widthXInsets = widthExcludingSystemInsets
        val marginRatio = when {
            (screenWidthDp < 589) -> 0.0f
            (screenHeightDp > 411 && screenWidthDp <= 959) -> 0.05f
            (screenWidthDp >= 960 && screenHeightDp <= 1919) -> 0.125f
            (screenWidthDp >= 1920) -> 0.25f
            else -> 0.0f
        }
        return SideMarginParams(
            maxOf((widthXInsets * marginRatio).toInt(), minSideMargin),
            screenWidthDp >= 589
        )
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun View.setSideMarginParams(
        smp: SideMarginParams,
        additionalLeft: Int,
        additionalRight: Int
    ) {
        (layoutParams as? MarginLayoutParams)?.let {
            updateLayoutParams<MarginLayoutParams> {
                if (smp.matchParent) width = ViewGroup.LayoutParams.MATCH_PARENT
                leftMargin = smp.sideMargin + additionalLeft
                rightMargin = smp.sideMargin + additionalRight
            }
        } ?: run {
            setPadding(
                smp.sideMargin + additionalLeft,
                paddingTop,
                smp.sideMargin + additionalRight,
                paddingBottom
            )
        }
    }

}
