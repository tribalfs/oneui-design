@file:JvmName("EdgeToEdge")
package dev.oneuiproject.oneui.utils

import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.content.withStyledAttributes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.oneuiproject.oneui.ktx.isLightMode
import androidx.core.graphics.toColorInt

typealias SystemBarsDarkMode = Pair<Boolean, Boolean>

private var Impl: EdgeToEdgeImpl? = null

fun darkSystemBars(): SystemBarsDarkMode = true to true
fun lightSystemBars(): SystemBarsDarkMode = false to false
fun Context.defaultSystemBarsDarkMode(): () -> Pair<Boolean, Boolean> = {
    val isLightMode = isLightMode()
    var statusBarIsDark = isLightMode
    var navigationBarIsDark = isLightMode
    withStyledAttributes(attrs = intArrayOf(android.R.attr.windowLightStatusBar, android.R.attr.windowLightNavigationBar)) {
        statusBarIsDark = !getBoolean(0, isLightMode)
        @Suppress("ResourceType")
        navigationBarIsDark = !getBoolean(1, isLightMode)
    }
    statusBarIsDark to navigationBarIsDark
}

@JvmName("apply")
fun ComponentActivity.applyEdgeToEdge(getSystemBarsDarkMode: () -> SystemBarsDarkMode = defaultSystemBarsDarkMode()) {
    val (statusBarIsDark, navigationBarIsDark) = getSystemBarsDarkMode()
    val impl = Impl ?: if (SDK_INT >= 30) {
        EdgeToEdgeApi30()
    } else if (SDK_INT >= 29) {
        EdgeToEdgeApi29()
    } else if (SDK_INT >= 26) {
        EdgeToEdgeApi26()
    } else if (SDK_INT >= 23) {
        EdgeToEdgeApi23()
    } else {
        EdgeToEdgeApi21()
    }.also { Impl = it }
    impl.setUp(window, window.decorView, statusBarIsDark, navigationBarIsDark)
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
        window.navigationBarColor = if (navigationBarIsDark) Color.TRANSPARENT else "#20000000".toColorInt()
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
private open class EdgeToEdgeApi29 : EdgeToEdgeImpl {

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

@RequiresApi(30)
private class EdgeToEdgeApi30 : EdgeToEdgeApi29() {

    override fun setUp(
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        super.setUp(window, view, statusBarIsDark, navigationBarIsDark)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

