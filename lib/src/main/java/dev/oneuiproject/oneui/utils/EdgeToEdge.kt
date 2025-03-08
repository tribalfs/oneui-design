@file:JvmName("EdgeToEdge")
package dev.oneuiproject.oneui.utils

import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.content.res.use
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.oneuiproject.oneui.ktx.isLightMode

private var Impl: EdgeToEdgeImpl? = null

@JvmName("apply")
fun ComponentActivity.applyEdgeToEdge() {
    val view = window.decorView
    val isLightMode = isLightMode()
    @Suppress("InlinedApi")
    obtainStyledAttributes(intArrayOf(android.R.attr.windowLightStatusBar, android.R.attr.windowLightNavigationBar)).use {a ->
        val statusBarIsDark = !a.getBoolean(0, isLightMode)
        @Suppress("ResourceType")
        val navigationBarIsDark = !a.getBoolean(1, isLightMode)

        val impl = Impl ?: if (SDK_INT in 29..34) {
            EdgeToEdgeApi29()
        } else if (SDK_INT >= 26) {
            EdgeToEdgeApi26()
        } else if (SDK_INT >= 23) {
            EdgeToEdgeApi23()
        } else {
            EdgeToEdgeApi21()
        }.also { Impl = it }
        impl.setUp(window, view, statusBarIsDark, navigationBarIsDark)
    }
}

private interface EdgeToEdgeImpl {
    fun setUp(
        window:Window,
        view:View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    )
}


private class EdgeToEdgeApi21 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

@RequiresApi(23)
private class EdgeToEdgeApi23 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (navigationBarIsDark) Color.TRANSPARENT else Color.parseColor("#20000000")
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !statusBarIsDark
    }
}

@RequiresApi(26)
private class EdgeToEdgeApi26 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }

    }
}

@RequiresApi(29)
private class EdgeToEdgeApi29 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}

