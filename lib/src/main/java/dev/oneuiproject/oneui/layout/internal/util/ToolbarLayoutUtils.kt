@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RestrictTo
import dev.oneuiproject.oneui.ktx.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.ktx.semAddExtensionFlags
import dev.oneuiproject.oneui.ktx.semClearExtensionFlags
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isScreenWidthLarge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletBuildOrIsDeskTopMode
import android.view.SemWindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT as FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT

internal object ToolbarLayoutUtils {

    @JvmStatic
    @Deprecated(
        "Replace CoordinatorLayout with AdaptiveCoordinatorLayout that " +
                "extends CoordinatorLayout and implements this behavior instead.")
    fun hideStatusBarForLandscape(activity: Activity, orientation: Int) =
        activity.updateStatusBarVisibility()

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
            window.attributes = lp
            window.semAddExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        } else {
            lp.flags = lp.flags and -(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            window.attributes = lp
            window.semClearExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        }
    }


}
