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
import dev.oneuiproject.oneui.ktx.windowWidthNetOfInsets
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isScreenWidthLarge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletBuildOrIsDeskTopMode
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.SideMarginParams

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


}
