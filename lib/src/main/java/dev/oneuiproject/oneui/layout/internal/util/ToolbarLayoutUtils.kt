@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.layout.internal.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets.Type
import android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.view.isVisible
import androidx.reflect.DeviceInfo
import dev.oneuiproject.oneui.ktx.findAncestorOfType
import dev.oneuiproject.oneui.ktx.isDescendantOf
import dev.oneuiproject.oneui.ktx.isInMultiWindowModeCompat
import dev.oneuiproject.oneui.ktx.semAddExtensionFlags
import dev.oneuiproject.oneui.ktx.semClearExtensionFlags
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isScreenWidthLarge
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isTabletBuildOrIsDeskTopMode
import android.view.SemWindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT as FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ToolbarLayoutUtils {

    inline fun Activity.updateStatusBarVisibility(landscapeHeightForStatusBar: Int) {
        if (isTabletBuildOrIsDeskTopMode(this)) return
        if (Build.VERSION.SDK_INT >= 30){
            updateStatusBarVisibilityApi30(landscapeHeightForStatusBar)
        }else {
            updateStatusBarVisibilityApi21(landscapeHeightForStatusBar)
        }
    }

    @RequiresApi(30)
    private fun Activity.updateStatusBarVisibilityApi30(landscapeHeightForStatusBar: Int) {
        val orientation = resources.configuration.orientation
        if (orientation == ORIENTATION_LANDSCAPE) {
            if (isInMultiWindowModeCompat || isScreenWidthLarge(this, landscapeHeightForStatusBar)) {
                window.insetsController?.show(Type.statusBars())
                window.clearFlags(FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            } else {
                window.insetsController?.apply {
                    hide(Type.statusBars())
                    systemBarsBehavior =  BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            window.semAddExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        } else {
            window.insetsController?.show(Type.statusBars())
            window.clearFlags(FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            window.semClearExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        }
    }

    @Suppress("DEPRECATION")
    private fun Activity.updateStatusBarVisibilityApi21(statusBarVisibilityThreshold: Int) {
        val orientation = resources.configuration.orientation
        if (orientation == ORIENTATION_LANDSCAPE) {
            if (isInMultiWindowModeCompat || isScreenWidthLarge(this, statusBarVisibilityThreshold)) {
                window.clearFlags(FLAG_FULLSCREEN or FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            } else {
                window.addFlags(FLAG_FULLSCREEN)
            }
            window.semAddExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        } else {
            window.clearFlags(FLAG_FULLSCREEN or FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            window.semClearExtensionFlags(FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT)
        }
    }

    inline fun View.setVisibilityNoAnimation(visibility: Int){
        (parent as? ViewGroup)?.layoutTransition?.let {
            it.setAnimateParentHierarchy(false)
            this.visibility = visibility
            it.setAnimateParentHierarchy(true)
        } ?: run {this.visibility = visibility}
    }

    @JvmOverloads
    inline fun View.setVisibility(visibility: Int, applyTransition: Boolean = false, delay: Long = 0){
        if (delay > 0) {
            if (!applyTransition) {
                postDelayed({ setVisibilityNoAnimation(visibility)}, delay)
            }else{
                postDelayed({ this.visibility = visibility }, delay)
            }
        }else{
            if (!applyTransition) {
                setVisibilityNoAnimation(visibility)
            }else{
                this.visibility = visibility
            }
        }
    }

    inline fun ViewGroup.hasShowingChild(): Boolean {
        if (!isVisible) return false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isVisible) return true
        }
        return false
    }

    internal data class InternalLayoutInfo(
        val isInsideTBLMainContainer: Boolean,
        val tblParent: ToolbarLayout? = null
    )

    internal fun View.getLayoutLocationInfo(): InternalLayoutInfo{
        return findAncestorOfType<ToolbarLayout>()?.let {
            InternalLayoutInfo(this.isDescendantOf(it.mainContainer), it)
        } ?: InternalLayoutInfo(false)
    }

    /**
     * **TODO: Needs further checking on different devices**
     *
     * Checks if the gesture hint is set to transparent by NavStar, a Good lock module.
     * When true, nav bar doesn't scroll out during immersive scrolling.
     */
    internal fun Context.navBarCanImmScroll(): Boolean{
        if (!DeviceInfo.isOneUI()) return true
        val navStarFlags = Settings.Global.getInt(contentResolver, NAV_STAR_FLAG_SETTINGS, 0)
        return navStarFlags and FLAG_TRANSPARENT_HINT_STATUS == 0
    }

    private const val FLAG_TRANSPARENT_HINT_STATUS = 0x2
    private const val NAV_STAR_FLAG_SETTINGS = "navigationbar_splugin_flags"

}
